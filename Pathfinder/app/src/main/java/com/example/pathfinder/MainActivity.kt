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
import com.chaquo.python.Python
import com.chaquo.python.PyObject

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var fullscreenButton: Button
    private var isFullscreen = false

    private val VIDEO_REQUEST_CODE = 100 // You can use any unique code for video recording result
    private val REQUEST_CAMERA_PERMISSION = 200 // Permission request code for camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get references to the buttons
        playerView = findViewById(R.id.player_view)
        fullscreenButton = findViewById(R.id.fullscreen_button)

        val buttonOption1 = findViewById<Button>(R.id.button_option1)
        val buttonOption2 = findViewById<Button>(R.id.button_option2)
        val buttonRunPython = findViewById<Button>(R.id.button_run_python) // New button to run Python script

        // Set click listeners for each video button
        buttonOption1.setOnClickListener {
            playLocalVideo("home_climbing_gym") // Video 1
        }

        buttonOption2.setOnClickListener {
            // Check for camera permission before starting video recording
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            } else {
                // Permission is already granted, proceed with video recording
                startVideoRecording()
            }
        }

        // Set click listener for fullscreen button
        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        // Set click listener for running Python script
        buttonRunPython.setOnClickListener {
            runPythonScript()
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

    private fun startVideoRecording() {
        // Start the video recording activity
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)

        // You can optionally specify the maximum duration for the video capture (in milliseconds)
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60) // Limit to 60 seconds

        // Start the video recording activity and listen for the result
        startActivityForResult(intent, VIDEO_REQUEST_CODE)
    }

    // Handle the result of the video capture
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VIDEO_REQUEST_CODE && resultCode == RESULT_OK) {
            // The video was successfully recorded
            val videoUri: Uri? = data?.data

            if (videoUri != null) {
                // Play the recorded video in the ExoPlayer
                playRecordedVideo(videoUri)
            }
        }
    }

    private fun playRecordedVideo(videoUri: Uri) {
        // Set PlayerView visibility to visible
        playerView.visibility = View.VISIBLE

        // Initialize ExoPlayer and set up media playback
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer

            // Set the media item to the recorded video
            val mediaItem = MediaItem.fromUri(videoUri)
            exoPlayer.setMediaItem(mediaItem)

            // Prepare and start playback
            exoPlayer.prepare()
            exoPlayer.play()
        }

        // Show fullscreen button after video starts playing
        fullscreenButton.visibility = View.VISIBLE
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen

        if (isFullscreen) {
            // Set PlayerView to fullscreen
            playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            fullscreenButton.text = "Exit Fullscreen"
        } else {
            // Set PlayerView to normal size
            playerView.layoutParams.height = 300 // or any normal height you prefer
            playerView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            fullscreenButton.text = "Fullscreen"
        }

        // Request layout update
        playerView.requestLayout()
    }

    // Handle the result of the camera permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with video recording
                startVideoRecording()
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Camera permission is required to record video", Toast.LENGTH_SHORT).show()
            }
        }
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

    // Run the Python script
    private fun runPythonScript() {
        val python = Python.getInstance()
        val py = python.getModule("Hello")  // Replace with the name of your Python script without the .py extension
        val result: PyObject = py.callAttr("greet")

        // Show the result in a Toast message
        Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show()
    }
}
