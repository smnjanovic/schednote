package com.moriak.schednote.fragments.of_schedule

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.App
import com.moriak.schednote.Palette
import com.moriak.schednote.R
import com.moriak.schednote.data.LessonType
import com.moriak.schednote.databinding.LessonTypesBinding
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.fragments.ListSubActivity
import com.moriak.schednote.fragments.of_schedule.LessonTypeList.EditResult.*
import com.moriak.schednote.widgets.ScheduleWidget
import java.util.*
import com.moriak.schednote.adapters.LessonTypeAdapter

/**
 * Fragment obsahuje abecedný zoznam a správu kategórií vyučovaných hodín.
 */
class LessonTypeList : ListSubActivity<LessonType?, LessonTypeAdapter, LessonTypesBinding>(5) {
    private enum class EditResult(@StringRes val res: Int?) {
        SUCCESS(null),
        INVALID_FORMAT(R.string.lesson_type_invalid_name),
        LT_EXISTS(R.string.lesson_type_exists)
    }

    private data class SaveResult(val state: EditResult, val type: LessonType, val isNew: Boolean)

    override val adapter = LessonTypeAdapter()
    override val adapterView: RecyclerView by lazy { binding.lessonTypeList }

    private fun setBundle(bdl: Bundle, lt: LessonType?) = bdl.apply {
        putInt(LessonTypeAdapter.ID, lt?.id ?: -1)
        putString(LessonTypeAdapter.NAME, lt?.name)
        remove(LessonTypeAdapter.CURSOR_START)
        remove(LessonTypeAdapter.CURSOR_END)
    }

    private fun findLTByID(lt: LessonType?, data: Any?) = (lt?.id ?: -1) == data

    private fun saveChanges(bdl: Bundle, color: Palette?): SaveResult {
        var id = bdl.getInt(LessonTypeAdapter.ID, -1)
        val name = bdl.getString(LessonTypeAdapter.NAME, "")
        val state = when {
            !name.matches("^[a-zA-ZÀ-ž0-9][a-zA-ZÀ-ž0-9 ]{0,23}$".toRegex()) -> INVALID_FORMAT
            SQLite.lessonType(name)?.let { it.id != id } == true -> LT_EXISTS
            else -> SUCCESS
        }
        val isNew = id == -1
        if (state == SUCCESS && isNew) {
            id = SQLite.insertLessonType(name)
            if (id == -1) throw Exception("Failed to add lesson type!")
        }
        else if (state == SUCCESS && SQLite.renameLessonType(id, name) != 1)
            throw Exception("Failed to rename lesson type!")

        return SaveResult(state, LessonType(id, when (name.length) {
            0, 1 -> name.uppercase()
            else -> name.substring(0, 1).uppercase() + name.substring(1).lowercase()
        }, color ?: Palette()), isNew)
    }

    override fun onItemAction(pos: Int, data: Bundle, code: Int) {
        when (code) {
            LessonTypeAdapter.ACTION_EDIT -> {
                val oldPos = adapter.findIndexOf(this::findLTByID, data.getInt(LessonTypeAdapter.ID, -1))
                if (oldPos == -1) {
                    setBundle(data, adapter.getItemAt(pos))
                    adapter.notifyItemChanged(pos)
                }
                else {
                    val saved = saveChanges(data, adapter.getItemAt(oldPos)?.color)
                    setBundle(data, adapter.getItemAt(pos))
                    adapter.notifyItemChanged(pos)
                    when {
                        saved.state == SUCCESS -> {
                            if (saved.isNew) SQLite.getTypeColor(saved.type.id, saved.type.color)
                            adapter.updateItem(saved.type, oldPos)
                        }
                        saved.isNew -> adapter.deleteItem(oldPos)
                        else -> adapter.notifyItemChanged(oldPos)
                    }
                    saved.state.res?.let(App::toast)
                }
            }
            LessonTypeAdapter.ACTION_SAVE -> {
                val saved = saveChanges(data, adapter.getItemAt(pos)?.color)
                if (saved.state == SUCCESS) {
                    if (saved.isNew) SQLite.getTypeColor(saved.type.id, saved.type.color)
                    setBundle(data, null)
                    adapter.updateItem(saved.type, pos)
                    if (adapter.itemCount < LessonTypeAdapter.MAX_COUNT)
                        adapter.insertItem(null)
                } else App.toast(saved.state.res!!)
            }
            LessonTypeAdapter.ACTION_DELETE -> {
                val oldPos = adapter.findIndexOf(this::findLTByID, data.getInt(LessonTypeAdapter.ID, -1))
                val item = adapter.getItemAt(pos)

                if (item == null) {
                    setBundle(data, null)
                    adapter.notifyItemChanged(pos)
                } else if (oldPos == -1 || oldPos == pos) {
                    setBundle(data, null)
                    adapter.updateItem(null, pos)
                    SQLite.deleteLessonType(item.id)
                    ScheduleWidget.update(requireContext())
                } else {
                    adapter.deleteItem(pos)
                    SQLite.deleteLessonType(item.id)
                    ScheduleWidget.update(requireContext())
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.lesson_types)
    }

    override fun makeBinder(inflater: LayoutInflater, container: ViewGroup?) =
        LessonTypesBinding.inflate(inflater, container, false)

    override fun firstLoad(): List<LessonType?> {
        setBundle(adapter.extras, null)
        val list = ArrayList<LessonType?>()
        list.addAll(SQLite.lessonTypes())
        if (list.size < LessonTypeAdapter.MAX_COUNT) list.add(null)
        return list
    }
}