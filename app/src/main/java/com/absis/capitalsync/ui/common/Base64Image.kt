package com.absis.capitalsync.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@Composable
fun Base64Image(
    base64: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = {},
) {
    if (base64.isBlank()) { fallback(); return }

    val raw = if (base64.contains(",")) base64.substringAfter(",") else base64

    val bitmap = remember(raw) {
        runCatching {
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap             = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier           = modifier,
            contentScale       = contentScale,
        )
    } else {
        fallback()
    }
}

@Composable
fun SmartImage(
    source: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = {},
) {
    when {
        source.isBlank() -> fallback()
        source.startsWith("data:") || (!source.startsWith("http") && source.length > 100) ->
            Base64Image(
                base64       = source,
                modifier     = modifier,
                contentScale = contentScale,
                fallback     = fallback
            )
        else -> AsyncImage(
            model              = source,
            contentDescription = null,
            modifier           = modifier,
            contentScale       = contentScale,
        )
    }
}

@Composable
fun FilePreviewDialog(
    fileName:  String,
    fileUrl:   String,
    mimeType:  String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape  = RoundedCornerShape(16.dp),
            color  = Color.White,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = fileName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        color      = Color(0xFF0F172A),
                        modifier   = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Surface(
                            Modifier.size(28.dp),
                            RoundedCornerShape(50.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✕", fontSize = 14.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                HorizontalDivider()

                Box(
                    Modifier.fillMaxSize().background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        mimeType.startsWith("image/") -> {
                            AsyncImage(
                                model              = fileUrl,
                                contentDescription = fileName,
                                contentScale       = ContentScale.Fit,
                                modifier           = Modifier.fillMaxSize().padding(8.dp)
                            )
                        }
                        mimeType.contains("pdf") || fileUrl.isNotEmpty() -> {
                            PdfPreviewContent(fileUrl = fileUrl, fileName = fileName)
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📄", fontSize = 40.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Preview not available for this file type.",
                                    fontSize = 13.sp,
                                    color    = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPreviewContent(fileUrl: String, fileName: String) {
    if (fileUrl.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📕", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text("No URL available for preview.", fontSize = 13.sp, color = Color(0xFF94A3B8))
        }
        return
    }
    val viewerUrl = "https://docs.google.com/gview?embedded=true&url=${
        java.net.URLEncoder.encode(fileUrl, "UTF-8")
    }"
    AndroidWebView(url = viewerUrl)
}

@Composable
fun AndroidWebView(url: String) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled    = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort      = true
                settings.builtInZoomControls  = true
                settings.displayZoomControls  = false
                webViewClient = android.webkit.WebViewClient()
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}