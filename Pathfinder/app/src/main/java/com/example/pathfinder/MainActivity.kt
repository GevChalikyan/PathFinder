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
import com.google.firebase.auth.FirebaseAuth

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

        // Initialize the PlayerView and buttons
        playerView = findViewById(R.id.player_view)
        fullscreenButton = findViewById(R.id.fullscreen_button)

        val buttonOption1 = findViewById<Button>(R.id.button_option1)
        val buttonOption2 = findViewById<Button>(R.id.button_option2)
        val buttonRunPython = findViewById<Button>(R.id.button_run_python)
        val signOutButton = findViewById<Button>(R.id.signOutButton)

        // Set click listeners for each button
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

        fullscreenButton.setOnClickListener {
            toggleFullscreen()
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

    private fun startVideoRecording() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60) // Limit to 60 seconds
        startActivityForResult(intent, VIDEO_REQUEST_CODE)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVideoRecording()
            } else {
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

    private fun runPythonScript() {
        val python = Python.getInstance()
        val py = python.getModule("Hello")
        val result: PyObject = py.callAttr("greet")
        Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show()
    }
}
