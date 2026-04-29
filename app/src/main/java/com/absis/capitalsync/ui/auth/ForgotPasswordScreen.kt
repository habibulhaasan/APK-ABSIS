// ui/auth/ForgotPasswordScreen.kt
package com.absis.capitalsync.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    var email by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Reset Password", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("We'll send a reset link to your email", fontSize = 14.sp, color = Color(0xFF64748B))
            Spacer(Modifier.height(32.dp))

            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    if (uiState.resetSent) {
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(8.dp)) {
                            Text("✓ Reset link sent to $email — check your inbox.",
                                color = Color(0xFF15803D), modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                        }
                    } else {
                        if (uiState.error.isNotEmpty()) {
                            Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                Text(uiState.error, color = Color(0xFFB91C1C), modifier = Modifier.padding(10.dp))
                            }
                        }
                        Text("Email", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            placeholder = { Text("you@example.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { vm.sendPasswordReset(email) },
                            enabled = !uiState.loading && email.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            if (uiState.loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Send Reset Link", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onBack) {
                Text("← Back to sign in", color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}