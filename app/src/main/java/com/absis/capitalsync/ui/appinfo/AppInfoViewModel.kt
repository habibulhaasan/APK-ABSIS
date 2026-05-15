package com.absis.capitalsync.ui.appinfo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject

data class AppInfoState(
    val currentVersion: String = "",
    val serverVersion: String = "",
    val releaseDate: String = "",
    val changelog: List<String> = emptyList(),
    val isUpdateAvailable: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val activeApkUrl: String = "https://absis-backup.vercel.app/absis-capital-sync.apk"
)

@HiltViewModel
class AppInfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppInfoState())
    val uiState = _uiState.asStateFlow()

    private val primaryJsonUrl = "https://absis-backup.vercel.app/app-release.json"
    private val fallbackJsonUrl = "https://absis.netlify.app/app-release.json"
    private val primaryApkUrl = "https://absis-backup.vercel.app/absis-capital-sync.apk"
    private val fallbackApkUrl = "https://absis.netlify.app/absis-capital-sync.apk"

    init {
        fetchAppInfo()
    }

    fun checkForUpdates() {
        _uiState.update { it.copy(loading = true, error = null) }
        fetchAppInfo()
    }

    private fun fetchAppInfo() = viewModelScope.launch {
        // 1. Get current installed version from Android Package Manager
        val currentVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        _uiState.update { it.copy(currentVersion = currentVersion) }

        var result: String? = null
        var activeApk = primaryApkUrl

        // 2. Try Primary Server
        try {
            result = withContext(Dispatchers.IO) { URL(primaryJsonUrl).readText() }
        } catch (e: Exception) {
            // 3. Try Fallback Server if Primary fails
            try {
                result = withContext(Dispatchers.IO) { URL(fallbackJsonUrl).readText() }
                activeApk = fallbackApkUrl
            } catch (e2: Exception) {
                _uiState.update { it.copy(loading = false, error = "Could not connect to update server.") }
                return@launch
            }
        }

        // 4. Parse JSON
        if (result != null) {
            try {
                val json = JSONObject(result)
                val serverVersion = json.optString("version", currentVersion)
                
                val changesArray = json.optJSONArray("changelog")
                val changesList = mutableListOf<String>()
                if (changesArray != null) {
                    for (i in 0 until changesArray.length()) {
                        changesList.add(changesArray.getString(i))
                    }
                }

                _uiState.update {
                    it.copy(
                        serverVersion = serverVersion,
                        releaseDate = json.optString("releaseDate", "Unknown"),
                        changelog = changesList,
                        isUpdateAvailable = isNewerVersion(currentVersion, serverVersion),
                        activeApkUrl = activeApk,
                        loading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = "Invalid data received from server.") }
            }
        }
    }

    private fun isNewerVersion(current: String, server: String): Boolean {
        val currParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val servParts = server.split(".").map { it.toIntOrNull() ?: 0 }
        
        val length = maxOf(currParts.size, servParts.size)
        for (i in 0 until length) {
            val c = currParts.getOrElse(i) { 0 }
            val s = servParts.getOrElse(i) { 0 }
            if (s > c) return true
            if (s < c) return false
        }
        return false
    }
}