// ui/profile/ProfileViewModel.kt
package com.absis.capitalsync.ui.profile

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

private const val GAS_URL = "https://script.google.com/macros/s/AKfycbyQ6L2d3SfAynofqAHfb1jHSn6ZA18pv2ABgXZDLNDR-DHtEyIxYEb8tCCsDBwbk0RF/exec"
private const val GAS_SECRET = "absis-secret-123"

data class ToastState(val message: String, val isError: Boolean)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private val http = OkHttpClient()

    private val _form          = MutableStateFlow<ProfileForm?>(null)
    private val _legalFiles    = MutableStateFlow<List<LegalFile>>(emptyList())
    private val _profileLocked = MutableStateFlow(false)
    private val _saving        = MutableStateFlow(false)
    private val _processing    = MutableStateFlow(false)
    private val _toast         = MutableStateFlow<ToastState?>(null)
    private val _lastUpdated   = MutableStateFlow<String?>(null)
    private val _memberInfo    = MutableStateFlow<Map<String, Any>?>(null)
    private val _fileTab       = MutableStateFlow("all")

    val form          = _form.asStateFlow()
    val legalFiles    = _legalFiles.asStateFlow()
    val profileLocked = _profileLocked.asStateFlow()
    val saving        = _saving.asStateFlow()
    val processing    = _processing.asStateFlow()
    val toast         = _toast.asStateFlow()
    val lastUpdated   = _lastUpdated.asStateFlow()
    val memberInfo    = _memberInfo.asStateFlow()
    val fileTab       = _fileTab.asStateFlow()

    private val uid: String get() = auth.currentUser?.uid ?: ""
    private var orgId: String = ""
    private var driveFolderId: String? = null

    init { loadProfile() }

    private fun loadProfile() = viewModelScope.launch {
        try {
            val userSnap   = db.collection("users").document(uid).get().await()
            val u          = userSnap.data ?: return@launch
            orgId          = u["activeOrgId"] as? String ?: return@launch
            driveFolderId  = u["driveFolderId"] as? String

            val memberSnap = db.collection("organizations/$orgId/members")
                .document(uid).get().await()
            val m = memberSnap.data ?: emptyMap()
            _memberInfo.value = m + u

            _form.value = ProfileForm(
                nameEnglish         = u["nameEnglish"] as? String ?: u["displayName"] as? String ?: "",
                nameBengali         = m["nameBengali"] as? String ?: "",
                fatherNameEn        = m["fatherNameEn"] as? String ?: u["fatherName"] as? String ?: "",
                fatherNameBn        = m["fatherNameBn"] as? String ?: "",
                motherNameEn        = m["motherNameEn"] as? String ?: u["motherName"] as? String ?: "",
                motherNameBn        = m["motherNameBn"] as? String ?: "",
                dob                 = m["dob"] as? String ?: u["dob"] as? String ?: "",
                nid                 = m["nid"] as? String ?: u["nid"] as? String ?: "",
                bloodGroup          = m["bloodGroup"] as? String ?: "",
                maritalStatus       = m["maritalStatus"] as? String ?: "",
                spouseNameEn        = m["spouseNameEn"] as? String ?: "",
                spouseNameBn        = m["spouseNameBn"] as? String ?: "",
                education           = m["education"] as? String ?: "",
                occupation          = m["occupation"] as? String ?: u["occupation"] as? String ?: "",
                monthlyIncome       = m["monthlyIncome"] as? String ?: "",
                phone               = u["phone"] as? String ?: m["phone"] as? String ?: "",
                alternativePhone    = m["alternativePhone"] as? String ?: "",
                email               = u["email"] as? String ?: "",
                presentAddressEn    = m["presentAddressEn"] as? String ?: u["address"] as? String ?: "",
                presentAddressBn    = m["presentAddressBn"] as? String ?: "",
                permanentAddressEn  = m["permanentAddressEn"] as? String ?: "",
                permanentAddressBn  = m["permanentAddressBn"] as? String ?: "",
                heirNameEn          = m["heirNameEn"] as? String ?: "",
                heirNameBn          = m["heirNameBn"] as? String ?: "",
                heirRelation        = m["heirRelation"] as? String ?: "",
                heirFatherHusbandEn = m["heirFatherHusbandEn"] as? String ?: "",
                heirFatherHusbandBn = m["heirFatherHusbandBn"] as? String ?: "",
                heirNID             = m["heirNID"] as? String ?: "",
                heirPhone           = m["heirPhone"] as? String ?: "",
                heirAddressEn       = m["heirAddressEn"] as? String ?: "",
                heirAddressBn       = m["heirAddressBn"] as? String ?: "",
                photoUrl            = u["photoURL"] as? String ?: "",
                nomineePhotoUrl     = m["nomineePhotoURL"] as? String ?: "",
                idNo                = m["idNo"] as? String ?: u["idNo"] as? String ?: "",
                joiningDate         = m["joiningDate"] as? String ?: ""
            )

            @Suppress("UNCHECKED_CAST")
            _legalFiles.value = (m["legalFiles"] as? List<Map<String, Any>> ?: emptyList())
                .map { it.toLegalFile() }

            _profileLocked.value = m["profileSubmitted"] as? Boolean ?: false
            _lastUpdated.value   = formatTimestamp(m["profileUpdatedAt"] ?: u["profileUpdatedAt"])

        } catch (e: Exception) {
            showToast("Failed to load profile: ${e.message}", true)
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                "data:$mimeType;base64,$base64String"
            } else null
        } catch (e: Exception) { null }
    }

    fun updateField(key: String, value: String) {
        val f = _form.value ?: return
        _form.value = when (key) {
            "nameEnglish"         -> f.copy(nameEnglish = value)
            "nameBengali"         -> f.copy(nameBengali = value)
            "fatherNameEn"        -> f.copy(fatherNameEn = value)
            "fatherNameBn"        -> f.copy(fatherNameBn = value)
            "motherNameEn"        -> f.copy(motherNameEn = value)
            "motherNameBn"        -> f.copy(motherNameBn = value)
            "dob"                 -> f.copy(dob = value)
            "nid"                 -> f.copy(nid = value)
            "bloodGroup"          -> f.copy(bloodGroup = value)
            "maritalStatus"       -> f.copy(maritalStatus = value)
            "spouseNameEn"        -> f.copy(spouseNameEn = value)
            "spouseNameBn"        -> f.copy(spouseNameBn = value)
            "education"           -> f.copy(education = value)
            "occupation"          -> f.copy(occupation = value)
            "monthlyIncome"       -> f.copy(monthlyIncome = value)
            "phone"               -> f.copy(phone = value)
            "alternativePhone"    -> f.copy(alternativePhone = value)
            "presentAddressEn"    -> f.copy(presentAddressEn = value)
            "presentAddressBn"    -> f.copy(presentAddressBn = value)
            "permanentAddressEn"  -> f.copy(permanentAddressEn = value)
            "permanentAddressBn"  -> f.copy(permanentAddressBn = value)
            "heirNameEn"          -> f.copy(heirNameEn = value)
            "heirNameBn"          -> f.copy(heirNameBn = value)
            "heirRelation"        -> f.copy(heirRelation = value)
            "heirFatherHusbandEn" -> f.copy(heirFatherHusbandEn = value)
            "heirFatherHusbandBn" -> f.copy(heirFatherHusbandBn = value)
            "heirNID"             -> f.copy(heirNID = value)
            "heirPhone"           -> f.copy(heirPhone = value)
            "heirAddressEn"       -> f.copy(heirAddressEn = value)
            "heirAddressBn"       -> f.copy(heirAddressBn = value)
            else -> f
        }
    }

    fun onPhotoSelected(uri: Uri, isNominee: Boolean) {
        val f = _form.value ?: return
        val base64 = uriToBase64(uri)
        if (base64 != null) {
            _form.value = if (isNominee) f.copy(nomineePhotoUrl = base64)
                          else           f.copy(photoUrl = base64)
        }
    }

    fun uploadFile(uri: Uri, type: String) = viewModelScope.launch {
        val f = _form.value ?: return@launch
        _processing.value = true
        try {
            val bytes    = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw Exception("Read failed")
            val base64   = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = resolveFileName(uri) ?: "${type}_${UUID.randomUUID()}"

            val body = JSONObject().apply {
                put("action", "uploadProfileFile")
                put("secret", GAS_SECRET)
                put("file", base64)
                put("fileName", fileName)
                put("mimeType", mimeType)
                put("userId", f.idNo)
                put("userName", f.nameEnglish.ifEmpty { "User" })
                put("memberId", f.idNo)
                put("userFolderId", driveFolderId ?: "")
                put("type", type)
            }.toString()

            val request = Request.Builder().url(GAS_URL).post(body.toRequestBody("text/plain".toMediaType())).build()
            val response = withContext(Dispatchers.IO) { http.newCall(request).execute() }
            val json = JSONObject(response.body?.string() ?: "{}")

            if (!json.optBoolean("success", false)) throw Exception(json.optString("error", "GAS upload failed"))

            val returnedFolderId = json.optString("folderId", "")
            if (returnedFolderId.isNotEmpty() && returnedFolderId != driveFolderId) {
                driveFolderId = returnedFolderId
                db.collection("users").document(uid).update("driveFolderId", returnedFolderId).await()
            }

            val category = when (type) {
                "nid", "nomineeNid", "nomineePhoto" -> "Identity Document"
                else -> "Other"
            }
            val newFile = LegalFile(
                name = fileName, title = fileName, url = json.optString("url", ""),
                fileId = json.optString("fileId", ""), mimeType = mimeType, uploadedBy = "member",
                uploadedAt = java.time.Instant.now().toString(), category = category, description = ""
            )

            val updatedFiles = _legalFiles.value + newFile
            _legalFiles.value = updatedFiles
            db.collection("organizations/$orgId/members").document(uid).update("legalFiles", updatedFiles.map { it.toMap() }).await()

            showToast("✅ File uploaded to Google Drive")
        } catch (e: Exception) {
            showToast("Upload failed: ${e.message}", true)
        }
        _processing.value = false
    }

    fun saveProfile() = viewModelScope.launch {
        val f = _form.value ?: return@launch
        if (_profileLocked.value) return@launch
        _saving.value = true
        try {
            val now = FieldValue.serverTimestamp()
            db.collection("users").document(uid).update(
                mapOf(
                    "idNo" to f.idNo, "nameEnglish" to f.nameEnglish, "nameBengali" to f.nameBengali,
                    "phone" to f.phone, "photoURL" to f.photoUrl, "bloodGroup" to f.bloodGroup,
                    "occupation" to f.occupation, "profileUpdatedAt" to now
                )
            ).await()

            db.collection("organizations/$orgId/members").document(uid).set(
                f.toFirestoreMap(f.photoUrl, f.nomineePhotoUrl) + mapOf("profileUpdatedAt" to now, "profileSubmitted" to true),
                SetOptions.merge()
            ).await()

            _profileLocked.value = true
            _lastUpdated.value = "just now"
            showToast("✅ Profile saved! Changes locked.")
        } catch (e: Exception) {
            showToast(e.message ?: "Save failed", true)
        }
        _saving.value = false
    }

    fun setFileTab(tab: String) { _fileTab.value = tab }

    private fun resolveFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && idx >= 0) it.getString(idx) else null
        }
    }

    private fun showToast(msg: String, isError: Boolean = false) {
        _toast.value = ToastState(msg, isError)
        viewModelScope.launch { delay(4000); _toast.value = null }
    }

    private fun formatTimestamp(ts: Any?): String? {
        if (ts == null) return null
        return try {
            when (ts) {
                is com.google.firebase.Timestamp -> android.text.format.DateFormat.format("dd MMM yyyy, HH:mm", ts.toDate()).toString()
                is String -> ts
                else -> ts.toString()
            }
        } catch (e: Exception) { null }
    }

    private fun Map<String, Any>.toLegalFile() = LegalFile(
        name        = this["name"] as? String ?: "",
        title       = this["title"] as? String ?: this["name"] as? String ?: "",
        url         = this["url"] as? String ?: "",
        fileId      = this["fileId"] as? String ?: "",
        mimeType    = this["mimeType"] as? String ?: "",
        uploadedBy  = this["uploadedBy"] as? String ?: "member",
        uploadedAt  = this["uploadedAt"] as? String ?: "",
        category    = this["category"] as? String ?: "",
        description = this["description"] as? String ?: ""
    )

    private fun LegalFile.toMap() = mapOf(
        "name" to name, "title" to title, "url" to url, "fileId" to fileId,
        "mimeType" to mimeType, "uploadedBy" to uploadedBy, "uploadedAt" to uploadedAt,
        "category" to category, "description" to description
    )

    private fun ProfileForm.toFirestoreMap(photoUrl: String, nomineePhotoUrl: String) = mapOf(
        "nameEnglish" to nameEnglish, "nameBengali" to nameBengali, "fatherNameEn" to fatherNameEn, "fatherNameBn" to fatherNameBn,
        "motherNameEn" to motherNameEn, "motherNameBn" to motherNameBn, "dob" to dob, "nid" to nid, "bloodGroup" to bloodGroup,
        "maritalStatus" to maritalStatus, "spouseNameEn" to spouseNameEn, "spouseNameBn" to spouseNameBn, "education" to education,
        "occupation" to occupation, "monthlyIncome" to monthlyIncome, "phone" to phone, "alternativePhone" to alternativePhone,
        "presentAddressEn" to presentAddressEn, "presentAddressBn" to presentAddressBn, "permanentAddressEn" to permanentAddressEn,
        "permanentAddressBn" to permanentAddressBn, "heirNameEn" to heirNameEn, "heirNameBn" to heirNameBn, "heirRelation" to heirRelation,
        "heirFatherHusbandEn" to heirFatherHusbandEn, "heirFatherHusbandBn" to heirFatherHusbandBn, "heirNID" to heirNID,
        "heirPhone" to heirPhone, "heirAddressEn" to heirAddressEn, "heirAddressBn" to heirAddressBn, "photoURL" to photoUrl,
        "nomineePhotoURL" to nomineePhotoUrl, "idNo" to idNo
    )
}