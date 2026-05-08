package com.absis.capitalsync.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh:    () -> Unit,
    modifier:     Modifier = Modifier,
    content:      @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        // The 'content' block here provides a BoxScope, 
        // so we wrap your passed-in content to match the expected type.
        content = {
            content()
        }
    )
}