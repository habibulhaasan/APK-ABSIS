package com.absis.capitalsync.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun appOutlinedTextFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor   = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor  = Color(0xFFF8FAFC),
        errorContainerColor     = Color(0xFFFFF5F5),
        focusedBorderColor      = Color(0xFF2563EB),
        unfocusedBorderColor    = Color(0xFFCBD5E1),
        disabledBorderColor     = Color(0xFFE2E8F0),
        errorBorderColor        = Color(0xFFDC2626),
        focusedTextColor        = Color(0xFF0F172A),
        unfocusedTextColor      = Color(0xFF0F172A),
        disabledTextColor       = Color(0xFF94A3B8),
        focusedLabelColor       = Color(0xFF2563EB),
        unfocusedLabelColor     = Color(0xFF64748B),
        disabledLabelColor      = Color(0xFF94A3B8),
        focusedPlaceholderColor   = Color(0xFF94A3B8),
        unfocusedPlaceholderColor = Color(0xFF94A3B8),
    )