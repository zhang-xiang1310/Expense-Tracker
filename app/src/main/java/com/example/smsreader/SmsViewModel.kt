package com.example.smsreader

import android.app.Application
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
                val smsList = mutableListOf<SmsItem>()
                val resolver = getApplication<Application>().contentResolver

                val cursor = resolver.query(
                    Telephony.Sms.CONTENT_URI,
                    null,
                    null,
                    null,
                    "${Telephony.Sms.DATE} DESC"
                )

                cursor?.use {
                    val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                    val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                    val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                    val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)

                    while (it.moveToNext()) {
                        smsList.add(
                            SmsItem(
                                id = it.getLong(idIdx),
                                address = it.getString(addressIdx) ?: "未知",
                                body = it.getString(bodyIdx) ?: "",
                                date = it.getLong(dateIdx),
                                type = it.getInt(typeIdx)
                            )
                        )
                    }
                }

                _uiState.update { it.copy(smsList = smsList, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
