package com.moriak.schednote

import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.Size
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Trieda slúži na dynamickú zmenu farieb a konverziu medzi farebnými modelmi
 * @property hue Odtieň
 * @property saturation Sýtosť
 * @property luminance Svetlosť
 * @property alpha Priehľadnosť
 * @property contrast Kontrastná farba
 */
class Palette {
    /**
     * @constructor Vytvorenie pôvodnej (bielej) farby
     */
    constructor(): this(-1)

    /**
     * @constructor Vytvorenie farby pomocou čísla
     * @param color farba reprezentovaná číslom
     */
    constructor(@ColorInt color: Int) {
        aColor = color
        hsl = rgb2hsl(intArrayOf(color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF))
        tmp = hsl.clone()
        tmp[2] = getContrastLuminance(tmp[2])
        aContrast = 0xFF00 or hsl2rgb(tmp)[0] shl 8 or tmp[1] shl 8 or tmp[2]
    }

    /**
     * @constructor Vytvorenie farby pomocou modelu hsl
     * @param hue Odtieň 0 - 359
     * @param saturation Sýtosť 0 - 100
     * @param luminance Svetlosť 0 - 100
     * @param alpha Priehľadná 0 - 100
     */
    constructor(
        @IntRange(from = 0, to = 359) hue: Int,
        @IntRange(from = 0, to = 100) saturation: Int,
        @IntRange(from = 0, to = 100) luminance: Int,
        @IntRange(from = 0, to = 100) alpha: Int

    ) {
        hsl = intArrayOf(hue, saturation, luminance)
        tmp = hsl.clone()
        aColor = (alpha * 2.55).roundToInt() shl 8 or hsl2rgb(tmp)[0] shl 8 or tmp[1] shl 8 or tmp[2]
        for (i in 0..1) tmp[i] = hsl[i]
        tmp[2] = getContrastLuminance(hsl[2])
        aContrast = 0xFF00 or hsl2rgb(tmp)[0] shl 8 or tmp[1] shl 8 or tmp[2]
    }

    private val tmp: IntArray
    private val hsl: IntArray
    @ColorInt private var aColor: Int = 0
    @ColorInt private var aContrast: Int = 0

    val hue: Int get() = hsl[0]
    val saturation: Int get() = hsl[1]
    val luminance: Int get() = hsl[2]
    val alpha: Int get() = ((aColor ushr 24) / 2.55F).roundToInt()
    val color: Int @ColorInt get() = aColor
    val contrast: Int @ColorInt get() = aContrast

    /**
     * Zmena dúhového odtieňa
     * @param h Odtieň 0 - 359
     * @return Vráti ten istý objekt, v ktorom je táto metóda volaná
     */
    fun setHue(h: Int) = setColor(h, hsl[1], hsl[2], alpha)

    /**
     * Zmena sýtosti
     * @param s Sýtosť 0 - 100
     * @return Vráti ten istý objekt, v ktorom je táto metóda volaná
     */
    fun setSaturation(@IntRange(from = 0, to = 100) s: Int) = setColor(hsl[0], s, hsl[2], alpha)

    /**
     * Zmena svetlosti
     * @param l Svetlosť 0 - 100
     * @return Vráti ten istý objekt, v ktorom je táto metóda volaná
     */
    fun setLuminance(@IntRange(from = 0, to = 100) l: Int) = setColor(hsl[0], hsl[1], l, alpha)

    /**
     * Zmena priesvitnosti.
     * @param a Priehľadnosť 0 - 100
     * @return Vráti ten istý objekt, v ktorom je táto metóda volaná
     */
    fun setAlpha(@IntRange(from = 0, to = 100) a: Int) =
        setColor((a * 2.55).roundToInt() shl 24 or (0xFFFFFF and color))

    /**
     * zmena farby pomocou údajov farebného modelu HSL.
     * @param hue Odtieň 0 - 359
     * @param saturation Sýtosť 0 - 100
     * @param luminance Svetlosť 0 - 100
     * @param alpha Priehľadná 0 - 100
     * @return Vráti ten istý objekt, v ktorom je táto metóda volaná
     */
    fun setColor(
        @IntRange(from = 0, to = 359) hue: Int,
        @IntRange(from = 0, to = 100) saturation: Int,
        @IntRange(from = 0, to = 100) luminance: Int,
        @IntRange(from = 0, to = 100) alpha: Int
    ) = also {
        hsl[0] = hue
        hsl[1] = saturation
        hsl[2] = luminance
        for (i in 0..2) tmp[i] = hsl[i]
        aColor = (alpha * 2.55).roundToInt() shl 8 or hsl2rgb(tmp)[0] shl 8 or tmp[1] shl 8 or tmp[2]
        for (i in 0..1) tmp[i] = hsl[i]
        tmp[2] = getContrastLuminance(luminance)
        aContrast = 0xFF00 or hsl2rgb(tmp)[0] shl 8 or tmp[1] shl 8 or tmp[2]
    }

    /**
     * Zmena farby pomocou farebného kódu [col].
     * @param col farebný kód
     * @return Vráti ten istý objekt, v ktorom je táto metóda volaná
     */
    fun setColor(@ColorInt col: Int) = also {
        aColor = col
        hsl[0] = aColor shr 16 and 0xFF
        hsl[1] = aColor shr 8 and 0xFF
        hsl[2] = aColor and 0xFF
        tmp[0] = rgb2hsl(hsl)[0]
        tmp[1] = hsl[1]
        tmp[2] = getContrastLuminance(hsl[2])
        aContrast = 0xFF00 or hsl2rgb(tmp)[0] shl 8 or tmp[1] shl 8 or tmp[2]
    }

    private fun getContrastLuminance(lum: Int): Int = if (lum < (if (hsl[1] > 35
            && hsl[0] in 45..200) 35 else 50)) 85 else 15

    private fun rgb2hsl(@Size(3) rgb: IntArray): IntArray {
        fun cmp(n1: Float, n2: Float): Boolean = abs(n1 - n2) < 0.001F

        val tmpR = rgb[0] / 255F
        val tmpG = rgb[1] / 255F
        val tmpB = rgb[2] / 255F

        val cMax = tmpR.coerceAtLeast(tmpG).coerceAtLeast(tmpB)
        val cMin = tmpR.coerceAtMost(tmpG).coerceAtMost(tmpB)
        val delta = cMax - cMin

        val tmpL = (cMax + cMin) / 2
        val tmpS = if (!cmp(cMax, cMin)) delta / (1 - abs(2 * tmpL - 1)) else 0F
        val tmpH = 60 * when {
            cmp(cMax, cMin) -> 0F
            cmp(cMax, tmpR) -> ((tmpG - tmpB) / delta) % 6
            cmp(cMax, tmpG) -> ((tmpB - tmpR) / delta) + 2
            else -> ((tmpR - tmpG) / delta) + 4
        }

        rgb[0] = (if (tmpH < 0) tmpH + 360 else tmpH).roundToInt()
        rgb[1] = (tmpS * 100).roundToInt()
        rgb[2] = (tmpL * 100).roundToInt()
        return rgb
    }

    private fun hsl2rgb(@Size(3) hsl: IntArray): IntArray {
        if (hsl[1] == 0) {
            hsl[0] = (hsl[2] * 2.55).roundToInt()
            hsl[1] = hsl[0]
            hsl[2] = hsl[0]
        } else {
            val c: Float = (1 - abs(0.02F * hsl[2] - 1)) * hsl[1] * 0.01F
            val x: Float = c * (1 - abs((hsl[0] / 60F) % 2 - 1))
            val m: Float = 0.01F * hsl[2] - c / 2

            val tmpR = m + if (hsl[0] in 120..239) 0F else if (hsl[0] !in 60..299) c else x
            val tmpG = m + if (hsl[0] >= 240) 0F else if (hsl[0] in 60..179) c else x
            val tmpB = m + if (hsl[0] < 120) 0F else if (hsl[0] in 180..299) c else x

            hsl[0] = (tmpR * 255).roundToInt().coerceIn(0..255)
            hsl[1] = (tmpG * 255).roundToInt().coerceIn(0..255)
            hsl[2] = (tmpB * 255).roundToInt().coerceIn(0..255)
        }
        return hsl
    }
}