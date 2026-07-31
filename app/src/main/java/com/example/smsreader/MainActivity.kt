package com.example.smsreader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val logBuilder = StringBuilder()

        val hasPerm = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPerm) {
            permLauncher.launch(Manifest.permission.READ_SMS)
        }

        logBuilder.append("SDK=${android.os.Build.VERSION.SDK_INT}\n")
        logBuilder.append("设备=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
        logBuilder.append("权限=${if (hasPerm) "OK" else "请求中"}\n")

        setContent {
            var logText by remember { mutableStateOf(logBuilder.toString()) }

            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("调试日志", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            logText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
