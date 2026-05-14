package com.absis.capitalsync.core.updater

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class UpdateInfo(
    val version: String,
    val releaseDate: String,
    val changelog: List<String>
)

@Composable
fun AppUpdateChecker(
    currentVersion: String,
    jsonUrl: String,
    apkDownloadUrl: String
) {
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Check for updates in the background when the component loads
    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) {
                URL(jsonUrl).readText()
            }
            val json = JSONObject(result)
            val serverVersion = json.optString("version", "1.0.0")
            
            // Compare versions
            if (isNewerVersion(currentVersion, serverVersion)) {
                val changesArray = json.optJSONArray("changelog")
                val changesList = mutableListOf<String>()
                if (changesArray != null) {
                    for (i in 0 until changesArray.length()) {
                        changesList.add(changesArray.getString(i))
                    }
                }
                updateInfo = UpdateInfo(
                    version = serverVersion,
                    releaseDate = json.optString("releaseDate", ""),
                    changelog = changesList
                )
                showDialog = true
            }
        } catch (e: Exception) {
            // Silently fail if there's no internet or the URL is unreachable
        }
    }

    if (showDialog && updateInfo != null) {
        UpdateDialog(
            info = updateInfo!!,
            apkDownloadUrl = apkDownloadUrl,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun UpdateDialog(
    info: UpdateInfo,
    apkDownloadUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp)) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚀", fontSize = 24.sp)
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "New Update Available!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "Version ${info.version} is ready to download.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Changelog
                if (info.changelog.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "WHAT'S NEW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.06.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            info.changelog.forEach { change ->
                                Row(Modifier.padding(bottom = 6.dp)) {
                                    Text("✓", color = Color(0xFF15803D), fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                                    Text(change, color = Color(0xFF334155), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Later", color = Color(0xFF475569))
                    }
                    Button(
                        onClick = {
                            // Opens the browser to download the APK
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkDownloadUrl))
                            context.startActivity(intent)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f).height(48.dp)
                    ) {
                        Text("Update Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper function to compare semantic versioning (e.g. "1.0.2" > "1.0.0")
private fun isNewerVersion(current: String, server: String): Boolean {
    val currParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val servParts = server.split(".").map { it.toIntOrNull() ?: 0 }
    
    val length = maxOf(currParts.size, servParts.size)
    for (i in 0 until length) {
        val c = currParts.getOrElse(i) { 0 }
        val s = servParts.getOrElse(i) { 0 }
        if (s > c) return true
        if (s < c) return false
    }
    return false
}