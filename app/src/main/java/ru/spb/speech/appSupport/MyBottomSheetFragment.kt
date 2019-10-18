package ru.spb.speech.appSupport

import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetBehavior
import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import kotlinx.android.synthetic.main.activity_training_statistics.*
import ru.spb.speech.R


class MyBottomSheetFragment: BottomSheetDialogFragment() {

    private var rollDownSheet: ImageButton? = null

    private var container: ViewGroup? = null
    private var inflater: LayoutInflater? = null

    fun newInstance(): MyBottomSheetFragment {
        return MyBottomSheetFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.container = container
        this.inflater = inflater

        return initializeUserInterface()
    }

    private fun initializeUserInterface(): View{
        val view = inflater?.inflate(R.layout.evaluation_information_sheet_coordinator, container,
                false)

        rollDownSheet = view?.findViewById(R.id.roll_up_sheet)
        rollDownSheet?.setOnClickListener {
            activity?.supportFragmentManager?.findFragmentById(R.id.root_view_training_statistics)?.let { it1 ->
                activity?.supportFragmentManager?.beginTransaction()?.remove(it1)?.commit()
            }
            activity?.root_view_training_statistics?.setBackgroundColor(resources.getColor(R.color.whiteColor))
        }

        return view!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog.setOnShowListener {
            val d = dialog as BottomSheetDialog

            val bottomSheet = d.findViewById<View>(R.id.bottomSheetContainer)

            BottomSheetBehavior.from(bottomSheet!!)
                    .setState(BottomSheetBehavior.STATE_EXPANDED)

        }

        return super.onCreateDialog(savedInstanceState)
    }
}