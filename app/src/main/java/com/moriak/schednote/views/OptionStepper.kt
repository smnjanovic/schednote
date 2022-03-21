package com.moriak.schednote.views

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.moriak.schednote.R
import com.moriak.schednote.views.OptionStepper.Format
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Okno, v ktorom užívateľ vyberá spomedzi niekoľkých volieb. Zvolená voľba sa mu zobrazí ako text.
 * Voľba môže byť objekt akéhokoľvek typu. Voľba je reprezentovaná textom, ktorý je výsledkom
 * funkcie [toString] alebo textom vo formáte nastavenom v objekte [Format].
 * Objekty na popredí možno meniť v rade za sebou jednym z dvoch smerov
 *
 * @property option vráti zvolený objekt. Ak je zoznam objektov prázdny, [option] je null.
 * @property index vráti pozíciu zvoleného objektu. Ak je zoznam objektov prázdny, [index] je -1.
 * @property text vráti text popisujúci zvolený objekt.
 */
class OptionStepper : CustomView {
    private object Touch: OnTouchListener {
        private class Runner(val v: OptionStepper, val l: Touch): Runnable {
            override fun run() {
                if (l.direction == 0) {
                    v.isPressed = false
                    v.performLongClick()
                    v.invalidate()
                }
            }
        }
        var direction: Int = 0
        private val handler = Handler(Looper.getMainLooper())
        private const val longClick = 500L
        private lateinit var longClickTimeOut: Runner

        override fun onTouch(v: View?, evt: MotionEvent?): Boolean {
            synchronized(v as OptionStepper) {
                if (!this::longClickTimeOut.isInitialized) longClickTimeOut = Runner(v, this)
                when (evt?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!v.isPressed) {
                            v.isPressed = true
                            // zistenie, ci uzivatel tukol na sipku, a ak ano, tak na ktoru
                            direction = v.checkStepDirection(evt.x, evt.y)
                            if (direction == 0) handler.postDelayed(longClickTimeOut, 2 * longClick)
                        }
                        v.invalidate()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // ked sa pouzivatel presunie do inej casti bloku, nestane sa nic
                        val oldDir = direction
                        direction = v.checkStepDirection(evt.x, evt.y)
                        if (oldDir != direction) {
                            v.invalidate()
                            v.isPressed = false
                            handler.removeCallbacks(longClickTimeOut)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (v.isPressed) {
                            handler.removeCallbacks(longClickTimeOut)
                            v.isPressed = false
                            v.index += direction
                            when {
                                direction == 0 -> when {
                                    evt.eventTime - evt.downTime > longClick -> v.performLongClick()
                                    v.hasOnClickListeners() -> v.performClick()
                                    v.options?.let { it.size > 2 } == true -> AlertDialog.Builder(v.context)
                                        .setItems(v.options!!.indices.map { v.getText(it) }
                                            .toTypedArray()) { _, which ->
                                            v.index = which
                                            v.onChange?.onChange(v, v.option)
                                        }
                                        .show()
                                }
                                direction < 0 && evt.x <= v.arrowWidth
                                        || direction > 0 && evt.x >= v.width - v.arrowWidth ->
                                    v.onChange?.onChange(v, if (v.index != -1) v.options!![v.index] else null)
                            }
                            direction = 0
                            v.invalidate()
                        }
                    }
                }
            }
            return true
        }
    }

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
            : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes) {
        if (attrs == null || context == null) {
            txtPaint.color = Color.BLACK
            arrowBgPaint.color = Color.TRANSPARENT
            arrowPaint.color = Color.BLACK
            txtPaint.textSize = dp(14F)
            arrowPaint.strokeWidth = dp(1F)
        } else {
            val arr = context.obtainStyledAttributes(attrs, R.styleable.OptionStepper)
            txtPaint.color = arr.getColor(R.styleable.OptionStepper_textColor, Color.BLACK)
            arrowBgPaint.color = arr.getColor(R.styleable.OptionStepper_arrowBackground, Color.TRANSPARENT)
            arrowPaint.color = arr.getColor(R.styleable.OptionStepper_arrowColor, Color.BLACK)
            txtPaint.textSize = arr.getDimension(R.styleable.OptionStepper_textSize, dp(14F))
            arrowPaint.strokeWidth = arr.getDimension(R.styleable.OptionStepper_arrowStroke, dp(1F))
            arr.recycle()
        }
    }

    /**
     * Objekty implementujúce tento interface sú schopné ovplivniť textové zobrazenie
     * zvoleného objektu
     */
    interface Format {
        /**
         * Objekt [item] bude reprezentovaný textom, ktorý vráti táto funkcia
         * @param item pôvodný objekt
         * @return popis objektu
         */
        fun getItemDescription(item: Any?): String
    }

    /**
     * Objekty implementujúce tento interface budú reagovať na zmeny vo voľbách vykonané používateľom
     */
    interface OnChange {
        /**
         * Funkcia, ktorá sa vykoná vždy, keď užívateľ zmení voľbu
         * @param v
         * @param item - zvolený objekt
         */
        fun onChange(v: View?, item: Any?)
    }

    private val txtPaint = Paint()
    private val arrowPaint = Paint()
    private val arrowBgPaint = Paint()
    private val path = Path()
    private val rect = Rect()

    private val arrowWidth: Int = dp(16)
    private val requiredTxtW: Float get() = txtPaint.measureText(text)
    private val requiredTxtH: Float get() = txtPaint.fontMetrics.let { it.descent - it.ascent }

    private var onChange: OnChange? = null
    private var format: Format? = null
    private var options: List<Any?>? = null
    private var isResizable: Boolean = false

    val option get() = if (index != -1) options!![index] else null
    var index: Int = -1; set(value) {
        val count = options?.count() ?: 0
        field = when {
            count in 0..1 -> options?.lastIndex ?: -1
            value < 0 -> value % count + count
            value >= count -> value % count
            else -> value
        }
        invalidate()
    }
    val text: String get() = getText(index)

    init {
        setOnTouchListener(Touch)
        txtPaint.style = Paint.Style.FILL
        arrowPaint.style = Paint.Style.STROKE
        arrowBgPaint.style = Paint.Style.FILL
        arrowPaint.strokeCap = Paint.Cap.ROUND
        arrowPaint.strokeJoin = Paint.Join.ROUND
        txtPaint.isAntiAlias = true
        arrowPaint.isAntiAlias = true
    }

    private fun checkStepDirection(x: Float, y: Float): Int = when (y.toInt()) {
        !in 0 .. height -> 0
        else -> when (x.toInt()) {
            in 0 .. arrowWidth * 2 -> -1
            in width - arrowWidth * 2 .. width -> 1
            else -> 0
        }
    }

    private fun getText(pos: Int): String {
        if (options?.let { pos in it.indices } != true) return ""
        return format?.getItemDescription(options!![pos]) ?: option?.toString() ?: ""
    }

    /**
     * Nastaví poslúchač na zmenu možnosti.
     * @param l poslúchač udalosti - zmena možnosti.
     */
    fun setOnChange(l: OnChange?) { onChange = l }

    /**
     * Nastaví poslúchač na zmenu možnosti, ktorý vykoná funkciu [fn].
     * @param fn funkcia, ktorá za vykoná po každej zmene možnosti.
     */
    fun setOnChange(fn: (Any?)->Unit) = setOnChange(object: OnChange {
        override fun onChange(v: View?, item: Any?) = fn(item)
    })

    /**
     * Nastavuje možností výberu
     * @param options zoznam možností na výber. Môže byť null.
     */
    fun setOptions(options: Array<out Any?>?) = setOptions(options?.toList())

    /**
     * Nastavuje možností výberu
     * @param options zoznam možností na výber. Môže byť null.
     */
    fun setOptions(options: List<Any?>?) {
        this.options = options
        index = 0
    }

    /**
     * Nastaví formát textu popisujúceho jednotlivé objekty v zozname
     * @param f objekt, ktorého funkcia sa zavolá pri výpise popisu zvolenej možnosti
     */
    fun setFormat(f: Format?) {
        format = f
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableW = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingRight
        val availableH = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
        val modeW = MeasureSpec.getMode(widthMeasureSpec)
        val modeH = MeasureSpec.getMode(heightMeasureSpec)

        var reqW = dp(60)
        options?.indices?.let { range ->
            for (r in range) {
                val required = ceil(txtPaint.measureText(getText(r))).toInt()
                if (required >= availableW) {
                    reqW = availableW
                    break
                }
                else if (required > reqW) reqW = required
            }
        }
        reqW += 2 * (arrowWidth + dp(1))
        val reqH = (requiredTxtH + txtPaint.textSize).roundToInt().coerceAtLeast(arrowWidth)

        var w = availableW
        var h = availableH

        isResizable = modeW == MeasureSpec.AT_MOST

        when (modeW) {
            MeasureSpec.EXACTLY -> if (availableW < dp(90)) w = dp(90)
            MeasureSpec.AT_MOST -> if (reqW < w) w = reqW
            MeasureSpec.UNSPECIFIED -> w = reqW
        }
        when(modeH) {
            MeasureSpec.EXACTLY -> Unit
            MeasureSpec.AT_MOST -> h = reqH.coerceAtMost(h)
            MeasureSpec.UNSPECIFIED -> h = reqH
        }
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        // sipky - pozadie
        rect.left = 0
        rect.top = 0
        rect.right = arrowWidth
        rect.bottom = height
        canvas.drawRect(rect, arrowBgPaint)
        rect.right = width
        rect.left = width - arrowWidth
        canvas.drawRect(rect, arrowBgPaint)

        val verticalPadding = (height - arrowWidth) / 2
        val arrowHeadX = dp(5F)
        val arrowButtX = dp(12F)
        val arrowHeadY = verticalPadding + dp(8F)
        val arrowButtY1 = verticalPadding + dp(2F)
        val arrowButtY2 = height - verticalPadding - dp(2F)
        path.reset()
        path.moveTo(arrowButtX, arrowButtY1)
        path.lineTo(arrowHeadX, arrowHeadY)
        path.lineTo(arrowButtX, arrowButtY2)
        path.moveTo(width - arrowButtX, arrowButtY1)
        path.lineTo(width - arrowHeadX, arrowHeadY)
        path.lineTo(width - arrowButtX, arrowButtY2)
        canvas.drawPath(path, arrowPaint)

        txtPaint.getTextBounds(text, 0, text.length, rect)
        val txtY = height - (height - rect.height()) / 2F

        // text - zmesti sa - na stred
        if (requiredTxtW < width - 2 * arrowWidth) {
            txtPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, width / 2F, txtY, txtPaint)
        }
        // text - nezmesti sa - zarovnat vlavo + odstrihnut
        else {
            val ellipsis = "…"
            val limit = (width - 2 * (arrowWidth + dp(1)) - txtPaint.measureText(ellipsis)
                .roundToInt()).coerceAtLeast(0)
            var t = text.substring(0, text.length * limit / requiredTxtW.roundToInt())
            while (t.length > 3 && txtPaint.measureText(t) > limit) t = t.substring(0, t.length - 3)
            while (txtPaint.measureText(t).roundToInt() > limit) t = t.substring(0, t.length - 1)
            t += ellipsis
            txtPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(t, arrowWidth + dp(1F), txtY, txtPaint)
        }
    }
}