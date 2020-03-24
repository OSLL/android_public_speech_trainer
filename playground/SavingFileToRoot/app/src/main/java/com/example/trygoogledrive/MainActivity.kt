package com.example.trygoogledrive

import android.Manifest
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

import kotlin.concurrent.thread
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.*
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File

import java.util.*


const val SAMPLING_RATE = 44100

class MainActivity : AppCompatActivity() {

    companion object {
        val REQUEST_CODE_SIGN_IN = 0
    }

    private var audioRecord: AudioRecord
    private var audioBuffer: ShortArray
    private var byteArrayOutputStream = ByteArrayOutputStream()

    private var isRecording = false

    private var mDriveService: Drive? = null

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

        requestSignIn()

        start.setOnClickListener {
            isRecording = true
            startRecording()
        }

        stop.setOnClickListener {
            isRecording = false
        }

    }

    private fun startRecording() {
        thread {
            audioRecord.startRecording()
            Log.d(TAG, "Recording started")

            while (isRecording) {
                val shortsRead = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                val bufferBytes = ByteBuffer.allocate(shortsRead * 2)
                bufferBytes.order(ByteOrder.LITTLE_ENDIAN)
                bufferBytes.asShortBuffer().put(audioBuffer, 0, shortsRead)
                val bytes = bufferBytes.array()
                byteArrayOutputStream.write(bytes)
            }

            Log.d(TAG, "Recording stopped")

            saveToDrive(byteArrayOutputStream)
        }
    }

    private fun requestSignIn() {
        Log.d(TAG, "Requesting sign-in")

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
        val client = GoogleSignIn.getClient(this, signInOptions)

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> {
                GoogleSignIn.getSignedInAccountFromIntent(data)
                        .addOnSuccessListener { googleAccount ->
                            Log.d(TAG, "Signed in as " + googleAccount.email!!)

                            val credential = usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE))
                            credential.setSelectedAccount(googleAccount.account)
                            val googleDriveService = Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    GsonFactory(),
                                    credential)
                                    .setApplicationName("Drive API Migration")
                                    .build()

                            mDriveService = googleDriveService
                        }
                        .addOnFailureListener { exception -> Log.e(TAG, "Unable to sign in.", exception) }
            }
        }
    }

    private fun saveToDrive(byteArrayOutputStream: ByteArrayOutputStream) {
        if (mDriveService != null) {
            val driveService = (mDriveService as Drive)

            var metadata = File()
                    .setParents(Collections.singletonList("root"))
                    .setMimeType("audio/vnd.wave")
                    .setName("My Recording.wav")

            val resultFile = driveService.files().create(metadata).execute()
            if (resultFile == null) {
                Log.e(TAG, "Null result when creating file")
                return
            }

            metadata = File().setName("My Recording.wav")

            val mediaContent = ByteArrayContent.fromString("audio/vnd.wave", byteArrayOutputStream.toString())
            driveService.files().update(resultFile.id, metadata, mediaContent).execute()

            Log.d(TAG, "File saved")
        }
    }
}
