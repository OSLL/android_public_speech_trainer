package ru.spb.speech.appSupport

import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.os.Parcelable
import android.os.Process
import android.util.Log
import kotlinx.android.parcel.Parcelize
import ru.spb.speech.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.log10


const val AUDIO_RECORDING = "APST.ANALYSIS_ACTIVITY"
const val RECORDING_FOLDER = "public_speech_trainer/recordings" // temporary name?
const val SAMPLING_RATE = 44100

class AudioAnalyzer(private val activity: Activity, controller: MutableLiveData<AudioAnalyzerState>? = null) {
    private val silenceCoefficient = 1.0
    private val shortPauseLength = 0.1
    private val shortSpeechLength = 0.05
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

    private var nextSlide = false
    private var continueCondition = true

    enum class AudioAnalyzerState {
        START_RECORD,
        FINISH,
        PAUSE,// TODO
        RESUME,// TODO
        NEXT_SLIDE
    }


    companion object {
        fun getInstance(act: Activity, controller: MutableLiveData<AudioAnalyzerState>)
                = AudioAnalyzer(act, controller)

        const val AUDIO_LOG_RESULT = "AUDIO_LOG_RESULT"
    }

    init {
        var bufferSize = AudioRecord.getMinBufferSize(
                SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLING_RATE * 2
        }

        audioBuffer = ShortArray(bufferSize / 2)

        countdownRecord = initAudioRecording(bufferSize)
        speechRecord = initAudioRecording(bufferSize)

        controller?.observe(activity as TrainingActivity, android.arch.lifecycle.Observer {
            when (it) {
                AudioAnalyzerState.FINISH -> this.continueCondition = false
                AudioAnalyzerState.START_RECORD -> startRecordSpeechAudio()
                AudioAnalyzerState.PAUSE -> { }
                AudioAnalyzerState.RESUME -> { }
                AudioAnalyzerState.NEXT_SLIDE -> this.nextSlide = true
            }
        })
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
        return if (amplitude > 0) 10 * log10(amplitude) else 0.0
    }

    private fun startRecordSpeechAudio() {
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

            silenceLevel = 50.0

            while (this.continueCondition) {
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

                if (this.nextSlide || !this.continueCondition) {
                    Log.d(AUDIO_RECORDING, "next slide")
                    this.nextSlide = false

                    val silencePercentageOnSlide = silentFragmentsOnSlide.toDouble() /
                            totalFragmentsOnSlide * silenceCoefficient
                    val averageSilenceLength = pausesPerSlide.average().toLong()
                    val longPausesAmount = pausesPerSlide
                            .filter { it > averageSilenceLength }.size

                    slides.add(SlideInfo(slideNumber++, silencePercentageOnSlide,
                            averageSilenceLength, longPausesAmount).apply {
                        Log.d(AUDIO_RECORDING, "added slide: $this")
                    })

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

            logStatistics()
            saveFile(byteArrayOutputStream)
        }
    }

    /**
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *
     * Ниже пример доступа к всевозможной статистике
     * Посмтреть ее можно по логу: AUDIO_LOG_RESULT
     */
    private fun log(vararg content: String) { for (s in content) Log.d(AUDIO_LOG_RESULT, s) }
    private fun getString(id: Int) = activity.getString(id)

    private fun outputPausesInfo(silencePercentage: Double) {
        when {
            tooMuchPauses(silencePercentage) -> {
                if (tooMuchPausesWarning(silencePercentage)) {
                    log(getString(R.string.too_much_pauses),
                            getString(R.string.revise_text_recommendation))
                } else {
                    log(getString(R.string.overly_many_pauses),
                            getString(R.string.revise_text))
                }

                log(getString(R.string.speak_faster))
            }
            notEnoughPauses(silencePercentage) ->
                log(getString(R.string.speak_slower))
            else -> log(getString(R.string.good_speech))
        }
    }

    private fun outputSlideInformation(slideInfo: SlideInfo) {
        val margin = "  "

        log("${getString(R.string.slide)} ${slideInfo.slideNumber}:\n")
        log("$margin${getString(R.string.silence_percentage_on_slide)}: " +
                "%.2f".format(slideInfo.silencePercentage * 100) + "%\n")
        log("$margin${getString(R.string.average_pause_length)}: " +
                "${formatTimeToSeconds(slideInfo.pauseAverageLength)} ${getString(R.string.seconds)}\n")
        log("$margin${getString(R.string.long_pauses_amount)}: " +
                "${slideInfo.longPausesAmount}\n")
    }

    private fun logStatistics() {
        if (speechTooSilent()) {
            if (speechTooSilentWarning()) log(getString(R.string.quiet_speech))
            else log(getString(R.string.overly_quiet_speech))
            log(getString(R.string.speak_louder))
        } else log(getString(R.string.good_volume))

        outputPausesInfo(getSilencePercentage())
        log(getString(R.string.duration),"\n${formatTime(getSpeechDuration())}\n")
        log(getString(R.string.breakdown_by_slide))
        getSlideInfo().forEach { outputSlideInformation(it) }
    }

    /**
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     */

    private fun getAudioVolumeLevel(maxLevel: Double, avgLevel: Double): Double =
            (maxLevel + avgLevel) / 2


    private fun getVolumeLevels(volumesList: MutableList<Double>): Triple<Double, Double, Double> {
        return if (volumesList.isEmpty()) {
            Triple(0.0, 0.0, 0.0)
        } else {
            Triple(volumesList[0], volumesList.last(), volumesList.average())
        }
    }

    private fun getSilencePercentage(): Double = silencePercentage

    private fun getSpeechDuration(): Long = speechDuration

    private fun getSlideInfo(): List<SlideInfo> = slides

    private fun tooMuchPausesWarning(silencePercentage: Double): Boolean =
            silencePercentage > maxWarningSilencePercentage

    private fun tooMuchPauses(silencePercentage: Double): Boolean =
            silencePercentage > maxSilencePercentage

    private fun notEnoughPauses(silencePercentage: Double): Boolean =
            silencePercentage < minWarningSilencePercentage

    private fun speechTooSilentWarning(): Boolean =
            silenceAndSpeechLevelsDifference() < minWarningSilenceAndSpeechLevelsDifference

    private fun speechTooSilent(): Boolean =
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
            activity.filesDir
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

    fun getLastSlideInfo() = slides.last()
}

@Parcelize
data class SlideInfo(val slideNumber: Int, val silencePercentage: Double,
                     val pauseAverageLength: Long, val longPausesAmount: Int) : Parcelable

fun List<SlideInfo>.toAllStatisticsInfo(): SlideInfo {
    var silencePercentage = 0.0
    var pauseAverageLength: Long = 0
    var longPausesAmount = 0
    for (slide in this) {
        silencePercentage += slide.silencePercentage
        pauseAverageLength += slide.pauseAverageLength
        longPausesAmount += slide.longPausesAmount
    }

    val count = this.count()

    return SlideInfo(-1,
            silencePercentage,
            pauseAverageLength/ count,
            longPausesAmount)
}

fun formatTime(timeInMillis: Long): String {
    return "${formatNumberTwoDigits(TimeUnit.MILLISECONDS.toMinutes(timeInMillis) / 60)}:" +
            "${formatNumberTwoDigits(TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60)}:" +
            formatNumberTwoDigits(TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60)
}

fun formatNumberTwoDigits(number: Long): String {
    return String.format("%02d", number)
}

fun formatTimeToSeconds(timeInMillis: Long): String {
    return "%.2f".format(timeInMillis.toDouble() / 1000)
}