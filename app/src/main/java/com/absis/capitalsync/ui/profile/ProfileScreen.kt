// ui/profile/ProfileScreen.kt
package com.absis.capitalsync.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.absis.capitalsync.ui.common.AndroidWebView

val BLOOD_GROUPS   = listOf("A+","A-","B+","B-","AB+","AB-","O+","O-")
val MARITAL_STATUS = listOf("Single","Married","Divorced","Widowed")
val EDUCATION_OPTS = listOf(
    "No Formal Education","Primary","Secondary (SSC)",
    "Higher Secondary (HSC)","Diploma","Bachelor's","Master's","PhD","Other"
)

data class LegalFile(
    val name: String, val title: String, val url: String, val fileId: String,
    val mimeType: String, val uploadedBy: String, val uploadedAt: String,
    val category: String, val description: String
)

data class ProfileForm(
    val nameEnglish: String = "", val nameBengali: String = "",
    val fatherNameEn: String = "", val fatherNameBn: String = "",
    val motherNameEn: String = "", val motherNameBn: String = "",
    val dob: String = "", val nid: String = "",
    val bloodGroup: String = "", val maritalStatus: String = "",
    val spouseNameEn: String = "", val spouseNameBn: String = "",
    val education: String = "", val occupation: String = "",
    val monthlyIncome: String = "", val phone: String = "",
    val alternativePhone: String = "", val email: String = "",
    val presentAddressEn: String = "", val presentAddressBn: String = "",
    val permanentAddressEn: String = "", val permanentAddressBn: String = "",
    val heirNameEn: String = "", val heirNameBn: String = "",
    val heirRelation: String = "", val heirFatherHusbandEn: String = "",
    val heirFatherHusbandBn: String = "", val heirNID: String = "",
    val heirPhone: String = "", val heirAddressEn: String = "",
    val heirAddressBn: String = "",
    val photoUri: Uri? = null,
    val photoUrl: String = "",       // can be https:// URL or data:image/... base64
    val nomineePhotoUri: Uri? = null,
    val nomineePhotoUrl: String = "", // same
    val idNo: String = "", val joiningDate: String = ""
)

// ── Document Preview Modal ────────────────────────────────────────────────────

@Composable
fun DocumentPreviewModal(file: LegalFile, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
        ) {
            Column(Modifier.fillMaxSize()) {

                // ── Header ──
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(12.dp, 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            fileIcon(file.mimeType),
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                file.title.ifEmpty { file.name },
                                fontWeight = FontWeight.Bold,
                                fontSize   = 14.sp,
                                color      = Color(0xFF0F172A),
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                            if (file.category.isNotEmpty()) {
                                Text(
                                    file.category,
                                    fontSize = 11.sp,
                                    color    = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                    Surface(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp),
                        shape    = RoundedCornerShape(8.dp),
                        color    = Color(0xFFF1F5F9),
                        border   = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✕", fontSize = 16.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                if (file.description.isNotEmpty()) {
                    Text(
                        file.description,
                        fontSize = 12.sp,
                        color    = Color(0xFF475569),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(12.dp, 6.dp)
                    )
                }

                HorizontalDivider()

                // ── Content ──
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        file.mimeType.startsWith("image/") -> {
                            AsyncImage(
                                model              = file.url,
                                contentDescription = file.name,
                                contentScale       = ContentScale.Fit,
                                modifier           = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                        file.fileId.isNotEmpty() -> {
                            // Google Drive inline preview (same as Next.js iframe)
                            AndroidWebView(
                                url = "https://drive.google.com/file/d/${file.fileId}/preview"
                            )
                        }
                        file.url.isNotEmpty() -> {
                            // Fallback: Google Docs viewer
                            val viewerUrl = "https://docs.google.com/gview?embedded=true&url=" +
                                    java.net.URLEncoder.encode(file.url, "UTF-8")
                            AndroidWebView(url = viewerUrl)
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📄", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Preview not available.",
                                    fontSize = 14.sp,
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

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(vm: ProfileViewModel = hiltViewModel()) {
    val form        by vm.form.collectAsState()
    val legalFiles  by vm.legalFiles.collectAsState()
    val locked      by vm.profileLocked.collectAsState()
    val saving      by vm.saving.collectAsState()
    val processing  by vm.processing.collectAsState()
    val toast       by vm.toast.collectAsState()
    val lastUpdated by vm.lastUpdated.collectAsState()
    val memberInfo  by vm.memberInfo.collectAsState()
    val activeTab   by vm.fileTab.collectAsState()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.onPhotoSelected(it, false) } }

    val nomineePhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.onPhotoSelected(it, true) } }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.uploadFile(it, "nid") } }

    val nomineeFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.uploadFile(it, "nomineeNid") } }

    val otherFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { vm.uploadFile(it, "other") } }

    if (form == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
        }
        return
    }

    val f = form!!

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // ── Header ──
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "My Profile",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF0F172A)
                )
                Text(
                    if (locked)
                        "⚠️ Profile submitted. Contact admin to make changes."
                    else
                        "Fill in all details carefully. You can submit only once.",
                    fontSize = 13.sp,
                    color    = Color(0xFF64748B)
                )
            }
            if (!locked) {
                Button(
                    onClick  = { vm.saveProfile() },
                    enabled  = !saving && !processing,
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (saving)
                        CircularProgressIndicator(
                            color    = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    else
                        Text(
                            "Submit Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp
                        )
                }
            }
        }

        // ── Toast ──
        toast?.let { t ->
            Surface(
                color    = if (t.isError) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    t.message,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (t.isError) Color(0xFFB91C1C) else Color(0xFF15803D),
                    modifier   = Modifier.padding(10.dp)
                )
            }
        }

        // ── Last updated ──
        lastUpdated?.let {
            Surface(
                color    = Color(0xFFF0F9FF),
                shape    = RoundedCornerShape(8.dp),
                border   = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    "✏️ Last updated: $it${if (locked) " — Locked. Contact admin to edit." else ""}",
                    fontSize = 12.sp,
                    color    = Color(0xFF1E40AF),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // ── Photo + Member ID strip ──
        Card(
            shape    = RoundedCornerShape(12.dp),
            colors   = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp),
            border   = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {

                // ── Avatar ──
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDBEAFE))
                            .border(3.dp, Color(0xFFBFDBFE), CircleShape)
                            .then(
                                if (!locked)
                                    Modifier.clickable { photoPicker.launch("image/*") }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            // Newly picked local Uri
                            f.photoUri != null -> AsyncImage(
                                model              = f.photoUri,
                                contentDescription = "Photo",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                            // Existing URL (https or data:image base64)
                            f.photoUrl.isNotEmpty() -> AsyncImage(
                                model              = f.photoUrl,
                                contentDescription = "Photo",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                            else -> Text(
                                f.nameEnglish.firstOrNull()?.uppercase() ?: "?",
                                fontSize   = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF1D4ED8)
                            )
                        }
                    }
                    if (!locked) {
                        TextButton(
                            onClick  = { photoPicker.launch("image/*") },
                            enabled  = !processing
                        ) {
                            Text("📷 Photo", fontSize = 11.sp, color = Color(0xFF475569))
                        }
                    }
                }

                Spacer(Modifier.width(20.dp))

                // ── Quick info ──
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Member ID"    to (memberInfo?.get("idNo") as? String ?: "—"),
                        "Joining Date" to (memberInfo?.get("joiningDate") as? String ?: "—"),
                        "Email"        to f.email,
                        "Status"       to if (memberInfo?.get("approved") == true) "✅ Active" else "⏳ Pending"
                    ).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (label, value) ->
                                Surface(
                                    color    = Color(0xFFF8FAFC),
                                    shape    = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(
                                            label.uppercase(),
                                            fontSize      = 9.sp,
                                            fontWeight    = FontWeight.Bold,
                                            color         = Color(0xFF94A3B8),
                                            letterSpacing = 0.06.sp
                                        )
                                        Text(
                                            value,
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = Color(0xFF0F172A),
                                            maxLines   = 1,
                                            overflow   = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Personal Information ──
        ProfileSection("👤 Personal Information") {
            BilingualField("Full Name", "nameEnglish", "nameBengali", f, locked, vm::updateField)
            BilingualField("Father's Name", "fatherNameEn", "fatherNameBn", f, locked, vm::updateField)
            BilingualField("Mother's Name", "motherNameEn", "motherNameBn", f, locked, vm::updateField)
            ProfileField("Date of Birth") {
                OutlinedTextField(value = f.dob, onValueChange = { if (!locked) vm.updateField("dob", it) },
                    placeholder = { Text("YYYY-MM-DD") }, singleLine = true, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("National ID (NID)") {
                OutlinedTextField(value = f.nid, onValueChange = { if (!locked) vm.updateField("nid", it) },
                    placeholder = { Text("NID number") }, singleLine = true, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("Blood Group") {
                DropdownField(BLOOD_GROUPS, f.bloodGroup, !locked) { vm.updateField("bloodGroup", it) }
            }
            ProfileField("Marital Status") {
                DropdownField(MARITAL_STATUS, f.maritalStatus, !locked) { vm.updateField("maritalStatus", it) }
            }
            BilingualField("Spouse Name", "spouseNameEn", "spouseNameBn", f, locked, vm::updateField)
            ProfileField("Education") {
                DropdownField(EDUCATION_OPTS, f.education, !locked) { vm.updateField("education", it) }
            }
            ProfileField("Occupation") {
                OutlinedTextField(value = f.occupation, onValueChange = { if (!locked) vm.updateField("occupation", it) },
                    placeholder = { Text("e.g. Business") }, singleLine = true, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("Monthly Income") {
                OutlinedTextField(value = f.monthlyIncome, onValueChange = { if (!locked) vm.updateField("monthlyIncome", it) },
                    placeholder = { Text("e.g. 25000") }, singleLine = true, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("Phone") {
                OutlinedTextField(value = f.phone, onValueChange = { if (!locked) vm.updateField("phone", it) },
                    placeholder = { Text("+880…") }, singleLine = true, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("Alternative Phone") {
                OutlinedTextField(value = f.alternativePhone, onValueChange = { if (!locked) vm.updateField("alternativePhone", it) },
                    placeholder = { Text("+880…") }, singleLine = true, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
        }

        // ── Address ──
        ProfileSection("📍 Address Information") {
            ProfileField("Present Address (English)", fullWidth = true) {
                OutlinedTextField(value = f.presentAddressEn, onValueChange = { if (!locked) vm.updateField("presentAddressEn", it) },
                    placeholder = { Text("Present address (English)") }, minLines = 2, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("Present Address (বাংলা)", fullWidth = true) {
                OutlinedTextField(value = f.presentAddressBn, onValueChange = { if (!locked) vm.updateField("presentAddressBn", it) },
                    placeholder = { Text("বর্তমান ঠিকানা") }, minLines = 2, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("Permanent Address (English)", fullWidth = true) {
                OutlinedTextField(value = f.permanentAddressEn, onValueChange = { if (!locked) vm.updateField("permanentAddressEn", it) },
                    placeholder = { Text("Permanent address") }, minLines = 2, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("Permanent Address (বাংলা)", fullWidth = true) {
                OutlinedTextField(value = f.permanentAddressBn, onValueChange = { if (!locked) vm.updateField("permanentAddressBn", it) },
                    placeholder = { Text("স্থায়ী ঠিকানা") }, minLines = 2, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
        }

        // ── Nominee ──
        ProfileSection("👨‍👩‍👧 Nominee / Heir Information") {
            ProfileField("Nominee Photo") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            f.nomineePhotoUri != null -> AsyncImage(
                                model = f.nomineePhotoUri, contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                            )
                            f.nomineePhotoUrl.isNotEmpty() -> AsyncImage(
                                model = f.nomineePhotoUrl, contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                            )
                            else -> Text("👤", fontSize = 26.sp)
                        }
                    }
                    if (!locked) {
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(
                            onClick  = { nomineePhotoPicker.launch("image/*") },
                            enabled  = !processing,
                            shape    = RoundedCornerShape(7.dp)
                        ) { Text("📷 Upload", fontSize = 11.sp) }
                    }
                }
            }
            BilingualField("Heir Name", "heirNameEn", "heirNameBn", f, locked, vm::updateField)
            ProfileField("Relationship") {
                OutlinedTextField(value = f.heirRelation, onValueChange = { if (!locked) vm.updateField("heirRelation", it) },
                    placeholder = { Text("e.g. Wife, Son, Father") }, singleLine = true, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            BilingualField("Husband's/Father's Name", "heirFatherHusbandEn", "heirFatherHusbandBn", f, locked, vm::updateField)
            ProfileField("NID / Birth Certificate No.") {
                OutlinedTextField(value = f.heirNID, onValueChange = { if (!locked) vm.updateField("heirNID", it) },
                    placeholder = { Text("NID or birth cert number") }, singleLine = true, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("Heir Phone") {
                OutlinedTextField(value = f.heirPhone, onValueChange = { if (!locked) vm.updateField("heirPhone", it) },
                    placeholder = { Text("+880…") }, singleLine = true, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("Heir Address (English)", fullWidth = true) {
                OutlinedTextField(value = f.heirAddressEn, onValueChange = { if (!locked) vm.updateField("heirAddressEn", it) },
                    placeholder = { Text("Heir's address (English)") }, minLines = 2, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            ProfileField("Heir Address (বাংলা)", fullWidth = true) {
                OutlinedTextField(value = f.heirAddressBn, onValueChange = { if (!locked) vm.updateField("heirAddressBn", it) },
                    placeholder = { Text("উত্তরাধিকারীর ঠিকানা") }, minLines = 2, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
        }

        // ── Document Uploads ──
        ProfileSection("📂 Document Uploads") {
            ProfileField("NID Document") {
                OutlinedButton(
                    onClick  = { filePicker.launch("*/*") },
                    enabled  = !locked && !processing,
                    shape    = RoundedCornerShape(8.dp)
                ) { Text("📎 Choose NID File", fontSize = 13.sp) }
            }
            ProfileField("Nominee NID") {
                OutlinedButton(
                    onClick  = { nomineeFilePicker.launch("*/*") },
                    enabled  = !locked && !processing,
                    shape    = RoundedCornerShape(8.dp)
                ) { Text("📎 Choose Nominee NID", fontSize = 13.sp) }
            }
            ProfileField("Other Documents", fullWidth = true) {
                OutlinedButton(
                    onClick  = { otherFilePicker.launch("*/*") },
                    enabled  = !locked && !processing,
                    shape    = RoundedCornerShape(8.dp)
                ) { Text("📎 Choose Files (multiple)", fontSize = 13.sp) }
                Text(
                    "You can select multiple files at once.",
                    fontSize = 11.sp,
                    color    = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (processing) {
                ProfileField("", fullWidth = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color       = Color(0xFF2563EB)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Uploading to Drive, please wait…",
                            fontSize = 12.sp,
                            color    = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // ── Uploaded files viewer ──
        MemberFileViewer(
            files       = legalFiles,
            activeTab   = activeTab,
            onTabChange = { vm.setFileTab(it) }
        )

        // ── Bottom save button ──
        if (!locked) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (processing) {
                    Text(
                        "⏳ Waiting for upload…",
                        fontSize = 12.sp,
                        color    = Color(0xFF64748B),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                Button(
                    onClick  = { vm.saveProfile() },
                    enabled  = !saving && !processing,
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.height(50.dp)
                ) {
                    if (saving)
                        CircularProgressIndicator(
                            color       = Color.White,
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    else
                        Text("Submit Profile", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Member File Viewer with document preview ───────────────────────────────────

@Composable
fun MemberFileViewer(
    files:       List<LegalFile>,
    activeTab:   String,
    onTabChange: (String) -> Unit
) {
    var previewFile by remember { mutableStateOf<LegalFile?>(null) }

    val adminFiles  = files.filter { it.uploadedBy == "admin" }
    val memberFiles = files.filter { it.uploadedBy != "admin" }
    val filtered    = when (activeTab) {
        "admin"  -> adminFiles
        "member" -> memberFiles
        else     -> files
    }

    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border   = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column {

            // Header
            Row(
                Modifier.fillMaxWidth().padding(12.dp, 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("📁 My Documents", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                Text(
                    "${files.size} file${if (files.size != 1) "s" else ""}",
                    fontSize = 11.sp,
                    color    = Color(0xFF64748B)
                )
            }

            HorizontalDivider()

            // Tabs
            Row(Modifier.fillMaxWidth()) {
                listOf(
                    "all"    to "All (${files.size})",
                    "admin"  to "From Admin (${adminFiles.size})",
                    "member" to "My Uploads (${memberFiles.size})"
                ).forEach { (key, label) ->
                    val sel = activeTab == key
                    Surface(
                        onClick  = { onTabChange(key) },
                        color    = if (sel) Color.White else Color(0xFFF8FAFC),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .then(
                                    if (sel) Modifier.border(
                                        width  = 0.dp,
                                        color  = Color.Transparent,
                                        shape  = RoundedCornerShape(0.dp)
                                    ) else Modifier
                                )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    label,
                                    fontSize   = 11.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (sel) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                                if (sel) {
                                    Spacer(Modifier.height(3.dp))
                                    Surface(
                                        Modifier.height(2.dp).fillMaxWidth(0.5f),
                                        color = Color(0xFF0F172A)
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            if (filtered.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No documents in this category.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            } else {
                filtered.forEachIndexed { i, file ->
                    val isAdmin = file.uploadedBy == "admin"
                    val fileUrl = file.url

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (i % 2 == 0) Color.White else Color(0xFFFAFAFA))
                            .padding(10.dp, 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            fileIcon(file.mimeType),
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 10.dp, top = 2.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                file.title.ifEmpty { file.name },
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF0F172A),
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                            Row(
                                Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                if (file.category.isNotEmpty()) {
                                    Surface(
                                        color = if (isAdmin) Color(0xFFEFF6FF) else Color(0xFFF0FDF4),
                                        shape = RoundedCornerShape(99.dp)
                                    ) {
                                        Text(
                                            file.category,
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = if (isAdmin) Color(0xFF1D4ED8) else Color(0xFF15803D),
                                            modifier   = Modifier.padding(7.dp, 2.dp)
                                        )
                                    }
                                }
                                Surface(
                                    color = if (isAdmin) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(99.dp)
                                ) {
                                    Text(
                                        if (isAdmin) "👤 From Admin" else "🧑 My Upload",
                                        fontSize = 10.sp,
                                        color    = if (isAdmin) Color(0xFF92400E) else Color(0xFF64748B),
                                        modifier = Modifier.padding(7.dp, 2.dp)
                                    )
                                }
                                if (file.uploadedAt.isNotEmpty()) {
                                    Text(
                                        file.uploadedAt.take(10),
                                        fontSize = 10.sp,
                                        color    = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        // Preview button (mirrors Next.js "👁 Preview" button)
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick  = { previewFile = file },
                            color    = Color(0xFFEFF6FF),
                            shape    = RoundedCornerShape(6.dp),
                            border   = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                "👁 Preview",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF1D4ED8),
                                modifier   = Modifier.padding(8.dp, 5.dp)
                            )
                        }
                    }

                    if (i < filtered.lastIndex) HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }

    // Document preview modal
    previewFile?.let { file ->
        DocumentPreviewModal(file = file, onClose = { previewFile = null })
    }
}

// ── Layout composables ────────────────────────────────────────────────────────

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border   = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column {
            Surface(
                color    = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    color      = Color(0xFF0F172A),
                    modifier   = Modifier.padding(12.dp, 11.dp)
                )
            }
            HorizontalDivider()
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
fun ProfileField(label: String, fullWidth: Boolean = false, content: @Composable () -> Unit) {
    Column {
        if (label.isNotEmpty()) {
            Text(
                label.uppercase(),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFF64748B),
                letterSpacing = 0.06.sp,
                modifier      = Modifier.padding(bottom = 5.dp)
            )
        }
        content()
    }
}

@Composable
fun BilingualField(
    label:    String,
    keyEn:    String,
    keyBn:    String,
    form:     ProfileForm,
    locked:   Boolean,
    onUpdate: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileField("$label (English)") {
            OutlinedTextField(
                value         = form.getField(keyEn),
                onValueChange = { if (!locked) onUpdate(keyEn, it) },
                placeholder   = { Text("$label (English)") },
                singleLine    = true,
                enabled       = !locked,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(8.dp)
            )
        }
        ProfileField("$label (বাংলা)") {
            OutlinedTextField(
                value         = form.getField(keyBn),
                onValueChange = { if (!locked) onUpdate(keyBn, it) },
                placeholder   = { Text("$label (বাংলা)") },
                singleLine    = true,
                enabled       = !locked,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    options:  List<String>,
    selected: String,
    enabled:  Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded         = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value         = selected.ifEmpty { "Select…" },
            onValueChange = {},
            readOnly      = true,
            enabled       = enabled,
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            shape         = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text    = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

fun fileIcon(mime: String): String = when {
    mime.startsWith("image/")                           -> "🖼️"
    mime.contains("pdf")                                -> "📕"
    mime.contains("word") || mime.contains("document") -> "📝"
    else                                                -> "📄"
}

fun ProfileForm.getField(key: String): String = when (key) {
    "nameEnglish"         -> nameEnglish
    "nameBengali"         -> nameBengali
    "fatherNameEn"        -> fatherNameEn
    "fatherNameBn"        -> fatherNameBn
    "motherNameEn"        -> motherNameEn
    "motherNameBn"        -> motherNameBn
    "spouseNameEn"        -> spouseNameEn
    "spouseNameBn"        -> spouseNameBn
    "heirNameEn"          -> heirNameEn
    "heirNameBn"          -> heirNameBn
    "heirFatherHusbandEn" -> heirFatherHusbandEn
    "heirFatherHusbandBn" -> heirFatherHusbandBn
    "heirAddressEn"       -> heirAddressEn
    "heirAddressBn"       -> heirAddressBn
    else                  -> ""
}