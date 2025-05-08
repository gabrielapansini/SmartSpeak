package com.example.looktospeakimp.ui

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.smartspeaktest.R


// I use this screen to play a specific tutorial video for the communication feature using ExoPlayer, and once the video ends, I automatically navigate the user to the camera screen to begin using eye gaze selection.

@Composable
fun TutorialVideoScreen(navController: NavController) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(
                Uri.parse("android.resource://${context.packageName}/raw/communication_tutorial")
            )
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }


    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300) // Wait for UI to settle
        player.playWhenReady = true
        player.play() // Force start playback
    }


    DisposableEffect(Unit) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    navController.navigate("camera") {
                        popUpTo("tutorialVideo") { inclusive = true }
                    }
                }
            }
        })

        onDispose { player.release() }
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F1F1))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Main Menu") },
                    onClick = { navController.navigate("main_menu") }
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
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        expanded = false
                        Toast.makeText(context, "SmartSpeak v1.0", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))



        Spacer(modifier = Modifier.height(16.dp))

        AndroidView(
            factory = {
                PlayerView(context).apply {
                    this.player = player
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f) // or even 0.85f

        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(
                    onClick = { navController.navigate("controlDevices") },



                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier
                    .padding(8.dp)
                    .height(70.dp)
                    .width(200.dp) // Optional: slightly wider for longer text
            ) {
                Text(
                    text = "Start Communicating",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))


        Text(
            text = "SmartSpeak v1.0",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
