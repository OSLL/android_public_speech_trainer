package com.example.company.myapplication

import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.os.Process
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class AudioAnalyzer(private val activity: VoiceAnalysisActivity?) {
    private val silenceLevel = 1.0

    private val audioBuffer: ShortArray
    private val countdownRecord: AudioRecord
    private val speechRecord: AudioRecord
    private val countdownVolumesList = mutableListOf<Double>()
    private val speechVolumesList = mutableListOf<Double>()
    private val byteArrayOutputStream = ByteArrayOutputStream()

    private lateinit var silenceAndSpeechPercentage: Pair<Double, Double>
    private var speechDuration: Long = 0

    init {
        var bufferSize = AudioRecord.getMinBufferSize(
            SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLING_RATE * 2
        }

        audioBuffer = ShortArray(bufferSize / 2)

        countdownRecord = initAudioRecording(bufferSize)
        speechRecord = initAudioRecording(bufferSize)
    }

    private fun initAudioRecording(bufferSize: Int): AudioRecord {
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLING_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(AUDIO_RECORDING, "can't initialize audio countdownRecord")
            // TODO maybe an exception
        }

        return record
    }

    private fun calculateVolume(shortsRead: Int): Double {
        var v: Long = 0
        for (i in 0 until shortsRead) {
            v += audioBuffer[i] * audioBuffer[i]
        }

        val amplitude = v / shortsRead.toDouble()
        return if (amplitude > 0) 10 * Math.log10(amplitude) else 0.0
    }

    fun recordCountdownAudio(continueCondition: () -> Boolean) {
        thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            countdownRecord.startRecording()
            Log.i(AUDIO_RECORDING, "started countdown audio recording")

            while (continueCondition.invoke()) {
                val shortsRead = countdownRecord.read(audioBuffer, 0, audioBuffer.size)
                countdownVolumesList.add(calculateVolume(shortsRead))
            }

            val postCountdownIntent = Intent()
            postCountdownIntent.action = POST_COUNTDOWN_ACTION
            sendBroadcastIntent(postCountdownIntent)

            countdownRecord.stop()
            countdownRecord.release()

            Log.i(AUDIO_RECORDING, "finished countdown audio recording")
        }
    }

    fun recordSpeechAudio(continueCondition: () -> Boolean){
        thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            speechRecord.startRecording()
            Log.i(AUDIO_RECORDING, "started speech audio recording")

            val startTime = System.currentTimeMillis()

            var totalFragmentsRead: Long = 0
            var silentFragmentsRead: Long = 0

            while (continueCondition.invoke()) {
                val shortsRead = speechRecord.read(audioBuffer, 0, audioBuffer.size)
                val fragmentVolume = calculateVolume(shortsRead)
                speechVolumesList.add(fragmentVolume)
                if (fragmentVolume < (getAverageCountdownVolumeLevel() + getMaximalCountdownVolumeLevel()) / 2) {
                    silentFragmentsRead++
                }
                totalFragmentsRead++

                val bufferBytes = ByteBuffer.allocate(shortsRead * 2)
                bufferBytes.order(ByteOrder.LITTLE_ENDIAN)
                bufferBytes.asShortBuffer().put(audioBuffer, 0, shortsRead)
                val bytes = bufferBytes.array()
                byteArrayOutputStream.write(bytes)
            }

            val duration = System.currentTimeMillis() - startTime
            Log.wtf(AUDIO_RECORDING, "finished audio recording at ${getCurrentDateForLog()}")
            Log.wtf(AUDIO_RECORDING, "audio file length: " + formatTime(duration))

            speechDuration = duration

            val silencePercentage = silentFragmentsRead.toDouble() / totalFragmentsRead * silenceLevel
            val speechPercentage = 1 - silencePercentage
            silenceAndSpeechPercentage = silencePercentage to speechPercentage

            val postRecordingIntent = Intent()
            postRecordingIntent.action = POST_SPEECH_ACTION
            sendBroadcastIntent(postRecordingIntent)

            saveFile(byteArrayOutputStream)
        }
    }

    private fun sendBroadcastIntent(intent: Intent) {
        val context = activity?.applicationContext
        if (context != null) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    private fun getVolumeLevels(volumesList: DoubleArray?): Triple<Double, Double, Double> {
        return if (volumesList == null) {
            Triple(0.0, 0.0, 0.0)
        } else {
            volumesList.sort()
            Triple(volumesList[0], volumesList.last(), volumesList.average())
        }
    }

    fun getCountdownVolumeLevels(): Triple<Double, Double, Double> {
        return getVolumeLevels(countdownVolumesList.toDoubleArray())
    }

    fun getSpeechVolumeLevels(): Triple<Double, Double, Double> {
        return getVolumeLevels(speechVolumesList.toDoubleArray())
    }

    fun getSilenceAndSpeechPercentage(): Pair<Double, Double> = silenceAndSpeechPercentage

    fun getSpeechDuration(): Long = speechDuration

    private fun getMaximalCountdownVolumeLevel(): Double {
        return getVolumeLevels(countdownVolumesList.toDoubleArray()).second
    }

    private fun getAverageCountdownVolumeLevel(): Double {
        return getVolumeLevels(countdownVolumesList.toDoubleArray()).third
    }

    private fun saveFile(byteArrayOutputStream: ByteArrayOutputStream) {
        val parent = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            Environment.getExternalStorageDirectory()
        } else {
            activity?.filesDir
        }

        val directory = File("${parent?.path}${File.separator}$RECORDING_FOLDER")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        try {
            val audioFile = File(directory, "recording-${getCurrentDateForName()}.pcm")
            audioFile.createNewFile()
            byteArrayOutputStream.writeTo(FileOutputStream(audioFile))

            Log.wtf(AUDIO_RECORDING, "audio file path: ${directory.absolutePath}")
            Log.wtf(AUDIO_RECORDING, "audio file name: ${audioFile.name}")
        } catch (e: IOException) {
            Log.e("error", "unable to create audio file for recording")
        }
    }

    // used for naming the audio recording file
    private fun getCurrentDateForName(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    // used for logging
    private fun getCurrentDateForLog(): String {
        return SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(Date())
    }
}


