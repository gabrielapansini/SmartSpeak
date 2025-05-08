package com.example.looktospeakimp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult


// I use this helper class to initialize and manage the MediaPipe Face Landmarker, process live camera frames,
// detect face landmarks in real-time, and notify the app when meaningful gaze-related movements (like upward movement) are detected.

class FaceLandmarkerHelper(
    var minFaceDetectionConfidence: Float = DEFAULT_FACE_DETECTION_CONFIDENCE,
    var minFaceTrackingConfidence: Float = DEFAULT_FACE_TRACKING_CONFIDENCE,
    var minFacePresenceConfidence: Float = DEFAULT_FACE_PRESENCE_CONFIDENCE,
    var maxNumFaces: Int = DEFAULT_NUM_FACES,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.IMAGE,
    val context: Context,
    val faceLandmarkerHelperListener: LandmarkerListener? = null
) {

    private var faceLandmarker: FaceLandmarker? = null

    init {
        setupFaceLandmarker()
    }

    // I use this to cleanly shut down the FaceLandmarker instance when it's no longer needed.

    fun clearFaceLandmarker() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    // I use this to check if the FaceLandmarker has already been closed or not initialized yet.

    fun isClose(): Boolean {
        return faceLandmarker == null
    }

    // I use this to configure and initialize the FaceLandmarker with confidence levels, running mode, and the delegate (CPU/GPU).
    // It also attaches result and error listeners when in live stream mode.
    fun setupFaceLandmarker() {
        val baseOptionBuilder = BaseOptions.builder()
        when (currentDelegate) {
            DELEGATE_CPU -> baseOptionBuilder.setDelegate(Delegate.CPU)
            DELEGATE_GPU -> baseOptionBuilder.setDelegate(Delegate.GPU)
        }
        baseOptionBuilder.setModelAssetPath(MP_FACE_LANDMARKER_TASK)

        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                if (faceLandmarkerHelperListener == null) {
                    throw IllegalStateException(
                        "faceLandmarkerHelperListener must be set when runningMode is LIVE_STREAM."
                    )
                }
            }
            RunningMode.IMAGE -> TODO()
            RunningMode.VIDEO -> TODO()
        }

        try {
            val baseOptions = baseOptionBuilder.build()
            val optionsBuilder =
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinFaceDetectionConfidence(minFaceDetectionConfidence)
                    .setMinTrackingConfidence(minFaceTrackingConfidence)
                    .setMinFacePresenceConfidence(minFacePresenceConfidence)
                    .setNumFaces(maxNumFaces)
                    .setOutputFaceBlendshapes(true)
                    .setRunningMode(runningMode)

            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }

            val options = optionsBuilder.build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            faceLandmarkerHelperListener?.onError(
                "Face Landmarker failed to initialize. See error logs for details"
            )
            Log.e(TAG, "MediaPipe failed to load the task with error: ${e.message}")
        }
    }


    // I call this on every camera frame in live stream mode. It converts the ImageProxy to a rotated Bitmap,
    // mirrors it if the front camera is used, and sends it to MediaPipe for asynchronous detection.
    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Attempting to call detectLiveStream while not using RunningMode.LIVE_STREAM"
            )
        }
        val frameTime = SystemClock.uptimeMillis()

        val bitmapBuffer =
            Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)

        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            }
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        detectAsync(mpImage, frameTime)
    }

    // I use this internal method to send the frame to the FaceLandmarker asynchronously for inference.

    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        faceLandmarker?.detectAsync(mpImage, frameTime)
    }

    // This is triggered when MediaPipe detects face landmarks. I calculate if the eyes are moving upward
    // by comparing the y-position of the eyes to the forehead and trigger a result or empty state accordingly.
    private fun returnLivestreamResult(result: FaceLandmarkerResult, input: MPImage) {
        if (result.faceLandmarks().isNotEmpty()) {
            val landmarks = result.faceLandmarks()[0]

            val leftEyeUpper = landmarks[159].y()
            val rightEyeUpper = landmarks[386].y()
            val forehead = landmarks[10].y()

            val leftDiff = leftEyeUpper - forehead
            val rightDiff = rightEyeUpper - forehead
            val upThreshold = 0.07

            Log.d(TAG, "Landmark Positions: Forehead: $forehead, Left Eye: $leftEyeUpper, Right Eye: $rightEyeUpper")
            Log.d(TAG, "Eye-Forehead Difference: Left: $leftDiff, Right: $rightDiff")

            if (leftDiff > upThreshold || rightDiff > upThreshold) {
                Log.d(TAG, "Detected UPWARD movement manually!")
                faceLandmarkerHelperListener?.onResults(
                    ResultBundle(result, SystemClock.uptimeMillis(), input.height, input.width)
                )
            } else {
                faceLandmarkerHelperListener?.onEmpty()
            }
        } else {
            faceLandmarkerHelperListener?.onEmpty()
        }
    }

    // If an error occurs during detection, I use this to send it back to the UI through the listener.

    private fun returnLivestreamError(error: RuntimeException) {
        faceLandmarkerHelperListener?.onError(error.message ?: "An unknown error has occurred")
    }

    // I use this tag to label log messages from this helper so I can filter them easily in Logcat.

    companion object {
        const val TAG = "FaceLandmarkerHelper"
        // This is the name of the .task file (MediaPipe model) stored in the assets folder that the landmarker will load.
        private const val MP_FACE_LANDMARKER_TASK = "face_landmarker.task"


        // These constants define which processing unit to use — CPU or GPU.

        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1

        // These are the default confidence levels used when detecting and tracking faces.

        const val DEFAULT_FACE_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_FACE_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_FACE_PRESENCE_CONFIDENCE = 0.5F

        // This limits the face detector to look for just 1 face at a time.

        const val DEFAULT_NUM_FACES = 1
        // This is a generic error code I return when something goes wrong but I don’t have a specific error code.

        const val OTHER_ERROR = 0
    }


    // I use this data class to bundle together the detection result, the time it took, and the input image dimensions.
// It makes it easier to send everything at once to the UI or another class that wants to display or act on this data.
    data class ResultBundle(
        val result: FaceLandmarkerResult,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )


    // I use this interface to send updates from the FaceLandmarkerHelper back to the UI or wherever it's being used.
// onError → sends errors
// onResults → sends results when a face and landmarks are found
// onEmpty → tells the UI that nothing useful was detected (optional to override)
    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
        fun onEmpty() {}
    }
}
