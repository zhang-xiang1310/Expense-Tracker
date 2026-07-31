package com.example.smsreader

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// ═══════════════════════════════════
// 数据类
// ═══════════════════════════════════

data class Sms(
    val id: Long,
    val body: String,
    val date: Long
)

data class Bill(
    val id: Long,
    val amount: Double,
    val bankName: String,
    val category: String,
    val date: Long,
    val type: String = ""
)

data class Package(
    val id: Long,
    val company: String,
    val address: String,
    val date: Long,
    val pickupCode: String = "",
    val status: String = "",
    val description: String = ""
)

data class Event(
    val id: Long,
    val date: Long,
    val body: String
)

data class BankCard(
    val id: Long = 0,
    val bank: String,
    val number: String,
    val balance: Double
)

data class MonthlyBill(
    val id: Long = 0,
    val year: Int,
    val month: Int,
    val income: Double,
    val expense: Double,
    val categoryJson: String = ""
)

data class AnnualBill(
    val id: Long = 0,
    val year: Int,
    val income: Double,
    val expense: Double,
    val categoryJson: String = ""
)

// ═══════════════════════════════════
// 数据库
// ═══════════════════════════════════

class SmsDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "smsreader.db"
        const val DB_VERSION = 10
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE sms (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                body TEXT,
                date INTEGER
            )
        """)
        db.execSQL("""
            CREATE TABLE bill (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount REAL,
                bank_name TEXT,
                category TEXT,
                date INTEGER,
                type TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE package_delivery (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                company TEXT,
                address TEXT,
                date INTEGER,
                pickup_code TEXT,
                status TEXT,
                description TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE event (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                date INTEGER,
                body TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE bank_card (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                bank TEXT,
                number TEXT,
                balance REAL
            )
        """)
        db.execSQL("""
            CREATE TABLE monthly_bill (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                month INTEGER,
                income REAL,
                expense REAL,
                category_json TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE annual_bill (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                year INTEGER,
                income REAL,
                expense REAL,
                category_json TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE category_kw (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT,
                keyword TEXT
            )
        """)
        seedDefaultKeywords(db)
    }

    private fun seedDefaultKeywords(db: SQLiteDatabase) {
        val defaults = mapOf(
            "工资" to listOf("您的工资已到账", "工资收入", "薪资发放到账", "您的账户收入工资"),
            "转账" to listOf("转账汇款到账", "向他行转账支出", "跨行转账转出", "转账收入到账"),
            "伙食" to listOf("餐饮消费支出", "外卖订单支付", "餐厅消费扣款", "食堂刷卡消费"),
            "网费" to listOf("宽带包月扣款", "宽带费用扣除", "网费缴费成功", "宽带月租扣费", "网费充值到账", "话费充值", "话费缴纳", "手机缴费", "话费扣款"),
            "其他" to listOf("网上消费支付", "快捷支付扣款", "代扣缴费支出", "银联消费支出"),
        )
        defaults.forEach { (cat, kws) ->
            kws.forEach { kw ->
                val v = ContentValues().apply { put("category", cat); put("keyword", kw) }
                db.insert("category_kw", null, v)
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS sms")
        db.execSQL("DROP TABLE IF EXISTS bill")
        db.execSQL("DROP TABLE IF EXISTS package_delivery")
        db.execSQL("DROP TABLE IF EXISTS event")
        db.execSQL("DROP TABLE IF EXISTS bank_card")
        onCreate(db)
    }

    // ═══════════════════ sms ═══════════════════

    fun insertSms(body: String, date: Long) {
        val v = ContentValues().apply {
            put("body", body)
            put("date", date)
        }
        writableDatabase.insert("sms", null, v)
    }

    fun getLatestSmsDate(): Long {
        readableDatabase.query("sms", arrayOf("MAX(date)"), null, null, null, null, null)
            .use { c -> if (c.moveToFirst()) return c.getLong(0) }
        return 0L
    }

    fun smsExists(body: String, date: Long): Boolean {
        readableDatabase.query("sms", null, "body=? AND date=?", arrayOf(body, date.toString()), null, null, null)
            .use { return it.count > 0 }
    }

    fun queryAllSms(): List<Sms> {
        val list = mutableListOf<Sms>()
        readableDatabase.query("sms", null, null, null, null, null, "date DESC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(Sms(
                        id = cursor.getLong(0),
                        body = cursor.getString(1) ?: "",
                        date = cursor.getLong(2)
                    ))
                }
            }
        return list
    }

    // ═══════════════════ bill ═══════════════════

    fun insertBill(amount: Double, bankName: String, category: String, date: Long, type: String = "") {
        val v = ContentValues().apply {
            put("amount", amount)
            put("bank_name", bankName)
            put("category", category)
            put("date", date)
            put("type", type)
        }
        writableDatabase.insert("bill", null, v)
        // 账单增减对应银行卡余额（type 才是收支方向）
        if (bankName.isNotEmpty()) {
            val exists = queryAllBankCards().any { it.bank == bankName }
            if (!exists) {
                insertBankCard(bankName, "", 0.0)
            }
            if (type == "收入") addBankBalance(bankName, amount)
            else deductBankBalance(bankName, amount)
        }
        // 同步累加到月账单和年账单
        upsertMonthlyBill(date, amount, category)
        upsertAnnualBill(date, amount, category)
    }

    private fun deductBankBalance(bankName: String, amount: Double) {
        writableDatabase.execSQL("UPDATE bank_card SET balance = balance - ? WHERE bank = ?", arrayOf(amount, bankName))
    }

    private fun addBankBalance(bankName: String, amount: Double) {
        writableDatabase.execSQL("UPDATE bank_card SET balance = balance + ? WHERE bank = ?", arrayOf(amount, bankName))
    }


    fun queryAllBills(): List<Bill> {
        val list = mutableListOf<Bill>()
        readableDatabase.query("bill", null, null, null, null, null, "date DESC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(Bill(
                        id = cursor.getLong(0),
                        amount = cursor.getDouble(1),
                        bankName = cursor.getString(2) ?: "",
                        category = cursor.getString(3) ?: "",
                        date = cursor.getLong(4),
                        type = cursor.getString(5) ?: ""
                    ))
                }
            }
        return list
    }

    fun queryBillsBetween(start: Long, end: Long): List<Bill> {
        val list = mutableListOf<Bill>()
        readableDatabase.query("bill", null, "date BETWEEN ? AND ?",
            arrayOf(start.toString(), end.toString()), null, null, "date DESC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(Bill(
                        id = cursor.getLong(0),
                        amount = cursor.getDouble(1),
                        bankName = cursor.getString(2) ?: "",
                        category = cursor.getString(3) ?: "",
                        date = cursor.getLong(4),
                        type = cursor.getString(5) ?: ""
                    ))
                }
            }
        return list
    }

    fun sumBillsBetween(start: Long, end: Long): Pair<Double, Double> {
        var income = 0.0
        var expense = 0.0
        queryBillsBetween(start, end).forEach { b ->
            if (b.category == "收入") income += b.amount else expense += b.amount
        }
        return Pair(income, expense)
    }

    fun deleteBillsBefore(time: Long) {
        writableDatabase.delete("bill", "date < ?", arrayOf(time.toString()))
    }

    // ═══════════════════ package_delivery ═══════════════════

    fun insertPackage(company: String, address: String, date: Long, pickupCode: String = "", status: String = "运输中", description: String = "") {
        val v = ContentValues().apply {
            put("company", company)
            put("address", address)
            put("date", date)
            put("pickup_code", pickupCode)
            put("status", status)
            put("description", description)
        }
        writableDatabase.insert("package_delivery", null, v)
    }

    fun queryAllPackages(): List<Package> {
        val list = mutableListOf<Package>()
        readableDatabase.query("package_delivery", null, null, null, null, null, "date DESC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(Package(
                        id = cursor.getLong(0),
                        company = cursor.getString(1) ?: "",
                        address = cursor.getString(2) ?: "",
                        date = cursor.getLong(3),
                        pickupCode = cursor.getString(4) ?: "",
                        status = cursor.getString(5) ?: "",
                        description = cursor.getString(6) ?: ""
                    ))
                }
            }
        return list
    }

    // ═══════════════════ event ═══════════════════

    fun insertEvent(date: Long, body: String) {
        val v = ContentValues().apply {
            put("date", date)
            put("body", body)
        }
        writableDatabase.insert("event", null, v)
    }

    fun queryAllEvents(): List<Event> {
        val list = mutableListOf<Event>()
        readableDatabase.query("event", null, null, null, null, null, "_id DESC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(Event(
                        id = cursor.getLong(0),
                        date = cursor.getLong(1),
                        body = cursor.getString(2) ?: ""
                    ))
                }
            }
        return list
    }

    fun deletePackage(id: Long) {
        writableDatabase.delete("package_delivery", "_id = ?", arrayOf(id.toString()))
    }

    fun updateEvent(id: Long, date: Long, body: String) {
        val v = ContentValues().apply { put("date", date); put("body", body) }
        writableDatabase.update("event", v, "_id = ?", arrayOf(id.toString()))
    }

    fun deleteEvent(id: Long) {
        writableDatabase.delete("event", "_id = ?", arrayOf(id.toString()))
    }

    // ═══════════════════ bill ═══════════════════

    fun updateBill(id: Long, amount: Double, bankName: String, category: String, date: Long, type: String) {
        val v = ContentValues().apply {
            put("amount", amount)
            put("bank_name", bankName)
            put("category", category)
            put("date", date)
            put("type", type)
        }
        writableDatabase.update("bill", v, "_id = ?", arrayOf(id.toString()))
    }

    // ═══════════════════ bank_card ═══════════════════

    fun insertBankCard(bank: String, number: String, balance: Double): Long {
        val v = ContentValues().apply {
            put("bank", bank)
            put("number", number)
            put("balance", balance)
        }
        return writableDatabase.insert("bank_card", null, v)
    }

    fun queryAllBankCards(): List<BankCard> {
        val list = mutableListOf<BankCard>()
        readableDatabase.query("bank_card", null, null, null, null, null, "_id ASC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(BankCard(
                        id = cursor.getLong(0),
                        bank = cursor.getString(1) ?: "",
                        number = cursor.getString(2) ?: "",
                        balance = cursor.getDouble(3)
                    ))
                }
            }
        return list
    }

    fun deleteBankCard(id: Long) {
        writableDatabase.delete("bank_card", "_id = ?", arrayOf(id.toString()))
    }

    fun updateBankCardBalance(id: Long, balance: Double) {
        val v = ContentValues().apply { put("balance", balance) }
        writableDatabase.update("bank_card", v, "_id = ?", arrayOf(id.toString()))
    }

    // ═══════════════════ monthly_bill ═══════════════════

    private fun upsertMonthlyBill(billDate: Long, amount: Double, category: String) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = billDate }
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val isIncome = category == "收入"

        val existing = readableDatabase.query("monthly_bill", null, "year=? AND month=?",
            arrayOf(year.toString(), month.toString()), null, null, null)
        if (existing.moveToFirst()) {
            val id = existing.getLong(0)
            val oldIncome = existing.getDouble(3)
            val oldExpense = existing.getDouble(4)
            val v = ContentValues().apply {
                put("income", oldIncome + if (isIncome) amount else 0.0)
                put("expense", oldExpense + if (isIncome) 0.0 else amount)
            }
            writableDatabase.update("monthly_bill", v, "_id=?", arrayOf(id.toString()))
        } else {
            val v = ContentValues().apply {
                put("year", year); put("month", month)
                put("income", if (isIncome) amount else 0.0)
                put("expense", if (isIncome) 0.0 else amount)
                put("category_json", "")
            }
            writableDatabase.insert("monthly_bill", null, v)
        }
        existing.close()
    }

    private fun upsertAnnualBill(billDate: Long, amount: Double, category: String) {
        val year = java.util.Calendar.getInstance().apply { timeInMillis = billDate }
            .get(java.util.Calendar.YEAR)
        val isIncome = category == "收入"

        val existing = readableDatabase.query("annual_bill", null, "year=?",
            arrayOf(year.toString()), null, null, null)
        if (existing.moveToFirst()) {
            val id = existing.getLong(0)
            val oldIncome = existing.getDouble(2)
            val oldExpense = existing.getDouble(3)
            val v = ContentValues().apply {
                put("income", oldIncome + if (isIncome) amount else 0.0)
                put("expense", oldExpense + if (isIncome) 0.0 else amount)
            }
            writableDatabase.update("annual_bill", v, "_id=?", arrayOf(id.toString()))
        } else {
            val v = ContentValues().apply {
                put("year", year)
                put("income", if (isIncome) amount else 0.0)
                put("expense", if (isIncome) 0.0 else amount)
                put("category_json", "")
            }
            writableDatabase.insert("annual_bill", null, v)
        }
        existing.close()
    }

    fun queryMonthlyBills(): List<MonthlyBill> {
        val list = mutableListOf<MonthlyBill>()
        readableDatabase.query("monthly_bill", null, null, null, null, null, "year DESC, month DESC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(MonthlyBill(
                        id = cursor.getLong(0),
                        year = cursor.getInt(1),
                        month = cursor.getInt(2),
                        income = cursor.getDouble(3),
                        expense = cursor.getDouble(4),
                        categoryJson = cursor.getString(5) ?: ""
                    ))
                }
            }
        return list
    }

    // ═══════════════════ annual_bill ═══════════════════

    fun queryAnnualBills(): List<AnnualBill> {
        val list = mutableListOf<AnnualBill>()
        readableDatabase.query("annual_bill", null, null, null, null, null, "year DESC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(AnnualBill(
                        id = cursor.getLong(0),
                        year = cursor.getInt(1),
                        income = cursor.getDouble(2),
                        expense = cursor.getDouble(3),
                        categoryJson = cursor.getString(4) ?: ""
                    ))
                }
            }
        return list
    }

    // ═══════════════════ category_kw ═══════════════════

    fun queryCategoryKeywords(): Map<String, List<String>> {
        val map = linkedMapOf<String, MutableList<String>>()
        readableDatabase.query("category_kw", null, null, null, null, null, "_id ASC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    map.getOrPut(cursor.getString(1)) { mutableListOf() }
                        .add(cursor.getString(2))
                }
            }
        return map
    }

    fun queryCategoryKwList(category: String): List<Pair<Long, String>> {
        val list = mutableListOf<Pair<Long, String>>()
        readableDatabase.query("category_kw", null, "category=?",
            arrayOf(category), null, null, "_id ASC")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(Pair(cursor.getLong(0), cursor.getString(2)))
                }
            }
        return list
    }

    fun insertCategoryKw(category: String, keyword: String) {
        val v = ContentValues().apply { put("category", category); put("keyword", keyword) }
        writableDatabase.insert("category_kw", null, v)
    }

    fun updateCategoryKw(id: Long, keyword: String) {
        val v = ContentValues().apply { put("keyword", keyword) }
        writableDatabase.update("category_kw", v, "_id=?", arrayOf(id.toString()))
    }

    fun deleteCategoryKw(id: Long) {
        writableDatabase.delete("category_kw", "_id=?", arrayOf(id.toString()))
    }
}
