package com.moriak.schednote.views

import android.content.Context
import android.graphics.*
import android.graphics.Shader.TileMode.CLAMP
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.moriak.schednote.R
import com.moriak.schednote.views.RangeView.RangeViewTouchListener.Moving.*
import java.util.*
import kotlin.math.roundToInt

/**
 * Tento blok zobrazuje posuvník, na ktorom možno nastaviť celočíselný rozsah.
 * @property extreme dolná a horná hranica rozsahu
 * @property value hodnota rozsahu
 */
class RangeView: CustomView {
    private companion object {
        private const val DESIRED_W = 180
        private const val CIRCLE_R = 6F
        private const val LINE_H = 8F
    }

    private class RangeViewTouchListener: OnTouchListener {
        private enum class Moving { START, END, BOTH, NONE }
        private var moving: Moving = NONE
        private var isClick = false
        private var oldPos: Int = 0
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            v as RangeView
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isTouched = true
                    isClick = true
                    oldPos = v.illustrator.detectPosition(event.x, v.extreme).coerceIn(v.extreme)
                    moving = when {
                        oldPos <= v.start -> START
                        oldPos >= v.end -> END
                        oldPos in v.value -> BOTH
                        else -> NONE
                    }
                    if (moving == START && oldPos < v.start) {
                        v.start = oldPos
                        v.invalidate()
                    }
                    else if (moving == END && oldPos > v.end) {
                        v.end = oldPos
                        v.invalidate()
                    }
                    v.rangeChangeListener?.onChange(v.value)
                }

                MotionEvent.ACTION_MOVE -> {
                    isClick = false
                    val pos = v.illustrator.detectPosition(event.x, v.extreme).coerceIn(v.extreme)
                    val dif = pos - oldPos
                    if (dif == 0) return true
                    oldPos = pos
                    if (moving == BOTH || moving == START) v.start = (v.start + dif).coerceIn(v.extreme)
                    if (moving == BOTH || moving == END) v.end = (v.end + dif).coerceIn(v.extreme)
                    if (v.end < v.start) {
                        val p = v.end
                        v.end = v.start
                        v.start = p
                        moving = if (moving == END) START else END
                    }
                    val viewRect = Rect(null)
                    v.getHitRect(viewRect)
                    v.isTouched = viewRect.contains((v.x + event.x).roundToInt(), (v.y + event.y).roundToInt())
                    v.rangeChangeListener?.let {
                        (if (v.isTouched) it::onChange else it::onSet)(v.value)
                    }
                    v.invalidate()
                }

                MotionEvent.ACTION_UP -> {
                    moving = NONE
                    if (isClick) v.performClick()
                    else v.rangeChangeListener?.onSet(v.value)
                    isClick = false
                    v.isTouched = false
                }
            }
            return true
        }
    }

    private inner class RangeViewIllustrator(@ColorInt color: Int) {
        private var lineL = 0F
        private var lineT = 0F
        private var lineW = 0F
        private var lineH = 0F

        private val thumbBg = Paint()
        private val lineBg = Paint()
        private val thumbStroke = Paint()
        private val areaFill = Paint()
        private val activeAreaFill = Paint()

        private val thumbGradients = TreeMap<Int, RadialGradient>()
        private var lineGradient: LinearGradient? = null

        private val thumbColor1: Int = color
        private val thumbColor2: Int
        private val lineColor1: Int
        private val lineColor2: Int

        init {
            val hsv = FloatArray(3) { 0F }
            Color.colorToHSV(thumbColor1, hsv)
            thumbBg.isDither = true

            hsv[1] = (hsv[1] - 0.30F).coerceAtLeast(0F)
            thumbColor2 = Color.HSVToColor(hsv)

            hsv[1] = 0.15F.coerceAtMost(hsv[1])
            hsv[2] = 0.85F.coerceAtMost(hsv[2])
            lineColor1 = Color.HSVToColor(hsv)

            hsv[1] = (hsv[1] - 0.10F).coerceAtLeast(0F)
            hsv[2] = (hsv[2] + 0.05F).coerceAtMost(1F)
            lineColor2 = Color.HSVToColor(hsv)

            Color.colorToHSV(color, hsv)
            hsv[2] = (hsv[2] - 0.50F).coerceAtLeast(0F)
            thumbStroke.color = Color.HSVToColor(hsv)
            thumbStroke.style = Paint.Style.STROKE
            thumbStroke.strokeWidth = dp(1F)

            areaFill.style = Paint.Style.FILL
            areaFill.color = Color.argb(114, Color.red(color), Color.green(color), Color.blue(color))
            activeAreaFill.style = Paint.Style.FILL
            activeAreaFill.color = Color.argb(191, Color.red(color), Color.green(color), Color.blue(color))
        }

        fun detectPosition(x: Float, range: IntRange) = ((x - lineL) / lineW * range.count()).toInt() + range.first

        private fun getX(pos: Int, range: IntRange) = lineL + lineW * (pos - range.first + 0.5F) / range.count()

        fun circleGradient(v: RangeView, pos: Int, canvas: Canvas?) {
            val x = getX(pos, v.extreme)
            val y = v.height / 2F
            thumbBg.reset()
            thumbBg.shader = thumbGradients[pos] ?: RadialGradient(x, y, lineL, thumbColor2,
                thumbColor1, CLAMP).also { gd -> thumbGradients[pos] = gd }
            canvas!!.drawCircle(x, y, lineL, thumbBg)
        }

        fun lineGradient(canvas: Canvas?) {
            lineBg.reset()
            lineBg.shader = lineGradient ?: LinearGradient(0F, lineT, 0F, lineT + lineH,
                intArrayOf(lineColor1, lineColor2, lineColor1), null, CLAMP)
                .also { lineGradient = it }
            canvas!!.drawRoundRect(lineL, lineT, lineL + lineW, lineT + lineH, lineH, lineH, lineBg)
        }

        fun lineArea(canvas: Canvas?, value: IntRange, bounds: IntRange, isTouched: Boolean) {
            val p = if (isTouched) activeAreaFill else areaFill
            val left = getX(value.first, bounds)
            val right = getX(value.last, bounds)
            canvas!!.drawRoundRect(left, lineT, right, lineT + lineH, lineH, lineH, p)
        }

        fun computeSize(widthMeasureSpec: Int, heightMeasureSpec: Int): Pair<Int, Int> {
            val sizeW = MeasureSpec.getSize(widthMeasureSpec)
            val sizeH = MeasureSpec.getSize(heightMeasureSpec)
            val modeW = MeasureSpec.getMode(widthMeasureSpec)
            val modeH = MeasureSpec.getMode(heightMeasureSpec)
            val w = when (modeW) {
                MeasureSpec.AT_MOST -> sizeW.coerceAtMost(DESIRED_W)
                MeasureSpec.EXACTLY -> sizeW
                else -> DESIRED_W
            }
            thumbGradients.clear()
            lineL = CIRCLE_R / DESIRED_W * w
            lineW = w - 2 * lineL
            lineH = LINE_H / DESIRED_W * w
            val h = ((lineL + 1) * 2).toInt().let {
                when (modeH) {
                    MeasureSpec.AT_MOST -> sizeH.coerceAtMost(it)
                    MeasureSpec.EXACTLY -> sizeH
                    else -> it
                }
            }
            lineT = (h - lineH) / 2
            lineGradient = null
            return Pair(w, h)
        }
    }

    /**
     * Poslúchač špeciálnych udalostí bloku [RangeView]
     */
    interface RangeChangeListener {
        /**
         * Funkcia sa vykoná po každej zmene rozsahu
         * @param range nový rozsah od do
         */
        fun onChange(range: IntRange)

        /**
         * Funkcia sa vykoná po poslednej zmene rozsahu
         * @param range nový rozsah od do
         */
        fun onSet(range: IntRange)
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?)
            : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int)
            : this(context, attributeSet, defStyleAttr, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attributeSet, defStyleAttr, defStyleRes)

    private var minimum: Int = 0
    private var maximum: Int = 0
    private var start: Int = 0
    private var end: Int = 0

    private var isTouched: Boolean = false; set(value) {
        field = value
        invalidate()
    }
    var extreme: IntRange get() = minimum .. maximum; set(value) {
        if (value.count() == 0)
            throw IllegalArgumentException("Range 'extreme' is invalid!")
        minimum = value.first
        maximum = value.last
        if (start !in extreme && end !in extreme) {
            start = minimum + maximum / 2
            end = start
        }
        else if (start !in extreme) start = end
        else if (end !in extreme) end = start
        rangeChangeListener?.onSet(start..end)
        invalidate()
    }
    var value: IntRange get() = start .. end; set(pVal) {
        if (pVal.count() == 0)
            throw IllegalArgumentException("Range 'value' is invalid!")
        start = pVal.first.coerceAtLeast(minimum)
        end = pVal.last.coerceAtMost(maximum)
        rangeChangeListener?.onSet(start..end)
        invalidate()
    }
    private var rangeChangeListener: RangeChangeListener? = null
    private val illustrator = RangeViewIllustrator(resources.getColor(R.color.colorAccent, null))
    private val touchListener = RangeViewTouchListener()

    init { setOnTouchListener(touchListener) }

    /**
     * Nastavenie poslúchača špeciálnych udalostí bloku [RangeView],
     * týkajúcich sa zmien hodnoty alebo hraníc rozsahu
     * @param l poslúchač udalostí
     */
    fun setRangeChangeListener(l: RangeChangeListener) { rangeChangeListener = l }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = illustrator.computeSize(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(size.first, size.second)
    }

    override fun onDraw(canvas: Canvas?) {
        illustrator.lineGradient(canvas)
        illustrator.lineArea(canvas, value, extreme, isTouched)
        illustrator.circleGradient(this, start, canvas)
        illustrator.circleGradient(this, end, canvas)
    }
}