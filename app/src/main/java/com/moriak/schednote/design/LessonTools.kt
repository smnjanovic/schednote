package com.moriak.schednote.design

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.StringRes
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.activities.MainActivity
import com.moriak.schednote.database.data.*
import com.moriak.schednote.menu.ScheduleDisplay
import com.moriak.schednote.menu.SubContent
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity
import kotlinx.android.synthetic.main.lesson_tools.view.*

/**
 * Trieda ponúka vykresľuje výber vlastností pre novo vznikajúce alebo upravované triedy.
 * Pokiaľ s rozvrhom ešte nie je možné pracovať z dôvodu chýbajúcich údajov, vykreslí sa
 * okno, v ktorom je užívateľ upozornený na to, čo mu chýba
 */
class LessonTools private constructor() {
    companion object {
        private val handler = Handler(Looper.myLooper()!!)

        /**
         * vytvorenie novej inštancie alebo aplikovanie už existujúcej inštancie
         * @param tools Stará inštancia vlastností. Môže byť null.
         */
        fun makeTools(tools: LessonTools?) = tools ?: LessonTools()
    }

    // uchováva informáciu o hodine ako časovej jednotke rozvrhu
    private abstract class LessonData(lesson: LessonTime) {
        val order = lesson.order
    }

    // slúži na výpis o začiatku hodiny
    private class LessonStart(lesson: LessonTime) : LessonData(lesson) {
        override fun toString() = Prefs.settings.lessonTimeFormat.startFormat(order)
    }

    // slúži na výpis o konci hodiny
    private class LessonEnd(lesson: LessonTime) : LessonData(lesson) {
        override fun toString() = Prefs.settings.lessonTimeFormat.endFormat(order)
    }

    private var editingLesson: Lesson? = null
    private var autoAdaptEventEnd = true

    private val parent get() = mainView.parent as ViewGroup?

    @SuppressLint("InflateParams")
    private val mainView = LayoutInflater.from(App.ctx)
        .inflate(R.layout.lesson_tools, null, false)
        ?.lesson_editor_tools ?: throw NullPointerException("No such layout is available!")

    // vzdy budu vkladane spravne hodnoty
    private var day: Day
        get() {
            if (days.isEmpty()) throw IndexOutOfBoundsException("List of days is Empty!")
            return days[mainView.day_setter.selectedItemPosition]
        }
        set(value) {
            val pos = days.indexOf(value)
            if (pos == -1)
                throw IndexOutOfBoundsException("This day isn't part of currently set WorkWeek!")
            mainView.day_setter.setSelection(pos)
        }
    private var time: IntRange
        get() {
            if (scheduleTimes.isEmpty() || lesStarts.isEmpty() || lesEnds.isEmpty())
                throw IndexOutOfBoundsException("Schedule consists of no Lessons!")
            val result = lesStarts[mainView.start_setter.selectedItemPosition].order..
                    lesEnds[mainView.duration_setter.selectedItemPosition].order
            ScheduleEvent.rangeCheck(result)
            return result
        }
        set(value) {
            ScheduleEvent.rangeCheck(value)
            if (scheduleTimes.firstOrNull()?.let { value.first < it.order } != false ||
                scheduleTimes.lastOrNull()?.let { value.last > it.order } != false)
                throw IndexOutOfBoundsException("This time period is out of schedule time period!")

            autoAdaptEventEnd = false
            mainView.start_setter.setSelection(value.first - 1)
            adaptEventEnd()
            mainView.duration_setter.setSelection(value.last - lesEnds.first().order)
            autoAdaptEventEnd = true
        }
    private var regularity: Regularity
        get() {
            if (regularities.isEmpty()) throw Exception("List of regularities is empty!")
            return regularities[mainView.regularity_setter.selectedItemPosition]
        }
        set(value) {
            val i = regularities.indexOf(value)
            if (i == -1) throw IndexOutOfBoundsException("This regularity isn't an option!")
            mainView.regularity_setter.setSelection(i)
        }

    private var subject: Subject
        get() {
            if (subjects.isEmpty()) throw IllegalArgumentException("List of subjects is Empty!")
            return subjects[mainView.subject_setter.selectedItemPosition]
        }
        set(value) {
            val pos = subjects.indexOf(value)
            if (pos == -1) throw IllegalArgumentException("This subject isn't an option!")
            mainView.subject_setter.setSelection(pos)
        }

    private var type: Int
        get() {
            if (types.isEmpty()) throw Exception("List of lesson types is empty!")
            return types[mainView.type_setter.selectedItemPosition].id
        }
        set(value) {
            for (t in types.indices)
                if (types[t].id == value)
                    return mainView.type_setter.setSelection(t)
            throw Exception("This type of lesson isn't an option!")
        }

    private var location
        get() = Lesson.roomValid(mainView.room.text)
        set(value) {
            mainView.room.text?.let { s -> s.replace(0, s.length, value) }
        }

    private var confirmBtn: View? = null
    private var abortBtn: View? = null
    private var clearBtn: View? = null

    private var days = ArrayList<Day>()
    private var scheduleTimes = ArrayList<LessonTime>()
    private var lesStarts = ArrayList<LessonStart>()
    private var lesEnds = ArrayList<LessonEnd>()
    private var regularities = ArrayList<Regularity>()
    private var types = ArrayList<LessonType>()
    private var subjects = ArrayList<Subject>()

    private val isModifiable get() = scheduleTimes.size * types.size * subjects.size > 0
    private var onInput: (ScheduleEvent?) -> Boolean = fun(_) = true
    private var onUpdateEnd: () -> Unit = fun() = Unit

    init {
        days.addAll(Prefs.settings.workWeek.workDay)
        scheduleTimes.addAll(App.data.lessonTimes())
        for (lesson in scheduleTimes) lesStarts.add(LessonStart(lesson))
        val range = (mainView.start_setter?.selectedItemPosition?.coerceAtLeast(0)
            ?: 0) until scheduleTimes.size
        for (l in range) lesEnds.add(LessonEnd(scheduleTimes[l]))
        regularities.addAll(Regularity.values)
        types.addAll(App.data.lessonTypes())
        subjects.addAll(App.data.subjects())

        val ctx = App.ctx
        val lay = android.R.layout.simple_list_item_1
        mainView.subject_setter.adapter = ArrayAdapter(ctx, lay, subjects)
        mainView.regularity_setter.adapter = ArrayAdapter(ctx, lay, regularities)
        mainView.day_setter.adapter = ArrayAdapter(ctx, lay, days)
        mainView.start_setter.adapter = ArrayAdapter(ctx, lay, lesStarts)
        mainView.start_setter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (autoAdaptEventEnd) adaptEventEnd()
            }
        }

        mainView.duration_setter.adapter = ArrayAdapter(ctx, lay, lesEnds)
        mainView.type_setter.adapter = ArrayAdapter(ctx, lay, types)
        mainView.room.addTextChangedListener(object : TextWatcher {
            private val invalid = "(\\s\\s+)|(^\\s+)".toRegex()
            private var st = 0
            private var en = 0
            override fun afterTextChanged(s: Editable?) {
                if (s == null) return
                if (s.length > 20 || s.contains(invalid)) s.delete(st, en)
                if (s.length > 20 || s.contains(invalid))
                    s.replace(0, s.length, s.trimStart().replace("\\s+".toRegex(), " "))
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                st = start
                en = start + count
            }
        })

        customizeViewVisibility()
    }

    private fun adaptEventEnd() {
        val start = lesStarts[mainView.start_setter.selectedItemPosition].order
        val end = lesEnds[mainView.duration_setter.selectedItemPosition].order

        lesEnds.clear()
        for (l in mainView.start_setter.selectedItemPosition until scheduleTimes.size) lesEnds.add(
            LessonEnd(scheduleTimes[l])
        )
        mainView.duration_setter.setSelection((end - start).coerceAtLeast(0))
        (mainView.duration_setter.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    /**
     * Nastavenie správania sa tlačidiel, ktoré slúžia na presmerovanie sa na fragmenty,
     * v ktorých je možné doplniť údaje chýbajúce k tomu, aby sa mohlo manipulovať s rozvrhom
     * @param activity Aktivita na v ktorej je funkcia na presmerovanie, teda prepnutie fragmentu
     */
    fun setEvents(activity: MainActivity) {
        mainView.subjects_restriction.setOnClickListener {
            activity.menuChoice(SubContent.SUBJECTS)
        }
        mainView.lesson_type_restriction.setOnClickListener {
            Prefs.states.lastScheduleDisplay = ScheduleDisplay.LESSON_TYPES
            activity.forceMenuChoice(SubContent.SCHEDULE)
        }
        mainView.schedule_restriction.setOnClickListener {
            Prefs.states.lastScheduleDisplay = ScheduleDisplay.TIME_SCHEDULE
            activity.forceMenuChoice(SubContent.SCHEDULE)
        }
    }

    /**
     * Zrušenie všetkých nadchádzajúcich akcií v handleri.
     */
    fun abortMessages() = handler.removeMessages(0)
    private fun parseLesson(id: Long) = Lesson(id, regularity, day, time, type, subject, location)
    private fun parseFree() = Free(regularity, day, time)
    private fun input(scheduleEvent: ScheduleEvent) {
        // ked pri podozreni ze nedoslo k zmene, skusat to znova a po 3 sekundach to vzdat.
        if (!onInput(scheduleEvent)) handler.postDelayed({ input(scheduleEvent) }, 200)
        handler.postDelayed({ handler.removeMessages(0) }, 2500)
    }

    /**
     * Nastaviť rodičovské okno, do ktorého bude okno vytvorené v tejto triede vložené
     * @param newParent rodič
     * @return Vráti tú istú inštancia Nástrojov na manipuláciu s rozvrhom
     */
    fun attachTo(newParent: ViewGroup?) = also {
        parent?.removeView(mainView)
        newParent?.addView(mainView)
    }

    /**
     * Uviesť, ktoré tlačidlá budú zastávať dané funkcie. Tlačidlá nie sú súčasťou layoutu načítaného
     * v tejto triede
     * @param confirmButton Tlačidlo, ktoré vloží nový alebo upraví označený predmet
     * @param abortButton Tlačidlo, ktoré odznačí upravovaný predmet
     * @param clearButton Tlačidlo, ktoré vymaže označený predmet alebo vyprázdni celý rozvrh
     * @return Vráti tú istú inštanciu nástrojov na manipuláciu s rozvrhom
     */
    fun involveButtons(confirmButton: View?, abortButton: View?, clearButton: View?) = also {
        confirmBtn = confirmButton
        abortBtn = abortButton
        clearBtn = clearButton
        updateButtons()
        confirmBtn?.setOnClickListener { confirm() }
        abortBtn?.setOnClickListener { abort() }
        clearBtn?.setOnClickListener { removeOrEmpty() }
        if (!isModifiable) {
            confirmBtn?.visibility = GONE
            abortBtn?.visibility = GONE
            clearBtn?.visibility = GONE
        }
    }

    /**
     * Nastavuje, čo sa má udiať, keď bol nejaký predmet už aktualizovaný
     * @param fn Metóda, ktorá sa má udiať, keď bol predmet aktualizovaný
     */
    fun setOnUpdateEnd(fn: () -> Unit) {
        onUpdateEnd = fn
    }

    /**
     * Nastavuje, čo sa má stať, keď sa vykonajú zmeny v rozvrhu
     * @fn Metóda, ktorá sa má vykonať po vykonaní zmeny rozvrhu
     */
    fun setOnInput(fn: (ScheduleEvent?) -> Boolean) {
        onInput = fn
    }

    private fun customizeViewVisibility() {
        mainView.apply {
            val modifiable = isModifiable
            error_zone.visibility = if (modifiable) GONE else VISIBLE
            edit_zone.visibility = if (modifiable) VISIBLE else GONE
            confirmBtn?.visibility = if (modifiable) VISIBLE else GONE
            abortBtn?.visibility = if (modifiable) VISIBLE else GONE
            clearBtn?.visibility = if (modifiable) VISIBLE else GONE

            if (!modifiable) {
                lesson_type_restriction.visibility =
                    if (App.data.hasLessonTypes()) GONE else VISIBLE
                schedule_restriction.visibility = if (App.data.isScheduleSet()) GONE else VISIBLE
                subjects_restriction.visibility = if (App.data.hasSubjects()) GONE else VISIBLE
            }
        }
    }

    /**
     * Označenie hodiny na ktorej budú vykonané zmeny. Toto označenie je po vykonaní zmien
     * vhodné zrušiť pomocou metódy [unsetLesson].
     * @param lesson Hodina, ktorú upravujem
     * @see unsetLesson
     */
    fun setLesson(lesson: Lesson) {
        editingLesson = lesson
        updateButtons()
        subject = lesson.sub
        day = lesson.day
        time = lesson.time
        regularity = lesson.regularity
        type = lesson.type
        location = lesson.room
    }

    /**
     * Zrušenie označenia hodiny, na ktorej mali byť vykonané úpravy.
     * @see setLesson
     */
    private fun unsetLesson() {
        if (editingLesson != null) {
            editingLesson = null
            updateButtons()
            onUpdateEnd()
        }
    }

    /**
     * Zhromaždiť informácie o vyučovacej hodine, ktorá je práve upravovaná.
     * @return Textový výpis informácii o danej hodine. Vráti null, ak nie je žiadna hodina upravovaná
     */
    fun getLessonInfo(): String? = editingLesson?.toString()

    private fun updateButtons() {
        fun txt(view: View?, @StringRes res: Int) {
            view?.let { if (it is TextView) it.setText(res) }
        }
        txt(confirmBtn, editingLesson?.let { R.string.edit } ?: R.string.insert)
        txt(abortBtn, R.string.abort)
        abortBtn?.visibility = editingLesson?.let { VISIBLE } ?: GONE
        txt(clearBtn, editingLesson?.let { R.string.delete } ?: R.string.to_empty)
    }

    private fun confirm() {
        if (!isModifiable) return
        val id = App.data.setLesson(
            editingLesson?.id ?: -1L,
            regularity,
            day,
            time,
            type,
            subject.id,
            location
        )
        if (id < 1) throw RuntimeException("Lesson could not be set! Check the inputs on \"LessonTools.kt\"!")
        val resId = editingLesson?.let { R.string.lesson_updated } ?: R.string.lesson_inserted
        unsetLesson()
        input(parseLesson(id))
        App.toast(resId, Gravity.CENTER)
    }

    private fun abort() {
        if (isModifiable) {
            unsetLesson()
            App.toast(R.string.lesson_edit_aborted, Gravity.CENTER)
        }
    }

    private fun removeOrEmpty() {
        if (!isModifiable) return
        when {
            editingLesson == null -> {
                App.data.clearSchedule()
                onInput(null)
                App.toast(R.string.schedule_emptied, Gravity.CENTER)
            }
            editingLesson!!.id < 1 -> {
                App.data.clearSchedule(day, time, regularity)
                input(parseFree())
                App.toast(R.string.time_cleared, Gravity.CENTER)
            }
            else -> {
                App.data.deleteLesson(editingLesson!!.id)
                input(parseFree())
                App.toast(R.string.lesson_removed, Gravity.CENTER)
            }
        }
        unsetLesson()
    }
}