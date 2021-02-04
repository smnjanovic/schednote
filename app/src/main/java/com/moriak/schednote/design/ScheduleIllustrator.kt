package com.moriak.schednote.design

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.view.children
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.Free
import com.moriak.schednote.database.data.Lesson
import com.moriak.schednote.database.data.ScheduleEvent
import com.moriak.schednote.database.data.TimeTable
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.ColorGroup.*
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity
import com.moriak.schednote.settings.Regularity.*
import com.moriak.schednote.settings.WorkWeek
import kotlinx.android.synthetic.main.schedule_cell.view.*
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Trieda slúži na vykreslenie tabuľky s rozvrhom. Jej inštancia umožňuje meniť vzhľad tejto triedy
 */
class ScheduleIllustrator private constructor(private val clickAllowed: Boolean = true) {
    companion object {
        /**
         * Funkcia vytvorí novú inštanciu, ktorá bude vykresľovať rozvrh a následne umožňovať
         * vykonávať v ňom zmeny.
         * @param illustrator Namiesto tvorby novej inštancie je možné použiť starú, pokiaľ nie je null.
         * @param clickable Rozhoduje o tom, či budú bunky reagovať na kliknutie
         * @return [ScheduleIllustrator]
         */
        fun schedule(illustrator: ScheduleIllustrator?, clickable: Boolean): ScheduleIllustrator {
            if (illustrator == null || illustrator.clickAllowed != clickable)
                return ScheduleIllustrator(clickable)
            return illustrator
        }

        /**
         * Vykreslenie rozvrhu ako obrázok
         * @param workWeek Pracovný týždeň
         * @param reg Párny, nepárny alebo každý týždeň
         * @return [Bitmap] obrázok s nakresleným rozvrhom
         */
        fun drawSchedule(workWeek: WorkWeek, reg: Regularity): Bitmap {
            return TimeTable(reg, workWeek).drawSchedule()
        }
    }

    private var regularity: Regularity = Regularity.currentWeek
    private var colors = PaletteStorage()
    private var background: View? = null
    private var oddWeek: View? = null
    private var evenWeek: View? = null
    private val table: TableLayout = TableLayout(App.ctx).also {
        it.layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }
    private val cols: TableRow = row()
    private val head: TableRow = row()
    private val days = TreeMap<Day, TableRow>()

    // events
    private val lessonClickEvent = View.OnClickListener { v ->
        if (v.tag !is Free) onLessonClick(v.tag as Lesson)
    }
    private val switchEvent = View.OnClickListener { v ->
        if (regularity != v.tag && v.tag is Regularity) {
            regularity = v.tag as Regularity
            redraw()
            recolorButtons()
            App.toast((v.tag as Regularity).toString(), Gravity.TOP)
        }
    }
    private var onLessonClick: ((lesson: Lesson) -> Unit) = fun(_) {}

    /**
     * Nastavenie rodiča tabuľky rozvrhu
     * @param v nový rodič
     * @return [ScheduleIllustrator] Vracia objekt, v ktorom sa nachádza
     */
    fun attachTo(v: ViewGroup) = also {
        (table.parent as ViewGroup?)?.removeView(table)
        v.addView(table)
        v.post { customizeColumnWidth(v.width) }
    }

    /**
     * Nastavenie ktoré okno sa použije ako farba pozadia
     * @param bg Okno s meniteľným pozadím
     * @return [ScheduleIllustrator] Vracia objekt, v ktorom sa nachádza
     */
    fun background(bg: View?) = also {
        background = bg
        bg?.setBackgroundColor(colors[-BACKGROUND.ordinal]!!.color)
    }

    /**
     * Zahrnutie tlačidiel, ktoré prevezmu určitu rolu. Tlačidlá budú skryté pokiaľ nie je
     * povolený dvojtýždenný rozvrh.
     * @param odd Tlačidlo bude slúžiť na zobrazenie rozvrhu pre nepárny týždeň
     * @param even Tlačidlo bude slúžiť na zobrazenie rozvrhu pre párny týždeň
     * @return [ScheduleIllustrator] Vracia objekt, v ktorom sa nachádza
     */
    fun involveButtons(odd: View?, even: View?) = also {
        oddWeek = odd?.also { b -> b.tag = ODD }
        evenWeek = even?.also { b -> b.tag = EVEN }
        oddWeek?.setOnClickListener(switchEvent)
        evenWeek?.setOnClickListener(switchEvent)
        recolorButtons()
        setButtonsVisible(regularity != EVERY)
    }

    /**
     * Nastavenie metódy, ktorá sa vykoná po kliknutí na niektorú z vyučovacích hodín
     * @param fn Metóda, ktrá sa má vykonať po kliknutí na ľubovoľnú hodinu
     */
    fun setOnLessonClick(fn: (lesson: Lesson) -> Unit) {
        onLessonClick = fn
    }

    /**
     * Zmeniť šírku tabuľky a jej stĺpcov tak, aby boli všetky stĺpce rovnako široké, až na posledný.
     * Posledný stĺpec môže byť širší od ostatných o zvyšok po delení šírky tabuľky počtom jej stĺpcov.
     * @param w Nastavenie šírky tabuľky.
     */
    fun customizeColumnWidth(w: Int = cols.width) {
        val c = cols.childCount
        // ani w ani c nesmu byt 0
        if (w * c == 0) return

        val allParams = TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also { it.width = w / c }
        val lastParam =
            TableRow.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also { it.width = (w / c) + (w % c) }
        val range = 0 until cols.childCount
        for (ch in range) cols.getChildAt(ch).layoutParams =
            if (ch < cols.childCount - 1) allParams else lastParam
    }

    /**
     * Tu dochádza k prekresleniu celého rozvrhu
     */
    fun redraw() {
        days.clear()
        TimeTable(regularity, Prefs.settings.workWeek)
            .buildSchedule(
                table,
                cols,
                head,
                this::row,
                this::tplCol,
                this::headCell,
                this::bodyCell
            )
            .forEach { (d, v) -> days[d] = v as TableRow }
        if (cols.width > 0) customizeColumnWidth()
        else table.post { if (cols.width > 0) customizeColumnWidth() }
    }

    private fun row() = TableRow(App.ctx).also {
        it.layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    private fun setButtonsVisible(visible: Boolean) {
        oddWeek?.visibility = if (visible) View.VISIBLE else View.GONE
        evenWeek?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun recolorButtons() {
        val palette =
            colors[-TABLE_HEAD.ordinal] ?: Prefs.settings.getColor(TABLE_HEAD).luminance(85)
        val dg = GradientDrawable().also { it.setStroke(2, palette.contrastColor) }
        oddWeek?.setBackgroundColor(palette.color)
        evenWeek?.setBackgroundColor(palette.color)
        oddWeek?.foreground = dg
        evenWeek?.foreground = dg
        if (oddWeek is TextView) (oddWeek as TextView).setTextColor(
            if (regularity == ODD) palette.contrastColor
            else palette.contrastColorLowAlpha
        )
        if (evenWeek is TextView) (evenWeek as TextView).setTextColor(
            if (regularity == EVEN) palette.contrastColor
            else palette.contrastColorLowAlpha
        )
    }

    @SuppressLint("InflateParams")
    private fun cell(span: Int): LinearLayout {
        val palette = colors[-FREE.ordinal] ?: throw NullPointerException("No palette is present!")
        val color = Color.argb(255, palette.red, palette.green, palette.blue)
        val c = LayoutInflater.from(App.ctx).inflate(R.layout.schedule_cell, null, false).cell!!
        if (c.layoutParams !is TableRow.LayoutParams) c.layoutParams = TableRow.LayoutParams()
        (c.layoutParams as TableRow.LayoutParams).span = span
        c.foreground = GradientDrawable().also { it.setStroke(1, color) }
        return c
    }

    private fun tplCol(): View = FrameLayout(App.ctx)

    private fun headCell(range: IntRange): View {
        ScheduleEvent.rangeCheck(range)
        return cell(range.count()).also { cell ->
            cell.setBackgroundColor(colors[-TABLE_HEAD.ordinal]!!.color)
            cell.header.setTextColor(colors[-TABLE_HEAD.ordinal]!!.contrastColor)
            cell.tag = TABLE_HEAD
            cell.header.text = Prefs.settings.lessonTimeFormat.startFormat(range.first)
            cell.cell_abb.visibility = View.GONE
            cell.cell_room.visibility = View.GONE
        }
    }

    private fun bodyCell(scheduleEvent: ScheduleEvent): View =
        cell(scheduleEvent.time.count()).also {
        it.tag = scheduleEvent
        it.setBackgroundColor(colors[if (scheduleEvent is Lesson) scheduleEvent.type else -FREE.ordinal]!!.color)
        it.header.visibility = View.GONE
        when (scheduleEvent) {
            is Lesson -> {
                it.cell_abb.text = scheduleEvent.sub.abb
                it.cell_room.text = scheduleEvent.room
                val textColor = colors[scheduleEvent.type]!!.contrastColor
                it.cell_abb.setTextColor(textColor)
                it.cell_room.setTextColor(textColor)
                if (clickAllowed) it.setOnClickListener(lessonClickEvent)
            }
        }
    }

    /**
     * Vizuálne sa zmenia farby tématickych prvkov ako sú množiny buniek v tabuľke a pozadie.
     * @param type typ bunky ku ktorej je farba nastavovaná
     * @param palette Dynamicky nastaviteľná farba
     */
    fun recolor(type: Int, palette: Palette) {
        colors[type] = palette

        when (type) {
            -BACKGROUND.ordinal -> background?.setBackgroundColor(palette.color)
            -TABLE_HEAD.ordinal -> {
                if (regularity != EVERY) recolorButtons()
                for (ch in head.children) {
                    ch.setBackgroundColor(palette.color)
                    ch.header.setTextColor(palette.contrastColor)
                }
            }
            -FREE.ordinal -> {
                val fullAlpha = Color.argb(255, palette.red, palette.green, palette.blue)
                fun stroke(v: View) {
                    if (v.foreground !is GradientDrawable) v.foreground = GradientDrawable()
                    (v.foreground as GradientDrawable).setStroke(1, fullAlpha)
                }
                for (col in cols.children) stroke(col)
                for (col in head.children) stroke(col)
                for ((_, row) in days) {
                    for (child in row.children) {
                        val tag = child.tag
                        if (tag is Free) {
                            child.setBackgroundColor(palette.color)
                            child.cell_abb.setTextColor(palette.contrastColor)
                            child.cell_room.setTextColor(palette.contrastColor)
                        }
                        stroke(child)
                    }
                }
            }
            else -> {
                for ((_, row) in days) {
                    for (child in row.children) {
                        val tag = child.tag
                        if (tag is Lesson && tag.type == type) {
                            child.setBackgroundColor(palette.color)
                            child.cell_abb.setTextColor(palette.contrastColor)
                            child.cell_room.setTextColor(palette.contrastColor)
                        }
                    }
                }
            }
        }
    }

    /**
     * Natrvalo uložiť súčasné zafarbenie prvkov daného typu [type]
     * @param type typ
     */
    fun storeColor(type: Int) = colors.save(type)
}