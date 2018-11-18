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

data class SlideInfo(val slideNumber: Int, val silencePercentage: Double,
                     val pauseAverageLength: Long, val longPausesAmount: Int)

class AudioAnalyzer(private val activity: VoiceAnalysisActivity?) {
    private val silenceCoefficient = 1.0

    private val audioBuffer: ShortArray
    private val countdownRecord: AudioRecord
    private val speechRecord: AudioRecord
    private val countdownVolumesList = mutableListOf<Double>()
    private val speechVolumesList = mutableListOf<Double>()
    private val byteArrayOutputStream = ByteArrayOutputStream()

    private val slides = mutableListOf<SlideInfo>()
    private val pausesPerSlide = mutableListOf<Long>()

    private lateinit var silenceAndSpeechPercentage: Pair<Double, Double>
    private var speechDuration: Long = 0

    private var isPause = false
    private var pauseStartTime: Long = 0

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

            val volumeLevels = getVolumeLevels(countdownVolumesList)
            Log.wtf(AUDIO_RECORDING, "silence min level: ${volumeLevels.first}")
            Log.wtf(AUDIO_RECORDING, "silence max level: ${volumeLevels.second}")
            Log.wtf(AUDIO_RECORDING, "silence average level: ${volumeLevels.third}")

            Log.i(AUDIO_RECORDING, "finished countdown audio recording")
        }
    }

    fun recordSpeechAudio(continueCondition: () -> Boolean, nextSlide: () -> Boolean){
        thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            speechRecord.startRecording()
            Log.i(AUDIO_RECORDING, "started speech audio recording")

            val startTime = System.currentTimeMillis()

            var totalFragmentsRead: Long = 0
            var silentFragmentsRead: Long = 0
            var totalFragmentsOnSlide: Long = 0
            var silentFragmentsOnSlide: Long = 0

            var slideNumber = 1

            val silenceLevel = (getAverageCountdownVolumeLevel() + getMaximalCountdownVolumeLevel()) / 2

            while (continueCondition.invoke()) {
                val shortsRead = speechRecord.read(audioBuffer, 0, audioBuffer.size)
                val fragmentVolume = calculateVolume(shortsRead)
                speechVolumesList.add(fragmentVolume)
                if (fragmentVolume < silenceLevel) {
                    if (!isPause) {
                        isPause = true
                        pauseStartTime = System.currentTimeMillis()
                    }
                    silentFragmentsRead++
                    silentFragmentsOnSlide++
                } else if (isPause) {
                    pausesPerSlide.add(System.currentTimeMillis() - pauseStartTime)
                    isPause = false
                }
                totalFragmentsOnSlide++
                totalFragmentsRead++

                if (nextSlide.invoke() || !(continueCondition.invoke())) {
                    sendBroadcastIntent(Intent(NEXT_SLIDE_BUTTON_ACTION))

                    val silencePercentageOnSlide = silentFragmentsOnSlide.toDouble() /
                            totalFragmentsOnSlide * silenceCoefficient
                    val averageSilenceLength = pausesPerSlide.average().toLong()
                    val longPausesAmount = pausesPerSlide
                        .filter { it -> it > averageSilenceLength }.size

                    slides.add(SlideInfo(slideNumber++, silencePercentageOnSlide,
                        averageSilenceLength, longPausesAmount))

                    totalFragmentsOnSlide = 0
                    silentFragmentsOnSlide = 0
                }

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

            val silencePercentage = silentFragmentsRead.toDouble() /
                    totalFragmentsRead * silenceCoefficient
            val speechPercentage = 1 - silencePercentage
            silenceAndSpeechPercentage = silencePercentage to speechPercentage

            Log.wtf(AUDIO_RECORDING, "silence: $silencePercentage")
            Log.wtf(AUDIO_RECORDING, "speech: $speechPercentage")

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

    private fun getVolumeLevels(volumesList: MutableList<Double>): Triple<Double, Double, Double> {
        return if (volumesList.isEmpty()) {
            Triple(0.0, 0.0, 0.0)
        } else {
            volumesList.sort()
            Triple(volumesList[0], volumesList.last(), volumesList.average())
        }
    }

    fun getCountdownVolumeLevels(): Triple<Double, Double, Double> {
        return getVolumeLevels(countdownVolumesList)
    }

    fun getSpeechVolumeLevels(): Triple<Double, Double, Double> {
        return getVolumeLevels(speechVolumesList)
    }

    fun getSilenceAndSpeechPercentage(): Pair<Double, Double> = silenceAndSpeechPercentage

    fun getSpeechDuration(): Long = speechDuration

    fun getSlideInfo(): List<SlideInfo> = slides

    private fun getMaximalCountdownVolumeLevel(): Double {
        return getVolumeLevels(countdownVolumesList).second
    }

    private fun getAverageCountdownVolumeLevel(): Double {
        return getVolumeLevels(countdownVolumesList).third
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


