package com.example.pathfinder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.chaquo.python.Python
import com.chaquo.python.PyObject
import com.google.firebase.auth.FirebaseAuth
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var tflite: Interpreter? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private lateinit var playerView: PlayerView
    private lateinit var fullscreenButton: Button
    private lateinit var previewView: PreviewView
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isFullscreen = false

    private val VIDEO_REQUEST_CODE = 100 // You can use any unique code for video recording result
    private val REQUEST_CAMERA_PERMISSION = 200 // Permission request code for camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the PlayerView and buttons
        previewView = findViewById(R.id.previewView)
        playerView = findViewById(R.id.player_view)
        fullscreenButton = findViewById(R.id.fullscreen_button)

        val buttonOption1 = findViewById<Button>(R.id.button_option1)
        val buttonOption2 = findViewById<Button>(R.id.button_option2)
        val buttonRunPython = findViewById<Button>(R.id.button_run_python)
        val signOutButton = findViewById<Button>(R.id.signOutButton)

        try {
            tflite = Interpreter(loadModelFile())

            val inputShape = tflite?.getInputTensor(0)?.shape() // Example: [1, 224, 224, 3]
            val inputHeight = inputShape?.get(1) ?: 224
            val inputWidth = inputShape?.get(2) ?: 224

            Log.d("Model Input Size", "Height: $inputHeight, Width: $inputWidth")

        } catch (e: Exception) {
            e.printStackTrace()
        }

        ProcessCameraProvider.getInstance(this).addListener({
            cameraProvider = ProcessCameraProvider.getInstance(this).get()
        }, ContextCompat.getMainExecutor(this))

        // Set click listeners for each button
        buttonOption1.setOnClickListener {
            playLocalVideo("home_climbing_gym") // Video 1
        }

        buttonOption2.setOnClickListener {
            previewView.visibility = View.VISIBLE
            startCamera()
        }

        buttonRunPython.setOnClickListener {
            runPythonScript()
        }

        // Sign-Out Button Logic
        signOutButton.setOnClickListener {
            // Sign the user out
            FirebaseAuth.getInstance().signOut()

            // Clear the "Remember Me" preference
            //val sharedPreferences = getSharedPreferences("pathfinder_prefs", MODE_PRIVATE)
            //sharedPreferences.edit().putBoolean("remember_me", false).apply()

            // Redirect to LandingActivity
            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun playLocalVideo(videoName: String) {
        // Set PlayerView visibility to visible
        playerView.visibility = View.VISIBLE

        // Initialize ExoPlayer and set up media playback
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer

            // Construct the URI for the selected video
            val videoUri = Uri.parse("android.resource://${packageName}/raw/$videoName")
            val mediaItem = MediaItem.fromUri(videoUri)
            exoPlayer.setMediaItem(mediaItem)

            // Prepare and start playback
            exoPlayer.prepare()
            exoPlayer.play()
        }

        // Show fullscreen button after video starts playing
        fullscreenButton.visibility = View.VISIBLE
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val overlay = CustomOverlay(this)
            addContentView(overlay, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Ensure real-time analysis
                .build()

            // Set the analyzer to process image frames
            imageAnalysis.setAnalyzer(executor) { image ->
                try {
                    val inputShape = tflite?.getInputTensor(0)?.shape()
                    val inputBuffer = resizeImage(image, inputShape?.get(1) ?: 224, inputShape?.get(2) ?: 224)

                    // Capture the output from runInference
                    val output = runInference(inputBuffer)

                    val keypoints = scaleKeypoints(output, image.width.toFloat(), image.height.toFloat())

                    overlay.setKeypoints(keypoints)
                } catch (e: Exception) {
                    Log.e("CameraActivity", "Error during inference: ${e.message}", e)
                } finally {
                    image.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll() // Unbind existing use cases
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis // Bind the image analyzer here
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to start camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun scaleKeypoints(rawOutput: Array<Array<Array<FloatArray>>>, imageWidth: Float, imageHeight: Float): List<Pair<Float, Float>> {
        val keypoints = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until 17) {
            val x = rawOutput[0][0][i][0] * imageWidth
            val y = rawOutput[0][0][i][1] * imageHeight
            val confidence = rawOutput[0][0][i][2]

            // Log even if confidence is low
            Log.d("Keypoint", "Keypoint $i: x=$x, y=$y, confidence=$confidence")

            if (confidence > 0.5) {
                keypoints.add(Pair(x, y))
            }
        }
        Log.d("Keypoints", "All keypoints: $keypoints")
        return keypoints
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VIDEO_REQUEST_CODE && resultCode == RESULT_OK) {
            val videoUri: Uri? = data?.data
            if (videoUri != null) {
                playRecordedVideo(videoUri)
            }
        }
    }

    private fun resizeImage(image: ImageProxy, targetWidth: Int, targetHeight: Int): ByteBuffer {
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

        val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val jpegBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true) // Use 'true' for smoother scaling

        // Create ByteBuffer for model input
        val inputBuffer = ByteBuffer.allocateDirect(4 * targetWidth * targetHeight * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(targetWidth * targetHeight)
        resizedBitmap.getPixels(intValues, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        for (pixel in intValues) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
        }

        val debugFile = File(filesDir, "debug_image.jpg")
        FileOutputStream(debugFile).use { out ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        Log.d("Debug Image", "Saved preprocessed image to: ${debugFile.absolutePath}")


        return inputBuffer
    }


    private fun runInference(byteBuffer: ByteBuffer): Array<Array<Array<FloatArray>>> {
        val output = Array(1) { Array(1) { Array(17) { FloatArray(3) } } }
        tflite?.run(byteBuffer, output)

        // Log the output to check if keypoints are being processed
        Log.d("InferenceOutput", "Output: ${output.joinToString(", ")}")
        return output
    }

    private fun smoothKeypoints(keypoints: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        // Simple moving average or Kalman filtering could go here
        return keypoints // This is just a placeholder for now
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd("lite-model_movenet_singlepose_lightning_3.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun playRecordedVideo(videoUri: Uri) {
        playerView.visibility = View.VISIBLE
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(videoUri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
        fullscreenButton.visibility = View.VISIBLE
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            fullscreenButton.text = "Exit Fullscreen"
        } else {
            playerView.layoutParams.height = 300
            playerView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            fullscreenButton.text = "Fullscreen"
        }
        playerView.requestLayout()
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    override fun onDestroy() {
        super.onDestroy()
        tflite?.close()
    }

    private fun runPythonScript() {
        val python = Python.getInstance()
        val py = python.getModule("Hello")
        val result: PyObject = py.callAttr("greet")
        Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show()
    }
}