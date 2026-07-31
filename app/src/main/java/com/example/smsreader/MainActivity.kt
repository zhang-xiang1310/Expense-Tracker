package com.example.smsreader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    data class Sms(val address: String, val body: String)

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) recreate() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.READ_SMS)
        }

        val smsList = if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            readSms()
        } else {
            emptyList()
        }

        setContent {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(smsList) { sms ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(sms.address, fontWeight = FontWeight.Bold)
                        Text(sms.body)
                    }
                }
            }
        }
    }

    private fun readSms(): List<Sms> {
        val list = mutableListOf<Sms>()
        contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf("address", "body"),
            null, null,
            "date DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    Sms(
                        cursor.getString(0) ?: "-",
                        cursor.getString(1) ?: ""
                    )
                )
            }
        }
        return list
    }
}
