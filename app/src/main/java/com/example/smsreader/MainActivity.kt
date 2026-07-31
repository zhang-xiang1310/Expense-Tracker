package com.example.smsreader

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val log = StringBuilder()
        fun addLog(s: String) {
            Log.d("SmsReader", s)
            log.append(s).append("\n")
        }

        setContent {
            var logLines by remember { mutableStateOf(listOf("启动中...")) }
            var smsItems by remember { mutableStateOf(listOf<Map<String, String>>()) }

            LaunchedEffect(Unit) {
                addLog("App启动, SDK=${android.os.Build.VERSION.SDK_INT}")
                addLog("设备: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")

                val hasPerm = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                addLog("权限: ${if (hasPerm) "OK" else "无"}")

                if (!hasPerm) {
                    addLog("等待授权...")
                    logLines = log.toString().split("\n").filter { it.isNotBlank() }
                    return@LaunchedEffect
                }

                try {
                    val list = withContext(Dispatchers.IO) {
                        val items = mutableListOf<Map<String, String>>()
                        var cursor: Cursor? = null
                        try {
                            addLog("查询短信...")
                            cursor = contentResolver.query(
                                Telephony.Sms.CONTENT_URI,
                                arrayOf("_id", "address", "body", "date", "type"),
                                null, null,
                                "date DESC LIMIT 50"
                            )
                            addLog("cursor=${if (cursor != null) "OK" else "NULL"}")

                            cursor?.use { c ->
                                val cnt = c.count
                                addLog("总数: $cnt")
                                while (c.moveToNext()) {
                                    items.add(
                                        mapOf(
                                            "id" to c.getLong(0).toString(),
                                            "address" to (c.getString(1) ?: "-"),
                                            "body" to (c.getString(2) ?: ""),
                                            "date" to c.getLong(3).toString(),
                                            "type" to c.getInt(4).toString()
                                        )
                                    )
                                }
                            }
                            addLog("成功读取 ${items.size} 条")
                        } catch (e: Throwable) {
                            addLog("读短信错误: ${e.javaClass.simpleName}: ${e.message}")
                        }
                        items
                    }
                    smsItems = list
                } catch (e: Throwable) {
                    addLog("外层错误: ${e.javaClass.simpleName}: ${e.message}")
                }
                logLines = log.toString().split("\n").filter { it.isNotBlank() }
            }

            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    if (smsItems.isEmpty()) {
                        // 调试界面
                        Column(Modifier.fillMaxSize().padding(16.dp)) {
                            Text("调试日志", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                LazyColumn(Modifier.padding(12.dp)) {
                                    itemsIndexed(logLines) { _, line ->
                                        Text(
                                            line,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Scaffold(
                            topBar = {
                                TopAppBar(title = { Text("短信 (${smsItems.size}条)") })
                            }
                        ) { padding ->
                            LazyColumn(
                                modifier = Modifier.padding(padding),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                itemsIndexed(smsItems) { _, item ->
                                    Card(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(
                                                item["address"] ?: "-",
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(item["body"] ?: "")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 请求权限
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) recreate()
            }.launch(Manifest.permission.READ_SMS)
        }
    }
}
