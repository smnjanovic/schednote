package com.moriak.schednote.views

import android.content.Context
import android.graphics.Point
import android.os.Build.VERSION.SDK_INT
import android.util.AttributeSet
import android.util.DisplayMetrics.DENSITY_DEFAULT
import android.view.View
import android.view.WindowManager
import android.os.Build.VERSION_CODES.R as VC_R

/**
 * Trieda poskytuje často používané metódy vo vlastných vyrobených widgetoch [View].
 */
abstract class CustomView(
    ctx: Context?,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : View(ctx, attrs, defStyleAttr, defStyleRes) {
    private lateinit var metrics: Point
    private val displaySquare: Pair<Int, Int> by lazy {
        val wm = context.getSystemService(WindowManager::class.java)
        when {
            SDK_INT >= VC_R -> wm.currentWindowMetrics.bounds.let { it.width() to it.height() }
            else -> @Suppress("DEPRECATION") Point().also(wm.defaultDisplay::getRealSize)
                .let { it.x to it.y }
        }
    }

    protected val dimW by lazy { displaySquare.first }
    protected val dimH by lazy { displaySquare.second }
    protected val dimLonger  by lazy {  dimW.coerceAtLeast(dimH) }
    protected val dimShorter  by lazy { dimW.coerceAtMost(dimH) }

    /**
     * Prevedie veľkosť z dp na px.
     * @param dp
     */
    protected fun dp(dp: Float): Float = dp * resources.displayMetrics.densityDpi / DENSITY_DEFAULT

    /**
     * Prevedie veľkosť z dp na px.
     * @param dp
     */
    protected fun dp(dp: Int): Int = dp * resources.displayMetrics.densityDpi / DENSITY_DEFAULT
}