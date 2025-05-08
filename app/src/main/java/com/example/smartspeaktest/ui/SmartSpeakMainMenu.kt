package com.example.smartspeaktest.ui

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.example.looktospeakimp.FaceLandmarkerHelper
import com.example.looktospeakimp.ui.UpdateResults
import com.example.smartspeaktest.R
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.Locale
import java.util.concurrent.Executors
import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu



var hasSeenTutorialThisSession = false
var shouldNavigateToCommunicateAfterVideo = false


// I use this screen as the main menu of the SmartSpeak app — it lets users choose between communication or device control using eye gaze or manual buttons. It also initializes the camera, gaze detection, and handles navigation based on where the user looks.

@Composable
fun SmartSpeakMainMenu(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val predictions = remember { mutableStateOf<List<Category?>>(emptyList()) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    var isSpeaking by remember { mutableStateOf(false) }
    var currentUtteranceId by remember { mutableStateOf("") }
    var pendingNavigation by remember { mutableStateOf<String?>(null) }

    var expanded by remember { mutableStateOf(false) }


    // Initialize Text-to-Speech
    val textToSpeech = remember {
        TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(context, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        textToSpeech.language = Locale.US
        textToSpeech.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == currentUtteranceId) {
                    isSpeaking = false
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e("TextToSpeech", "Error occurred while speaking")
                isSpeaking = false
            }
        })
    }

    val faceLandmarkerHelper = remember {
        FaceLandmarkerHelper(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            faceLandmarkerHelperListener = object : FaceLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String, errorCode: Int) {
                    Log.e("FaceLandmarkerHelper", "Error: $error, Code: $errorCode")
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }

                override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
                    predictions.value = UpdateResults(resultBundle.result)
                }

                override fun onEmpty() {
                    predictions.value = emptyList()
                }
            }
        )
    }

    // Handle eye gaze navigation
    LaunchedEffect(predictions.value) {
        if (!isSpeaking) {
            handleMainMenuGazeDetection(
                predictions = predictions.value,
                textToSpeech = textToSpeech,
                onNavigate = { destination ->
                    currentUtteranceId = System.currentTimeMillis().toString()
                    isSpeaking = true
                    pendingNavigation = destination
                    textToSpeech.speak("$destination selected", TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)
                }
            )
        }
    }

    // 🚀 Navigate AFTER TTS finishes
    LaunchedEffect(isSpeaking) {
        if (!isSpeaking && pendingNavigation != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                navController.navigate(
                    when (pendingNavigation) {
                        "Communicate" -> "tutorialVideo"
                        "Control Smart Devices" -> "controlDevices"
                        else -> "main_menu"
                    }
                )
                pendingNavigation = null
            }, 1000) // 🚀 Delay navigation by 1 second
        }
    }

    // Camera Setup
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView?.surfaceProvider)
        }
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    faceLandmarkerHelper.detectLiveStream(imageProxy, isFrontCamera = true)
                    imageProxy.close()
                }
            }
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e("SmartSpeakMainMenu", "Camera initialization failed", exc)
        }
    }


    //UI STARTS HERE
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F1F1))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 8.dp),
            contentAlignment = Alignment.TopStart
        )
        {
        IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.Black
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Watch General Tutorial") },
                    onClick = {
                        expanded = false
                        navController.navigate("tutorial")
                    }
                )

                DropdownMenuItem(
                    text = { Text("Communication Screen Tutorial") },
                    onClick = {
                        expanded = false
                        navController.navigate("tutorialVideo")
                    }
                )
                // Add more items if needed:
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        expanded = false
                        Toast.makeText(context, "SmartSpeak v1.0", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }



        Text(
            text = "Welcome to SmartSpeak!",
            fontSize = 20.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )


        AndroidView(
            factory = { ctx -> PreviewView(ctx).also { previewView = it } },
            modifier = Modifier
                .size(1.dp) // Hide the camera preview
                .alpha(0f)
        )

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "SmartSpeak Logo",
            modifier = Modifier.size(260.dp).padding(vertical = 16.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons for Manual Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    shouldNavigateToCommunicateAfterVideo = true
                        navController.navigate("tutorialVideo")
                    },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier
                    .padding(8.dp)
                    .height(70.dp)
                    .width(160.dp)
            ) {
                Text(
                    text = "Communicate",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = { navController.navigate("controlDevices") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier
                    .padding(8.dp)
                    .height(70.dp)
                    .width(160.dp)
            ) {
                Text(
                    text = "Control Smart Devices",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }



        Spacer(modifier = Modifier.height(24.dp))
        GazeInstructionCard()


        Spacer(modifier = Modifier.weight(1f)) // Push version to bottom
        Text(
            text = "SmartSpeak v1.0",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )

    }
}

fun handleMainMenuGazeDetection(
    predictions: List<Category?>,
    textToSpeech: TextToSpeech,
    onNavigate: (String) -> Unit
) {
    val leftGaze = predictions.find { it?.categoryName() == "eyeLookOutLeft" && it.score() > 0.55 }
    val rightGaze = predictions.find { it?.categoryName() == "eyeLookOutRight" && it.score() > 0.55 }

    when {
        rightGaze != null -> onNavigate("Communicate")
        leftGaze != null -> onNavigate("Control Smart Devices")
    }
}


@Composable
fun GazeInstructionCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(Color(0xFFEAEAEA), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp, horizontal = 24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "👈 Look Left to Communicate",
                fontSize = 16.sp,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "👉 Look Right to Control Devices",
                fontSize = 16.sp,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
        }
    }
}
