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

// ═══════════════════════════════════
// 数据库
// ═══════════════════════════════════

class SmsDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "smsreader.db"
        const val DB_VERSION = 8
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
}
