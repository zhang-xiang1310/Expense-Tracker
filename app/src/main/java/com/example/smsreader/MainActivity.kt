package com.example.smsreader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.smsreader.ui.SmsListScreen

class MainActivity : ComponentActivity() {

    private val viewModel: SmsViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadSms()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全局异常处理，防止未知异常导致直接闪退
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SmsReader", "Uncaught exception", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.loadSms()
        }

        setContent {
            var showPermissionScreen by remember { mutableStateOf(!hasPermission) }

            MaterialTheme {
                if (showPermissionScreen) {
                    PermissionScreen(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_SMS)
                            showPermissionScreen = false
                        }
                    )
                } else {
                    SmsListScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "需要短信权限",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "此应用需要读取短信权限才能显示您的短信列表。\n点击下方按钮授予权限。",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("授予权限")
            }
        }
    }
}
