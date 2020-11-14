package com.moriak.schednote.fragments.of_main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.SubjectAdapter
import com.moriak.schednote.design.ItemTopSpacing
import com.moriak.schednote.dialogs.SubjectEditDialog
import kotlinx.android.synthetic.main.list_layout.*

class SubjectsFragment : SubActivity() {
    companion object {
        private const val EDIT_DIALOG = "EDIT_DIALOG"
    }

    private lateinit var adapter: SubjectAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.subjects)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        inflater.inflate(R.layout.list_layout, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.apply {
            adapter = SubjectAdapter()
            empty.setText(R.string.no_subjects)
            list.layoutManager = LinearLayoutManager(context)
            list.adapter = adapter
            list.addItemDecoration(ItemTopSpacing(App.dp(4)))
            list.setEmptyView(empty)

            adapter.setShowDialog { sub ->
                showDialog(
                    EDIT_DIALOG,
                    SubjectEditDialog.newInstance(sub, adapter)
                )
            }
            list_add.setOnClickListener {
                showDialog(
                    EDIT_DIALOG,
                    SubjectEditDialog.newInstance(adapter)
                )
            }
            findFragment(EDIT_DIALOG, SubjectEditDialog::class.java)?.setAdapter(adapter)
        }
    }
}