package com.example.company.myapplication.appSupport

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.example.company.myapplication.EditPresentationActivity
import com.example.company.myapplication.R
import com.example.company.myapplication.TrainingActivity
import com.example.company.myapplication.views.PresentationStartpageItemRow
import com.example.putkovdimi.trainspeech.DBTables.DaoInterfaces.PresentationDataDao
import com.example.putkovdimi.trainspeech.DBTables.SpeechDataBase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class PresentationAdapterHelper(private val rw: RecyclerView, private val adapter: GroupAdapter<ViewHolder>, private val context: Context) {
    private val presentationDataDao: PresentationDataDao = SpeechDataBase.getInstance(context)!!.PresentationDataDao()
    private var updateListener: UpdateAdapterListener? = null

    init {
        adapter.setOnItemClickListener { item: Item<ViewHolder>, _ ->
            if (isOnline()) {
                val row = item as PresentationStartpageItemRow
                val i = Intent(context, TrainingActivity::class.java)
                i.putExtra(context.getString(R.string.CURRENT_PRESENTATION_ID), row.presentationId)
                startActivity(context, i, null)
            } else Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
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
                } catch (e: NullPointerException) { }

                presentationDataDao.deletePresentationWithId(row.presentationId!!)
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

    fun setUpdateAdapterListener(l: UpdateAdapterListener) {
        this.updateListener = l
    }

    fun fillAdapter() {
        for (p in presentationDataDao.getAll()) {
            if (p.timeLimit == null || p.pageCount == 0 || p.name.isNullOrEmpty()) {
                presentationDataDao.deletePresentationWithId(p.id!!)
                continue
            }
            addItemInAdapter(PresentationStartpageItemRow(p, null, context), p.imageBLOB)
        }
        runLayoutAnimation(rw)
    }

    fun addItemInAdapter(item: PresentationStartpageItemRow, imageBLOB: ByteArray?) {
        adapter.add(item)
        LoadItemAsync(item, imageBLOB).execute()
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
    }

    fun addLastItem() {
        val presentation = presentationDataDao.getLastPresentation()
        val row = PresentationStartpageItemRow(presentation, null, context)
        addItemInAdapter(row, presentation.imageBLOB)
    }

    private inner class LoadItemAsync(private val row: PresentationStartpageItemRow, private val imageBLOB: ByteArray?) : AsyncTask<Void, Void, Void>() {
        private var bitmap: Bitmap? = null

        override fun onPreExecute() {
            super.onPreExecute()
            if (imageBLOB == null) this.onPostExecute(null)
        }

        override fun doInBackground(vararg params: Void?): Void? {
            bitmap = BitmapFactory.decodeByteArray(imageBLOB, 0, imageBLOB!!.size)
            publishProgress()
            return null
        }

        override fun onProgressUpdate(vararg values: Void?) {
            super.onProgressUpdate(*values)
            if (bitmap == null) return
            updateRowBitmap(row, bitmap!!)
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
                socket.connect(InetSocketAddress("8.8.8.8", 53), 2500)
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