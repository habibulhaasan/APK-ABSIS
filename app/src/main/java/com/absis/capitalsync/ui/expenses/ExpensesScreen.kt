// ui/expenses/ExpensesScreen.kt
package com.absis.capitalsync.ui.expenses

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

@Composable
fun ExpensesScreen(vm: ExpensesViewModel = hiltViewModel()) {
    val items by vm.expenses.collectAsState()
    val total = items.sumOf { it.amount }

    Column(Modifier.fillMaxSize()) {
        // Header
        Column(Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)) {
            Text("Expenses", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text("Total: ৳${total.toLong()}", fontSize = 14.sp, color = Color(0xFF64748B))
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No expenses recorded", color = Color(0xFF94A3B8))
            }
            return@Column
        }

        // Header row
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf("Date", "Title", "Category", "Amount").forEach { h ->
                Text(h, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B),
                    modifier = Modifier.weight(1f))
            }
        }
        HorizontalDivider()

        LazyColumn {
            itemsIndexed(items) { i, item ->
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.date, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    // Category badge
                    Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(99.dp),
                        modifier = Modifier.weight(1f)) {
                        Text(item.category.ifEmpty { "—" }, fontSize = 11.sp,
                            modifier = Modifier.padding(7.dp, 2.dp))
                    }
                    Text("৳${item.amount.toLong()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFDC2626), modifier = Modifier.weight(1f))
                }
                if (i < items.lastIndex) HorizontalDivider(color = Color(0xFFF1F5F9))
            }
        }
    }
}