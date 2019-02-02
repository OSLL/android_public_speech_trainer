package com.example.company.myapplication

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.*
import android.preference.PreferenceManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.support.test.InstrumentationRegistry
import android.util.Log
import java.lang.ref.WeakReference

public class SpeechRecognitionService: Service() {
    protected lateinit var mAudioManager: AudioManager
    protected var mSpeechRecognizer: SpeechRecognizer? = null
    protected var mSpeechRecognizerIntent: Intent? = null
    protected val mServerMessenger = Messenger(IncomingHandler(this@SpeechRecognitionService))

    protected var mIsListening: Boolean = false
    @Volatile protected var mIsCountDownOn: Boolean = false
    private var mIsStreamSolo: Boolean = false

    internal val MSG_RECOGNIZER_START_LISTENING = 1
    internal val MSG_RECOGNIZER_CANCEL = 2

    private var MESSAGE = ""

    private val mLocalBinder = LocalBinder()


    override fun onCreate() {
        super.onCreate()
        Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING, "SERVICE: onCreate called")
        mAudioManager = (getSystemService(Context.AUDIO_SERVICE) as AudioManager?)!!

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        mSpeechRecognizer!!.setRecognitionListener(SpeechRecognitionListener())
        mSpeechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        mSpeechRecognizerIntent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mSpeechRecognizerIntent!!.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.packageName)


        mSpeechRecognizer!!.startListening(mSpeechRecognizerIntent)
    }

    @SuppressLint("HandlerLeak")
    protected inner class IncomingHandler internal constructor(target: SpeechRecognitionService) : Handler() {
        private val mtarget: WeakReference<SpeechRecognitionService> = WeakReference(target)


        @SuppressLint("ObsoleteSdkInt")
        override fun handleMessage(msg: Message) {
            val target = mtarget.get()

            when (msg.what) {
                MSG_RECOGNIZER_START_LISTENING -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

                        // turn off beep sound
                        if (!mIsStreamSolo) {
                            mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true)

                            mIsStreamSolo = true
                        }
                    }
                    if (!target!!.mIsListening) {
                        target!!.mSpeechRecognizer!!.startListening(target.mSpeechRecognizerIntent)
                        target.mIsListening = true
                    }
                }

                MSG_RECOGNIZER_CANCEL -> {
                    if (mIsStreamSolo) {
                        mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false)
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 10, 0)
                        mIsStreamSolo = false
                    }
                    target!!.mSpeechRecognizer!!.cancel()
                    target!!.mIsListening = false
                }
            }
        }
    }

    // Count down timer for Jelly Bean work around
    protected var mNoSpeechCountDown: CountDownTimer = object : CountDownTimer(5000, 500) {

        override fun onTick(millisUntilFinished: Long) {
            if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0)
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        }

        override fun onFinish() {
            mIsCountDownOn = false
            var message = Message.obtain(null, MSG_RECOGNIZER_CANCEL)
            try {
                mServerMessenger.send(message)
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING)
                mServerMessenger.send(message)
            } catch (e: RemoteException) {

            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mIsCountDownOn) {
            mNoSpeechCountDown.cancel()
        }
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer!!.destroy()
        }

        Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING, "SERVICE: onDestroy called")
        MESSAGE = ""

        Handler().postDelayed({
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 10, 0)
        }, 2000)

    }

    protected inner class SpeechRecognitionListener : RecognitionListener {

        override fun onBeginningOfSpeech() {
            if (mIsCountDownOn) {
                mIsCountDownOn = false
                mNoSpeechCountDown.cancel()
            }
        }

        override fun onBufferReceived(buffer: ByteArray) {

        }

        override fun onEndOfSpeech() {

        }

        override fun onError(error: Int) {
            if (mIsCountDownOn) {
                mIsCountDownOn = false
                mNoSpeechCountDown.cancel()
            }
            mIsListening = false
            val message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING)
            try {
                mServerMessenger.send(message)
            } catch (e: RemoteException) {

            }

        }

        override fun onEvent(eventType: Int, params: Bundle) {

        }

        override fun onPartialResults(partialResults: Bundle) {

        }

        @SuppressLint("ObsoleteSdkInt")
        override fun onReadyForSpeech(params: Bundle) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mIsCountDownOn = true
                mNoSpeechCountDown.start()

            }
        }

        override fun onResults(results: Bundle) {
            val res = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
            MESSAGE += " " + res[0].toString()

            if (mIsCountDownOn) {
                mIsCountDownOn = false
                mNoSpeechCountDown.cancel()
            }
            mIsListening = false
            val message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING)
            try {
                mServerMessenger.send(message)
            } catch (e: RemoteException) {

            }

        }

        override fun onRmsChanged(rmsdB: Float) {

        }

    }

    fun getMESSAGE(): String {
        return this.MESSAGE
    }

    fun setMESSAGE(MESSAGE: String) {
        this.MESSAGE = MESSAGE
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING, "SERVICE: onBind called")
        return mLocalBinder
    }

    override fun onUnbind(i: Intent): Boolean {
        Log.d(SPEECH_RECOGNITION_SERVICE_DEBUGGING, "SERVICE: onUnbind called")
        return super.onUnbind(i)
    }

    inner class LocalBinder : Binder() {
        val service: SpeechRecognitionService
            get() = this@SpeechRecognitionService
    }
}