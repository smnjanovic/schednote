package com.moriak.schednote.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.moriak.schednote.enums.Day
import com.moriak.schednote.enums.WorkWeek
import java.util.*
import kotlin.math.roundToInt

/**
 * Objekt [ScheduleView] je súčasťou Layoutu. Má za úlohu kresliť rozvrh hodín.
 */
@SuppressLint("ViewConstructor")
open class ScheduleView : CustomView {
    companion object {
        const val BACKGROUND_COLOR = 0
        const val HEADER_COLOR = -1
        const val FREE_COLOR = -2
        private const val SCH_W = 648
        private const val DAY_H = 65
        private const val HEAD_H = 33
        private const val HEAD_HORIZONTAL_PADDING = 4
        private const val HEAD_VERTICAL_PADDING = 6
        private const val TXT_HEAD = 23
        private const val TXT_ABB = 23
        private const val TXT_ROOM = 20
    }

    /**
     * Objekt, ktorého úlohou je získať formát rozvrhu
     */
    interface ColumnNameFormat {
        /**
         * Získa textový popis bloku [col].
         * @param col
         */
        fun getColumnDescription(col: Int): String
    }

    /**
     * Objekt slúži na kreslenie rozvrhu
     * @property schL Ľavý kraj rozvrhu
     * @property schT vrchný kraj rozvrhu
     * @property schW šírka rozvrhu
     * @property schH výška rozvrhu
     */
    class ScheduleIllustrator(strokeWidth: Float = 2F) {
        constructor(other: ScheduleIllustrator, strokeWidth: Float = 2F): this(strokeWidth) {
            copy(other)
        }
        var schL: Int = 0
        var schT: Int = 0
        var schW: Int = 0
        var schH: Int = 0
        private var colW: Int = 0
        private var colH: Int = 0
        private var hColH: Int = 0
        private var headHorizontalPadding: Int = 0
        private var headVerticalPadding: Int = 0

        private val rect = Rect()
        private val blockFill = Paint()
        private val blockStroke = Paint()
        private val headText = Paint()
        private val abbText = Paint()
        private val roomText = Paint()

        private var workWeek: WorkWeek = WorkWeek.MON_FRI
        private var range: IntRange = 0..0
        private val schedule: TreeMap<Int, TreeMap<Day, LessonUnit>> = TreeMap()
        private val tags = TreeMap<Int, TreeMap<Day, Any?>>()
        private val color: TreeMap<Int, Pair<Int, Int>> = TreeMap()
        private var columnNameFormat: ColumnNameFormat? = null

        init {
            blockFill.style = Paint.Style.FILL
            blockStroke.style = Paint.Style.STROKE
            blockStroke.strokeWidth = strokeWidth
            blockStroke.color = Color.BLACK
            headText.style = Paint.Style.FILL
            abbText.style = Paint.Style.FILL
            roomText.style = Paint.Style.FILL
            headText.isElegantTextHeight = true
            abbText.isElegantTextHeight = true
            roomText.isElegantTextHeight = true
            headText.textAlign = Paint.Align.LEFT
            abbText.textAlign = Paint.Align.CENTER
            roomText.textAlign = Paint.Align.CENTER
            headText.typeface = Typeface.DEFAULT_BOLD
            abbText.typeface = Typeface.DEFAULT_BOLD
            roomText.typeface = Typeface.DEFAULT
            headText.isAntiAlias = true
            abbText.isAntiAlias = true
            roomText.isAntiAlias = true
        }

        /**
         * Skopíruje obsah inštancie [other] do tejto inštancie.
         * @param other iný objekt toho istého typu
         */
        fun copy (other: ScheduleIllustrator) {
            workWeek = other.workWeek
            range = other.range
            schedule.clear()
            schedule.putAll(other.schedule)
            tags.clear()
            tags.putAll(other.tags)
            color.clear()
            color.putAll(other.color)
            columnNameFormat = other.columnNameFormat
        }

        private fun checkRange(r: IntRange) {
            if (r.count() == 0) throw IllegalArgumentException("Empty range not allowed!")
            if (r.first < 1) throw IllegalArgumentException("Range values cannot be lower than 0!")
        }

        /**
         * Nastaví formát popisu blokov rozvrhu
         * @param nameFormat Objekt, v ktorom je funkcia, ktorá získa reťazec popisujúci blok
         */
        fun setColumnFormat(nameFormat: ColumnNameFormat?) {
            columnNameFormat = nameFormat
        }

        private fun setItem(day: Day, time: IntRange, item: LessonUnit) {
            checkRange(time)
            if (range == 0..0 || time.first < range.first && time.last > range.last) range = time
            else if (time.first < range.first) range = time.first .. range.last
            else if (range.last < time.last) range = range.first .. time.last
            for (t in time) {
                if (schedule[t] == null) schedule[t] = TreeMap()
                schedule[t]!![day] = item
            }
        }

        /**
         * Uvolní bloky v riadku [day] a stĺpcoch v rozsahu [time]
         * @param day
         * @param time
         */
        fun free(day: Day, time: IntRange) {
            checkRange(time)
            for (t in time) schedule[t]?.remove(day)
            var st = range.first
            var en = range.last

            val bst = range.first in time
            val ben = range.last in time

            if (bst) {
                var r = time.first
                while (schedule[r]?.isEmpty() != false && r <= range.last) r++
                st = if (r > range.last) 0 else r
            }
            if (ben) {
                var r = time.last
                while (schedule[r]?.isEmpty() != false && r >= range.first) r--
                en = if (r < range.first) 0 else r
            }
            if (bst || ben) range = st .. en
        }

        /**
         * odloží prvok [tag] pod dvojicu: deň [day] a blok, pre každý blok z rozsahu [time]
         * @param day
         * @param time
         * @param tag
         */
        fun setTag(day: Day, time: IntRange, tag: Any?) {
            for (t in time) {
                if (tags[t] == null) tags[t] = TreeMap()
                tags[t]!![day] = tag
            }
        }

        /**
         * Zapíše hodinu typu [type] k predmetu so skratkou [abb] v miestnosti [room]
         * na deň [day] do blokov v rozsahu [time].
         * @param day
         * @param time
         * @param type
         * @param abb
         * @param room
         */
        fun addLesson(day: Day, time: IntRange, type: Int, abb: String, room: String?) {
            setItem(day, time, LessonUnit(type, abb, room))
        }

        /**
         * Vyprázdni rozvrh hodín
         */
        fun clear() {
            schedule.forEach { (_, map) -> map.clear() }
            range = 0..0
        }

        /**
         * Odstráni všetky doplnkové dáta (tagy)
         */
        fun clearTags() {
            tags.forEach { (_, map) -> map.clear() }
        }

        /**
         * Nastaví farbu výplne [background] a textu [text] pre všetky bloky s obsahom typu [type].
         */
        fun setTypeColor(type: Int, @ColorInt background: Int, @ColorInt text: Int) {
            color[type] = Pair(background, text)
            if (type == BACKGROUND_COLOR) blockStroke.color = text
        }

        /**
         * vráti dvojicu farbieb (výplň a text) k typu hodiny [type].
         * @param type typ vyučovacej hodiny
         * @return dvojica farieb vyjadrená číslom
         */
        fun getTypeColor(type: Int): Pair<Int, Int>? = color[type]

        /**
         * nastaví pracovný týždeň rozvrhu. Zmení sa, ake dni rozvrh bude zobrazovať
         * a od ktorého dňa sa počítajú
         * @param ww Pracovný týždeň
         */
        fun setWorkWeek(ww: WorkWeek) {
            workWeek = ww
        }

        /**
         * získa hodnotu odloženú pod kľúčom dvojice [day] a [block].
         * @param day
         * @param block
         * @return značka odložená pod kľúčom dvojice [day] a [block].
         */
        fun getTag(day: Day, block: Int) = tags[block]?.get(day)

        /**
         * Vráti rozsah využitých blokov v rozvrhu
         */
        fun getRange() = range

        /**
         * Prispôsobí veľkosť rozvrhu a textu v ňom podľa šírky [w]
         * @param w šírka v pixeloch
         */
        fun scaleScheduleByWidth(w: Int) {
            val len = range.count()
            colW = if (w < 1) 0 else w / len
            schW = if (w < 1) 0 else colW * len
            hColH = if (w < 1) 0 else HEAD_H * schW / SCH_W
            colH = if (w < 1) 0 else DAY_H * schW / SCH_W
            schH = if (w < 1) 0 else hColH + workWeek.workDays.size * colH
            headText.textSize = TXT_HEAD * schW / SCH_W.toFloat()
            abbText.textSize = TXT_ABB * schW / SCH_W.toFloat()
            roomText.textSize = TXT_ROOM * schW / SCH_W.toFloat()
            headVerticalPadding = HEAD_VERTICAL_PADDING * hColH / HEAD_H
            headHorizontalPadding = HEAD_HORIZONTAL_PADDING * hColH / HEAD_H
        }

        private fun drawHeaderBlock(canvas: Canvas, time: IntRange) {
            rect.left = schL + (time.first - range.first) * colW
            rect.top = schT
            rect.right = rect.left + time.count() * colW
            rect.bottom = rect.top + hColH
            val pair = color[HEADER_COLOR] ?: Pair(Color.WHITE, Color.BLACK)
            blockFill.color = pair.first
            canvas.drawRect(rect, blockFill)
            if (time != 0..0) {
                headText.color = pair.second
                val text: CharSequence = columnNameFormat?.getColumnDescription(time.first) ?: time.first.toString()
                val x = rect.left + headHorizontalPadding
                val y = rect.top + hColH - headVerticalPadding
                canvas.drawText(text, 0, text.length, x.toFloat(), y.toFloat(), headText)
            }
            canvas.drawRect(rect, blockStroke)
        }

        private fun drawBlock (canvas: Canvas, day: Day, time: IntRange, lu: LessonUnit) {
            rect.top = schT + hColH + workWeek.workDays.indexOf(day) * colH
            rect.left = schL + (time.first - range.first) * colW
            rect.right = rect.left + time.count() * colW
            rect.bottom = rect.top + colH
            val pair = color[lu.type] ?: Pair(when (lu.type) {
                FREE_COLOR -> Color.parseColor("#59000000")
                else -> Color.WHITE
            }, Color.BLACK)
            blockFill.color = pair.first
            canvas.drawRect(rect, blockFill)
            if (lu.abb.isNotEmpty()) {
                abbText.color = pair.second
                val x = rect.centerX().toFloat()
                val y = rect.top + colH * 14 / 33F
                canvas.drawText(lu.abb, 0, lu.abb.length, x, y, abbText)
            }
            if (lu.room?.isNotEmpty() == true) {
                roomText.color = pair.second
                val x = rect.centerX().toFloat()
                val y = rect.top + colH * 28 / 33F
                canvas.drawText(lu.room, 0, lu.room.length, x, y, roomText)
            }
            canvas.drawRect(rect, blockStroke)
        }

        /**
         * Nakreslí rozvrh na plátno [canvas]
         */
        fun drawSchedule(canvas: Canvas) {
            val jmp = when (val count = range.count()) {
                in 0..7 -> 1
                in 8..11 -> 2
                else -> if (count % 3 != 1) 3 else 2
            }
            val blank = LessonUnit(FREE_COLOR, "", "")

            for (r in range step jmp)
                drawHeaderBlock(canvas, r .. (r + jmp - 1).coerceAtMost(range.last))

            for (day in workWeek.workDays) {
                var st = range.first
                for (en in range) {
                    if (st != en && schedule[st]?.get(day) != schedule[en]?.get(day)) {
                        drawBlock(canvas, day, st until en, schedule[st]?.get(day) ?: blank)
                        st = en
                    }
                    if (en == range.last)
                        drawBlock(canvas, day, st..en, schedule[st]?.get(day) ?: blank)
                }
            }
        }

        /**
         * @param x súradnica [x]
         * @return vráti číslo bloku ktorý sa na súradnici nachádza.
         * Pokiaľ sa tam žiadny blok nenachádza, vráti null.
         */
        fun detectBlock(x: Int): Int = if (x !in schL .. schL + schW) -1 else range.first + (x - schL) / colW

        /**
         * @param y súradnica [y]
         * @return null, ak na súradnici [y], nie je riadok patriaci dňu v týždni, inak vráti deň
         */
        fun detectDay(y: Int): Day? = if (y !in schT + hColH .. schT + schH) null
        else workWeek.workDays[(y - schT - hColH) / colH]

        /**
         * Vráti true, ak je na súradniciach [[x], [y]] od ľavého horného rohu
         * v bode [[offsetX], [offsetY]] nakreslená tabuľka s rozvrhom
         * @param x súradnica x
         * @param y súradnica y
         * @param offsetX začiatok merania súradnice [x]
         * @param offsetY začiatok merania súradnice [y]
         * @return true, ak je na uvedených súradniciach
         */
        fun isSchedule(x: Float, y: Float, offsetX: Int = 0, offsetY: Int = 0): Boolean =
            x.roundToInt() in offsetX + schL .. offsetX + schL + schW
                    && y.roundToInt() in offsetY + schT .. offsetY + schT + schH
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    private data class LessonUnit(val type: Int, val abb: String, val room: String?)

    protected val schedule = ScheduleIllustrator(dp(1F))

    /**
     * Nastavienie spôsobu označovania časov v rozvrhu
     * @param nameFormat Objekt s funkciou, ktorá vráti reťazec popisujúci daný čas v rozvrhu
     */
    fun setFormat(nameFormat: ColumnNameFormat?) {
        schedule.setColumnFormat(nameFormat)
        invalidate()
    }

    /**
     * Uvolnenie času v rozvrhu
     * @param day deň
     * @param time časové bloky od..do
     */
    fun free(day: Day, time: IntRange) {
        val old = schedule.getRange()
        schedule.free(day, time)
        if (old.count() != schedule.getRange().count()) requestLayout() else invalidate()
    }

    /**
     * Zapísanie hodiny typu [type] k predmetu so skratkou [abb] v miestnosti [room]
     * na deň [day] do časový blokov [time].
     * @param day
     * @param time
     * @param type
     * @param abb
     * @param room
     */
    fun addLesson(day: Day, time: IntRange, type: Int, abb: String, room: String?) {
        val old = schedule.getRange()
        schedule.addLesson(day, time, type, abb, room)
        if (old.count() != schedule.getRange().count()) requestLayout() else invalidate()
    }

    /**
     * Vyprádzni celý rozvrh
     */
    fun clear() {
        val old = schedule.getRange()
        schedule.clear()
        if (old.count() != schedule.getRange().count()) requestLayout() else invalidate()
    }

    /**
     * Nastaví sfarbenie bloku [background] a farbu textu v ňom [text] pre typ hodiny [type].
     * @param type
     * @param background
     * @param text
     */
    fun setTypeColor(type: Int, @ColorInt background: Int, @ColorInt text: Int) {
        schedule.setTypeColor(type, background, text)
        invalidate()
    }

    /**
     * Nastaví pracovný týždeň, čo ovplyvní ako bude rozvrh vykreslený
     * @param ww pracovný týždeň
     */
    fun setWorkWeek(ww: WorkWeek) {
        schedule.setWorkWeek(ww)
        requestLayout()
    }

    /**
     * Zistí aký blok rozvrhu sa nachádza na súradniciach [x, y].
     * Ak na týchto súradniciach nie je blok, alebo je to hlavičkový blok,
     * tak vráti null, inak vráti dvojicu typov [Day] a [Int], pričom
     * číselná hodnota vypovedá o čísle bloku a hodnota typu [Day] vypovedá
     * o tom, ku ktorému dňu vybraný blok patrí.
     * @param x súradnica x
     * @param y súradnica y
     * @return Dvojica "deň a blok" alebo null
     */
    fun detectSchedulePart(x: Float, y: Float): Pair<Day, Int>? {
        val day = schedule.detectDay(y.roundToInt())
        val block = schedule.detectBlock(x.roundToInt())
        return if (block != -1 && day != null) Pair(day, block) else null
    }

    /**
     * Získa odložený údaj uložený pod dvojicou kľúčov [day] a [block].
     * @param day deň
     * @param block blok
     * @return údaj uložený pod dvojicou kľúčov [day] a [block]
     */
    fun getTag(day: Day, block: Int): Any? = schedule.getTag(day, block)

    /**
     * Zmaže všetky odložené údaje
     */
    fun clearTags() = schedule.clearTags()

    /**
     * Zapíše odložený údaj [tag] pod dvojicou kľúčov "deň [day] a blok" pre každý blok z rozsahu [time].
     * @param day deň
     * @param time rozsah blokov
     * @param tag údaj
     */
    fun setTag(day: Day, time: IntRange, tag: Any?) = schedule.setTag(day, time, tag)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        var w = availableWidth
        var h = availableHeight
        schedule.scaleScheduleByWidth(w)
        if (schedule.schH > h) schedule.scaleScheduleByWidth(w * h / schedule.schH)
        if (wMode == MeasureSpec.AT_MOST) w = schedule.schW
        if (hMode == MeasureSpec.AT_MOST) h = schedule.schH
        schedule.schL = (w - schedule.schW) / 2
        schedule.schT = (h - schedule.schH) / 2
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        schedule.drawSchedule(canvas!!)
    }
}
