package com.kaminova.novalogistikmobile.ui.screens.capture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kaminova.novalogistikmobile.ui.components.ErrorBanner
import com.kaminova.novalogistikmobile.ui.components.PrimaryButton
import com.kaminova.novalogistikmobile.ui.components.SecondaryButton
import com.kaminova.novalogistikmobile.util.EvidenceZones
import java.io.File
import java.time.Instant
import kotlinx.coroutines.delay
import androidx.camera.core.UseCaseGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.view.doOnLayout
import androidx.core.view.WindowCompat
import com.kaminova.novalogistikmobile.util.EvidenceStamp
import com.kaminova.novalogistikmobile.util.EvidenceStampData

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    BackHandler {
        if (uiState.step != CaptureStep.PROCESSING) viewModel.cancel(onNavigateBack)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val hasCamera = perms[Manifest.permission.CAMERA] == true
        val hasLocation = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionsResult(hasCamera, hasLocation)
    }

    LaunchedEffect(Unit) {
        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasCam && hasLoc) {
            viewModel.onPermissionsResult(true, true)
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    val req = uiState.request
    if (req == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tidak ada permintaan foto bukti yang aktif.",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                SecondaryButton(label = "Tutup", onClick = { viewModel.cancel(onNavigateBack) })
            }
        }
        return
    }

    if (uiState.step == CaptureStep.LIVE || uiState.step == CaptureStep.PROCESSING || uiState.step == CaptureStep.PREVIEW) {
        DisposableEffect(context) {
            val bars = (context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
            val lightStatus = bars?.isAppearanceLightStatusBars
            val lightNavigation = bars?.isAppearanceLightNavigationBars
            bars?.isAppearanceLightStatusBars = false
            bars?.isAppearanceLightNavigationBars = false
            onDispose {
                if (lightStatus != null) bars.isAppearanceLightStatusBars = lightStatus
                if (lightNavigation != null) bars.isAppearanceLightNavigationBars = lightNavigation
            }
        }
    }

    when (uiState.step) {
        CaptureStep.PERMISSIONS -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(color = Color.White)
            }
        }

        CaptureStep.BLOCKED -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Izin diperlukan",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.blockReason ?: "Izin kamera atau lokasi diperlukan.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    PrimaryButton(
                        label = "Buka Pengaturan",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                    SecondaryButton(
                        label = "Batal",
                        onClick = { viewModel.cancel(onNavigateBack) }
                    )
                }
            }
        }

        CaptureStep.PREVIEW -> {
            val frozen = uiState.frozen ?: return
            Column(
                Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tinjau foto bukti", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Image(
                    bitmap = frozen.previewBitmap.asImageBitmap(),
                    contentDescription = "Pratinjau foto bukti",
                    modifier = Modifier.weight(1f).fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
                        .background(Color.White.copy(alpha = .08f)),
                    contentScale = ContentScale.Fit
                )
                Column(Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.error?.let { ErrorBanner(message = it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.retake() },
                            modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("Ambil Ulang") }
                        PrimaryButton(label = "Gunakan Foto", onClick = { viewModel.usePhoto(onNavigateBack) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        CaptureStep.LIVE, CaptureStep.PROCESSING -> {
            var front by remember { mutableStateOf(req.preferFrontCamera) }
            var canSwitch by remember { mutableStateOf(false) }
            var imageCapture by remember(front) { mutableStateOf<ImageCapture?>(null) }
            var camera by remember(front) { mutableStateOf<Camera?>(null) }
            var cameraError by remember(front) { mutableStateOf<String?>(null) }
            var now by remember { mutableStateOf(Instant.now()) }
            val renderer = remember(context) { EvidenceStamp(context) }
            val processing = uiState.step == CaptureStep.PROCESSING
            val canCapture = imageCapture != null && !processing && (!req.requireGps || uiState.liveFix != null)
            val viewfinder = remember(context, front) {
                PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
            }
            DisposableEffect(viewfinder, lifecycleOwner) {
                val future = ProcessCameraProvider.getInstance(context)
                var disposed = false
                var preview: Preview? = null
                var capture: ImageCapture? = null
                viewfinder.doOnLayout {
                    future.addListener({
                        if (disposed) return@addListener
                        try {
                            val viewport = checkNotNull(viewfinder.viewPort)
                            val live = Preview.Builder().build().also { it.surfaceProvider = viewfinder.surfaceProvider }
                            val photo = ImageCapture.Builder()
                                .setTargetRotation(viewfinder.display.rotation)
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            preview = live
                            capture = photo
                            val useCases = UseCaseGroup.Builder().setViewPort(viewport)
                                .addUseCase(live).addUseCase(photo).build()
                            val provider = future.get()
                            val hasFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                            val hasBack = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                            canSwitch = hasFront && hasBack
                            val selector = if (hasFront && (front || !hasBack)) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                            camera = provider.bindToLifecycle(lifecycleOwner, selector, useCases)
                            imageCapture = photo
                        } catch (_: Exception) {
                            cameraError = "Kamera tidak dapat dibuka."
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
                onDispose {
                    disposed = true
                    if (future.isDone) runCatching {
                        future.get().unbind(*listOfNotNull(preview, capture).toTypedArray())
                    }
                }
            }
            LaunchedEffect(uiState.flashEnabled, camera) {
                camera?.cameraControl?.enableTorch(uiState.flashEnabled)
            }
            LaunchedEffect(Unit) {
                while (true) {
                    now = Instant.now()
                    delay(1000)
                }
            }

            Box(Modifier.fillMaxSize().background(Color.Black)) {
                key(viewfinder) {
                    AndroidView(factory = { viewfinder }, modifier = Modifier.fillMaxSize())
                }
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        FilledTonalIconButton(
                            onClick = { viewModel.cancel(onNavigateBack) },
                            enabled = !processing,
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(Icons.Default.Close, "Tutup kamera")
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 52.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 3.dp
                        ) {
                            Text(
                                "Kamera Bukti",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val stamp = EvidenceStampData(uiState.company, req.title, now, uiState.timeZone,
                            uiState.liveFix, uiState.address, req.contextLines, req.vehicle, req.driver)
                        Canvas(Modifier.fillMaxSize().semantics {
                            contentDescription = listOfNotNull(
                                uiState.company, req.title,
                                EvidenceZones.formatTimeInZone(now, uiState.timeZone),
                                req.driver?.name,
                                req.driver?.phone?.takeIf(String::isNotBlank)?.let { "No. HP sopir $it" },
                                uiState.address,
                                if (uiState.liveFix == null) "GPS belum tersedia" else null
                            ).plus(req.contextLines).joinToString(", ")
                        }) {
                            drawIntoCanvas { renderer.draw(it.nativeCanvas, size.width, size.height, stamp) }
                        }
                        if (processing) LoadingIndicator(Modifier.align(Alignment.Center), color = Color.White)
                    }

                    Column(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val error = cameraError ?: uiState.error
                        if (error != null) {
                            ErrorBanner(message = error)
                        } else if (req.requireGps && uiState.liveFix == null) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text(
                                    "Menunggu GPS…",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val controlColors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .8f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f)
                            )
                            FilledTonalIconButton(
                                onClick = viewModel::toggleFlash,
                                enabled = camera?.cameraInfo?.hasFlashUnit() == true && !processing,
                                shapes = IconButtonDefaults.shapes(),
                                colors = controlColors,
                                modifier = Modifier.semantics {
                                    contentDescription = if (uiState.flashEnabled) "Matikan lampu" else "Nyalakan lampu"
                                }
                            ) {
                                Icon(if (uiState.flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, null)
                            }
                            FilledIconButton(
                                enabled = canCapture,
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.size(64.dp).semantics { contentDescription = "Ambil foto" },
                                onClick = {
                                    val capture = imageCapture ?: return@FilledIconButton
                                    val file = runCatching { File.createTempFile("evidence_", ".jpg", context.cacheDir) }
                                        .getOrElse {
                                            viewModel.retake("Tidak dapat menyiapkan berkas foto.")
                                            return@FilledIconButton
                                        }
                                    val stamp = viewModel.beginCapture()
                                    if (stamp == null) {
                                        file.delete()
                                        return@FilledIconButton
                                    }
                                    capture.takePicture(
                                        ImageCapture.OutputFileOptions.Builder(file).build(),
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                viewModel.processCapturedPhoto(file, stamp)
                                            }
                                            override fun onError(exception: ImageCaptureException) {
                                                file.delete()
                                                viewModel.retake("Foto gagal diambil. Silakan coba lagi.")
                                            }
                                        }
                                    )
                                }
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(28.dp))
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    if (uiState.flashEnabled) viewModel.toggleFlash()
                                    front = !front
                                },
                                enabled = canSwitch && !processing,
                                shapes = IconButtonDefaults.shapes(),
                                colors = controlColors,
                                modifier = Modifier.semantics {
                                    contentDescription = if (front) "Gunakan kamera belakang" else "Gunakan kamera depan"
                                }
                            ) {
                                Icon(Icons.Default.Cameraswitch, null)
                            }
                        }
                    }
                }
            }
        }
    }
}
