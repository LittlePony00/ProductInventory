package com.android.rut.miit.productinventory.ui.screen.barcode

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.R
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft
import com.android.rut.miit.productinventory.feature.barcode.presentation.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.koin.compose.viewmodel.koinViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    householdId: String,
    onBack: () -> Unit,
    onManualEntry: (String) -> Unit,
    onDraftEntry: (BarcodeProductDraft) -> Unit,
    viewModel: BarcodeScanViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    var hasCameraPermission by remember { mutableStateOf(false) }

    LaunchedEffect(householdId) { viewModel.householdId = householdId }

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is BarcodeScanAction.NavigateBack -> onBack()
                is BarcodeScanAction.ProductAdded -> onBack()
                is BarcodeScanAction.NavigateToManualEntry -> onManualEntry(action.barcode)
                is BarcodeScanAction.NavigateToDraftEntry -> onDraftEntry(action.draft)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.barcode_scan_title)) },
                navigationIcon = {
                    TextButton(onClick = { viewModel.onEvent(BarcodeScanEvent.OnBackClick) }) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (!hasCameraPermission) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.camera_permission_required))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text(stringResource(R.string.camera_permission_grant))
                    }
                }
            } else {
                when (val s = state) {
                    is BarcodeScanState.Scanning -> {
                        CameraPreview(onBarcodeDetected = { code ->
                            viewModel.onEvent(BarcodeScanEvent.OnBarcodeScanned(code))
                        })
                        Box(
                            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                )
                            ) {
                                Text(
                                    stringResource(R.string.barcode_scanning),
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    is BarcodeScanState.Loading -> {
                        CircularProgressIndicator()
                    }
                    is BarcodeScanState.ProductFound -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.barcode_product_found),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(s.product.name, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${s.product.quantity} ${s.product.quantityUnit.name}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { viewModel.onEvent(BarcodeScanEvent.OnDismissProduct) }) {
                                Text(stringResource(R.string.barcode_scan_title))
                            }
                        }
                    }
                    is BarcodeScanState.DraftFound -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.barcode_product_found),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                s.draft.name ?: s.draft.barcode,
                                style = MaterialTheme.typography.titleLarge
                            )
                            s.draft.brand?.let { brand ->
                                Spacer(Modifier.height(8.dp))
                                Text(brand, style = MaterialTheme.typography.bodyLarge)
                            }
                            s.draft.category?.let { category ->
                                Spacer(Modifier.height(8.dp))
                                Text(category.name, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = {
                                viewModel.onEvent(BarcodeScanEvent.OnUseDraftClick(s.draft.barcode))
                            }) {
                                Text(stringResource(R.string.barcode_confirm_entry))
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                viewModel.onEvent(BarcodeScanEvent.OnDraftManualEntryClick(s.draft))
                            }) {
                                Text(stringResource(R.string.barcode_edit_entry))
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.onEvent(BarcodeScanEvent.OnRetry) }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                    is BarcodeScanState.ManualEntry -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.barcode_not_found),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(s.barcode, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { onManualEntry(s.barcode) }) {
                                Text(stringResource(R.string.barcode_manual_entry))
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.onEvent(BarcodeScanEvent.OnRetry) }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                    is BarcodeScanState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.onEvent(BarcodeScanEvent.OnRetry) }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun CameraPreview(onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage, imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull()?.rawValue?.let { value ->
                                    onBarcodeDetected(value)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
