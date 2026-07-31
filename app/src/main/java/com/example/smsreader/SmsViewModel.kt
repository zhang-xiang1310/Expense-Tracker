package com.example.smsreader

import android.app.Application
import android.database.Cursor
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsreader.model.SmsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val smsList: List<SmsItem> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadSms() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val smsList = readAllSms()
                _uiState.update { it.copy(smsList = smsList, isLoading = false) }
            } catch (e: Throwable) {
                val msg = e.message ?: e.javaClass.simpleName
                _uiState.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    private fun readAllSms(): List<SmsItem> {
        val smsList = mutableListOf<SmsItem>()
        val resolver = getApplication<Application>().contentResolver

        var cursor: Cursor? = null
        try {
            // 限制最多读取1000条，防止OOM
            cursor = resolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 1000"
            )
        } catch (e: Throwable) {
            // 有些ROM不支持LIMIT语法，回退
            cursor = resolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )
        }

        cursor?.use { c ->
            if (!c.moveToFirst()) return smsList

            val idIdx = c.getColumnIndex(Telephony.Sms._ID)
            val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndex(Telephony.Sms.TYPE)

            // 验证必需的列
            if (addressIdx == -1 || dateIdx == -1) return smsList

            do {
                try {
                    smsList.add(
                        SmsItem(
                            id = safeGetLong(c, idIdx),
                            address = safeGetString(c, addressIdx, "未知"),
                            body = safeGetString(c, bodyIdx, ""),
                            date = safeGetLong(c, dateIdx),
                            type = safeGetInt(c, typeIdx, Telephony.Sms.MESSAGE_TYPE_INBOX)
                        )
                    )
                } catch (_: Throwable) {
                    // 跳过单条损坏的记录
                }
            } while (c.moveToNext())
        }

        return smsList
    }

    private fun safeGetLong(c: Cursor, idx: Int, default: Long = 0L): Long {
        return if (idx >= 0) c.getLong(idx) else default
    }

    private fun safeGetString(c: Cursor, idx: Int, default: String): String {
        return if (idx >= 0) c.getString(idx) ?: default else default
    }

    private fun safeGetInt(c: Cursor, idx: Int, default: Int): Int {
        return if (idx >= 0) c.getInt(idx) else default
    }
}
