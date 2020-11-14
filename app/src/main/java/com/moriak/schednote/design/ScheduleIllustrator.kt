package com.moriak.schednote.design

import android.annotation.SuppressLint
import android.graphics.*
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

class ScheduleIllustrator private constructor(private val clickAllowed: Boolean = true) {
    companion object {
        fun schedule(
            illustrator: ScheduleIllustrator?,
            allowClick: Boolean = true
        ): ScheduleIllustrator = illustrator ?: ScheduleIllustrator(allowClick)

        fun drawSchedule(workWeek: WorkWeek, reg: Regularity): Bitmap {
            // data
            val colors = PaletteStorage()
            val range = App.data.scheduleRange(workWeek, reg)
            val events = App.data.fullSchedule(workWeek, reg)

            // rozmery
            val colW = (App.w.coerceAtMost(App.h) / range.count())
            val headH = App.dp(18)
            val rowH = App.dp(35)
            val width = colW * range.count()
            val height = headH + workWeek.days.count() * rowH

            // kreslenie
            val cellFill = Paint()
            val cellStroke = Paint()
            val headText = Paint()
            val abbText = Paint()
            val roomText = Paint()

            fun Canvas.drawTopCell(time: IntRange) {
                if (time.first < range.first || time.last > range.last) throw Exception("Invalid range!")
                val left = (time.first - range.first) * colW
                val right = left + time.count() * colW
                val r = Rect(left, 0, right, headH)
                val palette = colors[-TABLE_HEAD.ordinal]!!
                cellFill.color = palette.color
                headText.color = palette.contrastColor
                drawRect(r, cellFill)
                drawRect(r, cellStroke)
                if (time.first != 0) {
                    val txt = Prefs.settings.lessonTimeFormat.startFormat(time.first)
                    val padding = App.dp(5).toFloat()
                    drawText(txt, 0, txt.length, r.left + padding, r.bottom - padding, headText)
                }
            }

            fun Canvas.drawScheduleEvent(e: ScheduleEvent) {
                val d = workWeek.days.indexOf(e.day)
                if (d == -1) throw Exception("Invalid day!")
                if (e.time.first < range.first || e.time.last > range.last) throw Exception("Invalid range!")
                val left = (e.time.first - range.first) * colW
                val right = left + e.time.count() * colW
                val top = d * rowH + headH
                val bottom = top + rowH
                val r = Rect(left, top, right, bottom)
                val palette = colors[if (e is Lesson) e.type else -FREE.ordinal]!!
                cellFill.color = palette.color

                drawRect(r, cellFill)
                drawRect(r, cellStroke)
                if (e is Lesson) {
                    abbText.color = palette.contrastColor
                    roomText.color = palette.contrastColor
                    val sub = e.sub.abb
                    val room = e.room
                    val center = r.centerX().toFloat()
                    drawText(sub, 0, sub.length, center, r.top + (rowH * 14 / 33F), abbText)
                    if (room != null) drawText(
                        room,
                        0,
                        room.length,
                        center,
                        r.top + (rowH * 28 / 33F),
                        roomText
                    )
                }
            }


            fun text(p: Paint, size: Int, bold: Boolean, align: Paint.Align = Paint.Align.CENTER) {
                p.style = Paint.Style.FILL
                p.textSize = App.dp(size).toFloat()
                p.isElegantTextHeight = true
                p.textAlign = align
                if (bold) p.typeface = Typeface.DEFAULT_BOLD
            }

            text(headText, 11, true, Paint.Align.LEFT)
            text(abbText, 13, true)
            text(roomText, 10, false)

            cellFill.style = Paint.Style.FILL
            cellStroke.strokeWidth = App.dp(1).toFloat()
            cellStroke.style = Paint.Style.STROKE
            cellStroke.color = colors[-FREE.ordinal]!!.color

            // kreslenie
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val step = headerSpan(range)
            for (i in range step step) canvas.drawTopCell(i..(i + step - 1).coerceAtMost(range.last))
            for (event in events) canvas.drawScheduleEvent(event)
            return bitmap
        }

        private fun headerSpan(range: IntRange) = when (val count = range.count()) {
            in 0..7 -> 1
            in 8..11 -> 2
            else -> if (count % 3 != 1) 3 else 2
        }
    }

    private var View.span
        get() = if (layoutParams !is TableRow.LayoutParams) 0 else (layoutParams as TableRow.LayoutParams).span
        set(value) {
            if (layoutParams == null) layoutParams =
                TableRow.LayoutParams().also { it.span = value }
            else if (layoutParams is TableRow.LayoutParams) (layoutParams as TableRow.LayoutParams).span =
                value
        }

    //data
    private var regularity: Regularity = Regularity.currentWeek
    private var workWeek: WorkWeek = Prefs.settings.workWeek
    private var lessonTimeFormat = Prefs.settings.lessonTimeFormat
    private var range = App.data.scheduleRange(workWeek, regularity)
    private var colors = PaletteStorage()

    // buttons
    private var background: View? = null
    private var oddWeek: View? = null
    private var evenWeek: View? = null

    // built layout
    private val table: TableLayout = TableLayout(App.ctx).also {
        it.layoutParams = TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }
    private val cols: TableRow = row().also { table.addView(it) }
    private val head: TableRow = row().also { table.addView(it) }
    private val days = TreeMap<Day, TableRow>().apply {
        for (day in workWeek.days)
            put(day, row().also { table.addView(it) })
    }

    // events
    private val lessonClickEvent = View.OnClickListener { v ->
        if (v.tag !is Free) onLessonClick(v.tag as Lesson)
    }

    private val switchEvent = View.OnClickListener { v ->
        if (regularity != v.tag) {
            regularity = v.tag as Regularity
            range = App.data.scheduleRange(workWeek, regularity)
            redraw()
            recolorButtons()
            App.toast((v.tag as Regularity).toString(), Gravity.TOP)
        }
    }
    private var onLessonClick: ((lesson: Lesson) -> Unit) = fun(_) {}

    init {
        fillTop()
        fillContent()
    }

    fun attachTo(v: ViewGroup) = also {
        (table.parent as ViewGroup?)?.removeView(table)
        v.addView(table)
    }

    fun background(bg: View?) = also {
        background = bg
        bg?.setBackgroundColor(colors[-BACKGROUND.ordinal]!!.color)
    }

    fun involveButtons(odd: View?, even: View?) = also {
        oddWeek = odd?.also { b -> b.tag = ODD }
        evenWeek = even?.also { b -> b.tag = EVEN }
        oddWeek?.setOnClickListener(switchEvent)
        evenWeek?.setOnClickListener(switchEvent)
        recolorButtons()
        setButtonsVisible(regularity != EVERY)
    }

    fun setOnLessonClick(fn: (lesson: Lesson) -> Unit) {
        onLessonClick = fn
    }

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

    fun empty() {
        range = 0..0
        clear()
        fillTop()
        for ((d, r) in days) r.addView(bodyCell(Free(EVERY, d, range)))
    }

    fun put(added: ScheduleEvent): Boolean {
        val oldRange = range
        range = App.data.scheduleRange(workWeek, regularity)
        redraw()

        if (range != oldRange) return true

        val row = days[added.day]!!
        for (ch in row.children) {
            val timeRange = ch.tag as ScheduleEvent
            if (added is Lesson && added.time == timeRange.time) return true
            else if (added is Free && added.time.first >= timeRange.time.first
                && added.time.last <= timeRange.time.last
            ) return true
        }

        return false
    }

    private fun clear() {
        cols.removeAllViews()
        head.removeAllViews()
        for ((_, r) in days) r.removeAllViews()
    }

    private fun clear(row: TableRow?) {
        row?.removeAllViews()
    }

    private fun fillTop() {
        clear(cols)
        clear(head)
        for (r in range) cols.addView(FrameLayout(App.ctx))

        val cols = headerSpan(range)

        for (order in range step cols)
            head.addView(
                headCell(
                    (range.last - order + 1).coerceAtMost(cols),
                    if (range != 0..0) order else null
                )
            )
        customizeColumnWidth()
    }

    private fun fillContent() {
        val schedule = App.data.fullSchedule(workWeek, regularity)
        for (scheduleEvent in schedule) days[scheduleEvent.day]?.addView(bodyCell(scheduleEvent))
    }

    private fun redraw() {
        clear()
        fillTop()
        fillContent()
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
        c.span = span
        c.foreground = GradientDrawable().also {
            it.setStroke(1, color)
        }
        return c
    }

    private fun headCell(span: Int, order: Int?): View = cell(span).also { cell ->
        cell.setBackgroundColor(colors[-TABLE_HEAD.ordinal]!!.color)
        cell.header.setTextColor(colors[-TABLE_HEAD.ordinal]!!.contrastColor)
        cell.tag = TABLE_HEAD
        cell.header.tag = order
        cell.header.text = order?.let { lessonTimeFormat.startFormat(order) }
        cell.cell_abb.visibility = View.GONE
        cell.cell_room.visibility = View.GONE
    }

    private fun bodyCell(scheduleEvent: ScheduleEvent) = cell(scheduleEvent.time.count()).also {
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

    fun storeColor(type: Int) = colors.save(type)
}