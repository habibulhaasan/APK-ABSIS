// util/ReceiptUploader.kt
package com.absis.capitalsync.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64

private const val GAS_RECEIPT_URL =
    "https://script.google.com/macros/s/AKfycbxRTFQzexjQZR7NilSxodFMiEABYvPfbPycrGB-CK2emCG6fiDQvW6OFcEkCmmPptPU2w/exec"
private const val GAS_SECRET = "my_absis_gas_secret_123"

data class ReceiptUploadResult(
    val fileId:   String,
    val url:      String,
    val name:     String,
    val folderId: String,
)

suspend fun uploadReceipt(
    context:      Context,
    uri:          Uri,
    memberId:     String,
    memberName:   String,
    userFolderId: String? = null,
): ReceiptUploadResult = withContext(Dispatchers.IO) {

    val safeMemberId   = memberId.ifBlank   { "UNKNOWN" }
    val safeMemberName = memberName.ifBlank { "Unknown" }

    // ── Resolve MIME type and file name ──────────────────────────────────────
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
    val fileName = resolveFileName(context, uri) ?: "receipt.jpg"

    // ── Read bytes → Base64 ──────────────────────────────────────────────────
    val inputStream: InputStream = contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("Cannot open URI: $uri")
    val bytes  = inputStream.use { it.readBytes() }
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

    // ── Build JSON payload ───────────────────────────────────────────────────
    val payload = JSONObject().apply {
        put("secret",       GAS_SECRET)
        put("action",       "uploadReceiptFile")
        put("file",         base64)
        put("mimeType",     mimeType)
        put("fileName",     fileName)
        put("memberId",     safeMemberId)
        put("memberName",   safeMemberName)
        put("userFolderId", userFolderId ?: "")
    }

    // ── POST to GAS ──────────────────────────────────────────────────────────
    val connection = (URL(GAS_RECEIPT_URL).openConnection() as HttpURLConnection).apply {
        requestMethod     = "POST"
        doOutput          = true
        connectTimeout    = 30_000
        readTimeout       = 60_000
        setRequestProperty("Content-Type", "text/plain")
    }

    connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

    val responseText = connection.inputStream.use { it.bufferedReader().readText() }

    val json = try {
        JSONObject(responseText)
    } catch (e: Exception) {
        throw IllegalStateException("GAS returned non-JSON: ${responseText.take(200)}")
    }

    if (!json.optBoolean("success", false)) {
        throw IllegalStateException(json.optString("error", "GAS upload failed"))
    }

    ReceiptUploadResult(
        fileId   = json.getString("fileId"),
        url      = json.getString("url"),
        name     = json.getString("name"),
        folderId = json.optString("folderId", ""),
    )
}

// ── Helper: resolve a human-readable filename from a URI ─────────────────────
private fun resolveFileName(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
    }
    return uri.lastPathSegment
}