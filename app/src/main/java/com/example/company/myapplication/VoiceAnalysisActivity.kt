package com.example.company.myapplication

import android.Manifest
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface.BOLD
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.StyleSpan
import android.view.*
import android.widget.Button
import android.widget.TextView
import java.util.concurrent.TimeUnit

const val AUDIO_RECORDING = "APST.ANALYSIS_ACTIVITY"
const val RECORD_AUDIO_PERMISSION = 200 // change constant?
const val RECORDING_FOLDER = "public_speech_trainer/recordings" // temporary name?
const val POST_COUNTDOWN_ACTION = "com.example.company.myapplication.ACTION_POST_COUNTDOWN" // TODO: change name later when we get a new package name
const val POST_SPEECH_ACTION = "com.example.company.myapplication.ACTION_POST_RECORDING"
const val NEXT_SLIDE_BUTTON_ACTION = "com.example.company.myapplication.ACTION_NEXT_SLIDE_BUTTON"
const val SAMPLING_RATE = 44100


class VoiceAnalysisActivity : AppCompatActivity() {
    private val postCountdownReceiver = PostCountdownReceiver()
    private val postSpeechReceiver = PostSpeechReceiver()
    private val setNextSlideButtonReceiver = SetNextSlideButtonReceiver()

    lateinit var audioAnalyzer: AudioAnalyzer

    private var finishedRecording = false

    private lateinit var resultsTextView: TextView
    private lateinit var stopRecordingButton: Button

    var nextButtonPressed = false

    private fun initLayout() {
        val startRecordingButton = findViewById<Button>(R.id.start_recording_button)
        stopRecordingButton = findViewById(R.id.stop_recording_button)
        val nextSlideButton = findViewById<Button>(R.id.next_slide_button)
        resultsTextView = findViewById(R.id.voice_analysis_text_view)

        stopRecordingButton.isEnabled = false

        startRecordingButton.setOnClickListener {
            startRecordingButton.isEnabled = false
            stopRecordingButton.isEnabled = true
            resultsTextView.text = ""

            audioAnalyzer = AudioAnalyzer(this)
            initCountdown()
        }

        stopRecordingButton.setOnClickListener {
            startRecordingButton.isEnabled = true
            stopRecordingButton.isEnabled = false
        }

        nextSlideButton.setOnClickListener {
            nextButtonPressed = true
        }

        resultsTextView.movementMethod = ScrollingMovementMethod()
    }

    private fun initCountdown() {
        val countdownDialogFragment = CountdownDialogFragment()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction
            .add(android.R.id.content, countdownDialogFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun initAudioRecording() {
        val sharedPref = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        val defaultValue = false
        val isRecordingOn = sharedPref.getBoolean(getString(R.string.audio_recording), defaultValue)

        if (isRecordingOn) {
            addPermissionsForAudioRecording()
        } else {
            finishedRecording = true
        }
    }

    private fun addPermissionsForAudioRecording() {
        val recordingPermissionStatus =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val storingPermissionStatus =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val permissionsToRequest = mutableListOf<String>()
        if (recordingPermissionStatus != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (storingPermissionStatus != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, permissionsToRequest.toTypedArray(),
                RECORD_AUDIO_PERMISSION
            )
        } else {
            audioAnalyzer.recordSpeechAudio({ stopRecordingButton.isEnabled }, { nextButtonPressed })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                audioAnalyzer.recordSpeechAudio({ stopRecordingButton.isEnabled }, { nextButtonPressed })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_analysis)

        audioAnalyzer = AudioAnalyzer(this)
        initLayout()
    }

    override fun onPause() {
        super.onPause()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(postCountdownReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(postSpeechReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(setNextSlideButtonReceiver)
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(postCountdownReceiver,
                IntentFilter(POST_COUNTDOWN_ACTION))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(postSpeechReceiver,
                IntentFilter(POST_SPEECH_ACTION))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(setNextSlideButtonReceiver,
                IntentFilter(NEXT_SLIDE_BUTTON_ACTION))
    }

    abstract inner class AudioAnalyzerReceiver : BroadcastReceiver() {
        protected fun outputTitleAndContent(title: String, vararg content: String) {
            resultsTextView.append("\n")
            val spannableStringBuilder = SpannableStringBuilder(title)
            for (string in content) {
                spannableStringBuilder.append(string)
            }
            spannableStringBuilder.setSpan(StyleSpan(BOLD), 0, title.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            resultsTextView.append(spannableStringBuilder)
        }

        protected fun outputVolumeLevels(title: String, volumeLevels: Triple<Double, Double, Double>) {
            outputTitleAndContent(title,
                "\n${getString(R.string.minimal)}: %.2f dB".format(volumeLevels.first),
                "\n${getString(R.string.maximal)}: %.2f dB".format(volumeLevels.second),
                "\n${getString(R.string.average)}: %.2f dB\n".format(volumeLevels.third))
        }
    }

    inner class PostCountdownReceiver : AudioAnalyzerReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            outputVolumeLevels(getString(R.string.countdown_volume_levels),
                audioAnalyzer.getCountdownVolumeLevels())
            initAudioRecording()
        }
    }

    inner class PostSpeechReceiver : AudioAnalyzerReceiver() {
        private fun outputSlideInformation(slideInfo: SlideInfo) {
            val margin = "  "

            outputTitleAndContent("${getString(R.string.slide)} ${slideInfo.slideNumber}:\n")
            resultsTextView.append("$margin${getString(R.string.silence_percentage_on_slide)}: " +
                    "%.2f".format(slideInfo.silencePercentage) + "%\n")
            resultsTextView.append("$margin${getString(R.string.average_pause_length)}: " +
                    "${formatTimeToSeconds(slideInfo.pauseAverageLength)} ${getString(R.string.seconds)}\n")
            resultsTextView.append("$margin${getString(R.string.long_pauses_amount)}: " +
                    "${slideInfo.longPausesAmount}\n")
        }

        override fun onReceive(p0: Context?, p1: Intent?) {
            outputVolumeLevels(getString(R.string.speech_volume_levels),
                audioAnalyzer.getSpeechVolumeLevels())

            val silenceAndSpeechPercentage = audioAnalyzer.getSilenceAndSpeechPercentage()
            outputTitleAndContent(getString(R.string.silence_and_speech_time),
                "\n${getString(R.string.pauses)}: %.2f"
                    .format(silenceAndSpeechPercentage.first * 100) + "%",
                "\n${getString(R.string.voice)}: %.2f"
                    .format(silenceAndSpeechPercentage.second* 100) + "%\n" )

            outputTitleAndContent(getString(R.string.duration),
                "\n${formatTime(audioAnalyzer.getSpeechDuration())}\n")

            outputTitleAndContent(getString(R.string.breakdown_by_slide))
            audioAnalyzer.getSlideInfo().forEach { it -> outputSlideInformation(it) }
        }
    }

    inner class SetNextSlideButtonReceiver : AudioAnalyzerReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            nextButtonPressed = false
        }
    }
}

class CountdownDialogFragment : DialogFragment() {
    private var continueRecording = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.countdown_dialog, container, false)

        // disabling the back button during the countdown
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, i, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                if (i == KeyEvent.KEYCODE_BACK) {
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener false
        }

        val activity = activity as? VoiceAnalysisActivity
            ?: // TODO log mistake? exit?
            return null
        val audioAnalyzer = activity.audioAnalyzer
        audioAnalyzer.recordCountdownAudio { continueRecording }

        val countdownTextView = view.findViewById<TextView>(R.id.countdown_text_view)
        timer(5000, 1000, countdownTextView).start()
        return view
    }

    private fun timer(millisInFuture: Long,
                      countDownInterval: Long,
                      textView: TextView): CountDownTimer {
        return object: CountDownTimer(millisInFuture, countDownInterval) {
            override fun onTick(millisUntilFinished: Long) {
                textView.text = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished).toString()
            }

            override fun onFinish() {
                continueRecording = false
                dismiss()
            }
        }
    }
}

fun formatTime(timeInMillis: Long): String {
    return "${formatNumberTwoDigits(timeInMillis / 1000 / 60 / 60)}:" +
            "${formatNumberTwoDigits(timeInMillis / 1000 / 60 % 60)}:" +
            formatNumberTwoDigits(timeInMillis / 1000 % 60)
}

fun formatNumberTwoDigits(number: Long): String {
    return String.format("%02d", number)
}

fun formatTimeToSeconds(timeInMillis: Long): String {
    return "%.2f".format(timeInMillis.toDouble() / 1000)
}
