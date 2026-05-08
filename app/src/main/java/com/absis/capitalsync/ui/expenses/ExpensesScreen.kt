// ui/expenses/ExpensesScreen.kt
package com.absis.capitalsync.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
// ── Imports for new documentation ──
import com.absis.capitalsync.ui.common.PullToRefreshBox


@Composable
fun ExpensesScreen(vm: ExpensesViewModel = hiltViewModel()) {
    val items by vm.expenses.collectAsState()
    val loading by vm.loading.collectAsState() // Required for PullToRefresh
    val total = items.sumOf { it.amount }

    // ── Step 1: Wrap root with PullToRefreshBox ──
    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh    = { vm.refresh() },
        modifier     = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize()) {
            
            // ── Header ──
            Column(Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)) {
                Text(
                    text = "Expenses", 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Total Pool Expenditure: ৳${total.toLong()}", 
                    fontSize = 14.sp, 
                    color = Color(0xFF64748B)
                )
            }

            if (items.isEmpty() && !loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expenses recorded yet", color = Color(0xFF94A3B8))
                }
            } else {
                // ── Table Header Row ──
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    listOf("Date", "Title", "Category", "Amount").forEach { h ->
                        Text(
                            text = h, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFF64748B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))

                // ── Expense List ──
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(items) { i, item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (i % 2 == 0) Color.White else Color(0xFFFAFAFA)) // Premium striped effect
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.date, 
                                fontSize = 13.sp, 
                                color = Color(0xFF334155),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = item.title, 
                                fontSize = 13.sp, 
                                fontWeight = FontWeight.Medium, 
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Category badge
                            Box(Modifier.weight(1f)) {
                                Surface(
                                    color = Color(0xFFF1F5F9), 
                                    shape = RoundedCornerShape(99.dp)
                                ) {
                                    Text(
                                        text = item.category.ifEmpty { "General" }, 
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            Text(
                                text = "৳${item.amount.toLong()}", 
                                fontSize = 13.sp, 
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626), 
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (i < items.lastIndex) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}