package com.absis.capitalsync.ui.auth

import androidx.compose.ui.graphics.Color

data class PasswordStrength(val score: Int, val label: String, val color: Color)

fun getStrength(pw: String): PasswordStrength {
    var score = 0
    if (pw.length >= 8)                   score++
    if (pw.any { it.isUpperCase() })      score++
    if (pw.any { it.isDigit() })          score++
    if (pw.any { !it.isLetterOrDigit() }) score++
    return when (score) {
        1    -> PasswordStrength(1, "Weak",   Color(0xFFEF4444))
        2    -> PasswordStrength(2, "Fair",   Color(0xFFF59E0B))
        3    -> PasswordStrength(3, "Good",   Color(0xFF3B82F6))
        4    -> PasswordStrength(4, "Strong", Color(0xFF22C55E))
        else -> PasswordStrength(0, "",       Color(0xFFE2E8F0))
    }
}