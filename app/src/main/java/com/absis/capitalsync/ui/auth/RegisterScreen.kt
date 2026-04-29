// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ui/auth/RegisterScreen.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
package com.absis.capitalsync.ui.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    vm: RegisterViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()

    var name    by remember { mutableStateOf("") }
    var email   by remember { mutableStateOf("") }
    var phone   by remember { mutableStateOf("") }
    var orgCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    // On success — show confirmation then redirect
    if (state.success) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color(0xFFDCFCE7), shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✓", fontSize = 26.sp, color = Color(0xFF15803D))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Account Created!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your account is pending admin approval. You'll be able to log in once an admin approves your membership.",
                        fontSize = 13.sp, color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) { Text("Go to Login", fontWeight = FontWeight.Bold) }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // ── Header ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2563EB),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Text("❤", fontSize = 22.sp) }
            }
            Spacer(Modifier.height(16.dp))
            Text("Create your account", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A))
            Text("Join ABSIS Capital Sync", fontSize = 14.sp, color = Color(0xFF64748B))
        }

        // ── Card ──
        Card(
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {

                // Error banner
                if (state.error.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFEE2E2), shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    ) {
                        Text(state.error, color = Color(0xFFB91C1C), fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp))
                    }
                }

                // ── Full Name ──
                RegLabel("Full Name (English)")
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    placeholder = { Text("e.g. Ahmed Rahman", color = Color(0xFF94A3B8)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // ── Email ──
                RegLabel("Email")
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    placeholder = { Text("you@example.com", color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // ── Phone ──
                RegLabel("Phone Number")
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    placeholder = { Text("+880…", color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // ── Organisation Code ──
                RegLabel("Organisation Code")
                OutlinedTextField(
                    value = orgCode, onValueChange = { orgCode = it },
                    placeholder = { Text("Enter your invite code", color = Color(0xFF94A3B8)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Text("Ask your organisation admin for the join code.",
                    fontSize = 11.sp, color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 14.dp))

                // ── Password ──
                RegLabel("Password")
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    placeholder = { Text("Minimum 8 characters", color = Color(0xFF94A3B8)) },
                    visualTransformation = if (showPass) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showPass = !showPass }) {
                            Text(if (showPass) "Hide" else "Show",
                                fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // ── Submit ──
                Button(
                    onClick = { vm.register(name, email, password, phone, orgCode) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    if (state.loading)
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    else
                        Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // ── Footer ──
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", fontSize = 14.sp, color = Color(0xFF64748B))
            TextButton(
                onClick = onNavigateToLogin,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Sign in", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB))
            }
        }
    }
}

@Composable
private fun RegLabel(text: String) {
    Text(
        text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = Color(0xFF64748B), letterSpacing = 0.07.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}