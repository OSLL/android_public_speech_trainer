package com.example.trygoogledrive

import androidx.appcompat.app.AppCompatActivity

import android.app.Activity
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.drive.*

import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

import kotlin.concurrent.thread

const val SAMPLING_RATE = 44100

class MainActivity : AppCompatActivity() {

    companion object {
        val REQUEST_CODE_SIGN_IN = 0
        val REQUEST_CODE_CREATOR = 2
    }

    private var audioRecord: AudioRecord
    private var audioBuffer: ShortArray
    private var byteArrayOutputStream = ByteArrayOutputStream()

    private var isRecording = false

    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var mDriveClient: DriveClient? = null
    private var mDriveResourceClient: DriveResourceClient? = null

    private val TAG = "mytag"

    init {
        var bufferSize = AudioRecord.getMinBufferSize(
                SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLING_RATE * 2
        }

        audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

        audioBuffer = ShortArray(bufferSize)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        signIn()

        start.setOnClickListener {
            isRecording = true
            startRecording()
        }

        stop.setOnClickListener {
            isRecording = false
        }
    }

    private fun signIn() {
        Log.i(TAG, "Start sign in")
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Drive.SCOPE_FILE)
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
        startActivityForResult(mGoogleSignInClient?.getSignInIntent(), REQUEST_CODE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> {
                Log.i(TAG, "Sign in request code")
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Signed in successfully.")
                    mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
                    mDriveResourceClient = Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
                }
            }
            REQUEST_CODE_CREATOR -> {
                Log.i(TAG, "Creating request code")
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "File created")
                }
            }
        }
    }

    private fun startRecording() {
        thread {
            audioRecord.startRecording()

            while (isRecording) {
                val shortsRead = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                val bufferBytes = ByteBuffer.allocate(shortsRead * 2)
                bufferBytes.order(ByteOrder.LITTLE_ENDIAN)
                bufferBytes.asShortBuffer().put(audioBuffer, 0, shortsRead)
                val bytes = bufferBytes.array()
                byteArrayOutputStream.write(bytes)
            }

            saveToDrive(byteArrayOutputStream)
        }
    }

    private fun saveToDrive(byteArrayOutputStream: ByteArrayOutputStream) {
        mDriveResourceClient!!
                .createContents()
                .continueWithTask { task ->
                    val os = task.result!!.outputStream
                    try {
                        os.write(byteArrayOutputStream.toByteArray())
                    } catch (e: IOException) {
                        Log.w(TAG, "Unable to write file contents.", e)
                    }

                    val metadataChangeSet =
                            MetadataChangeSet.Builder()
                                    .setStarred(true)
                                    .setMimeType("audio/vnd.wave")
                                    .setTitle("My Recording.wav")
                                    .build()


                    val createFileActivityOptions =
                            CreateFileActivityOptions.Builder()
                                    .setInitialMetadata(metadataChangeSet)
                                    .setInitialDriveContents(task.result!!)
                                    .build()

                    mDriveClient!!
                            .newCreateFileActivityIntentSender(createFileActivityOptions)
                            .continueWith { task ->
                                startIntentSenderForResult(task.result, REQUEST_CODE_CREATOR, null, 0, 0, 0)
                                null
                            }
                }
                .addOnFailureListener {
                    Log.w(TAG, "Failed to create new contents.", it)
                }
    }
}
