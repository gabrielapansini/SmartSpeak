package com.example.smartspeaktest.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.looktospeakimp.FaceLandmarkerHelper
import com.example.looktospeakimp.ui.UpdateResults
import com.example.smartspeaktest.R
import com.example.smartspeaktest.lightRef
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.*
import java.util.concurrent.Executors




// Global variables to help track selected phrases and cooldowns
var lastSelectedPhrase: String? = null
var lastSelectionTime: Long = 0L
const val GAZE_COOLDOWN_MS = 3000 // 3 seconds
var navigationCooldownStartTime: Long = 0L
const val NAVIGATION_COOLDOWN_MS = 3000L // 3 seconds pause after navigation
var controlRightGazeStartTime: Long? = null
var isControlRightGazeTriggered = false


// This function checks whether a new phrase should be selected and spoken
fun shouldSelectNew(phrase: String): Boolean {
    val now = SystemClock.elapsedRealtime()
    return phrase != lastSelectedPhrase || (now - lastSelectionTime > GAZE_COOLDOWN_MS)

}

// Main Composable for controlling smart devices with gaze and touch

@Composable
fun ControlDevicesScreen(navController: NavHostController) {
    // I use this to update the status like “Light turned ON”
    var statusMessage by remember { mutableStateOf("") }

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



    // When the screen is shown, I set the language and track when speaking is finished
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

    // I set up the face landmark helper to detect the user’s gaze in real-time
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

    // Here’s where I handle eye gaze logic: control lights or go back to main menu
    handleDeviceControlGazeDetection(
        predictions = predictions.value,
        textToSpeech = textToSpeech,
        onCommand = { command ->
            if (shouldSelectNew(command)) {
                lastSelectedPhrase = command
                lastSelectionTime = SystemClock.elapsedRealtime()

                currentUtteranceId = System.currentTimeMillis().toString()
                isSpeaking = true
                textToSpeech.speak("$command", TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)

                when (command) {
                    "Turn Light On" -> {
                        lightRef.setValue("on")
                        statusMessage = "Light turned ON"
                    }
                    "Turn Light Off" -> {
                        lightRef.setValue("off")
                        statusMessage = "Light turned OFF"
                    }
                }
            }
        },
        context = context,
        navController = navController
    )





    // I’m setting up the camera so MediaPipe can analyze the user's face in real-time
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

    // UI layout starts here

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F1F1))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var expanded by remember { mutableStateOf(false) }
        // Menu dropdown

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopStart
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Main Menu") },
                    onClick = {
                        safeNavigateWithCameraCleanup(context, navController, "main_menu")
                    }
                )

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

            }
        }
        // Logo image

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "SmartSpeak Logo",
            modifier = Modifier.size(220.dp).padding(vertical = 16.dp),
            contentScale = ContentScale.Fit
        )

        Text("Control Smart Lights", fontSize = 20.sp, color = Color.Black)

        // Invisible camera view for MediaPipe
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    // Set small but non-zero dimensions
                    layoutParams = FrameLayout.LayoutParams(1, 1)
                    // Use media overlay instead of ZOrderOnTop
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView = it }
            },
            modifier = Modifier.size(1.dp).alpha(0f)
        )

        // Buttons for manual control
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                val phrase = "Turn Light On"
                currentUtteranceId = System.currentTimeMillis().toString()
                isSpeaking = true
                textToSpeech.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)
                lightRef.setValue("on")
                statusMessage = "Light turned ON"
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier.height(70.dp).width(160.dp)) {
                Text("Turn Light On", color = Color.White, fontSize = 16.sp)
            }

            Button(onClick = {
                val phrase = "Turn Light Off"
                currentUtteranceId = System.currentTimeMillis().toString()
                isSpeaking = true
                textToSpeech.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)
                lightRef.setValue("off")
                statusMessage = "Light turned OFF"
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier.height(70.dp).width(160.dp)) {
                Text("Turn Light Off", color = Color.White, fontSize = 16.sp)
            }
        }

        Text("Status: $statusMessage", fontSize = 16.sp, color = Color.Black)


        // Gaze instructions

        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                .background(Color(0xFFEAEAEA), shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👈 Look Left to Turn On", fontSize = 16.sp, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(8.dp))
                Text("👉 Look Right to Turn Off", fontSize = 16.sp, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(8.dp))
                Text("👉 Long Look Right to Return to Main Menu", fontSize = 16.sp, color = Color(0xFF555555))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual back button in case the gaze doesn’t work
        Button(
            onClick = {
                safeNavigateWithCameraCleanup(context, navController, "main_menu")
            },

            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
            modifier = Modifier.fillMaxWidth().height(70.dp)) {
            Text("Back to Main Menu", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("SmartSpeak v1.0", fontSize = 14.sp, color = Color.Gray)
    }
}

// Gaze logic for detecting direction and acting accordingly
// --- Eye Gaze Logic ---
fun handleDeviceControlGazeDetection(
    predictions: List<Category?>,
    textToSpeech: TextToSpeech,
    onCommand: (String) -> Unit,
    context: Context,
    navController: NavHostController
) {
    val rightGaze = predictions.find { it?.categoryName() == "eyeLookOutRight" && it.score() > 0.55 }
    val leftGaze = predictions.find { it?.categoryName() == "eyeLookOutLeft" && it.score() > 0.55 }

    val currentTime = SystemClock.elapsedRealtime()

    // Long LEFT gaze returns to main menu
    if (leftGaze != null) {
        if (controlRightGazeStartTime == null) {
            controlRightGazeStartTime = currentTime
            isControlRightGazeTriggered = false
        } else if (!isControlRightGazeTriggered && currentTime - controlRightGazeStartTime!! >= 2000) {
            textToSpeech.stop()
        }

        if (!isControlRightGazeTriggered && currentTime - controlRightGazeStartTime!! >= 4000) {
            isControlRightGazeTriggered = true
            textToSpeech.speak("Returning to main menu", TextToSpeech.QUEUE_FLUSH, null, null)
            navigationCooldownStartTime = SystemClock.elapsedRealtime()

            Handler(Looper.getMainLooper()).postDelayed({
                safeNavigateWithCameraCleanup(context, navController, "main_menu")
                controlRightGazeStartTime = null
                isControlRightGazeTriggered = false
            }, 1000)
            return
        }
    } else {
        controlRightGazeStartTime = null
        isControlRightGazeTriggered = false
    }

    // Short RIGHT gaze turns light ON
    if (rightGaze != null && !isControlRightGazeTriggered) {
        onCommand("Turn Light On")
    }

    // Handle short LEFT gaze (turn OFF light)
    if (leftGaze != null && !isControlRightGazeTriggered) {
        onCommand("Turn Light Off")
    }
}



// Helper to unbind camera safely before navigating (to avoid crashes)

fun safeNavigateWithCameraCleanup(
    context: Context,
    navController: NavHostController,
    destination: String,
    delayMillis: Long = 300
) {
    Handler(Looper.getMainLooper()).post {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider.unbindAll()
        } catch (e: Exception) {
            Log.e("CameraCleanup", "Error during camera unbind", e)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            navController.navigate(destination)
        }, delayMillis)
    }
}
