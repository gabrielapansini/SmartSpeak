package com.example.looktospeakimp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

// I use this class to extract, filter, and display the most relevant eye gaze predictions (like "eyeLookOutLeft")
// from MediaPipe's FaceLandmarkerResult, so I can debug and visually confirm what the model is detecting in real time.

@Composable
fun PredictionsList(predictions: List<Category?>) {
    // I'm using LazyColumn to efficiently render a scrollable list of predictions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("New Prediction")
        }
        items(predictions) { category ->
            category?.let {
                PredictionItem(categoryName = it.categoryName(), score = it.score())
            }
        }
    }
}

@Composable
fun PredictionItem(categoryName: String?, score: Float?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = categoryName ?: "--")
        Text(text = score?.let { String.format("%.2f", it) } ?: "--")
    }
}


fun UpdateResults(faceLandmarkerResult: FaceLandmarkerResult?): List<Category?> {
    val scoreThreshold = 0.40f
    val newCategories = mutableListOf<Category?>()
    if (faceLandmarkerResult != null && faceLandmarkerResult.faceBlendshapes().isPresent) {
        val faceBlendshapes = faceLandmarkerResult.faceBlendshapes().get()
        val sortedCategories = faceBlendshapes[0]
            .filter { it.categoryName().startsWith("eyeLook") && it.score() >= scoreThreshold }
            .sortedByDescending { it.score() }
        newCategories.addAll(sortedCategories)
    }
    return newCategories
}