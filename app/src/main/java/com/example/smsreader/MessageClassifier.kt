package com.example.smsreader

import kotlin.math.sqrt

object MessageClassifier {

    private val blockKw = listOf("回复", "拒收", "贷款", "保险")

    // 银行
    private val bankNames = listOf(
        "邮政储蓄银行", "工商银行", "建设银行", "招商银行", "农业银行",
        "中国银行", "交通银行", "邮储银行", "民生银行", "兴业银行",
        "浦发银行", "中信银行", "光大银行", "华夏银行", "广发银行",
        "平安银行", "北京银行", "上海银行", "南京银行", "宁波银行",
        "江苏银行", "杭州银行", "浙商银行", "渤海银行", "徽商银行",
        "重庆银行", "成都银行", "长沙银行", "郑州银行", "苏州银行"
    )
    private val amtRegex = Regex("""\d+(?:\.\d{1,2})?\s*元""")

    // 快递
    private val companyList = listOf(
        "顺丰", "申通", "圆通", "中通", "韵达", "邮政", "EMS",
        "百世", "极兔", "菜鸟", "德邦", "京东物流", "丹鸟", "宅急送", "优速"
    )
    private val pickupRegex = Regex("""取件码[：:\s]*(\S+)""")
    private val dateRegex = Regex("""(\d{1,2})月(\d{1,2})[日号]""")

    // 分类原型
    private val prototypes = mapOf(
        "工资" to listOf("您的工资已到账", "工资收入", "薪资发放到账", "您的账户收入工资"),
        "转账" to listOf("转账汇款到账", "向他行转账支出", "跨行转账转出", "转账收入到账"),
        "伙食" to listOf("餐饮消费支出", "外卖订单支付", "餐厅消费扣款", "食堂刷卡消费"),
        "其他" to listOf("网上消费支付", "快捷支付扣款", "代扣缴费支出", "银联消费支出"),
    )
    private val incomeKw = listOf("收入", "存入", "到账", "汇入", "转入", "工资", "退款", "报销")
    private val expenseKw = listOf("消费", "支付", "扣款", "支出", "缴费", "取款", "转出", "代扣")

    // 提取结果
    data class BillInfo(val bankName: String, val amount: Double, val rawText: String)
    data class PackageInfo(val company: String, val pickupCode: String, val rawText: String)
    data class ClassifyResult(val category: String, val direction: String)

    fun isBlocked(title: String, body: String): Boolean =
        blockKw.any { title.contains(it) || body.contains(it) }

    fun extractBill(title: String, body: String): BillInfo? {
        val text = "$title $body"
        val bank = extractBank(text) ?: return null
        val amt = extractAmount(text) ?: return null
        return BillInfo(bank, amt, text)
    }

    fun extractEventDate(title: String, body: String): Long? {
        val text = "$title $body"
        val match = dateRegex.find(text) ?: return null
        val month = match.groupValues[1].toIntOrNull() ?: return null
        val day = match.groupValues[2].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        val cal = java.util.Calendar.getInstance()
        cal.set(cal.get(java.util.Calendar.YEAR), month - 1, day, 12, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun extractPackage(title: String, body: String): PackageInfo? {
        val text = "$title $body"
        val company = companyList.find { text.contains(it) } ?: return null
        val code = pickupRegex.find(text)?.groupValues?.getOrNull(1) ?: ""
        return PackageInfo(company, code, text)
    }

    // ═══════════════════════════════════
    // 嵌入向量分类
    // ═══════════════════════════════════

    /** 对账单文本做嵌入分类，返回 (category, direction) */
    fun classifyBill(embed: (String) -> FloatArray, rawText: String, amount: Double): ClassifyResult {
        // 规则优先：房租固定750
        if (amount == 750.0) return ClassifyResult("房租", "支出")

        val vec = embed(rawText)
        val category = classifyByPrototype(vec)

        // 规则：工资必须 >3000 的收入
        if (category == "工资") {
            return if (amount > 3000) ClassifyResult("工资", "收入")
            else ClassifyResult("其他", "收入")
        }

        // 方向：关键词 + 类别默认
        val direction = classifyDirection(rawText, category)
        return ClassifyResult(category, direction)
    }

    private var protoVecs: Map<String, List<FloatArray>>? = null

    private fun classifyByPrototype(vec: FloatArray): String {
        val pvs = protoVecs ?: return "其他"
        var best = "其他"
        var bestSim = -1f
        for ((cat, vecs) in pvs) {
            for (pv in vecs) {
                val sim = cosineSim(vec, pv)
                if (sim > bestSim) { bestSim = sim; best = cat }
            }
        }
        return best
    }

    private fun classifyDirection(text: String, category: String): String {
        val inc = incomeKw.count { text.contains(it) }
        val exp = expenseKw.count { text.contains(it) }
        if (inc > exp) return "收入"
        if (exp > inc) return "支出"
        return when (category) {
            "工资" -> "收入"
            "转账" -> "支出"
            else -> "支出"
        }
    }

    /** 预计算原型向量，由 Listener 在 embedder 就绪后调用一次 */
    fun initPrototypes(embed: (String) -> FloatArray) {
        protoVecs = prototypes.mapValues { (_, texts) -> texts.map { embed(it) } }
    }

    private fun cosineSim(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var na = 0f; var nb = 0f
        for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        return if (na == 0f || nb == 0f) 0f else dot / (sqrt(na) * sqrt(nb))
    }

    /** 纯关键词分类（无需 BERT 模型），用于降级或主动读取场景 */
    fun classifyBillByKeywords(rawText: String, amount: Double): ClassifyResult {
        if (amount == 750.0) return ClassifyResult("房租", "支出")
        val direction = classifyDirection(rawText, "其他")
        return if (amount > 3000 && direction == "收入") ClassifyResult("工资", "收入")
        else ClassifyResult("其他", direction)
    }

    // ═══════════════════════════════════
    // 内部提取
    // ═══════════════════════════════════

    private fun extractBank(text: String): String? {
        Regex("""[【\[](.+?银行)[】\]]""").find(text)?.let { return it.groupValues[1] }
        bankNames.find { text.contains(it) }?.let { return it }
        Regex("""(\S{2,6}(?:银行|支行|信用社))""").find(text)?.let { m ->
            val name = m.groupValues[1]
            if (name !in listOf("网上银行", "手机银行", "电话银行", "网上支行")) return name
        }
        return null
    }

    private fun extractAmount(text: String): Double? {
        val match = amtRegex.find(text) ?: return null
        return match.value.replace("元", "").trim().toDoubleOrNull()
    }
}
