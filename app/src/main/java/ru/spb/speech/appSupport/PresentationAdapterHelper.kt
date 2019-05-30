package ru.spb.speech.appSupport

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import ru.spb.speech.EditPresentationActivity
import ru.spb.speech.R
import ru.spb.speech.TrainingActivity
import ru.spb.speech.views.PresentationStartpageItemRow
import ru.spb.speech.database.interfaces.PresentationDataDao
import ru.spb.speech.database.SpeechDataBase
import ru.spb.speech.APST_TAG
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ru.spb.speech.database.helpers.PresentationDBHelper
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

const val testHost = "8.8.8.8"
const val testPost = 53
const val timeLimit = 2500

class PresentationAdapterHelper(private val rw: RecyclerView, private val adapter: GroupAdapter<ViewHolder>, private val context: Context) {
    private val presentationDataDao: PresentationDataDao = SpeechDataBase.getInstance(context)!!.PresentationDataDao()
    private var updateListener: UpdateAdapterListener? = null
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences =  PreferenceManager.getDefaultSharedPreferences(context)

        adapter.setOnItemClickListener { item: Item<ViewHolder>, _ ->
            val builder = AlertDialog.Builder(context)
            builder.setMessage(context.getString(R.string.start_training))
            builder.setPositiveButton(context.getString(R.string.yes)) { _, _ ->
                startTraining(item as PresentationStartpageItemRow)
            }
            builder.setNegativeButton(context.getString(R.string.no)) { _, _ ->
            }
            builder.create().show()
        }

        adapter.setOnItemLongClickListener { item: Item<ViewHolder>, view ->
            val row = item as PresentationStartpageItemRow

            val defaultBackGround = view.background
            view.background = context.getDrawable(R.drawable.training_not_end_item_background)

            val builder = AlertDialog.Builder(context)
            builder.setMessage(context.getString(R.string.request_for_remove_presentation) + "${row.presentationName} ?")
            builder.setPositiveButton(context.getString(R.string.remove)) { _, _ ->
                val position = adapter.getAdapterPosition(item)
                adapter.remove(item)
                adapter.notifyItemRemoved(position)
                rw.adapter = adapter

                try {
                    rw.scrollToPosition(position)
                } catch (e: NullPointerException) {
                    Log.d(APST_TAG, "no clicked position")
                }

                PresentationDBHelper(context).removePresentation(row.presentationId!!)
                updateListener?.onAdapterUpdate()
            }

            builder.setNegativeButton(context.getString(R.string.change)) { _, _ ->
                val i = Intent(context, EditPresentationActivity::class.java)
                i.putExtra(context.getString(R.string.CURRENT_PRESENTATION_ID), row.presentationId)
                i.putExtra(context.getString(R.string.changePresentationFlag), PresentationStartpageItemRow.activatedChangePresentationFlag)
                i.putExtra(context.getString(R.string.presentationPosition), adapter.getAdapterPosition(item))
                startActivityForResult(context as Activity, i, context.resources.getInteger(R.integer.editPresentationResultCode), null)
                view.background = defaultBackGround
            }
            builder.setOnCancelListener {
                view.background = defaultBackGround
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
            true
        }
    }

    private fun startTraining(row: PresentationStartpageItemRow) {
        if (isOnline()) {
            val i = Intent(context, TrainingActivity::class.java)
            i.putExtra(context.getString(R.string.CURRENT_PRESENTATION_ID), row.presentationId)
            startActivity(context, i, null)
        } else Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
    }

    fun setUpdateAdapterListener(l: UpdateAdapterListener) {
        this.updateListener = l
    }

    fun fillAdapter() {
        for (p in presentationDataDao.getAll()) {
            if (p.isUnfinished()) {
                presentationDataDao.deletePresentationWithId(p.id!!)
                continue
            }
            addItemInAdapter(PresentationStartpageItemRow(p, null, context), p.imageBLOB)
        }
        runLayoutAnimation(rw)
    }

    fun addItemInAdapter(item: PresentationStartpageItemRow, imageBLOB: ByteArray?) {
        adapter.add(item)
        loadItemAsync(item, imageBLOB)
    }


    private fun updateRowBitmap(row: PresentationStartpageItemRow, bm: Bitmap) {
        val p = adapter.getAdapterPosition(row)
        row.setBitmap(bm)
        adapter.notifyItemChanged(p)
    }

    fun notifyItemChanged(position: Int) {
        val row = adapter.getItem(position) as PresentationStartpageItemRow
        val presentation = presentationDataDao.getPresentationWithId(row.presentationId!!) ?: return
        row.setPresentationData(presentation)
        adapter.notifyItemChanged(position)

        if (row.presentationUri != presentation.stringUri)
            loadItemAsync(adapter.getItem(position) as PresentationStartpageItemRow, presentation.imageBLOB)
    }

    fun addLastItem() {
        val presentation = presentationDataDao.getLastPresentation()

        if (presentation.isUnfinished()) {
            presentationDataDao.deletePresentationWithId(presentation.id!!)
            return
        }

        val row = PresentationStartpageItemRow(presentation, null, context)
        addItemInAdapter(row, presentation.imageBLOB)
    }

    private fun loadItemAsync(item: PresentationStartpageItemRow, imageBLOB: ByteArray?) {
        GlobalScope.launch {
            try {
                val bm = async(IO) { BitmapFactory.decodeByteArray(imageBLOB, 0, imageBLOB!!.size) }
                updateRowBitmap(item, bm.await())
            } catch (e: Exception) {
                Log.d(APST_TAG, "error while getting bitmap: $e")
            }
        }
    }

    private fun runLayoutAnimation(recyclerView: RecyclerView) {
        val context: Context = recyclerView.context
        val controller =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down);
        recyclerView.layoutAnimation = controller
        try {
            recyclerView.adapter.notifyDataSetChanged()
        } catch (e: NullPointerException) { }
        recyclerView.scheduleLayoutAnimation()
    }

    private fun isOnline(): Boolean {
        var connection = false
        val thread = Thread(Runnable {
            connection = try {
                val socket = Socket()
                socket.connect(InetSocketAddress(testHost, testPost), timeLimit)
                socket.close()
                true
            } catch (e: IOException) {
                false
            }
        })
        thread.start()
        thread.join()

        return connection
    }
}

interface UpdateAdapterListener {
    fun onAdapterUpdate()
}