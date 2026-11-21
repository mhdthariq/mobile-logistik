package com.kaminova.novalogistikmobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProofPhotos(title: String, result: Result<List<String>>?, onClose: () -> Unit) {
    val photos = result?.getOrNull().orEmpty()
    val pager = rememberPagerState { photos.size }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Default.Close, "Tutup")
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when {
                            result == null -> LoadingIndicator()
                            result.isFailure -> ErrorBanner(
                                result.exceptionOrNull()?.message ?: "Bukti gagal dimuat.", Modifier.padding(16.dp)
                            )
                            photos.isEmpty() -> Text("Belum ada foto bukti.")
                            else -> HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                                EvidenceImage(
                                    source = photos[page],
                                    contentDescription = "Foto bukti ${page + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
                if (photos.size > 1) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraLarge) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                                enabled = pager.currentPage > 0,
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Foto sebelumnya")
                            }
                            Text("${pager.currentPage + 1} / ${photos.size}", style = MaterialTheme.typography.labelLarge)
                            FilledTonalIconButton(
                                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                                enabled = pager.currentPage < photos.lastIndex,
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Foto berikutnya")
                            }
                        }
                    }
                }
            }
        }
    }
}
