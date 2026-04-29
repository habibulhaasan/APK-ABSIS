package com.absis.capitalsync.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

data class PasswordStrength(val score: Int, val label: String, val color: Color)

fun getStrength(pw: String): PasswordStrength {
    var s = 0
    if (pw.length >= 8) s++
    if (pw.any { it.isUpperCase() }) s++
    if (pw.any { it.isDigit() }) s++
    if (pw.any { !it.isLetterOrDigit() }) s++
    return when (s) {
        1 -> PasswordStrength(1, "Weak", Color(0xFFEF4444))
        2 -> PasswordStrength(2, "Fair", Color(0xFFF59E0B))
        3 -> PasswordStrength(3, "Good", Color(0xFF3B82F6))
        4 -> PasswordStrength(4, "Strong", Color(0xFF22C55E))
        else -> PasswordStrength(0, "", Color(0xFFE2E8F0))
    }
}

enum class ResetPhase { VERIFYING, FORM, SUCCESS, INVALID }

@Composable
fun ResetPasswordScreen(
    oobCode: String = "",
    onDone: () -> Unit,
    vm: ResetPasswordViewModel = hiltViewModel()
) {
    val phase by vm.phase.collectAsState()
    val email by vm.email.collectAsState()
    val error by vm.error.collectAsState()
    val loading by vm.loading.collectAsState()

    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var showCf by remember { mutableStateOf(false) }
    val strength = getStrength(password)

    LaunchedEffect(oobCode) { vm.verifyCode(oobCode) }

    LaunchedEffect(phase) {
        if (phase == ResetPhase.SUCCESS) {
            kotlinx.coroutines.delay(2500)
            onDone()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2563EB),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🔒", fontSize = 22.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Reset your password", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("ABSIS Capital Sync", fontSize = 14.sp, color = Color(0xFF64748B))
            Spacer(Modifier.height(32.dp))

            when (phase) {
                ResetPhase.VERIFYING -> {
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Verifying your reset link…",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                ResetPhase.INVALID -> {
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✕", fontSize = 22.sp, color = Color(0xFFEF4444))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Link Invalid or Expired",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This reset link has expired or already been used. Please request a new one.",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = onDone,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F172A)
                                )
                            ) {
                                Text("Back to Login")
                            }
                        }
                    }
                }

                ResetPhase.SUCCESS -> {
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✓", fontSize = 22.sp, color = Color(0xFF22C55E))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Password Updated!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Your password has been reset. Redirecting to login…",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                ResetPhase.FORM -> {
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Setting new password for $email",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            )

                            if (error.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Text(
                                        error,
                                        color = Color(0xFFB91C1C),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Text(
                                "New Password",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("Minimum 8 characters") },
                                visualTransformation = if (showPw) VisualTransformation.None
                                                       else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(onClick = { showPw = !showPw }) {
                                        Text(
                                            if (showPw) "Hide" else "Show",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            if (password.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement =
                                        androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                                ) {
                                    repeat(4) { i ->
                                        Surface(
                                            modifier = Modifier.weight(1f).height(4.dp),
                                            shape = RoundedCornerShape(99.dp),
                                            color = if ((i + 1) <= strength.score) strength.color
                                                    else Color(0xFFE2E8F0)
                                        ) {}
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        verticalArrangement =
                                            androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
                                    ) {
                                        listOf(
                                            password.length >= 8 to "8+ characters",
                                            password.any { it.isUpperCase() } to "Uppercase letter",
                                            password.any { it.isDigit() } to "Number",
                                            password.any { !it.isLetterOrDigit() } to "Special character"
                                        ).forEach { (ok, text) ->
                                            Text(
                                                "${if (ok) "✓" else "○"} $text",
                                                fontSize = 11.sp,
                                                color = if (ok) Color(0xFF22C55E)
                                                        else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                    if (strength.label.isNotEmpty()) {
                                        Text(
                                            strength.label.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = strength.color
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                "Confirm Password",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = confirm,
                                onValueChange = { confirm = it },
                                placeholder = { Text("Re-enter your new password") },
                                visualTransformation = if (showCf) VisualTransformation.None
                                                       else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(onClick = { showCf = !showCf }) {
                                        Text(
                                            if (showCf) "Hide" else "Show",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            if (confirm.isNotEmpty()) {
                                Text(
                                    if (confirm == password) "✓ Passwords match"
                                    else "Passwords do not match",
                                    fontSize = 12.sp,
                                    color = if (confirm == password) Color(0xFF22C55E)
                                            else Color(0xFFEF4444),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = { vm.resetPassword(oobCode, password, confirm) },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F172A)
                                )
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Reset Password", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    TextButton(onClick = onDone) {
                        Text(
                            "Remember your password? Sign in",
                            fontSize = 14.sp,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }
        }
    }
}