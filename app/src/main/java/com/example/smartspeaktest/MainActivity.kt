package com.example.smartspeaktest


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
//import com.example.looktospeakimp.ui.CameraScreen
import com.example.looktospeakimp.ui.EyeGazeSelectionScreen
import com.example.looktospeakimp.ui.PermissionsScreen
import com.example.looktospeakimp.ui.TutorialVideoScreen
import com.example.looktospeakimp.ui.theme.SmartSpeakTestTheme
import com.example.smartspeaktest.ui.ControlDevicesScreen
import com.example.smartspeaktest.ui.SmartSpeakMainMenu
import com.example.smartspeaktest.ui.TutorialScreen
import com.example.smartspeaktest.ui.WelcomeScreen
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartSpeakTestTheme   {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

val database = Firebase.database("https://smartspeak-d1d14-default-rtdb.europe-west1.firebasedatabase.app/")
val lightRef = database.getReference("light")


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "permissions") {
        composable("permissions") { PermissionsScreen(navController) }
        composable("camera") { EyeGazeSelectionScreen(navController = navController) }
        composable("main_menu") { SmartSpeakMainMenu(navController) }
        composable("controlDevices") { ControlDevicesScreen(navController) }
        composable("welcome") { WelcomeScreen(navController) }
        composable("tutorial") { TutorialScreen(navController) }
        composable("tutorialVideo") { TutorialVideoScreen(navController) }


    }

}


