package com.example.looktospeakimp.ui

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState


// I use this screen to request and check camera permission before starting the app,
// and based on whether the user has seen the tutorial before, I navigate them either to the welcome screen or straight to the main menu.

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(navController: NavController) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA)
    )

    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    if (permissionsState.allPermissionsGranted && !hasNavigated) {
        hasNavigated = true

        // 🔐 Check if the tutorial was already watched
        val sharedPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val hasWatchedTutorial = sharedPrefs.getBoolean("hasWatchedTutorial", false)

        Toast.makeText(context, "Permission request granted", Toast.LENGTH_LONG).show()

        navController.navigate(
            if (hasWatchedTutorial) "main_menu" else "welcome"
        ) {
            popUpTo("permissions") { inclusive = true }
        }

    } else if (permissionsState.shouldShowRationale) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera permission is required to use this app.")
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    } else if (!permissionsState.allPermissionsGranted && hasNavigated) {
        Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
    }
}
