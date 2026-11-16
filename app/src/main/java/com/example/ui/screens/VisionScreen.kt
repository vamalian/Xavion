package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LiveConnectionState
import com.example.ui.components.HoloPanel
import com.example.ui.components.HoloSectionHeader
import com.example.ui.theme.XavionAmber
import com.example.ui.theme.XavionBlack
import com.example.ui.theme.XavionBorderBright
import com.example.ui.theme.XavionBorderSubtle
import com.example.ui.theme.XavionCrimson
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionEmerald
import com.example.ui.theme.XavionPanelDark
import com.example.ui.theme.XavionTextMuted
import com.example.ui.theme.XavionTextPrimary
import com.example.ui.theme.XavionTextSecondary
import com.example.ui.viewmodel.AsyncActionState
import com.example.ui.viewmodel.XavionViewModel
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

private enum class VisionTaskMode(val label: String, val defaultPrompt: String) {
    GENERAL("Scene Q&A", "Describe what you see concisely and highlight key details."),
    OBJECTS("Identify Objects", "Identify and list the main objects visible in this image with brief context."),
    OCR("OCR & Document", "Perform accurate OCR: extract all readable text from this document or screen and summarize key points.")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisionScreen(viewModel: XavionViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()
    val analysisState by viewModel.visionAnalysisState.collectAsStateWithLifecycle()
    val outputText by viewModel.visionOutputText.collectAsStateWithLifecycle()

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var customPrompt by remember { mutableStateOf("") }
    var selectedTask by remember { mutableStateOf(VisionTaskMode.GENERAL) }
    var speakResult by remember { mutableStateOf(false) }
    var streamToLive by remember { mutableStateOf(false) }
    var showScreenshotAuthDialog by remember { mutableStateOf(false) }
    var lastFrameSentMillis by remember { mutableLongStateOf(0L) }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPerm = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val prompt = customPrompt.ifBlank { selectedTask.defaultPrompt }
            viewModel.analyzeVisionUri(uri, prompt, speakResult)
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    val isLiveConnected = liveState !is LiveConnectionState.Disconnected && liveState !is LiveConnectionState.Error

    if (showScreenshotAuthDialog) {
        AlertDialog(
            onDismissRequest = { showScreenshotAuthDialog = false },
            containerColor = XavionPanelDark,
            title = { Text("Authorize Screenshot Analysis", color = XavionCyan) },
            text = {
                Text(
                    "Confirm that you want to select a screenshot from your device using the privacy-preserving Android Photo Picker for XAVION to analyze.",
                    color = XavionTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showScreenshotAuthDialog = false
                        selectedTask = VisionTaskMode.OCR
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                ) {
                    Text("Authorize & Pick")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showScreenshotAuthDialog = false }) {
                    Text("Cancel", color = XavionCrimson)
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val isWide = maxWidth >= 780.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HoloPanel(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    borderColor = XavionBorderBright
                ) {
                    VisionCameraPane(
                        hasCameraPerm = hasCameraPerm,
                        streamToLive = streamToLive,
                        isLiveConnected = isLiveConnected,
                        onRequestCamera = { cameraPermLauncher.launch(Manifest.permission.CAMERA) },
                        onToggleStreamToLive = { streamToLive = it },
                        imageCapture = imageCapture,
                        onFrameAnalyzed = { bytes ->
                            val now = System.currentTimeMillis()
                            if (streamToLive && isLiveConnected && now - lastFrameSentMillis > 1600L) {
                                lastFrameSentMillis = now
                                viewModel.sendVisionFrameToLive(bytes)
                            }
                        },
                        cameraExecutor = cameraExecutor,
                        lifecycleOwner = lifecycleOwner
                    )
                }

                HoloPanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    VisionControlsAndResultPane(
                        selectedTask = selectedTask,
                        onSelectTask = { selectedTask = it },
                        customPrompt = customPrompt,
                        onCustomPromptChange = { customPrompt = it },
                        speakResult = speakResult,
                        onToggleSpeak = { speakResult = it },
                        analysisState = analysisState,
                        outputText = outputText,
                        hasCameraPerm = hasCameraPerm,
                        onCaptureAndAsk = {
                            imageCapture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val jpeg = imageProxyToResizedJpeg(image)
                                        image.close()
                                        if (jpeg != null) {
                                            val prompt = customPrompt.ifBlank { selectedTask.defaultPrompt }
                                            viewModel.analyzeVisionImage(jpeg, prompt, speakResult)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        // Handled gracefully
                                    }
                                }
                            )
                        },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onAuthorizedScreenshot = { showScreenshotAuthDialog = true },
                        onSpeakOutput = { viewModel.speakText(outputText) }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HoloPanel(borderColor = XavionBorderBright) {
                    VisionCameraPane(
                        hasCameraPerm = hasCameraPerm,
                        streamToLive = streamToLive,
                        isLiveConnected = isLiveConnected,
                        onRequestCamera = { cameraPermLauncher.launch(Manifest.permission.CAMERA) },
                        onToggleStreamToLive = { streamToLive = it },
                        imageCapture = imageCapture,
                        onFrameAnalyzed = { bytes ->
                            val now = System.currentTimeMillis()
                            if (streamToLive && isLiveConnected && now - lastFrameSentMillis > 1600L) {
                                lastFrameSentMillis = now
                                viewModel.sendVisionFrameToLive(bytes)
                            }
                        },
                        cameraExecutor = cameraExecutor,
                        lifecycleOwner = lifecycleOwner,
                        previewHeightDp = 260
                    )
                }
                HoloPanel {
                    VisionControlsAndResultPane(
                        selectedTask = selectedTask,
                        onSelectTask = { selectedTask = it },
                        customPrompt = customPrompt,
                        onCustomPromptChange = { customPrompt = it },
                        speakResult = speakResult,
                        onToggleSpeak = { speakResult = it },
                        analysisState = analysisState,
                        outputText = outputText,
                        hasCameraPerm = hasCameraPerm,
                        onCaptureAndAsk = {
                            imageCapture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val jpeg = imageProxyToResizedJpeg(image)
                                        image.close()
                                        if (jpeg != null) {
                                            val prompt = customPrompt.ifBlank { selectedTask.defaultPrompt }
                                            viewModel.analyzeVisionImage(jpeg, prompt, speakResult)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {}
                                }
                            )
                        },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onAuthorizedScreenshot = { showScreenshotAuthDialog = true },
                        onSpeakOutput = { viewModel.speakText(outputText) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VisionCameraPane(
    hasCameraPerm: Boolean,
    streamToLive: Boolean,
    isLiveConnected: Boolean,
    onRequestCamera: () -> Unit,
    onToggleStreamToLive: (Boolean) -> Unit,
    imageCapture: ImageCapture,
    onFrameAnalyzed: (ByteArray) -> Unit,
    cameraExecutor: java.util.concurrent.ExecutorService,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewHeightDp: Int? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HoloSectionHeader(
            title = "Optical Vision Feed",
            subtitle = when {
                !hasCameraPerm -> "CAMERA PERMISSION REQUIRED"
                streamToLive && isLiveConnected -> "CAMERA ACTIVE • STREAMING TO GEMINI LIVE"
                else -> "CAMERA ACTIVE • READY FOR CAPTURE"
            }
        )
        Spacer(modifier = Modifier.height(10.dp))

        val boxModifier = if (previewHeightDp != null) {
            Modifier
                .fillMaxWidth()
                .height(previewHeightDp.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .height(340.dp)
        }

        Box(
            modifier = boxModifier
                .clip(RoundedCornerShape(14.dp))
                .background(XavionBlack)
                .border(1.dp, XavionBorderSubtle, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!hasCameraPerm) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = XavionAmber, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Camera access is not granted. Authorize camera permission for live capture, OCR, and object recognition.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XavionTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestCamera,
                        modifier = Modifier.testTag("vision_grant_camera_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener({
                            runCatching {
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(cameraExecutor) { proxy ->
                                            val bytes = yuvImageProxyToJpeg(proxy)
                                            proxy.close()
                                            if (bytes != null) onFrameAnalyzed(bytes)
                                        }
                                    }
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                    analysis
                                )
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            color = XavionPanelDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, XavionBorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = if (isLiveConnected) XavionEmerald else XavionTextMuted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Live Vision Stream", style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                        Text(
                            if (isLiveConnected) "Streams periodic frames to active Gemini Live session" else "Connect Live Voice first to stream video",
                            style = MaterialTheme.typography.labelSmall,
                            color = XavionTextMuted
                        )
                    }
                }
                Switch(
                    checked = streamToLive,
                    onCheckedChange = onToggleStreamToLive,
                    enabled = hasCameraPerm && isLiveConnected,
                    colors = SwitchDefaults.colors(checkedThumbColor = XavionCyan)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VisionControlsAndResultPane(
    selectedTask: VisionTaskMode,
    onSelectTask: (VisionTaskMode) -> Unit,
    customPrompt: String,
    onCustomPromptChange: (String) -> Unit,
    speakResult: Boolean,
    onToggleSpeak: (Boolean) -> Unit,
    analysisState: AsyncActionState,
    outputText: String,
    hasCameraPerm: Boolean,
    onCaptureAndAsk: () -> Unit,
    onPickPhoto: () -> Unit,
    onAuthorizedScreenshot: () -> Unit,
    onSpeakOutput: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HoloSectionHeader(title = "Vision Intelligence")

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VisionTaskMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedTask == mode,
                    onClick = { onSelectTask(mode) },
                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        OutlinedTextField(
            value = customPrompt,
            onValueChange = onCustomPromptChange,
            label = { Text("Ask XAVION about the image or document (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vision_prompt_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = XavionCyan,
                unfocusedBorderColor = XavionBorderSubtle
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Speak Vision Answer Aloud", style = MaterialTheme.typography.bodyMedium, color = XavionTextSecondary)
            Switch(
                checked = speakResult,
                onCheckedChange = onToggleSpeak,
                colors = SwitchDefaults.colors(checkedThumbColor = XavionCyan)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCaptureAndAsk,
                enabled = hasCameraPerm && analysisState !is AsyncActionState.Loading,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("vision_capture_ask_button"),
                colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Capture & Ask")
            }

            OutlinedButton(
                onClick = onPickPhoto,
                enabled = analysisState !is AsyncActionState.Loading,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("vision_pick_photo_button"),
                border = BorderStroke(1.dp, XavionBorderBright)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick Photo", color = XavionCyan)
            }
        }

        OutlinedButton(
            onClick = onAuthorizedScreenshot,
            enabled = analysisState !is AsyncActionState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("vision_screenshot_button"),
            border = BorderStroke(1.dp, XavionBorderSubtle)
        ) {
            Icon(Icons.Default.Screenshot, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Analyze Authorized Screenshot / Document", color = XavionTextPrimary)
        }

        if (analysisState is AsyncActionState.Loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = XavionCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing visual data...", style = MaterialTheme.typography.labelMedium, color = XavionCyan)
            }
        }

        Surface(
            color = XavionPanelDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, XavionBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ANALYSIS OUTPUT", style = MaterialTheme.typography.labelLarge, color = XavionCyan)
                    }
                    if (outputText.isNotBlank()) {
                        IconButton(onClick = onSpeakOutput, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak analysis", tint = XavionCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = outputText.ifBlank { "Capture a frame or pick a photo/document to perform OCR, object identification, or scene analysis." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (outputText.isBlank()) XavionTextMuted else XavionTextPrimary
                )
            }
        }
    }
}

private fun imageProxyToResizedJpeg(image: ImageProxy): ByteArray? {
    val buffer = image.planes.firstOrNull()?.buffer ?: return null
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val rotation = image.imageInfo.rotationDegrees
    val rotated = if (rotation != 0) {
        val m = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true).also { decoded.recycle() }
    } else {
        decoded
    }
    val maxDim = 1024
    val scale = minOf(1f, maxDim.toFloat() / rotated.width, maxDim.toFloat() / rotated.height)
    val finalBmp = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            rotated,
            (rotated.width * scale).toInt().coerceAtLeast(1),
            (rotated.height * scale).toInt().coerceAtLeast(1),
            true
        ).also { rotated.recycle() }
    } else {
        rotated
    }
    val out = ByteArrayOutputStream()
    finalBmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
    finalBmp.recycle()
    return out.toByteArray()
}

private fun yuvImageProxyToJpeg(image: ImageProxy): ByteArray? {
    return runCatching {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val rawOut = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, image.width, image.height), 65, rawOut)
        rawOut.toByteArray()
    }.getOrNull()
}
