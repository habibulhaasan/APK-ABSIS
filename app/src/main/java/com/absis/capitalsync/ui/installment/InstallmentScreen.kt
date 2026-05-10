// ui/installment/InstallmentScreen.kt
package com.absis.capitalsync.ui.installment

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

fun fmtAmt(n: Number) = "৳${NumberFormat.getNumberInstance(Locale.US).format(n.toLong())}"

// ── Data models (unchanged) ───────────────────────────────────────────────────

data class SpecialSub(
    val id: String, val title: String, val description: String,
    val amount: Double, val deadline: String, val daysLeft: Int,
    val type: String, val allowCustomAmount: Boolean, val active: Boolean
)

data class PaymentAccount(
    val id: String, val label: String, val number: String, val enabled: Boolean
)

enum class PayMode { MONTHLY, SPECIAL }

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun InstallmentScreen(
    onNavigateToLedger: () -> Unit,
    vm: InstallmentViewModel = hiltViewModel()
) {
    val uiState      by vm.uiState.collectAsState()
    val specialSubs  by vm.specialSubs.collectAsState()
    val unpaidMonths by vm.unpaidMonths.collectAsState()
    val context       = LocalContext.current
    val snackbarHost  = remember { SnackbarHostState() }

    // Show error snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHost.showSnackbar(it)
            vm.dismissError()
        }
    }

    if (uiState.success) {
        SuccessScreen(
            onPayAgain   = { vm.resetSuccess() },
            onViewLedger = onNavigateToLedger
        )
        return
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // ── Header ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(bottom = 16.dp)
            ) {
                uiState.orgLogoUrl?.let {
                    Surface(
                        Modifier.size(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE2E8F0)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🏦", fontSize = 20.sp)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                }
                Column {
                    Text(
                        "Pay Installment",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF0F172A)
                    )
                    Text(
                        "${uiState.orgName}${if (uiState.monthlyEnabled) " · Monthly: ${fmtAmt(uiState.baseAmount)}" else ""}",
                        fontSize = 14.sp,
                        color    = Color(0xFF64748B)
                    )
                }
            }

            // ── Payments paused ──
            if (!uiState.hasAnything) {
                Card(
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(48.dp, 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⏸️", fontSize = 32.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Payments paused", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Monthly installments are currently disabled.",
                            fontSize = 13.sp,
                            color    = Color(0xFF94A3B8)
                        )
                    }
                }
                return@Column
            }

            // ── Banners ──
            if (uiState.isImpersonating) {
                InfoBanner(
                    bg    = Color(0xFFFEF3C7),
                    border= Color(0xFFFDE68A),
                    text  = "👤 Viewing as member — Payment submission is disabled."
                )
            }
            if (uiState.isLatePayer) {
                InfoBanner(
                    bg         = Color(0xFFFEF2F2),
                    border     = Color(0xFFFCA5A5),
                    title      = "⚠️ You are marked as a Late Payer",
                    text       = "You have ${uiState.missedCount} unpaid month${if (uiState.missedCount != 1) "s" else ""}. Please pay overdue installments.",
                    titleColor = Color(0xFFB91C1C),
                    textColor  = Color(0xFF7F1D1D)
                )
            }
            if (uiState.reregistrationPending) {
                InfoBanner(
                    bg         = Color(0xFFFEF3C7),
                    border     = Color(0xFFFDE68A),
                    title      = "🔄 Re-Registration Fee Required",
                    text       = "Due to ${uiState.missedCount} unpaid months, a re-registration fee has been assigned. Contact admin if this is an error.",
                    titleColor = Color(0xFF92400E),
                    textColor  = Color(0xFF78350F)
                )
            }
            if (uiState.reregistrationGranted) {
                InfoBanner(
                    bg        = Color(0xFFF0FDF4),
                    border    = Color(0xFF86EFAC),
                    text      = "✅ Re-registration fee waived — your admin has granted a rebate.",
                    textColor = Color(0xFF15803D)
                )
            }

            // ── Payment accounts reference card ──
            if (uiState.paymentAccounts.isNotEmpty()) {
                Card(
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFAAD1F5)),
                    border   = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "📋 Payment Instructions",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                            color      = Color(0xFF475569),
                            modifier   = Modifier.padding(bottom = 12.dp)
                        )
                        uiState.paymentAccounts.forEach { (method, accounts) ->
                            if (method == "Cash") {
                                Surface(
                                    shape    = RoundedCornerShape(10.dp),
                                    color    = Color.White,
                                    border   = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Surface(
                                            color = Color(0xFF15803D),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "CASH",
                                                fontSize   = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color      = Color.White,
                                                modifier   = Modifier.padding(8.dp, 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Hand-to-Hand",
                                            fontSize   = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = Color(0xFF0F172A)
                                        )
                                        Text(
                                            "Pay directly to admin",
                                            fontSize = 11.sp,
                                            color    = Color(0xFF64748B)
                                        )
                                    }
                                }
                            } else {
                                accounts.filter { it.enabled }.forEach { acc ->
                                    Surface(
                                        shape    = RoundedCornerShape(10.dp),
                                        color    = Color.White,
                                        border   = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Surface(
                                                color = Color(0xFF2563EB),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    method.uppercase(),
                                                    fontSize   = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color      = Color.White,
                                                    modifier   = Modifier.padding(8.dp, 2.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                acc.label,
                                                fontSize      = 11.sp,
                                                fontWeight    = FontWeight.SemiBold,
                                                color         = Color(0xFF64748B),
                                                letterSpacing = 0.025.sp
                                            )
                                            Text(
                                                acc.number,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontSize   = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color      = Color(0xFF0F172A)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Mode switcher ──
            if (uiState.monthlyEnabled && uiState.hasSpecialSubs) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeTab(
                        "📅 Monthly Installment",
                        uiState.payMode == PayMode.MONTHLY,
                        Modifier.weight(1f)
                    ) { vm.setPayMode(PayMode.MONTHLY) }
                    ModeTab(
                        "🎯 Special Subscription",
                        uiState.payMode == PayMode.SPECIAL,
                        Modifier.weight(1f)
                    ) { vm.setPayMode(PayMode.SPECIAL) }
                }
            }

            // ── Month picker ──
            if (uiState.payMode == PayMode.MONTHLY && uiState.monthlyEnabled) {
                MonthPickerCard(
                    unpaidMonths = unpaidMonths,
                    selected     = uiState.selectedMonths,
                    penalty      = uiState.penalty,
                    onToggle     = { vm.toggleMonth(it) }
                )
            }

            // ── Special subs ──
            if (uiState.payMode == PayMode.SPECIAL ||
                (!uiState.monthlyEnabled && specialSubs.isNotEmpty())) {
                SpecialSubsCard(
                    subs           = specialSubs,
                    paidSpecial    = uiState.paidSpecialIds,
                    selectedSub    = uiState.selectedSpecial,
                    customAmount   = uiState.customSpecialAmount,
                    onSelect       = { vm.selectSpecialSub(it) },
                    onCustomAmount = { vm.setCustomAmount(it) }
                )
            }

            // ── Payment method card ──
            val showPayment = (uiState.payMode == PayMode.MONTHLY && uiState.selectedMonths.isNotEmpty()) ||
                              (uiState.payMode == PayMode.SPECIAL && uiState.selectedSpecial != null)
            if (showPayment) {
                PaymentMethodCard(
                    enabledMethods  = uiState.enabledMethods,
                    selectedMethod  = uiState.selectedMethod,
                    methodAccounts  = uiState.currentMethodAccounts,
                    selectedAccount = uiState.selectedAccount,
                    txId            = uiState.txId,
                    receiptUri      = uiState.receiptUri,
                    receiptName     = uiState.receiptName,
                    totalBase       = uiState.totalBase,
                    totalPenalty    = uiState.totalPenalty,
                    fee             = uiState.fee,
                    grandTotal      = uiState.grandTotal,
                    feeRate         = uiState.feeRate,
                    selectedMonths  = uiState.selectedMonths,
                    baseAmount      = uiState.baseAmount,
                    selectedSub     = uiState.selectedSpecial,
                    payMode         = uiState.payMode,
                    loading         = uiState.loading,
                    isUploadingReceipt = uiState.isUploadingReceipt,
                    isImpersonating = uiState.isImpersonating,
                    onSelectMethod  = { vm.selectMethod(it) },
                    onSelectAccount = { vm.selectAccount(it) },
                    onTxIdChange    = { vm.setTxId(it) },
                    onReceiptPicked = { uri, name, mime -> vm.setReceiptUri(uri, name, mime) },
                    onReceiptCleared = { vm.clearReceipt() },
                    onSubmit        = { vm.submitPayment(context) }
                )
            }
        }
    }
}

// ── Receipt Picker Section ────────────────────────────────────────────────────

@Composable
fun ReceiptPickerSection(
    receiptUri:      Uri?,
    receiptName:     String,
    txId:            String,
    onReceiptPicked: (Uri, String, String) -> Unit,
    onReceiptCleared: () -> Unit,
) {
    val context = LocalContext.current

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Resolve name and MIME type
        var name     = uri.lastPathSegment ?: "receipt.jpg"
        var mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
            }
        }
        onReceiptPicked(uri, name, mimeType)
    }

    Column {
        Text(
            "Payment Receipt (Upload screenshot)",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF374151),
            modifier   = Modifier.padding(bottom = 6.dp)
        )

        if (receiptUri != null) {
            // ── Confirmed state ──
            Surface(
                shape    = RoundedCornerShape(8.dp),
                color    = Color(0xFFF0FDF4),
                border   = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(10.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail
                    Surface(
                        shape  = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFD1FAE5)),
                        modifier = Modifier.size(52.dp)
                    ) {
                        AsyncImage(
                            model             = receiptUri,
                            contentDescription = "Receipt preview",
                            contentScale      = ContentScale.Crop,
                            modifier          = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "✓ Receipt selected",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF15803D)
                        )
                        Text(
                            receiptName.ifBlank { "receipt" },
                            fontSize  = 11.sp,
                            color     = Color(0xFF64748B),
                            maxLines  = 1,
                            overflow  = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = onReceiptCleared,
                        shape   = RoundedCornerShape(6.dp),
                        color   = Color(0xFFFEE2E2)
                    ) {
                        Text(
                            "Remove",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFFB91C1C),
                            modifier   = Modifier.padding(10.dp, 4.dp)
                        )
                    }
                }
            }
        } else {
            // ── Upload button ──
            Surface(
                onClick  = { launcher.launch("image/*") },
                shape    = RoundedCornerShape(8.dp),
                color    = Color(0xFFF8FAFC),
                border   = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier.padding(vertical = 16.dp)
                ) {
                    Text(
                        "📎  Tap to upload receipt image",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color(0xFF64748B)
                    )
                }
            }
        }

        // Validation hint — shown when neither txId nor receipt provided
        if (txId.isBlank() && receiptUri == null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "⚠️ Please upload Payment receipt or Transaction ID — at least one is required.",
                fontSize = 11.sp,
                color    = Color(0xFFDC2626)
            )
        }
    }
}

// ── Success Screen ────────────────────────────────────────────────────────────

@Composable
fun SuccessScreen(onPayAgain: () -> Unit, onViewLedger: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color    = Color(0xFFDCFCE7),
                    shape    = RoundedCornerShape(50.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", fontSize = 24.sp, color = Color(0xFF15803D))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Payment Submitted!",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = Color(0xFF0F172A)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your payment has been recorded. An admin will verify it shortly.",
                    fontSize  = 13.sp,
                    color     = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPayAgain,
                        shape   = RoundedCornerShape(8.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) { Text("Pay Again") }
                    OutlinedButton(
                        onClick = onViewLedger,
                        shape   = RoundedCornerShape(8.dp)
                    ) { Text("View Ledger") }
                }
            }
        }
    }
}

// ── Month Picker Card ─────────────────────────────────────────────────────────

@Composable
fun MonthPickerCard(
    unpaidMonths: List<String>,
    selected:     Set<String>,
    penalty:      Double,
    onToggle:     (String) -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Select Months to Pay", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                "Only unpaid months are shown.",
                fontSize = 12.sp,
                color    = Color(0xFF94A3B8),
                modifier = Modifier.padding(bottom = 14.dp)
            )

            if (unpaidMonths.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Text("🎉", fontSize = 28.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("All caught up!", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("No pending months to pay.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            } else {
                unpaidMonths.chunked(2).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { month ->
                            val isSel = month in selected
                            val isLate = run {
                                val parts = month.split("-")
                                val y = parts[0].toIntOrNull() ?: 0
                                val m = parts[1].toIntOrNull() ?: 0
                                val cal = java.util.Calendar.getInstance()
                                cal.time > java.util.Calendar.getInstance()
                                    .apply { set(y, m - 1, 10) }.time
                            }
                            Surface(
                                onClick  = { onToggle(month) },
                                shape    = RoundedCornerShape(8.dp),
                                color    = if (isSel) Color(0xFFEFF6FF) else Color.White,
                                border   = BorderStroke(
                                    if (isSel) 2.dp else 1.dp,
                                    if (isSel) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(
                                        month.replace("-", " / "),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 12.sp,
                                        color      = if (isSel) Color(0xFF1D4ED8) else Color(0xFF475569)
                                    )
                                    if (isLate && penalty > 0) {
                                        Text(
                                            "+ ${fmtAmt(penalty)} late fee",
                                            fontSize = 10.sp,
                                            color    = if (isSel) Color(0xFF1D4ED8) else Color(0xFFF59E0B),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Special Subs Card ─────────────────────────────────────────────────────────

@Composable
fun SpecialSubsCard(
    subs:           List<SpecialSub>,
    paidSpecial:    Set<String>,
    selectedSub:    SpecialSub?,
    customAmount:   String,
    onSelect:       (SpecialSub?) -> Unit,
    onCustomAmount: (String) -> Unit
) {
    val unpaid = subs.filter { it.id !in paidSpecial }
    Card(
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Special Subscriptions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                "Select one to pay.",
                fontSize = 12.sp,
                color    = Color(0xFF94A3B8),
                modifier = Modifier.padding(bottom = 14.dp)
            )

            if (unpaid.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Text("✅", fontSize = 28.sp)
                    Text(
                        "No pending special subscriptions.",
                        fontSize = 13.sp,
                        color    = Color(0xFF94A3B8)
                    )
                }
            } else {
                unpaid.forEach { sub ->
                    val isSel = selectedSub?.id == sub.id
                    Surface(
                        onClick  = { onSelect(if (isSel) null else sub) },
                        shape    = RoundedCornerShape(10.dp),
                        color    = Color.White,
                        border   = BorderStroke(
                            if (isSel) 2.dp else 1.dp,
                            if (isSel) Color(0xFF2563EB) else Color(0xFFB9BCC2)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.Top
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        sub.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 13.sp,
                                        color      = if (isSel) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                                    )
                                    if (sub.description.isNotEmpty()) {
                                        Text(
                                            sub.description,
                                            fontSize = 12.sp,
                                            color    = Color(0xFF64748B),
                                            modifier = Modifier.padding(top = 3.dp)
                                        )
                                    }
                                    Text(
                                        "Due: ${sub.deadline} (${if (sub.daysLeft > 0) "${sub.daysLeft} day${if (sub.daysLeft != 1) "s" else ""} left" else "Today!"})",
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color      = if (sub.daysLeft <= 3) Color(0xFFDC2626) else Color(0xFFF59E0B),
                                        modifier   = Modifier.padding(top = 4.dp)
                                    )
                                    if (sub.type == "entry_fee" || sub.type == "reregistration_fee") {
                                        Spacer(Modifier.height(5.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            TypeBadge(
                                                if (sub.type == "entry_fee") "Entry Fee" else "Re-Registration Fee",
                                                Color(0xFFDBEAFE), Color(0xFF1D4ED8)
                                            )
                                            TypeBadge("→ Expenses Fund", Color(0xFFFEF3C7), Color(0xFF92400E))
                                        }
                                    }
                                    if (sub.allowCustomAmount) {
                                        Text(
                                            "✏️ Pay any amount you choose",
                                            fontSize   = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = Color(0xFF7C3AED),
                                            modifier   = Modifier.padding(top = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    if (sub.allowCustomAmount) "${fmtAmt(sub.amount)}+" else fmtAmt(sub.amount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp,
                                    color      = if (isSel) Color(0xFF1D4ED8) else Color(0xFF2563EB)
                                )
                            }

                            // Custom amount input
                            if (isSel && sub.allowCustomAmount) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color    = Color(0xFFFAF5FF),
                                    shape    = RoundedCornerShape(8.dp),
                                    border   = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            "Enter your contribution amount",
                                            fontSize   = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = Color(0xFF7C3AED),
                                            modifier   = Modifier.padding(bottom = 6.dp)
                                        )
                                        OutlinedTextField(
                                            value         = customAmount,
                                            onValueChange = onCustomAmount,
                                            prefix        = {
                                                Text(
                                                    "৳",
                                                    fontWeight = FontWeight.Bold,
                                                    color      = Color(0xFF7C3AED)
                                                )
                                            },
                                            placeholder   = { Text("Suggested: ${fmtAmt(sub.amount)}") },
                                            singleLine    = true,
                                            modifier      = Modifier.fillMaxWidth(),
                                            shape         = RoundedCornerShape(8.dp)
                                        )
                                        Text(
                                            "Suggested: ${fmtAmt(sub.amount)}. You can pay more or less.",
                                            fontSize = 11.sp,
                                            color    = Color(0xFF7C3AED),
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Payment Method Card ───────────────────────────────────────────────────────

@Composable
fun PaymentMethodCard(
    enabledMethods:    List<String>,
    selectedMethod:    String,
    methodAccounts:    List<PaymentAccount>,
    selectedAccount:   PaymentAccount?,
    txId:              String,
    receiptUri:        Uri?,
    receiptName:       String,
    totalBase:         Double,
    totalPenalty:      Double,
    fee:               Double,
    grandTotal:        Double,
    feeRate:           Double,
    selectedMonths:    Set<String>,
    baseAmount:        Double,
    selectedSub:       SpecialSub?,
    payMode:           PayMode,
    loading:           Boolean,
    isUploadingReceipt: Boolean,
    isImpersonating:   Boolean,
    onSelectMethod:    (String) -> Unit,
    onSelectAccount:   (PaymentAccount?) -> Unit,
    onTxIdChange:      (String) -> Unit,
    onReceiptPicked:   (Uri, String, String) -> Unit,
    onReceiptCleared:  () -> Unit,
    onSubmit:          () -> Unit,
) {
    val needsAccountPick = methodAccounts.size > 1 && selectedAccount == null

    Card(
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Payment Method",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                modifier   = Modifier.padding(bottom = 14.dp)
            )

            // Method chips
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.padding(bottom = 16.dp)
            ) {
                items(enabledMethods.size) { i ->
                    val m   = enabledMethods[i]
                    val sel = m == selectedMethod
                    Surface(
                        onClick  = { onSelectMethod(m) },
                        shape    = RoundedCornerShape(8.dp),
                        color    = if (sel) Color(0xFFEFF6FF) else Color.White,
                        border   = BorderStroke(
                            if (sel) 2.dp else 1.dp,
                            if (sel) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Text(
                            m,
                            modifier   = Modifier.padding(18.dp, 9.dp),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = if (sel) Color(0xFF1D4ED8) else Color(0xFF475569)
                        )
                    }
                }
            }

            // Multi-account picker
            if (selectedMethod != "Cash" && methodAccounts.size > 1) {
                Text(
                    "Which account did you send to? *",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(bottom = 8.dp)
                )
                methodAccounts.forEach { acc ->
                    val isSel = selectedAccount?.id == acc.id
                    Surface(
                        onClick  = { onSelectAccount(if (isSel) null else acc) },
                        shape    = RoundedCornerShape(8.dp),
                        color    = if (isSel) Color(0xFFEFF6FF) else Color(0xFFFAFAFA),
                        border   = BorderStroke(
                            if (isSel) 2.dp else 1.dp,
                            if (isSel) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSel,
                                onClick  = { onSelectAccount(if (isSel) null else acc) },
                                colors   = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    acc.label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 13.sp,
                                    color      = if (isSel) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                                )
                                Text(
                                    acc.number,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize   = 12.sp,
                                    color      = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Single account info
            if (selectedMethod != "Cash" && methodAccounts.size == 1) {
                Surface(
                    color    = Color(0xFFF0FDF4),
                    shape    = RoundedCornerShape(8.dp),
                    border   = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) {
                    Column(Modifier.padding(12.dp, 10.dp)) {
                        Text(
                            "Send to:",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF15803D)
                        )
                        Text(
                            methodAccounts[0].number,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFF0F172A)
                        )
                        if (methodAccounts[0].label.isNotEmpty()) {
                            Text(
                                methodAccounts[0].label,
                                fontSize = 11.sp,
                                color    = Color(0xFF424B57)
                            )
                        }
                    }
                }
            }

            // Cash note
            if (selectedMethod == "Cash") {
                Surface(
                    color    = Color(0xFFEFF6FF),
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) {
                    Text(
                        "Pay cash directly to your organization admin.",
                        fontSize = 13.sp,
                        color    = Color(0xFF1E40AF),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ── Receipt picker (non-Cash only) ──
            if (selectedMethod.isNotEmpty() && selectedMethod != "Cash") {
                Spacer(Modifier.height(4.dp))
                ReceiptPickerSection(
                    receiptUri       = receiptUri,
                    receiptName      = receiptName,
                    txId             = txId,
                    onReceiptPicked  = onReceiptPicked,
                    onReceiptCleared = onReceiptCleared,
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── TxID (optional when receipt present) ──
            if (selectedMethod.isNotEmpty() && selectedMethod != "Cash") {
                Text(
                    buildString {
                        append("Transaction ID (TxID)")
                        if (receiptUri != null) append("  (optional — receipt uploaded)")
                    },
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (receiptUri != null) Color(0xFF64748B) else Color(0xFF374151),
                    modifier   = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value         = txId,
                    onValueChange = onTxIdChange,
                    placeholder   = { Text("Paste your $selectedMethod transaction ID") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape         = RoundedCornerShape(8.dp)
                )
            }

            // ── Payment Summary ──
            Surface(
                color    = Color(0xFFF8FAFC),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "PAYMENT SUMMARY",
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = Color(0xFF64748B),
                        letterSpacing = 0.06.sp,
                        modifier      = Modifier.padding(bottom = 10.dp)
                    )
                    if (payMode == PayMode.MONTHLY) {
                        SummaryRow(
                            label = "Installment (${selectedMonths.size} month${if (selectedMonths.size > 1) "s" else ""} × ${fmtAmt(baseAmount)})",
                            value = fmtAmt(totalBase)
                        )
                    } else {
                        SummaryRow(
                            label = selectedSub?.title ?: "",
                            value = fmtAmt(totalBase)
                        )
                    }
                    if (totalPenalty > 0) {
                        SummaryRow(
                            label      = "Late fee",
                            value      = fmtAmt(totalPenalty),
                            valueColor = Color(0xFFDC2626)
                        )
                    }
                    if (fee > 0) {
                        SummaryRow(
                            label = "Gateway fee (${"%.2f".format(feeRate * 100)}%)",
                            value = fmtAmt(fee)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total to send", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            fmtAmt(grandTotal),
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF2563EB)
                        )
                    }
                }
            }

            // ── Submit button ──
            Button(
                onClick  = onSubmit,
                enabled  = !loading && !needsAccountPick && !isImpersonating,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                if (loading) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            if (isUploadingReceipt) "Uploading receipt…" else "Submitting…",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp
                        )
                    }
                } else {
                    Text(
                        when {
                            isImpersonating  -> "Disabled in impersonation mode"
                            needsAccountPick -> "Select account above first"
                            else             -> "Submit Payment — ${fmtAmt(grandTotal)}"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp
                    )
                }
            }
        }
    }
}

// ── Small reusable composables ────────────────────────────────────────────────

@Composable
fun ModeTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(8.dp),
        modifier = modifier,
        color    = if (selected) Color(0xFFEFF6FF) else Color.White,
        border   = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
            Text(
                label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (selected) Color(0xFF1D4ED8) else Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun TypeBadge(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(99.dp)) {
        Text(
            text,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            color      = fg,
            modifier   = Modifier.padding(8.dp, 2.dp)
        )
    }
}

@Composable
fun SummaryRow(label: String, value: String, valueColor: Color = Color(0xFF0F172A)) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun InfoBanner(
    bg:         Color,
    border:     Color,
    text:       String,
    title:      String      = "",
    titleColor: Color       = Color(0xFF0F172A),
    textColor:  Color       = Color(0xFF475569)
) {
    Surface(
        color    = bg,
        shape    = RoundedCornerShape(8.dp),
        border   = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (title.isNotEmpty()) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color      = titleColor,
                    fontSize   = 13.sp,
                    modifier   = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text,
                color    = textColor,
                fontSize = if (title.isNotEmpty()) 12.sp else 13.sp
            )
        }
    }
}