package com.absis.capitalsync.ui.appinfo

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.absis.capitalsync.R

@Composable
fun AppInfoScreen(
    onBack: () -> Unit,
    vm: AppInfoViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text("← Back", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("App Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            
            // ── App Header Identity (Using your custom drawable logo) ──
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // This safely loads your app_logo.png from the res/drawable folder!
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                Spacer(Modifier.height(16.dp))
                Text("ABSIS Capital Sync", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                Text("Version ${state.currentVersion}", fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp))
            }

            // ── Update Status Card ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("UPDATE STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.06.sp)
                    Spacer(Modifier.height(12.dp))

                    if (state.loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF2563EB), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Checking for updates...", fontSize = 14.sp, color = Color(0xFF475569))
                        }
                    } else if (state.error != null) {
                        Text("⚠️ ${state.error}", fontSize = 14.sp, color = Color(0xFFDC2626))
                    } else {
                        if (state.isUpdateAvailable) {
                            Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2563EB)))
                                        Spacer(Modifier.width(8.dp))
                                        Text("New Version Available: ${state.serverVersion}", fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), fontSize = 14.sp)
                                    }
                                    Text("Released on ${state.releaseDate}", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp, start = 16.dp))
                                }
                            }

                            if (state.changelog.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text("What's New:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                Spacer(Modifier.height(6.dp))
                                state.changelog.forEach { change ->
                                    Row(Modifier.padding(bottom = 4.dp)) {
                                        Text("•", color = Color(0xFF94A3B8), modifier = Modifier.padding(end = 6.dp))
                                        Text(change, fontSize = 13.sp, color = Color(0xFF475569))
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.activeApkUrl))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Download Update", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFDCFCE7)), contentAlignment = Alignment.Center) {
                                    Text("✓", color = Color(0xFF15803D), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Your app is up to date.", fontSize = 14.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    if (!state.loading && !state.isUpdateAvailable) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { vm.checkForUpdates() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Check for Updates", color = Color(0xFF475569))
                        }
                    }
                }
            }

            // ── Developer Info Card ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("DEVELOPER INFO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.06.sp)
                    Spacer(Modifier.height(12.dp))
                    
                    Text("ABSIS App Solutions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Developed and maintained exclusively for capital and organization management.", fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp))
                    
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://absis-backup.vercel.app"))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Visit Official Website", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}