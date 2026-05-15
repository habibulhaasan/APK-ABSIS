// ui/installment/InstallmentScreen.kt
@file:Suppress("SpellCheckingInspection")

package com.absis.capitalsync.ui.installment

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.absis.capitalsync.R

fun fmtAmt(n: Number) = "৳${NumberFormat.getNumberInstance(Locale.US).format(n.toLong())}"

// ── Data models ───────────────────────────────────────────────────

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentScreen(
    onNavigateToLedger: () -> Unit,
    vm: InstallmentViewModel = hiltViewModel()
) {
    val uiState      by vm.uiState.collectAsState()
    val specialSubs  by vm.specialSubs.collectAsState()
    val unpaidMonths by vm.unpaidMonths.collectAsState()
    val context       = LocalContext.current
    val snackBarHost  = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ── Pull-to-Refresh State ──
    var isRefreshing by remember { mutableStateOf(false) }

    // Show error snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackBarHost.showSnackbar(it)
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHost) },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        // PullToRefreshBox handles all the nested scrolling and refreshing UI
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    vm.refresh() // Refetches data. Paid months will automatically disappear!
                    delay(1000)  // Minimum UI delay for visual feedback
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // ── Header ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(bottom = 24.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_round),
                        contentDescription = "App Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Pay Installment",
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFF0F172A)
                        )
                        Text(
                            "${uiState.orgName}${if (uiState.monthlyEnabled) " · Monthly: ${fmtAmt(uiState.baseAmount)}" else ""}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color    = Color(0xFF64748B)
                        )
                    }
                }

                // ── Payments paused ──
                if (!uiState.hasAnything) {
                    Card(
                        shape    = RoundedCornerShape(16.dp),
                        colors   = CardDefaults.cardColors(containerColor = Color.White),
                        border   = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(48.dp, 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⏸️", fontSize = 36.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("Payments paused", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Monthly installments are currently disabled.",
                                fontSize = 14.sp,
                                color    = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    return@PullToRefreshBox
                }

                // ── Banners ──
                if (uiState.isImpersonating) {
                    InfoBanner(bg = Color(0xFFFEF3C7), border= Color(0xFFFDE68A), text = "👤 Viewing as member — Payment submission is disabled.")
                }
                if (uiState.isLatePayer) {
                    InfoBanner(bg = Color(0xFFFEF2F2), border = Color(0xFFFCA5A5), title = "⚠️ You are marked as a Late Payer", text = "You have ${uiState.missedCount} unpaid month${if (uiState.missedCount != 1) "s" else ""}. Please pay overdue installments.", titleColor = Color(0xFFB91C1C), textColor = Color(0xFF7F1D1D))
                }
                if (uiState.reregistrationPending) {
                    InfoBanner(bg = Color(0xFFFEF3C7), border = Color(0xFFFDE68A), title = "🔄 Re-Registration Fee Required", text = "Due to ${uiState.missedCount} unpaid months, a re-registration fee has been assigned. Contact admin if this is an error.", titleColor = Color(0xFF92400E), textColor = Color(0xFF78350F))
                }
                if (uiState.reregistrationGranted) {
                    InfoBanner(bg = Color(0xFFF0FDF4), border = Color(0xFF86EFAC), text = "✅ Re-registration fee waived — your admin has granted a rebate.", textColor = Color(0xFF15803D))
                }

                // ── Mode switcher ──
                if (uiState.monthlyEnabled && uiState.hasSpecialSubs) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeTab("📅 Monthly Installment", uiState.payMode == PayMode.MONTHLY, Modifier.weight(1f)) { vm.setPayMode(PayMode.MONTHLY) }
                        ModeTab("🎯 Special Subscription", uiState.payMode == PayMode.SPECIAL, Modifier.weight(1f)) { vm.setPayMode(PayMode.SPECIAL) }
                    }
                }

                // ── Form Step Logic ──
                val step1Complete = uiState.selectedMonths.isNotEmpty() || uiState.selectedSpecial != null
                val step2Complete = step1Complete && uiState.selectedMethod.isNotEmpty() && (uiState.selectedMethod == "Cash" || uiState.currentMethodAccounts.size <= 1 || uiState.selectedAccount != null)
                val step3Complete = step2Complete && (uiState.selectedMethod == "Cash" || uiState.receiptUri != null || uiState.txId.isNotBlank())

                val alphaStep2 by animateFloatAsState(if (step1Complete) 1f else 0.4f, label = "a2")
                val alphaStep3 by animateFloatAsState(if (step2Complete) 1f else 0.4f, label = "a3")
                val alphaStep4 by animateFloatAsState(if (step3Complete) 1f else 0.4f, label = "a4")

                // ── STEP 1: Select Installment ──
                Column(Modifier.padding(bottom = 24.dp)) {
                    StepHeader(1, if (uiState.payMode == PayMode.MONTHLY) "Select Installment Month" else "Select Subscription")
                    if (uiState.payMode == PayMode.MONTHLY && uiState.monthlyEnabled) {
                        MonthPickerCard(
                            unpaidMonths = unpaidMonths,
                            selected     = uiState.selectedMonths,
                            penalty      = uiState.penalty,
                            onToggle     = { vm.toggleMonth(it) }
                        )
                    } else {
                        SpecialSubsCard(
                            subs           = specialSubs,
                            paidSpecial    = uiState.paidSpecialIds,
                            selectedSub    = uiState.selectedSpecial,
                            customAmount   = uiState.customSpecialAmount,
                            onSelect       = { vm.selectSpecialSub(it) },
                            onCustomAmount = { vm.setCustomAmount(it) }
                        )
                    }
                }

                // ── STEP 2: Payment Method ──
                Column(Modifier.alpha(alphaStep2).padding(bottom = 24.dp)) {
                    StepHeader(2, "Payment Method")
                    PaymentMethodSection(
                        enabledMethods  = uiState.enabledMethods,
                        selectedMethod  = uiState.selectedMethod,
                        methodAccounts  = uiState.currentMethodAccounts,
                        selectedAccount = uiState.selectedAccount,
                        isStepEnabled   = step1Complete,
                        onSelectMethod  = { if (step1Complete) vm.selectMethod(it) },
                        onSelectAccount = { if (step1Complete) vm.selectAccount(it) }
                    )
                }

                // ── STEP 3: Proof of Payment (Hidden if Cash) ──
                val requiresProof = uiState.selectedMethod.isNotEmpty() && uiState.selectedMethod != "Cash"
                if (requiresProof) {
                    Column(Modifier.alpha(alphaStep3).padding(bottom = 24.dp)) {
                        StepHeader(3, "Proof of Payment")
                        ProofOfPaymentSection(
                            receiptUri       = uiState.receiptUri,
                            receiptName      = uiState.receiptName,
                            txId             = uiState.txId,
                            selectedMethod   = uiState.selectedMethod,
                            isStepEnabled    = step2Complete,
                            onReceiptPicked  = { uri, name, mime -> if (step2Complete) vm.setReceiptUri(uri, name, mime) },
                            onReceiptCleared = { if (step2Complete) vm.clearReceipt() },
                            onTxIdChange     = { if (step2Complete) vm.setTxId(it) }
                        )
                    }
                }

                // ── STEP 4: Review & Submit ──
                val finalStepNum = if (requiresProof) 4 else 3
                Column(Modifier.alpha(alphaStep4).padding(bottom = 24.dp)) {
                    StepHeader(finalStepNum, "Review & Submit")
                    ReviewAndSubmitCard(
                        uiState = uiState,
                        isStepEnabled = step3Complete,
                        onSubmit = { vm.submitPayment(context) }
                    )
                }
            }
        }
    }
}

// ── Refined Step Sections ─────────────────────────────────────────────────────

@Composable
fun StepHeader(step: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFDBEAFE),
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(step.toString(), color = Color(0xFF1D4ED8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
    }
}

@Composable
fun PaymentMethodSection(
    enabledMethods: List<String>,
    selectedMethod: String,
    methodAccounts: List<PaymentAccount>,
    selectedAccount: PaymentAccount?,
    isStepEnabled: Boolean,
    onSelectMethod: (String) -> Unit,
    onSelectAccount: (PaymentAccount?) -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(enabledMethods.size) { i ->
                    val m = enabledMethods[i]
                    val sel = m == selectedMethod
                    Surface(
                        onClick = { onSelectMethod(m) },
                        enabled = isStepEnabled, // Lint Fix: Ensures isStepEnabled is used
                        shape = RoundedCornerShape(10.dp),
                        color = if (sel) Color(0xFFEFF6FF) else Color.White,
                        border = BorderStroke(if (sel) 1.5.dp else 1.dp, if (sel) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                    ) {
                        Text(m, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color(0xFF1D4ED8) else Color(0xFF475569))
                    }
                }
            }

            if (selectedMethod == "Cash") {
                Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFFBFDBFE)), modifier = Modifier.fillMaxWidth()) {
                    Text("Pay cash directly to your organization admin. No receipt required.", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E40AF), modifier = Modifier.padding(14.dp))
                }
            }

            if (selectedMethod.isNotEmpty() && selectedMethod != "Cash" && methodAccounts.isNotEmpty()) {
                Text("Which account did you send to? *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 10.dp))
                methodAccounts.filter { it.enabled }.forEach { acc ->
                    val isSel = selectedAccount?.id == acc.id
                    Surface(
                        onClick = { onSelectAccount(if (isSel) null else acc) },
                        enabled = isStepEnabled, // Lint Fix: Ensures isStepEnabled is used
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (methodAccounts.size > 1) {
                                Box(modifier = Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (isSel) Color(0xFF2563EB) else Color(0xFF94A3B8), CircleShape), contentAlignment = Alignment.Center) {
                                    if (isSel) Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF2563EB)))
                                }
                                Spacer(Modifier.width(12.dp))
                            } else {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFDBEAFE)), contentAlignment = Alignment.Center) { Text("🏦", fontSize = 16.sp) }
                                Spacer(Modifier.width(12.dp))
                            }
                            Column {
                                Text(acc.label.ifEmpty { selectedMethod }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isSel) Color(0xFF1D4ED8) else Color(0xFF0F172A))
                                Spacer(Modifier.height(2.dp))
                                Text(acc.number, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 15.sp, color = Color(0xFF475569), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProofOfPaymentSection(
    receiptUri: Uri?,
    receiptName: String,
    txId: String,
    selectedMethod: String,
    isStepEnabled: Boolean,
    onReceiptPicked: (Uri, String, String) -> Unit,
    onReceiptCleared: () -> Unit,
    onTxIdChange: (String) -> Unit
) {
    val context = LocalContext.current
    val isUploaded = receiptUri != null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        var resolvedName: String? = null // Lint fix: modified properly below

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0) resolvedName = cursor.getString(nameIdx)
            }
        }
        val finalName = resolvedName ?: uri.lastPathSegment ?: "receipt.jpg"

        onReceiptPicked(uri, finalName, mimeType)
    }

    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Payment Receipt (Upload screenshot)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 8.dp))

            val dashStroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .drawBehind { if (!isUploaded) drawRoundRect(color = Color(0xFFCBD5E1), style = dashStroke, cornerRadius = CornerRadius(12.dp.toPx())) }
                    .background(if (isUploaded) Color(0xFFF0FDF4) else Color.Transparent, RoundedCornerShape(12.dp))
                    .border(if (isUploaded) 1.dp else 0.dp, if (isUploaded) Color(0xFFBBF7D0) else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable(enabled = isStepEnabled) { if (!isUploaded) launcher.launch("image/*") }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUploaded) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(model = receiptUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFD1FAE5), RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("✓ Receipt attached", color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(receiptName.ifEmpty { "receipt_image.jpg" }, color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Surface(onClick = onReceiptCleared, shape = RoundedCornerShape(6.dp), color = Color(0xFFFEE2E2)) {
                            Text("Remove", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📎", fontSize = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap to upload receipt image", color = Color(0xFF64748B), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Text("Transaction ID (TxID) ${if (isUploaded) "(optional)" else ""}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = txId,
                onValueChange = onTxIdChange,
                enabled = isStepEnabled, // Lint Fix: Ensures isStepEnabled is used
                placeholder = { Text("Paste your $selectedMethod transaction ID", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = Color(0xFF2563EB))
            )
        }
    }
}

@Composable
fun ReviewAndSubmitCard(
    uiState: InstallmentUiState,
    isStepEnabled: Boolean,
    onSubmit: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            val label = if (uiState.payMode == PayMode.MONTHLY) "Installment (${uiState.selectedMonths.size}x)" else uiState.selectedSpecial?.title ?: "Payment"

            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = Color(0xFF94A3B8), fontSize = 14.sp)
                Text(fmtAmt(uiState.totalBase), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (uiState.totalPenalty > 0) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Late fee", color = Color(0xFFFCA5A5), fontSize = 14.sp)
                    Text(fmtAmt(uiState.totalPenalty), color = Color(0xFFFCA5A5), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (uiState.fee > 0) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gateway Fee (${"%.2f".format(uiState.feeRate * 100)}%)", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    Text(fmtAmt(uiState.fee), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFF334155))
            Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total to send", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(fmtAmt(uiState.grandTotal), color = Color(0xFF60A5FA), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }

            Button(
                onClick = onSubmit,
                enabled = isStepEnabled && !uiState.loading && !uiState.isImpersonating,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), disabledContainerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (uiState.loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(if (uiState.isUploadingReceipt) "Uploading receipt..." else "Submitting...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Text(if (uiState.isImpersonating) "Disabled in Impersonation Mode" else "Submit Payment — ${fmtAmt(uiState.grandTotal)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ── Success Screen ────────────────────────────────────────────────────────────

@Composable
fun SuccessScreen(onPayAgain: () -> Unit, onViewLedger: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).background(Color(0xFFDCFCE7), CircleShape).border(6.dp, Color(0xFFF0FDF4), CircleShape), contentAlignment = Alignment.Center) {
                Text("✓", fontSize = 48.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
            Text("Payment Submitted!", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color(0xFF0F172A))
            Spacer(Modifier.height(8.dp))
            Text("Your payment has been recorded.\nAn admin will verify it shortly.", fontSize = 15.sp, color = Color(0xFF64748B), textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 22.sp)
            Spacer(Modifier.height(40.dp))
            Button(onClick = onPayAgain, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))) {
                Text("Pay Another Installment", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onViewLedger, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(54.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                Text("View My Ledger", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 15.sp)
            }
        }
    }
}

// ── Shared Composables ────────────────────────────────────────

@Composable
fun MonthPickerCard(unpaidMonths: List<String>, selected: Set<String>, penalty: Double, onToggle: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Select Months to Pay", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0F172A))
            Text("Only unpaid months are shown.", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(bottom = 16.dp))
            if (unpaidMonths.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                    Text("🎉", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("All caught up!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Text("No pending months to pay.", fontSize = 14.sp, color = Color(0xFF94A3B8))
                }
            } else {
                unpaidMonths.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { month ->
                            val isSel = month in selected
                            val isLate = run {
                                val parts = month.split("-")
                                val y = parts[0].toIntOrNull() ?: 0
                                val m = parts[1].toIntOrNull() ?: 0
                                val cal = java.util.Calendar.getInstance()
                                cal.time > java.util.Calendar.getInstance().apply { set(y, m - 1, 10) }.time
                            }
                            Surface(onClick = { onToggle(month) }, shape = RoundedCornerShape(10.dp), color = if (isSel) Color(0xFFEFF6FF) else Color.White, border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) Color(0xFF2563EB) else Color(0xFFE2E8F0)), modifier = Modifier.weight(1f)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(month.replace("-", " / "), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSel) Color(0xFF1D4ED8) else Color(0xFF475569))
                                    if (isLate && penalty > 0) Text("+ ${fmtAmt(penalty)} late fee", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isSel) Color(0xFF1D4ED8) else Color(0xFFF59E0B), modifier = Modifier.padding(top = 4.dp))
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

@Composable
fun SpecialSubsCard(subs: List<SpecialSub>, paidSpecial: Set<String>, selectedSub: SpecialSub?, customAmount: String, onSelect: (SpecialSub?) -> Unit, onCustomAmount: (String) -> Unit) {
    val unpaid = subs.filter { it.id !in paidSpecial }
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Special Subscriptions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0F172A))
            Text("Select one to pay.", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(bottom = 16.dp))
            if (unpaid.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                    Text("✅", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No pending special subscriptions.", fontSize = 14.sp, color = Color(0xFF94A3B8))
                }
            } else {
                unpaid.forEach { sub ->
                    val isSel = selectedSub?.id == sub.id
                    Surface(onClick = { onSelect(if (isSel) null else sub) }, shape = RoundedCornerShape(12.dp), color = if (isSel) Color(0xFFEFF6FF) else Color.White, border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) Color(0xFF2563EB) else Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text(sub.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isSel) Color(0xFF1D4ED8) else Color(0xFF0F172A))
                                    if (sub.description.isNotEmpty()) Text(sub.description, fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp))
                                    Text("Due: ${sub.deadline} (${if (sub.daysLeft > 0) "${sub.daysLeft} day${if (sub.daysLeft != 1) "s" else ""} left" else "Today!"})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (sub.daysLeft <= 3) Color(0xFFDC2626) else Color(0xFFF59E0B), modifier = Modifier.padding(top = 6.dp))
                                    if (sub.type == "entry_fee" || sub.type == "reregistration_fee") {
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TypeBadge(if (sub.type == "entry_fee") "Entry Fee" else "Re-Registration", Color(0xFFDBEAFE), Color(0xFF1D4ED8)) }
                                    }
                                }
                                Text(if (sub.allowCustomAmount) "${fmtAmt(sub.amount)}+" else fmtAmt(sub.amount), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isSel) Color(0xFF1D4ED8) else Color(0xFF2563EB))
                            }
                            if (isSel && sub.allowCustomAmount) {
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(value = customAmount, onValueChange = onCustomAmount, prefix = { Text("৳ ", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) }, placeholder = { Text("Suggested: ${fmtAmt(sub.amount)}") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), modifier = modifier, color = if (selected) Color(0xFFEFF6FF) else Color.White, border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0))) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) Color(0xFF1D4ED8) else Color(0xFF64748B)) }
    }
}

@Composable
fun TypeBadge(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(99.dp)) { Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
}

@Composable
fun InfoBanner(bg: Color, border: Color, text: String, title: String = "", titleColor: Color = Color(0xFF0F172A), textColor: Color = Color(0xFF475569)) {
    Surface(color = bg, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, border), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            if (title.isNotEmpty()) Text(title, fontWeight = FontWeight.Bold, color = titleColor, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(text, color = textColor, fontSize = if (title.isNotEmpty()) 13.sp else 14.sp, fontWeight = if (title.isEmpty()) FontWeight.Medium else FontWeight.Normal)
        }
    }
}