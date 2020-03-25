package com.example.trygoogledrive

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList


import kotlinx.android.synthetic.main.fragment_folder_picker.*
import kotlin.collections.ArrayList


class FolderPickerDialog (val driveServise: Drive) : DialogFragment() {

    private var updateChildren = false
    private val TAG = "mytag"

    private var folderId: String = "root"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_folder_picker, container, false)
    }

    override fun onStart() {
        super.onStart()

        Log.d(TAG, "Start picker dialog")

        val ctx = context
        if (ctx != null) {
            var arrayOfChildren = ArrayList<String>()
            var adapter = ArrayAdapter(ctx, android.R.layout.simple_list_item_1, arrayOfChildren)
            list_of_children.adapter = adapter

            current_folder_text.text = "Мой диск"

            Log.d(TAG, "Start getting children thread")
            var result: FileList? = null
            try {
                result = driveServise.files().list()
//                        .setSpaces("drive")
                        .setQ("mimeType = 'application/vnd.google-apps.folder' and '$folderId' in parents")
                        .execute()
            } catch (e: Exception) {
                Log.e(TAG, "Error while requesting from drive", e)
                updateChildren = true
            }

            Log.d(TAG, "Got answer from drive")

            if (result == null) {
                Log.d(TAG, "Got null answer")
                updateChildren = true
            } else {

                for (file in result.files) {
                    arrayOfChildren.add(file.id)
                }
                updateChildren = true
                Log.d(TAG, "Update children array")
            }

            adapter.notifyDataSetChanged()
            Log.d(TAG, "Update listview")

            ok.setOnClickListener {
                MainActivity.folderToSave = folderId
                dismiss()
            }
        }


    }
}
