package fr.openllm.luciole.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import fr.openllm.luciole.scan.ImageOrientation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.openllm.luciole.R
import fr.openllm.luciole.ui.theme.Encre
import fr.openllm.luciole.ui.theme.Fond
import fr.openllm.luciole.ui.theme.Surface
import fr.openllm.luciole.ui.theme.TexteFaible
import java.util.concurrent.Executors

@Composable
fun ScanCarteScreen(
    viewModel: ScanCarteViewModel,
    onBack: () -> Unit,
    onCreateContact: (fr.openllm.luciole.contact.ContactCard) -> Unit,
    onExportVcf: (fr.openllm.luciole.contact.ContactCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    when (val s = state) {
        is ScanUiState.DraftReady -> ContactDraftScreen(
            draft = s.draft,
            onCardChange = viewModel::updateDraft,
            onCreateContact = { onCreateContact(s.draft.card) },
            onExportVcf = { onExportVcf(s.draft.card) },
            onRetry = viewModel::reset,
            modifier = modifier,
        )
        ScanUiState.Scanning -> ScanProgress(R.string.scan_en_cours)
        ScanUiState.OcrRunning -> ScanProgress(R.string.scan_ocr_en_cours)
        ScanUiState.Structuring -> ScanProgress(R.string.scan_structuration)
        is ScanUiState.Error -> ScanErrorScreen(s.message, onRetry = viewModel::reset, onBack = onBack)
        ScanUiState.Capturing, ScanUiState.Idle -> ScanCaptureScreen(viewModel, onBack, modifier)
    }
}

@Composable
private fun ScanCaptureScreen(
    viewModel: ScanCarteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCamera = it
    }
    LaunchedEffect(Unit) {
        if (!hasCamera) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(95)
            .build()
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    Column(modifier.fillMaxSize().background(Fond)) {
        RowTop(onBack)
        Text(
            stringResource(R.string.scan_instructions),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            color = TexteFaible,
        )
        if (hasCamera) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                )
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Button(
                onClick = {
                    viewModel.onCaptureReady()
                    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bmp = imageProxyToBitmap(image)
                            image.close()
                            if (bmp != null) {
                                viewModel.processCapture(bmp)
                            } else {
                                viewModel.onCaptureDecodeFailed()
                            }
                        }
                        override fun onError(exception: ImageCaptureException) {
                            viewModel.onCaptureDecodeFailed()
                        }
                    })
                },
                modifier = Modifier.fillMaxWidth().padding(18.dp),
            ) { Text(stringResource(R.string.scan_capturer)) }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.scan_autoriser_camera))
                }
            }
        }
    }
}

@Composable
private fun ScanErrorScreen(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Fond).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = Encre)
        Button(onClick = onRetry) { Text(stringResource(R.string.scan_reprendre)) }
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.scan_retour)) }
    }
}

@Composable
private fun RowTop(onBack: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(Surface).padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.scan_retour))
        }
        Text(
            stringResource(R.string.scan_titre),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Center),
            color = Encre,
        )
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val plane = image.planes.firstOrNull() ?: return null
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    // EXIF prioritaire ; sinon rotation CameraX. Jamais les deux.
    return ImageOrientation.fromJpegBytes(
        jpegBytes = bytes,
        cameraRotationDegrees = image.imageInfo.rotationDegrees,
    )?.bitmap
}
