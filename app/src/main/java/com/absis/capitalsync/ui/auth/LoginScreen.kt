// ui/auth/LoginScreen.kt
package com.absis.capitalsync.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToSuperAdmin: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToRegister: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.destination) {
        when (uiState.destination) {
            "dashboard"  -> onNavigateToDashboard()
            "admin"      -> onNavigateToAdmin()
            "superadmin" -> onNavigateToSuperAdmin()
            else -> {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {

            // ── Logo ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2563EB),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("❤", fontSize = 22.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Sign in to ABSIS Capital Sync", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text("Welcome back", fontSize = 14.sp, color = Color(0xFF64748B))
            Spacer(Modifier.height(32.dp))

            // ── Card ──
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Error banner
                    if (uiState.error.isNotEmpty()) {
                        Surface(
                            color = Color(0xFFFEE2E2), shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(uiState.error, color = Color(0xFFB91C1C), fontSize = 13.sp,
                                modifier = Modifier.padding(10.dp))
                        }
                    }

                    // Email
                    Text("Email", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B), letterSpacing = 0.07.sp)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        placeholder = { Text("you@example.com", color = Color(0xFF94A3B8)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(14.dp))

                    // Password
                    Text("Password", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B), letterSpacing = 0.07.sp)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        placeholder = { Text("••••••••", color = Color(0xFF94A3B8)) },
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPass = !showPass }) {
                                Text(if (showPass) "Hide" else "Show", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            }
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Forgot password
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = onNavigateToForgotPassword) {
                            Text("Forgot password?", fontSize = 13.sp, color = Color(0xFF2563EB))
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // Sign in button
                    Button(
                        onClick = { vm.login(email, password) },
                        enabled = !uiState.loading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        if (uiState.loading)
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else
                            Text("Sign in", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row {
                Text("No account? ", fontSize = 14.sp, color = Color(0xFF64748B))
                TextButton(onClick = onNavigateToRegister, contentPadding = PaddingValues(0.dp)) {
                    Text("Create one", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }
            }
        }
    }
}