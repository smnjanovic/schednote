package com.moriak.schednote.fragments.of_schedule

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.moriak.schednote.R
import com.moriak.schednote.adapters.LessonTypesAdapter
import com.moriak.schednote.design.ItemTopSpacing
import com.moriak.schednote.fragments.of_main.SubActivity
import kotlinx.android.synthetic.main.lesson_types.view.*

/**
 * Fragment zobrazuje upraviteľný zoznam typov hodín, ktoré sú potrebné pre prácu s rozvrhom
 */
class LessonTypesList : SubActivity(), SchedulePart {
    private val adapter: LessonTypesAdapter = LessonTypesAdapter()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.lesson_types)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        inflater.inflate(R.layout.lesson_types, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.lesson_type_list.layoutManager = LinearLayoutManager(context)
        view.lesson_type_list.adapter = adapter
        view.lesson_type_list.addItemDecoration(ItemTopSpacing(5))
    }
}
