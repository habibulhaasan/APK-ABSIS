// ui/profile/ProfileScreen.kt
package com.absis.capitalsync.ui.profile
import androidx.compose.material3.ExperimentalMaterial3Api
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

// ── Constants ─────────────────────────────────────────────────────────────────
val BLOOD_GROUPS    = listOf("A+","A-","B+","B-","AB+","AB-","O+","O-")
val MARITAL_STATUS  = listOf("Single","Married","Divorced","Widowed")
val EDUCATION_OPTS  = listOf("No Formal Education","Primary","Secondary (SSC)",
    "Higher Secondary (HSC)","Diploma","Bachelor's","Master's","PhD","Other")

// ── Data models ───────────────────────────────────────────────────────────────
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
    val photoUri: Uri? = null,       // local selected photo
    val photoUrl: String = "",       // existing remote URL
    val nomineePhotoUri: Uri? = null,
    val nomineePhotoUrl: String = "",
    val idNo: String = "", val joiningDate: String = ""
)

// ── Main Screen ───────────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(vm: ProfileViewModel = hiltViewModel()) {
    val form         by vm.form.collectAsState()
    val legalFiles   by vm.legalFiles.collectAsState()
    val locked       by vm.profileLocked.collectAsState()
    val saving       by vm.saving.collectAsState()
    val processing   by vm.processing.collectAsState()
    val toast        by vm.toast.collectAsState()
    val lastUpdated  by vm.lastUpdated.collectAsState()
    val memberInfo   by vm.memberInfo.collectAsState()
    val activeTab    by vm.fileTab.collectAsState()

    // Photo pickers
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.onPhotoSelected(it, false) }
    }
    val nomineePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.onPhotoSelected(it, true) }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.uploadFile(it, "nid") }
    }
    val nomineeFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.uploadFile(it, "nomineeNid") }
    }
    val otherFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { vm.uploadFile(it, "other") }
    }

    if (form == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val f = form!!

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

        // ── Header ──
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("My Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(
                    if (locked) "⚠️ Profile submitted. Contact admin to make changes."
                    else "Fill in all details carefully. You can submit only once.",
                    fontSize = 13.sp, color = Color(0xFF64748B)
                )
            }
            if (!locked) {
                Button(
                    onClick = { vm.saveProfile() },
                    enabled = !saving && !processing,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (saving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Submit Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // ── Toast ──
        toast?.let { t ->
            Surface(
                color = if (t.isError) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(t.message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (t.isError) Color(0xFFB91C1C) else Color(0xFF15803D),
                    modifier = Modifier.padding(10.dp))
            }
        }

        // ── Last updated banner ──
        lastUpdated?.let {
            Surface(color = Color(0xFFF0F9FF), shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("✏️ Last updated: $it${if (locked) " — Locked. Contact admin to edit." else ""}",
                    fontSize = 12.sp, color = Color(0xFF1E40AF), modifier = Modifier.padding(10.dp))
            }
        }

        // ── Photo + Member ID strip ──
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                // Avatar
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDBEAFE))
                            .border(3.dp, Color(0xFFBFDBFE), CircleShape)
                            .then(if (!locked) Modifier.clickable { photoPicker.launch("image/*") } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            f.photoUri != null -> AsyncImage(model = f.photoUri, contentDescription = "Photo",
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            f.photoUrl.isNotEmpty() -> AsyncImage(model = f.photoUrl, contentDescription = "Photo",
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else -> Text(f.nameEnglish.firstOrNull()?.toString()?.uppercase() ?: "?",
                                fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                        }
                    }
                    if (!locked) {
                        TextButton(onClick = { photoPicker.launch("image/*") }, enabled = !processing) {
                            Text("📷 Photo", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.width(20.dp))

                // Quick info grid
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Member ID"    to (memberInfo?.get("idNo") as? String ?: "—"),
                        "Joining Date" to (memberInfo?.get("joiningDate") as? String ?: "—"),
                        "Email"        to f.email,
                        "Status"       to if (memberInfo?.get("approved") == true) "✅ Active" else "⏳ Pending"
                    ).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (label, value) ->
                                Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)) {
                                    Column(Modifier.padding(10.dp, 10.dp)) {
                                        Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8), letterSpacing = 0.06.sp)
                                        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Personal Information ──
        ProfileSection(title = "👤 Personal Information") {
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
                DropdownField(options = BLOOD_GROUPS, selected = f.bloodGroup,
                    enabled = !locked, onSelect = { vm.updateField("bloodGroup", it) })
            }
            ProfileField("Marital Status") {
                DropdownField(options = MARITAL_STATUS, selected = f.maritalStatus,
                    enabled = !locked, onSelect = { vm.updateField("maritalStatus", it) })
            }
            BilingualField("Spouse Name", "spouseNameEn", "spouseNameBn", f, locked, vm::updateField)
            ProfileField("Education") {
                DropdownField(options = EDUCATION_OPTS, selected = f.education,
                    enabled = !locked, onSelect = { vm.updateField("education", it) })
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

        // ── Address Information ──
        ProfileSection(title = "📍 Address Information") {
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

        // ── Nominee / Heir Information ──
        ProfileSection(title = "👨‍👩‍👧 Nominee / Heir Information") {
            // Nominee photo
            ProfileField("Nominee Photo") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            f.nomineePhotoUri != null -> AsyncImage(model = f.nomineePhotoUri, contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            f.nomineePhotoUrl.isNotEmpty() -> AsyncImage(model = f.nomineePhotoUrl, contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else -> Text("👤", fontSize = 22.sp)
                        }
                    }
                    if (!locked) {
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(onClick = { nomineePhotoPicker.launch("image/*") }, enabled = !processing,
                            shape = RoundedCornerShape(7.dp)) {
                            Text("📷 Upload", fontSize = 11.sp)
                        }
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
        ProfileSection(title = "📂 Document Uploads") {
            ProfileField("NID Document") {
                OutlinedButton(onClick = { filePicker.launch("*/*") }, enabled = !locked && !processing,
                    shape = RoundedCornerShape(8.dp)) { Text("📎 Choose NID File", fontSize = 13.sp) }
            }
            ProfileField("Nominee NID") {
                OutlinedButton(onClick = { nomineeFilePicker.launch("*/*") }, enabled = !locked && !processing,
                    shape = RoundedCornerShape(8.dp)) { Text("📎 Choose Nominee NID", fontSize = 13.sp) }
            }
            ProfileField("Other Documents", fullWidth = true) {
                OutlinedButton(onClick = { otherFilePicker.launch("*/*") }, enabled = !locked && !processing,
                    shape = RoundedCornerShape(8.dp)) { Text("📎 Choose Files (multiple)", fontSize = 13.sp) }
                Text("You can select multiple files at once.", fontSize = 11.sp, color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp))
            }
            if (processing) {
                ProfileField("", fullWidth = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Uploading to Storage, please wait…", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }

        // ── Uploaded Files Viewer ──
        MemberFileViewer(
            files     = legalFiles,
            activeTab = activeTab,
            onTabChange = { vm.setFileTab(it) }
        )

        // ── Bottom action bar ──
        if (!locked) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (processing) {
                    Text("⏳ Waiting for upload…", fontSize = 12.sp, color = Color(0xFF64748B),
                        modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp))
                }
                Button(
                    onClick = { vm.saveProfile() },
                    enabled = !saving && !processing,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.height(50.dp)
                ) {
                    if (saving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Submit Profile", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Member File Viewer ────────────────────────────────────────────────────────
@Composable
fun MemberFileViewer(files: List<LegalFile>, activeTab: String, onTabChange: (String) -> Unit) {
    val adminFiles  = files.filter { it.uploadedBy == "admin" }
    val memberFiles = files.filter { it.uploadedBy != "admin" }
    val filtered    = when (activeTab) {
        "admin"  -> adminFiles
        "member" -> memberFiles
        else     -> files
    }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column {
            // Header
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📁 My Documents", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${files.size} file${if (files.size != 1) "s" else ""}", fontSize = 11.sp, color = Color(0xFF64748B))
            }
            HorizontalDivider()

            // Tab bar
            Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC))) {
                listOf("all" to "All (${files.size})", "admin" to "From Admin (${adminFiles.size})", "member" to "My Uploads (${memberFiles.size})").forEach { (key, label) ->
                    val sel = activeTab == key
                    Surface(
                        onClick = { onTabChange(key) },
                        color = if (sel) Color.White else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(label, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) Color(0xFF0F172A) else Color(0xFF64748B))
                        }
                    }
                }
            }
            HorizontalDivider()

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No documents in this category.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            } else {
                filtered.forEachIndexed { i, file ->
                    val isAdmin = file.uploadedBy == "admin"
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (i % 2 == 0) Color.White else Color(0xFFFAFAFA))
                            .padding(10.dp, 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(fileIcon(file.mimeType), fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(file.title.ifEmpty { file.name }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (file.category.isNotEmpty()) {
                                    Surface(color = if (isAdmin) Color(0xFFEFF6FF) else Color(0xFFF0FDF4),
                                        shape = RoundedCornerShape(99.dp)) {
                                        Text(file.category, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                            color = if (isAdmin) Color(0xFF1D4ED8) else Color(0xFF15803D),
                                            modifier = Modifier.padding(7.dp, 2.dp))
                                    }
                                }
                                Surface(color = if (isAdmin) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(99.dp)) {
                                    Text(if (isAdmin) "👤 From Admin" else "🧑 My Upload",
                                        fontSize = 10.sp, color = if (isAdmin) Color(0xFF92400E) else Color(0xFF64748B),
                                        modifier = Modifier.padding(7.dp, 2.dp))
                                }
                            }
                        }
                    }
                    if (i < filtered.lastIndex) HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

// ── Reusable layout composables ───────────────────────────────────────────────

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column {
            Surface(color = Color(0xFFF8FAFC), modifier = Modifier.fillMaxWidth()) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A),
                    modifier = Modifier.padding(12.dp, 11.dp))
            }
            HorizontalDivider()
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun ProfileField(label: String, fullWidth: Boolean = false, content: @Composable () -> Unit) {
    Column {
        if (label.isNotEmpty()) {
            Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B), letterSpacing = 0.06.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
        content()
    }
}

@Composable
fun BilingualField(
    label: String,
    keyEn: String, keyBn: String,
    form: ProfileForm, locked: Boolean,
    onUpdate: (String, String) -> Unit
) {
    val enVal = form.getField(keyEn)
    val bnVal = form.getField(keyBn)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileField("$label (English)") {
            OutlinedTextField(value = enVal, onValueChange = { if (!locked) onUpdate(keyEn, it) },
                placeholder = { Text("$label (English)") }, singleLine = true, enabled = !locked,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
        }
        ProfileField("$label (বাংলা)") {
            OutlinedTextField(value = bnVal, onValueChange = { if (!locked) onUpdate(keyBn, it) },
                placeholder = { Text("$label (বাংলা)") }, singleLine = true, enabled = !locked,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
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
        expanded        = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value          = selected.ifEmpty { "Select…" },
            onValueChange  = {},
            readOnly       = true,
            enabled        = enabled,
            trailingIcon   = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier       = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape          = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(
            expanded        = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text    = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
fun fileIcon(mime: String): String = when {
    mime.startsWith("image/")                             -> "🖼️"
    mime.contains("pdf")                                  -> "📕"
    mime.contains("word") || mime.contains("document")   -> "📝"
    else                                                  -> "📄"
}

// Reflection-free field getter via when
fun ProfileForm.getField(key: String): String = when (key) {
    "nameEnglish"        -> nameEnglish
    "nameBengali"        -> nameBengali
    "fatherNameEn"       -> fatherNameEn
    "fatherNameBn"       -> fatherNameBn
    "motherNameEn"       -> motherNameEn
    "motherNameBn"       -> motherNameBn
    "spouseNameEn"       -> spouseNameEn
    "spouseNameBn"       -> spouseNameBn
    "heirNameEn"         -> heirNameEn
    "heirNameBn"         -> heirNameBn
    "heirFatherHusbandEn"-> heirFatherHusbandEn
    "heirFatherHusbandBn"-> heirFatherHusbandBn
    "heirAddressEn"      -> heirAddressEn
    "heirAddressBn"      -> heirAddressBn
    else -> ""
}