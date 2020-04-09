package ru.spb.speech.appSupport

import android.app.Activity
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.ByteArrayOutputStream
import java.util.*


const val DRIVE_TAG = "GOOGLE_DRIVE_TAG"

class GoogleDriveHelper {
    companion object {
        const val REQUEST_CODE_SIGN_IN = 0

        private val driveHelper = GoogleDriveHelper()

        fun getInstance() = driveHelper
    }

    var driveService: Drive? = null

    fun requestSignIn(activity: Activity) {
        Log.d(DRIVE_TAG, "Requesting sign-in")

        if (driveService == null) {
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                    .build()
            val client = GoogleSignIn.getClient(activity, signInOptions)

            // The result of the sign-in Intent is handled in onActivityResult.
            activity.startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
        } else {
            Log.d(DRIVE_TAG, "Already signed in")
        }
    }

    fun heldSignInResult(activity: Activity, data: Intent?) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener { googleAccount ->
                    Log.d(DRIVE_TAG, "Signed in as " + googleAccount.email!!)

                    val credential = GoogleAccountCredential.usingOAuth2(
                            activity, Collections.singleton(DriveScopes.DRIVE_FILE))
                    credential.setSelectedAccount(googleAccount.account)
                    val googleDriveService = Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            GsonFactory(),
                            credential)
                            .setApplicationName("Drive API Migration")
                            .build()

                    driveService = googleDriveService
                }
                .addOnFailureListener { exception ->
                    Log.e(DRIVE_TAG, "Unable to sign in.", exception)
                }
    }

    fun saveFileToDrive(activity: Activity, byteArrayOutputStream: ByteArrayOutputStream) {
        if (driveService != null) {
            val driveService = (driveService as Drive)

            val parentId = PreferenceManager
                    .getDefaultSharedPreferences(activity)
                    .getString("drive_folder_key", "root")
            if (parentId == null) {
                Log.e(DRIVE_TAG, "Error while getting folder id from preferences")
                Toast.makeText(activity, "Ошибка при получении ID папки Google-диска из настроек",
                        Toast.LENGTH_SHORT).show()
            }

            var metadata = com.google.api.services.drive.model.File()
                    .setParents(Collections.singletonList(parentId))
                    .setMimeType("audio/vnd.wave")
                    .setName("My Recording.wav")

            val resultFile = driveService.files().create(metadata).execute()
            if (resultFile == null) {
                Log.e(DRIVE_TAG, "Null result when creating file")
                return
            }

            metadata = com.google.api.services.drive.model.File().setName("My Recording.wav")

            val mediaContent = ByteArrayContent.fromString("audio/vnd.wave", byteArrayOutputStream.toString())
            driveService.files().update(resultFile.id, metadata, mediaContent).execute()

            Log.d(DRIVE_TAG, "File saved")
        }
    }
}