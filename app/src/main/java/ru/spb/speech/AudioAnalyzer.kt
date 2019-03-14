package ru.spb.speech

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
    private val shortPauseLength = 0.1
    private val shortSpeechLength = 0.05
    private val maxAcceptableSilenceLevel = 40
    private val minWarningSilencePercentage = 0.085
    private val maxWarningSilencePercentage = 0.18
    private val maxSilencePercentage = 0.26
    private val minWarningSilenceAndSpeechLevelsDifference = 30
    private val minSilenceAndSpeechLevelsDifference = 15

    private val audioBuffer: ShortArray
    private val countdownRecord: AudioRecord
    private val speechRecord: AudioRecord
    private val countdownVolumesList = mutableListOf<Double>()
    private val speechVolumesList = mutableListOf<Double>()
    private val byteArrayOutputStream = ByteArrayOutputStream()

    private val slides = mutableListOf<SlideInfo>()
    private val pausesPerSlide = mutableListOf<Long>()

    private var silenceLevel: Double? = null

    private var silencePercentage: Double = 1.0
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

            countdownVolumesList.sort()

            val postCountdownIntent = Intent()
            postCountdownIntent.action = POST_COUNTDOWN_ACTION
            sendBroadcastIntent(postCountdownIntent)

            countdownRecord.stop()
            countdownRecord.release()

            val volumeLevels = getVolumeLevels(countdownVolumesList)
            Log.d(AUDIO_RECORDING, "silence min level: ${volumeLevels.first}")
            Log.d(AUDIO_RECORDING, "silence max level: ${volumeLevels.second}")
            Log.d(AUDIO_RECORDING, "silence average level: ${volumeLevels.third}")

            Log.i(AUDIO_RECORDING, "finished countdown audio recording")
        }
    }

    fun recordSpeechAudio(continueCondition: () -> Boolean, nextSlide: () -> Boolean){
        thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            speechRecord.startRecording()
            Log.i(AUDIO_RECORDING, "started speech audio recording")

            val startTime = System.currentTimeMillis()

            var speechStartTime: Long = 0

            var totalFragmentsRead: Long = 0
            var silentFragmentsRead: Long = 0
            var totalFragmentsOnSlide: Long = 0
            var silentFragmentsOnSlide: Long = 0

            var slideNumber = 1


            if (silenceLevel == null) {
                silenceLevel = getAudioVolumeLevel(getAverageCountdownVolumeLevel(),
                    getMaximalCountdownVolumeLevel())
            }


            while (continueCondition.invoke()) {
                val shortsRead = speechRecord.read(audioBuffer, 0, audioBuffer.size)
                val fragmentVolume = calculateVolume(shortsRead)
                speechVolumesList.add(fragmentVolume)
                if (fragmentVolume < silenceLevel!!) {
                    if (!isPause) {
                        isPause = true
                        if (millisecondsToSeconds(System.currentTimeMillis() - speechStartTime) < shortSpeechLength) {
                            // the pause is still continuing, so we remove the pause in the list
                            // and later it will be replaced with the current, longer pause
                            // which has the same start time
                            pausesPerSlide.dropLast(1)
                        } else {
                            pauseStartTime = System.currentTimeMillis()
                        }
                    }
                    silentFragmentsRead++
                    silentFragmentsOnSlide++
                } else if (isPause) {
                    val pauseLength = System.currentTimeMillis() - pauseStartTime
                    if (millisecondsToSeconds(pauseLength) >= shortPauseLength) {
                        pausesPerSlide.add(pauseLength)
                    }
                    isPause = false
                    speechStartTime = System.currentTimeMillis()
                }
                totalFragmentsOnSlide++
                totalFragmentsRead++

                if (nextSlide.invoke() || !(continueCondition.invoke())) {
                    Log.d(AUDIO_RECORDING, "next slide")

                    sendBroadcastIntent(Intent(NEXT_SLIDE_BUTTON_ACTION))

                    synchronized(obj) {
                        obj.wait()
                    }

                    val silencePercentageOnSlide = silentFragmentsOnSlide.toDouble() /
                            totalFragmentsOnSlide * silenceCoefficient
                    val averageSilenceLength = pausesPerSlide.average().toLong()
                    val longPausesAmount = pausesPerSlide
                        .filter { it -> it > averageSilenceLength }.size

                    slides.add(SlideInfo(slideNumber++, silencePercentageOnSlide,
                        averageSilenceLength, longPausesAmount))

                    totalFragmentsOnSlide = 0
                    silentFragmentsOnSlide = 0
                    pausesPerSlide.clear()
                }

                val bufferBytes = ByteBuffer.allocate(shortsRead * 2)
                bufferBytes.order(ByteOrder.LITTLE_ENDIAN)
                bufferBytes.asShortBuffer().put(audioBuffer, 0, shortsRead)
                val bytes = bufferBytes.array()
                byteArrayOutputStream.write(bytes)
            }

            val duration = System.currentTimeMillis() - startTime
            Log.i(AUDIO_RECORDING, "finished audio recording at ${getCurrentDateForLog()}")
            Log.i(AUDIO_RECORDING, "audio file length: " + formatTime(duration))

            speechDuration = duration

            val silencePercentage = silentFragmentsRead.toDouble() /
                    totalFragmentsRead * silenceCoefficient
            this.silencePercentage = silencePercentage

            Log.i(AUDIO_RECORDING, "silence: $silencePercentage")

            speechVolumesList.sort()

            speechRecord.stop()
            speechRecord.release()

            val postRecordingIntent = Intent()
            postRecordingIntent.action = POST_SPEECH_ACTION
            sendBroadcastIntent(postRecordingIntent)

            saveFile(byteArrayOutputStream)
        }
    }

    private fun getAudioVolumeLevel(maxLevel: Double, avgLevel: Double): Double =
        (maxLevel + avgLevel) / 2

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
            Triple(volumesList[0], volumesList.last(), volumesList.average())
        }
    }

    fun isRoomNoisy(): Boolean = getAudioVolumeLevel(getMaximalCountdownVolumeLevel(),
        getAverageCountdownVolumeLevel()) > maxAcceptableSilenceLevel

    /*fun getCountdownVolumeLevels(): Triple<Double, Double, Double> =
        getVolumeLevels(countdownVolumesList)*/

    fun getSpeechVolumeLevels(): Triple<Double, Double, Double> = getVolumeLevels(speechVolumesList)

    fun getSilencePercentage(): Double = silencePercentage

    fun getSpeechDuration(): Long = speechDuration

    fun getSlideInfo(): List<SlideInfo> = slides

    fun tooMuchPausesWarning(silencePercentage: Double): Boolean =
        silencePercentage > maxWarningSilencePercentage

    fun tooMuchPauses(silencePercentage: Double): Boolean =
        silencePercentage> maxSilencePercentage

    fun notEnoughPauses(silencePercentage: Double): Boolean =
        silencePercentage < minWarningSilencePercentage

    fun speechTooSilentWarning(): Boolean =
        silenceAndSpeechLevelsDifference() < minWarningSilenceAndSpeechLevelsDifference

    fun speechTooSilent(): Boolean =
        silenceAndSpeechLevelsDifference() < minSilenceAndSpeechLevelsDifference



    private fun getMaximalCountdownVolumeLevel(): Double {
        return getVolumeLevels(countdownVolumesList).second
    }

    private fun getAverageCountdownVolumeLevel(): Double {
        return getVolumeLevels(countdownVolumesList).third
    }

    private fun getMaximalRecordingVolumeLevel(): Double {
        return getVolumeLevels(speechVolumesList).second
    }

    private fun getAverageRecordingVolumeLevel(): Double {
        return getVolumeLevels(speechVolumesList).third
    }

    private fun silenceAndSpeechLevelsDifference(): Double =
        getAudioVolumeLevel(getAverageRecordingVolumeLevel(), getMaximalRecordingVolumeLevel()) -
        getAudioVolumeLevel(getAverageCountdownVolumeLevel(), getMaximalCountdownVolumeLevel())

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

            Log.i(AUDIO_RECORDING, "audio file path: ${directory.absolutePath}")
            Log.i(AUDIO_RECORDING, "audio file name: ${audioFile.name}")
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

    private fun millisecondsToSeconds(timeInMillis: Long): Double {
        return timeInMillis / 1000.toDouble()
    }
}

