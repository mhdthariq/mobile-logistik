package com.kaminova.novalogistikmobile.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.runtime.remember
import com.kaminova.novalogistikmobile.ui.components.ErrorBanner
import com.kaminova.novalogistikmobile.ui.components.ExpressiveRefreshBox
import com.kaminova.novalogistikmobile.ui.components.LabeledRow
import com.kaminova.novalogistikmobile.ui.components.PrimaryButton
import com.kaminova.novalogistikmobile.ui.theme.supportsPhoneColors
import com.kaminova.novalogistikmobile.util.EvidenceZones
import com.kaminova.novalogistikmobile.util.Formatters
import kotlin.math.abs

private enum class ProfileDialog { APP, LOGOUT }

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val usePhoneColors by viewModel.usePhoneColors.collectAsState()
    var dialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }
    var showZones by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLoggedOut()
    }

    val session = state.session ?: return
    val user = session.user
    val config = state.config
    val skew = state.clockSkewSeconds

    when (dialog) {
        ProfileDialog.APP -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Tentang aplikasi") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val context = LocalContext.current
                    val appVersion = remember(context) {
                        val info = context.packageManager.getPackageInfo(context.packageName, 0)
                        "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
                    }
                    LabeledRow("Versi", appVersion)
                    LabeledRow("Sesi berlaku sampai", Formatters.formatDateTime(session.expiresAt))
                    if (config != null) {
                        LabeledRow("Waktu server", Formatters.formatDateTime(config.serverTime))
                    }
                    config?.android?.let { android ->
                        LabeledRow("Distribusi", if (android.distribution == "public") "Publik" else "Privat")
                        LabeledRow("Versi minimum", android.minimumVersionCode.toString())
                        LabeledRow("Versi terbaru", android.latestVersionCode.toString())
                    }
                    state.configError?.let { ErrorBanner("Gagal memuat konfigurasi server: $it") }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Tutup") } }
        )

        ProfileDialog.LOGOUT -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Keluar dari akun?") },
            text = { Text("Anda perlu masuk kembali untuk melanjutkan perjalanan.") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    viewModel.logout()
                }) {
                    Text("Keluar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Batal") } }
        )

        null -> Unit
    }

    ExpressiveRefreshBox(state.isLoadingConfig, viewModel::loadConfig, Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Profil", style = MaterialTheme.typography.headlineMedium)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(user.name.firstOrNull()?.uppercase() ?: "A", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(user.name, style = MaterialTheme.typography.titleLarge)
                            Text(user.username, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(session.companyName, style = MaterialTheme.typography.labelLarge)
                }
            }

            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    LabeledRow("Gudang", user.warehouseName)
                    LabeledRow("Peran", user.roles.joinToString(", ") { it.name }.ifBlank { "—" })
                    user.employeeId?.let { LabeledRow("ID Karyawan", "EMP-$it") }
                }
            }

            if (skew != null && abs(skew) > 30) {
                ErrorBanner("Waktu perangkat berbeda ${abs(skew)} detik dari server. Periksa pengaturan tanggal dan jam sebelum mengirim bukti.")
            }

            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    Box {
                        ProfileItem(
                            "Zona waktu bukti", Icons.Default.Schedule,
                            EvidenceZones.getShortLabel(state.selectedTimeZone)
                        ) { showZones = true }
                        DropdownMenu(expanded = showZones, onDismissRequest = { showZones = false }) {
                            EvidenceZones.ZONES.forEach { zone ->
                                DropdownMenuItem(
                                    text = { Text(zone.shortLabel) },
                                    trailingIcon = if (zone.id == state.selectedTimeZone) {
                                        { Icon(Icons.Default.Check, "Dipilih") }
                                    } else null,
                                    onClick = {
                                        viewModel.onTimeZoneSelected(zone.id)
                                        showZones = false
                                    }
                                )
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        checked = usePhoneColors && supportsPhoneColors,
                        onCheckedChange = viewModel::setUsePhoneColors,
                        enabled = supportsPhoneColors,
                        leadingContent = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                        supportingContent = {
                            Text(if (supportsPhoneColors) "Ikuti warna wallpaper" else "Tersedia mulai Android 12")
                        },
                        trailingContent = { Switch(checked = usePhoneColors && supportsPhoneColors, onCheckedChange = null, enabled = supportsPhoneColors) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) { Text("Warna tema ponsel") }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ProfileItem("Tentang aplikasi", Icons.Default.Info) { dialog = ProfileDialog.APP }
                }
            }

            PrimaryButton(
                label = if (state.isLoggingOut) "Keluar…" else "Keluar dari akun",
                onClick = { dialog = ProfileDialog.LOGOUT },
                danger = true,
                loading = state.isLoggingOut
            )
        }
    }
}

@Composable
private fun ProfileItem(label: String, icon: ImageVector, detail: String? = null, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        supportingContent = detail?.let { { Text(it) } },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) { Text(label) }
}
