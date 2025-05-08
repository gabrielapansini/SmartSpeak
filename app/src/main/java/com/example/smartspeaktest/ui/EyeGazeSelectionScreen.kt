package com.example.looktospeakimp.ui

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
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.looktospeakimp.FaceLandmarkerHelper
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.*
import java.util.concurrent.Executors
import android.Manifest
import android.widget.FrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay



// These variables help prevent repeating the same selection too fast
var lastSelectedPhrase: String? = null
var lastSelectionTime: Long = 0L
const val GAZE_COOLDOWN_MS = 3000 // 3 seconds
var navigationCooldownStartTime: Long = 0L
const val NAVIGATION_COOLDOWN_MS = 3000L // 3 seconds pause after navigation



// I use this helper to decide if the same phrase can be selected again

fun shouldSelectNew(phrase: String): Boolean {
    val now = SystemClock.elapsedRealtime()
    return phrase != lastSelectedPhrase || (now - lastSelectionTime > GAZE_COOLDOWN_MS)

}



@Composable
fun EyeGazeSelectionScreen(navController: NavController) {
    // Just grabbing some essentials from Compose to work with context, activity, lifecycle

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as? Activity
    // This will hold the MediaPipe gaze prediction results

    val predictions = remember { mutableStateOf<List<Category?>>(emptyList()) }
    // Camera needs its own executor so it doesn’t block UI

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    // Used to control the edit phrase dialog visibility

    val showEditDialog = remember { mutableStateOf(false) }

    // The camera preview view

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    // Tracks selected text for display or speaking

    var selectedText by remember { mutableStateOf("") }

    // Default phrase groups

    var greetingsPhrases by remember { mutableStateOf(listOf("Hello", "How are you?", "What's your name?", "Bye")) }
    var emergencyPhrases by remember { mutableStateOf(listOf("Help!", "Call Emergency Contact", "I need assistance", "Fire!")) }

    // The currently displayed phrase list
    var currentPhrases by remember { mutableStateOf(emptyList<String>()) }
    // These flags control which step of the selection we are in
    var isMainScreen by remember { mutableStateOf(true) }
    var isFinalSelection by remember { mutableStateOf(false) }
    var isIntermediateStep by remember { mutableStateOf(false) }

    // TTS state tracking
    var isSpeaking by remember { mutableStateOf(false) }
    var currentUtteranceId by remember { mutableStateOf("") }

    // Controls the dropdown menu in the corner
    val expanded = remember { mutableStateOf(false) }



    // This map makes sure phrases aren't spoken more than twice
    val phraseSpokenCount = remember { mutableStateMapOf<String, Int>() }

    val textToSpeech = remember {
        TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(context, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show()
            }
        }
    }



    // I configure the TTS engine when the screen loads

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
                isSpeaking = false
            }
        })
    }

    //Speaking a phrase only once or twice to avoid looping
    val speakOnceOrTwice = { phrase: String ->
        val count = phraseSpokenCount[phrase] ?: 0
        if (count < 2) {
            currentUtteranceId = System.currentTimeMillis().toString()
            isSpeaking = true
            textToSpeech.speak("$phrase selected", TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)
            phraseSpokenCount[phrase] = count + 1
        }
    }

    //MediaPipe Gaze Detection setup
    val faceLandmarkerHelper = remember {
        FaceLandmarkerHelper(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            faceLandmarkerHelperListener = object : FaceLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String, errorCode: Int) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }




                override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
                    predictions.value = UpdateResults(resultBundle.result)
                    // Don’t process if we are still speaking or recently navigated
                    if (!isSpeaking) {
                        if (SystemClock.elapsedRealtime() - navigationCooldownStartTime < NAVIGATION_COOLDOWN_MS) {
                            return // Skip if still in cooldown period
                        }
                        handleGazeDetection(
                            predictions = predictions.value,
                            context = context,
                            textToSpeech = textToSpeech,
                            isMainScreen = isMainScreen,
                            isFinalSelection = isFinalSelection,
                            isIntermediateStep = isIntermediateStep,
                            currentPhrases = currentPhrases,
                            onCategorySelected = { category ->
                                // First selection: greetings or emergency

                                speakOnceOrTwice(category)
                                currentPhrases = if (category == "Greetings") greetingsPhrases else emergencyPhrases
                                isMainScreen = false
                                isIntermediateStep = true
                            },
                            onPhraseSelected = { phrase ->
                                // Intermediate or final selection of phrases

                                if (isIntermediateStep) {
                                    val mid = (currentPhrases.size + 1) / 2
                                    val leftGroup = currentPhrases.subList(0, mid)
                                    val rightGroup = currentPhrases.subList(mid, currentPhrases.size)
                                    val selectedGroup = if (leftGroup.contains(phrase)) "Group Phrases 1" else "Group Phrases 2"
                                    speakOnceOrTwice(selectedGroup)
                                    currentPhrases = if (leftGroup.contains(phrase)) leftGroup else rightGroup

                                    isIntermediateStep = false
                                    isFinalSelection = true
                                } else if (isFinalSelection) {
                                    speakOnceOrTwice(phrase)

                                    if (phrase == "Call Emergency Contact") {
                                        makeDirectCall(context, activity!!, "+353851234567") // Replace with your desired number
                                    }
                                }
                            },

                                    onRestart = {
                                        // Resets everything to main menu state

                                        currentUtteranceId = System.currentTimeMillis().toString()
                                isSpeaking = true
                                textToSpeech.speak("Restarting", TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)
                                isMainScreen = true
                                isIntermediateStep = false
                                isFinalSelection = false
                                currentPhrases = emptyList()
                                phraseSpokenCount.clear()
                                lastSelectedPhrase = null
                                lastSelectionTime = 0L
                            },
                            onMainMenu = {
                                currentUtteranceId = System.currentTimeMillis().toString()
                                isSpeaking = true
                                textToSpeech.speak(
                                    "Returning to main menu",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    currentUtteranceId
                                )
                                Handler(Looper.getMainLooper()).postDelayed({
                                    safeNavigateWithCameraCleanup(context, navController as NavHostController, "main_menu")
                                }, 1000)
                                phraseSpokenCount.clear()
                                lastSelectedPhrase = null
                                lastSelectionTime = 0L

                            }
                        )
                    }
                }

                override fun onEmpty() {
                    predictions.value = emptyList()
                }
            }
        )
    }

    //Camera Setup
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
                    faceLandmarkerHelper.detectLiveStream(imageProxy, isFrontCamera = false)
                    imageProxy.close()
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e("CameraScreen", "Use case binding failed", exc)
        }
    }

    val (left, right) = when {
        isMainScreen -> listOf("Greetings") to listOf("Emergency Phrases")
        isIntermediateStep -> {
            val mid = (currentPhrases.size + 1) / 2
            currentPhrases.subList(0, mid) to currentPhrases.subList(mid, currentPhrases.size)
        }
        isFinalSelection -> listOfNotNull(currentPhrases.getOrNull(0)) to listOfNotNull(currentPhrases.getOrNull(1))
        else -> emptyList<String>() to emptyList()
    }


    //Handles layout and UI elements
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F1F1))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopStart
        ) {
            IconButton(onClick = { expanded.value = true }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.Black
                )
            }

            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Watch Tutorial") },
                    onClick = {
                        expanded.value = false
                        safeNavigateWithCameraCleanup(context, navController as NavHostController, "tutorial")
                    }
                )

                DropdownMenuItem(
                    text = { Text("Communication Screen Tutorial") },
                    onClick = {
                        expanded.value = false
                        safeNavigateWithCameraCleanup(context, navController as NavHostController, "tutorialVideo")
                    }
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        expanded.value = false
                        Toast.makeText(context, "SmartSpeak v1.0", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit Phrases") },
                    onClick = {
                        expanded.value = false
                        showEditDialog.value = true
                    }
                )

            }
        }

        Text(
            text = "SmartSpeak",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )



//erase the camera so the user don't see the camera
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


        Spacer(modifier = Modifier.height(24.dp))





        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "CLICK HERE TO START AGAIN",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                .padding(12.dp)
                .clickable {
                    currentUtteranceId = System.currentTimeMillis().toString()
                    isSpeaking = true
                    textToSpeech.speak("Restarting", TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)
                    isMainScreen = true
                    isIntermediateStep = false
                    isFinalSelection = false
                    currentPhrases = emptyList()
                },
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) //
        )
        {
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 8.dp) // Space between columns
                        .background(Color(0xFF4F7FE1), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxHeight()
                    )
                    {
                        left.forEach { phrase ->
                            Text(
                                text = phrase,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(Color(0xFF3F51B5), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                                    .clickable {
                                        if (!isSpeaking) {
                                            handlePhraseClick(
                                                phrase,
                                                isMainScreen,
                                                isIntermediateStep,
                                                textToSpeech,
                                                greetingsPhrases,
                                                emergencyPhrases,
                                                currentPhrases,
                                                onUpdate = {
                                                    currentPhrases = it
                                                    isMainScreen = false
                                                    isIntermediateStep = true
                                                },
                                                onFinal = { selectedGroup ->
                                                    currentPhrases = selectedGroup
                                                    isIntermediateStep = false
                                                    isFinalSelection = true
                                                }
                                            )

                                        }
                                    }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp) // Space between columns
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxHeight()
                    )

                    {
                        right.forEach { phrase ->
                            Text(
                                text = phrase,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                                    .clickable {
                                        if (!isSpeaking) {
                                            handlePhraseClick(
                                                phrase,
                                                isMainScreen,
                                                isIntermediateStep,
                                                textToSpeech,
                                                greetingsPhrases,
                                                emergencyPhrases,
                                                currentPhrases,
                                                onUpdate = {
                                                    currentPhrases = it
                                                    isMainScreen = false
                                                    isIntermediateStep = true
                                                },
                                                onFinal = { selectedGroup ->
                                                    currentPhrases = selectedGroup
                                                    isIntermediateStep = false
                                                    isFinalSelection = true
                                                }
                                            )

                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))


        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(
                onClick = {
                    currentUtteranceId = System.currentTimeMillis().toString()
                    isSpeaking = true
                    textToSpeech.speak("Returning to main menu", TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)
                    Handler(Looper.getMainLooper()).postDelayed({
                        safeNavigateWithCameraCleanup(context, navController as NavHostController, "main_menu")
                    }, 1000)
                },

                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier
                    .padding(8.dp)
                    .height(70.dp)
                    .width(200.dp) // Optional: slightly wider for longer text
            ) {
                Text(
                    text = "Back to Main Menu",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .background(Color(0xFFEAEAEA), shape = RoundedCornerShape(12.dp))
                .padding(vertical = 16.dp, horizontal = 24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "👈 Look Left to Restart Options",
                    fontSize = 16.sp,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "👉 Look Right to Return to Main Menu",
                    fontSize = 16.sp,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SmartSpeak v1.0",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )



        if (showEditDialog.value) {
            AlertDialog(
                onDismissRequest = { showEditDialog.value = false },
                title = { Text("Edit Phrases") },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 400.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("Greetings:")
                            greetingsPhrases.forEachIndexed { index, phrase ->
                                var updatedPhrase by remember { mutableStateOf(phrase) }
                                OutlinedTextField(
                                    value = updatedPhrase,
                                    onValueChange = {
                                        updatedPhrase = it
                                        greetingsPhrases = greetingsPhrases.toMutableList().also { list ->
                                            list[index] = it
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Emergency:")
                            emergencyPhrases.forEachIndexed { index, phrase ->
                                var updatedPhrase by remember { mutableStateOf(phrase) }
                                OutlinedTextField(
                                    value = updatedPhrase,
                                    onValueChange = {
                                        updatedPhrase = it
                                        emergencyPhrases = emergencyPhrases.toMutableList().also { list ->
                                            list[index] = it
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showEditDialog.value = false
                        Toast.makeText(context, "Phrases updated", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Done")
                    }
                }
            )
        }

    }

    }


//helper function to add touch screen and TTS
fun handlePhraseClick(
    phrase: String,
    isMainScreen: Boolean,
    isIntermediateStep: Boolean,
    textToSpeech: TextToSpeech,
    greetings: List<String>,
    emergency: List<String>,
    currentPhrases: List<String>,
    onUpdate: (List<String>) -> Unit,
    onFinal: (List<String>) -> Unit
) {
    val utteranceId = System.currentTimeMillis().toString()
    textToSpeech.speak("$phrase selected", TextToSpeech.QUEUE_FLUSH, null, utteranceId)

    if (isMainScreen) {
        val phrases = if (phrase == "Greetings") greetings else emergency
        onUpdate(phrases)
    } else if (isIntermediateStep) {
        val mid = (currentPhrases.size + 1) / 2
        val leftGroup = currentPhrases.subList(0, mid)
        val rightGroup = currentPhrases.subList(mid, currentPhrases.size)
        val selectedGroup = if (leftGroup.contains(phrase)) leftGroup else rightGroup
        onFinal(selectedGroup)
    }
}


//helper function for eye gaze
var leftGazeStartTime: Long? = null
var rightGazeStartTime: Long? = null
var isLeftGazeTriggered = false
var isRightGazeTriggered = false

fun handleGazeDetection(
    predictions: List<Category?>,
    context: Context,
    textToSpeech: TextToSpeech,
    isMainScreen: Boolean,
    isFinalSelection: Boolean,
    isIntermediateStep: Boolean,
    currentPhrases: List<String>,
    onCategorySelected: (String) -> Unit,
    onPhraseSelected: (String) -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    val leftGaze = predictions.find { it?.categoryName() == "eyeLookOutLeft" && it.score() > 0.55 }
    val rightGaze = predictions.find { it?.categoryName() == "eyeLookOutRight" && it.score() > 0.55 }

    val currentTime = SystemClock.elapsedRealtime()

    // Handle LEFT gaze
    if (leftGaze != null) {
        if (leftGazeStartTime == null) {
            leftGazeStartTime = currentTime
            isLeftGazeTriggered = false
        } else if (!isLeftGazeTriggered && currentTime - leftGazeStartTime!! >= 2000) {
            // Stop any speech after 2 seconds
            textToSpeech.stop()
        }

        if (!isLeftGazeTriggered && currentTime - leftGazeStartTime!! >= 4000) {
            isLeftGazeTriggered = true
            textToSpeech.speak("Restarting options", TextToSpeech.QUEUE_FLUSH, null, null)
            navigationCooldownStartTime = SystemClock.elapsedRealtime()

            Handler(Looper.getMainLooper()).postDelayed({
                onRestart()
                leftGazeStartTime = null
                isLeftGazeTriggered = false
            }, 1000)
            return
        }
    } else {
        leftGazeStartTime = null
        isLeftGazeTriggered = false
    }

// Handle RIGHT gaze
    if (rightGaze != null) {
        if (rightGazeStartTime == null) {
            rightGazeStartTime = currentTime
            isRightGazeTriggered = false
        } else if (!isRightGazeTriggered && currentTime - rightGazeStartTime!! >= 2000) {
            textToSpeech.stop()
        }

        if (!isRightGazeTriggered && currentTime - rightGazeStartTime!! >= 4000) {
            isRightGazeTriggered = true
            textToSpeech.speak("Returning to main menu", TextToSpeech.QUEUE_FLUSH, null, null)
            navigationCooldownStartTime = SystemClock.elapsedRealtime()

            Handler(Looper.getMainLooper()).postDelayed({
                onMainMenu()
                rightGazeStartTime = null
                isRightGazeTriggered = false
            }, 1000)
            return
        }
    } else {
        rightGazeStartTime = null
        isRightGazeTriggered = false
    }


    // Handle quick gaze-based selections
    if (isMainScreen) {
        if (SystemClock.elapsedRealtime() - navigationCooldownStartTime < NAVIGATION_COOLDOWN_MS) return

        if (leftGaze != null && shouldSelectNew("Greetings")) {
            lastSelectedPhrase = "Greetings"
            lastSelectionTime = SystemClock.elapsedRealtime()
            onCategorySelected("Greetings")
        } else if (rightGaze != null && shouldSelectNew("Emergency Phrases")) {
            lastSelectedPhrase = "Emergency Phrases"
            lastSelectionTime = SystemClock.elapsedRealtime()
            onCategorySelected("Emergency Phrases")
        }
    } else if (isIntermediateStep) {
        if (SystemClock.elapsedRealtime() - navigationCooldownStartTime < NAVIGATION_COOLDOWN_MS) return

        val mid = (currentPhrases.size + 1) / 2
        val left = currentPhrases.subList(0, mid)
        val right = currentPhrases.subList(mid, currentPhrases.size)

        if (leftGaze != null && left.isNotEmpty() && shouldSelectNew(left[0])) {
            lastSelectedPhrase = left[0]
            lastSelectionTime = SystemClock.elapsedRealtime()
            onPhraseSelected(left[0])
        } else if (rightGaze != null && right.isNotEmpty() && shouldSelectNew(right[0])) {
            lastSelectedPhrase = right[0]
            lastSelectionTime = SystemClock.elapsedRealtime()
            onPhraseSelected(right[0])
        }
    } else if (isFinalSelection) {
        if (SystemClock.elapsedRealtime() - navigationCooldownStartTime < NAVIGATION_COOLDOWN_MS) return

        if (leftGaze != null && currentPhrases.isNotEmpty() && shouldSelectNew(currentPhrases[0])) {
            lastSelectedPhrase = currentPhrases[0]
            lastSelectionTime = SystemClock.elapsedRealtime()
            onPhraseSelected(currentPhrases[0])
        } else if (rightGaze != null && currentPhrases.size > 1 && shouldSelectNew(currentPhrases[1])) {
            lastSelectedPhrase = currentPhrases[1]
            lastSelectionTime = SystemClock.elapsedRealtime()
            onPhraseSelected(currentPhrases[1])
        }
    }}

fun requestCallPermission(context: Context, activity: Activity) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.CALL_PHONE),
        1
    )
}

fun makeDirectCall(context: Context, activity: Activity, number: String) {
    val intent = Intent(Intent.ACTION_CALL).apply {
        data = Uri.parse("tel:$number")
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
        == PackageManager.PERMISSION_GRANTED) {
        context.startActivity(intent)
    } else {
        requestCallPermission(context, activity)
    }
}

//helper function to avoid app to crash
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
