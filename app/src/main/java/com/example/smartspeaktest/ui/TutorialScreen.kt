package com.example.smartspeaktest.ui

import android.content.Context
import android.net.Uri
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
import com.example.smartspeaktest.R


// I use this screen to show a tutorial video using ExoPlayer, so users can learn how to use the SmartSpeak app before navigating to the main menu or other sections.

@Composable
fun TutorialScreen(navController: NavController) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(
                Uri.parse("android.resource://${context.packageName}/raw/intro")
            )
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    var expanded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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

        Text(
            text = "SmartSpeak Tutorial",
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AndroidView(
            factory = {
                PlayerView(context).apply {
                    this.player = player
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("main_menu") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
        ) {
            Text("Back to Menu", color = Color.White)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "SmartSpeak v1.0",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
