package ru.spb.speech.measurementAutomation

import android.app.Activity
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.provider.DocumentFile
import android.util.Log
import ru.spb.speech.R
import ru.spb.speech.TrainingActivity
import ru.spb.speech.appSupport.PdfToBitmap
import ru.spb.speech.database.PresentationData
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.database.helpers.PresentationDBHelper
import ru.spb.speech.database.interfaces.PresentationDataDao

class RunningTraining {

    companion object {
       const val LOG = "test_folder_log"
    }
    private val context: Activity
    private val presentationDataDao: PresentationDataDao
    private var presentationList: List<PresentationData>? = null
    private val timeList = arrayListOf<Int>()

    constructor(context: Activity) {
        this.context = context
        presentationDataDao = SpeechDataBase.getInstance(context)?.PresentationDataDao()!!
        presentationDataDao.deleteTestFolderPres()


    }

    fun startTrainings(directoryFile: DocumentFile){
        findPdfPresentations(directoryFile)
    }

    private fun findPdfPresentations(file: DocumentFile) {

        val files = file.listFiles()

        for(singleFile in files){
            Log.d(LOG, "file: $singleFile")
            if(singleFile.name?.endsWith(".pdf")!!){
                addPres(singleFile.name!!, singleFile.uri.toString())
            } else if (singleFile.name?.endsWith(".txt")!!) {
                try {
                    val stream = context.contentResolver.openInputStream(singleFile.uri)

                    val data = String(stream.readBytes())
                    timeList.addAll(data
                            .replace("\n", "")
                            .replace(" ", "")
                            .split(",")
                            .map { it.toInt() })

                    Log.d(LOG, data)
                } catch (e: Exception) {
                    e.printStackTrace()
                    presentationDataDao.deleteTestFolderPres()
                    return
                }
            }
        }
        presentationList = presentationDataDao.getAllTestFolderPres()
        if (presentationList!!.isEmpty()) { return }
        setTestAudioMode(true)
        startActivityWithReqCode(presentationList!!.first().id!!, 0)
    }

    private fun startActivityWithReqCode(presId: Int, requestCode: Int) {
        context.startActivityForResult(TrainingActivity.testFolderIntent(context, timeList).apply {
            putExtra(context.getString(R.string.CURRENT_PRESENTATION_ID), presId)
        }, requestCode)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode >= (presentationList?.size?:0)-1) {
                setTestAudioMode(false)
                presentationDataDao.deleteTestFolderPres()
            }
            else
                startActivityWithReqCode(presentationList!![requestCode+1].id!!, requestCode+1)
        }
    }

    private fun setTestAudioMode(b: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.deb_speech_audio_key), b)
                .apply()
    }

    private var presentationData: PresentationData? = null
    private lateinit var pdfReader: PdfToBitmap

    private fun addPres(presName: String, path: String) {
        presentationData = PresentationData()
        presentationData!!.stringUri = path
        presentationData!!.debugFlag = 2
        presentationData!!.name = presName

        presentationDataDao.insert(presentationData!!)
        presentationData = presentationDataDao.getLastPresentation()

        pdfReader = PdfToBitmap(presentationData!!, context)
        presentationData?.apply {
            pageCount = pdfReader.getPageCount()
            timeLimit = context.resources.getInteger(R.integer.seconds_in_a_minute).toLong()
            presentationDate = context.getString(R.string.app_creation_date)
            notifications = false

        }
        presentationDataDao.updatePresentation(presentationData!!)
        val presF = PresentationDBHelper(context)
        presF.saveDefaultPresentationImageOnMainThread(presentationData!!.id!!)
    }
}