package com.example.videofeed

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize PlayerView
        playerView = findViewById(R.id.player_view)

        // Initialize ExoPlayer
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer

            // Set the media source (URL of your video feed)
            val videoUri = Uri.parse("https://v.animethemes.moe/HunterHunter2011-OP1.webm") // Replace with your stream URL
            val mediaItem = MediaItem.fromUri(videoUri)
            exoPlayer.setMediaItem(mediaItem)

            // Prepare and start playback
            exoPlayer.prepare()
            exoPlayer.play()
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
}