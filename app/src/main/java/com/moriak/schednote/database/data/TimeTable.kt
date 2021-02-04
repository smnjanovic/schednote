package com.moriak.schednote.database.data

import android.graphics.*
import android.view.View
import android.view.ViewGroup
import com.moriak.schednote.App
import com.moriak.schednote.design.PaletteStorage
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.ColorGroup
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity
import com.moriak.schednote.settings.WorkWeek
import java.util.*
import kotlin.collections.ArrayList

data class TimeTable(private val reg: Regularity, private val week: WorkWeek) {
    private val min: Int
    private val max: Int
    private val size: Int
    private val list: List<ScheduleEvent>

    init {
        var vMin = Int.MAX_VALUE
        var vMax = 0
        val tree = App.data.getLessons(week, reg)
        for ((_, a) in tree) {
            if (a.size > 0) {
                vMin = vMin.coerceAtMost(a.first().time.first)
                vMax = vMax.coerceAtLeast(a.last().time.last)
            }
        }
        min = if (vMax < vMin) 0 else vMin
        max = if (vMax < vMin) 0 else vMax
        size = max - min + 1
        list = ArrayList()

        for (d in week.workDays) {
            val a = tree[d]!!
            val size = a.size
            var end = max + 1
            for (i in (0 until size).reversed()) {
                val next = a[i].time.last + 1
                if (next < end) a.add(i + 1, Free(reg, d, next until end))
                end = a[i].time.first
            }
            if (min < end) a.add(0, Free(reg, d, min until end))
            list.addAll(a)
        }
    }

    private fun headerSpan() = when {
        size in 0..7 -> 1
        size in 8..11 || size % 3 == 1 -> 2
        else -> 3
    }

    fun drawSchedule(): Bitmap {
        val colors = PaletteStorage()

        // rozmery
        val colW = (App.w.coerceAtMost(App.h) / size)
        val headH = App.dp(18)
        val rowH = App.dp(35)
        val width = colW * size
        val height = headH + week.workDays.count() * rowH

        // kreslenie
        val cellFill = Paint()
        val cellStroke = Paint()
        val headText = Paint()
        val abbText = Paint()
        val roomText = Paint()

        fun Canvas.drawTopCell(cellTime: IntRange) {
            if (cellTime.first < min || cellTime.last > max) throw Exception("Invalid range!")
            val left = (cellTime.first - min) * colW
            val right = left + cellTime.count() * colW
            val r = Rect(left, 0, right, headH)
            val palette = colors[-ColorGroup.TABLE_HEAD.ordinal]!!
            cellFill.color = palette.color
            headText.color = palette.contrastColor
            drawRect(r, cellFill)
            drawRect(r, cellStroke)
            if (cellTime.first != 0) {
                val txt = Prefs.settings.lessonTimeFormat.startFormat(cellTime.first)
                val padding = App.dp(5).toFloat()
                drawText(txt, 0, txt.length, r.left + padding, r.bottom - padding, headText)
            }
        }

        fun Canvas.drawScheduleEvent(e: ScheduleEvent) {
            val d = week.workDays.indexOf(e.day)
            if (d == -1) throw Exception("Invalid day!")
            if (e.time.first < min || e.time.last > max) throw Exception("Event time [${e.time}] out of range [${min..max}]!")
            val left = (e.time.first - min) * colW
            val right = left + e.time.count() * colW
            val top = d * rowH + headH
            val bottom = top + rowH
            val r = Rect(left, top, right, bottom)
            val palette = colors[if (e is Lesson) e.type else -ColorGroup.FREE.ordinal]!!
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
        cellStroke.color = colors[-ColorGroup.FREE.ordinal]!!.color

        // kreslenie
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val step = headerSpan()
        for (i in min..max step step) canvas.drawTopCell(i..(i + step - 1).coerceAtMost(max))
        for (event in list) canvas.drawScheduleEvent(event)
        return bitmap
    }

    fun buildSchedule(
        frame: ViewGroup,
        cols: ViewGroup,
        headRow: ViewGroup,
        newRow: () -> ViewGroup,
        tplCol: () -> View,
        headCol: (IntRange) -> View,
        evtCol: (ScheduleEvent) -> View
    ): TreeMap<Day, ViewGroup> {
        frame.removeAllViews()
        cols.removeAllViews()
        headRow.removeAllViews()
        frame.addView(cols)
        frame.addView(headRow)

        val dayRows = TreeMap<Day, ViewGroup>()
        week.workDays.forEach { dayRows[it] = newRow().also(frame::addView) }

        for (i in min..max) cols.addView(tplCol())
        val step = headerSpan()
        for (i in min..max step step) headRow.addView(headCol(i..(i + step - 1).coerceAtMost(max)))
        for (evt in list) dayRows[evt.day]?.addView(evtCol(evt))
        return dayRows
    }
}