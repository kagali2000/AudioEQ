package com.example.audioeq

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null

    private lateinit var btnPlay: Button
    private lateinit var tvStatus: TextView
    private lateinit var bandsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnPlay = findViewById(R.id.btnPlay)
        tvStatus = findViewById(R.id.tvStatus)
        bandsContainer = findViewById(R.id.bandsContainer)

        btnPlay.setOnClickListener {
            if (mediaPlayer == null) {
                startPlayback()
            } else {
                togglePlayback()
            }
        }
    }

    private fun startPlayback() {
        // create MediaPlayer with a local resource in res/raw/sample.mp3
        val afd = resources.openRawResourceFd(R.raw.sample)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            isLooping = true
            prepareAsync()
            setOnPreparedListener {
                start()
                tvStatus.text = "Status: playing"
                btnPlay.text = "Pause"
                initEqualizer(audioSessionId)
            }
            setOnCompletionListener {
                tvStatus.text = "Status: completed"
            }
        }
    }

    private fun togglePlayback() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                tvStatus.text = "Status: paused"
                btnPlay.text = "Play"
            } else {
                mp.start()
                tvStatus.text = "Status: playing"
                btnPlay.text = "Pause"
            }
        }
    }

    private fun initEqualizer(sessionId: Int) {
        equalizer?.release()

        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot create Equalizer: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        val eq = equalizer ?: return

        val numberOfBands = eq.numberOfBands.toInt()
        val bandLevelRange = eq.bandLevelRange
        val minLevel = bandLevelRange[0].toInt()
        val maxLevel = bandLevelRange[1].toInt()

        bandsContainer.removeAllViews()

        val header = TextView(this).apply {
            text = "Equalizer - $numberOfBands bands (range ${minLevel/100}..${maxLevel/100} dB)"
            textSize = 16f
        }
        bandsContainer.addView(header)

        for (i in 0 until numberOfBands) {
            val freqRange = eq.getCenterFreq(i.toShort()) / 1000
            val bandLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                }
            }

            val label = TextView(this).apply {
                text = "Band ${i + 1} — ${freqRange} Hz"
            }
            bandLayout.addView(label)

            val seek = SeekBar(this).apply {
                max = maxLevel - minLevel
                val currentLevel = eq.getBandLevel(i.toShort()).toInt()
                progress = currentLevel - minLevel
            }

            val valueLabel = TextView(this).apply {
                val currentDb = (eq.getBandLevel(i.toShort()).toInt() / 100.0)
                text = String.format("%.2f dB", currentDb)
            }

            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val level = (progress + minLevel).toShort()
                    eq.setBandLevel(i.toShort(), level)
                    val db = level.toInt() / 100.0
                    valueLabel.text = String.format("%.2f dB", db)
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            bandLayout.addView(seek)
            bandLayout.addView(valueLabel)
            bandsContainer.addView(bandLayout)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        equalizer?.apply {
            enabled = false
            release()
        }
        equalizer = null

        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }
}
