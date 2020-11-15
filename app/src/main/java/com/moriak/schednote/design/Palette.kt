package com.moriak.schednote.design

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import com.moriak.schednote.App
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Trieda slúži na dynamickú zmenu farieb a konverziu medzi farebnými modelmi
 * @property red Červená
 * @property green Zelená
 * @property blue Modrá
 * @property hue Odtieň
 * @property saturation Sýtosť
 * @property luminance Svetlosť
 * @property alpha Priehľadnosť
 * @property ahex Textové hexadecimálne vyjadrenie farby
 * @property contrastColor Kontrastná farba
 * @property contrastColorLowAlpha Kontrastná farba s nízkym kontrastom
 * @property contrastLuminance Kontrastná svetlosť
 */
class Palette {
    /**
     * Vínimka ktorá bude hodená, keď hodnoty prvkov farieb budú mimo povoleného rozsahu
     */
    class ValueOutOfRangeException(num: Int, range: IntRange) :
        IllegalArgumentException("$num is out of range: ${range.first} - ${range.last}")

    companion object {
        private const val defaultHex = "#A5A5A5"
        private fun Int.check(range: IntRange) {
            if (this !in range) throw ValueOutOfRangeException(this, range)
        }

        /**
         * Vytvorenie farby pomocou modelu rgb
         * @param r Červená 0 - 255
         * @param g Zelená 0 - 255
         * @param b Modrá 0 - 255
         * @return farba v rámci inštancie Palette
         */
        fun rgb(r: Int, g: Int, b: Int) = Palette().rgb(r, g, b)

        /**
         * Vytvorenie farby pomocou modelu rgb
         * @param a Priehľadná 0 - 100
         * @param r Červená 0 - 255
         * @param g Zelená 0 - 255
         * @param b Modrá 0 - 255
         * @return farba v rámci inštancie Palette
         */
        fun argb(a: Int, r: Int, g: Int, b: Int) = Palette().argb(a, r, g, b)

        /**
         * Vytvorenie farby pomocou modelu hsl
         * @param h Odtieň 0 - 359
         * @param s Sýtosť 0 - 100
         * @param l Svetlosť 0 - 100
         * @return farba v rámci inštancie Palette
         */
        fun hsl(h: Int, s: Int, l: Int) = Palette().hsl(h, s, l)

        /**
         * Vytvorenie farby pomocou modelu hsl
         * @param a Priehľadná 0 - 100
         * @param h Odtieň 0 - 359
         * @param s Sýtosť 0 - 100
         * @param l Svetlosť 0 - 100
         * @return farba v rámci inštancie Palette
         */
        fun ahsl(a: Int, h: Int, s: Int, l: Int) = Palette().ahsl(a, h, s, l)

        /**
         * Vytvorenie farby pomocou modelu rgb v hexadecimálnej sústave
         * @param hex Zápis farby v hexadecimálnej sústave
         * @return farba v rámci inštancie Palette
         */
        fun ahex(hex: String) = Palette().ahex(hex)

        /**
         * Vytvorenie farby pomocou modelu rgb v hexadecimálnej sústave
         * @param resId Zdroj farby
         * @return farba v rámci inštancie Palette
         */
        fun resource(@ColorRes resId: Int) = Palette().resourceColor(resId)
    }

    private var r: Int = 0
    private var g: Int = 0
    private var b: Int = 0
    private var h: Int = 0
    private var s: Int = 0
    private var l: Int = 0
    private var a: Int = 0
    val contrastLuminance get() = if (l < if (s > 35 && h in 45..200) 35 else 50) 85 else 15

    val red get() = r
    val green get() = g
    val blue get() = b
    val hue get() = h
    val saturation get() = s
    val luminance get() = l
    val alpha get() = a

    val ahex get() = rgb2hex(a, r, g, b)
    val color @ColorInt get() = Color.argb(a * 255 / 100, r, g, b)
    val contrastColor: Int
        get() {
            hsl2rgb(h, s, contrastLuminance)
            return Color.argb(255, RGB.r, RGB.g, RGB.b)
        }
    val contrastColorLowAlpha: Int
        get() {
            hsl2rgb(h, s, contrastLuminance)
            return Color.argb(100, RGB.r, RGB.g, RGB.b)
        }

    private object RGB {
        var r: Int = 0
        var g: Int = 0
        var b: Int = 0
    }

    private object HSL {
        var h: Int = 0
        var s: Int = 0
        var l: Int = 0
    }

    /**
     * Nastavenie odtieňa červenej
     * @param value Červená 0 - 255
     * @return farba v rámci inštancie Palette
     */
    fun red(value: Int) = also {
        rgb2hsl(value, g, b)
        r = value
        h = HSL.h
        s = HSL.s
        l = HSL.l
    }

    /**
     * Nastavenie odtieňa zelenej
     * @param value Zelená 0 - 255
     * @return farba v rámci inštancie Palette
     */
    fun green(value: Int) = also {
        rgb2hsl(r, value, b)
        g = value
        h = HSL.h
        s = HSL.s
        l = HSL.l
    }

    /**
     * Nastavenie odtieňa modrej
     * @param value Modrá 0 - 255
     * @return farba v rámci inštancie Palette
     */
    fun blue(value: Int) = also {
        rgb2hsl(r, g, value)
        b = value
        h = HSL.h
        s = HSL.s
        l = HSL.l
    }

    /**
     * Nastavenie odtieňa
     * @param value Odtieň 0 - 359
     * @return farba v rámci inštancie Palette
     */
    fun hue(value: Int) = also {
        hsl2rgb(value, s, l)
        h = value
        r = RGB.r
        g = RGB.g
        b = RGB.b
    }

    /**
     * Nastavenie sýtosti
     * @param value Sýtosť 0 - 100
     * @return farba v rámci inštancie Palette
     */
    fun saturation(value: Int) = also {
        hsl2rgb(h, value, l)
        s = value
        r = RGB.r
        g = RGB.g
        b = RGB.b
    }

    /**
     * Nastavenie svetlosti
     * @param value Svetlosť 0 - 100
     * @return farba v rámci inštancie Palette
     */
    fun luminance(value: Int) = also {
        hsl2rgb(h, s, value)
        l = value
        r = RGB.r
        g = RGB.g
        b = RGB.b
    }

    /**
     * Nastavenie priehľadnosti
     * @param value Priehľadnosť 0 - 100
     * @return farba v rámci inštancie Palette
     */
    fun alpha(value: Int) = also {
        a.check(0..100)
        a = value
    }

    private fun duplicateChars(str: String) = str.split("")
        .joinToString("", transform = fun(str) = str + str)

    private fun fixHex(hex: String): String = when {
        !hex.matches("^[#][0-9a-fA-F]+$".toRegex()) -> defaultHex
        hex.length == 4 -> "#FF" + duplicateChars(hex.substring(1))
        hex.length == 5 -> "#" + duplicateChars(hex.substring(1))
        hex.length == 7 -> "#FF" + hex.substring(1)
        hex.length == 9 -> hex
        else -> defaultHex
    }

    /**
     * Vytvorenie farby pomocou modelu rgb
     * @param red Červená 0 - 255
     * @param green Zelená 0 - 255
     * @param blue Modrá 0 - 255
     * @return farba v rámci inštancie Palette
     */
    fun rgb(red: Int, green: Int, blue: Int) = argb(100, red, green, blue)

    /**
     * Vytvorenie farby pomocou modelu rgb
     * @param alpha Priehľadná 0 - 100
     * @param red Červená 0 - 255
     * @param green Zelená 0 - 255
     * @param blue Modrá 0 - 255
     * @return farba v rámci inštancie Palette
     */
    fun argb(alpha: Int, red: Int, green: Int, blue: Int) = also {
        alpha.check(0..100)
        rgb2hsl(red, green, blue)
        r = red
        g = green
        b = blue
        a = alpha
        h = HSL.h
        s = HSL.s
        l = HSL.l
    }

    /**
     * Vytvorenie farby pomocou modelu rgb v hexadecimálnej sústave
     * @param hex Zápis farby v hexadecimálnej sústave
     * @return farba v rámci inštancie Palette
     */
    fun ahex(hex: String) = also {
        var dec = fixHex(hex).substring(1).toLong(16)
        val pb = (dec % 256).toInt(); dec /= 256
        val pg = (dec % 256).toInt(); dec /= 256
        val pr = (dec % 256).toInt(); dec /= 256
        val pa = dec.toInt() * 100 / 255
        argb(pa, pr, pg, pb)
    }

    /**
     * Vytvorenie farby pomocou modelu hsl
     * @param hue Odtieň 0 - 359
     * @param saturation Sýtosť 0 - 100
     * @param luminance Svetlosť 0 - 100
     * @return farba v rámci inštancie Palette
     */
    fun hsl(hue: Int, saturation: Int, luminance: Int) = ahsl(100, hue, saturation, luminance)

    /**
     * Vytvorenie farby pomocou modelu hsl
     * @param alpha Priehľadná 0 - 100
     * @param hue Odtieň 0 - 359
     * @param saturation Sýtosť 0 - 100
     * @param luminance Svetlosť 0 - 100
     * @return farba v rámci inštancie Palette
     */
    fun ahsl(alpha: Int, hue: Int, saturation: Int, luminance: Int) = also {
        alpha.check(0..100)
        hsl2rgb(hue, saturation, luminance)
        h = hue
        s = saturation
        l = luminance
        a = alpha
        r = RGB.r
        g = RGB.g
        b = RGB.b
    }

    /**
     * Nastavenie farby
     * @param resId Zdroj farby
     * @return farba v rámci inštancie Palette
     */
    fun resourceColor(@ColorRes resId: Int) = also {
        val c = App.res.getColor(resId, null)
        argb(Color.alpha(c) * 100 / 255, Color.red(c), Color.green(c), Color.blue(c))
    }

    /**
     * Nastavenie farby
     * @param col inštancia ktorej hodnoty sa len skopírujú [Color]
     * @return farba v rámci inštancie Palette
     */
    fun set(col: Palette) = also {
        r = col.r
        g = col.g
        b = col.b
        a = col.a
        h = col.h
        s = col.s
        l = col.l
    }

    /**
     * Nastavenie farby
     * @param col číselná hodnota farby podľa [Color]
     * @return farba v rámci inštancie Palette
     */
    fun set(col: Int) = argb(
        Color.alpha(col) * 100 / 255,
        Color.red(color),
        Color.green(color),
        Color.blue(color)
    )

    private fun checkHSL(h: Int, s: Int, l: Int) {
        h.check(0..359)
        s.check(0..100)
        l.check(0..100)
    }

    private fun checkRGB(r: Int, g: Int, b: Int) {
        r.check(0..255)
        g.check(0..255)
        b.check(0..255)
    }

    private fun hsl2rgb(h: Int, s: Int, l: Int) {
        checkHSL(h, s, l)
        if (s == 0) {
            RGB.r = (l * 2.55).roundToInt()
            RGB.g = RGB.r
            RGB.b = RGB.r
        } else {
            val c: Double = (1 - abs(0.02 * l - 1)) * s * 0.01
            val x: Double = c * (1 - abs((h / 60.0) % 2 - 1))
            val m: Double = 0.01 * l - c / 2

            RGB.r =
                ((m + (if (h in 120..239) 0.0 else if (h < 60 || h >= 300) c else x)) * 255).roundToInt()
                    .coerceIn(0..255)
            RGB.g = ((m + (if (h >= 240) 0.0 else if (h in 60..179) c else x)) * 255).roundToInt()
                .coerceIn(0..255)
            RGB.b = ((m + (if (h < 120) 0.0 else if (h in 180..299) c else x)) * 255).roundToInt()
                .coerceIn(0..255)
        }
    }

    private fun rgb2hsl(r: Int, g: Int, b: Int) {
        checkRGB(r, g, b)
        fun cmp(n1: Double, n2: Double): Boolean {
            return abs(n1 - n2) < 0.000001
        }

        val tmpR = r / 255.0
        val tmpG = g / 255.0
        val tmpB = b / 255.0

        val cMax = tmpR.coerceAtLeast(tmpG).coerceAtLeast(tmpB)
        val cMin = tmpR.coerceAtMost(tmpG).coerceAtMost(tmpB)
        val delta = cMax - cMin

        val tmpL = (cMax + cMin) / 2
        val tmpS = if (!cmp(cMax, cMin)) delta / (1 - abs(2 * tmpL - 1)) else 0.0
        val tmpH = (60 * (when {
            cmp(cMax, cMin) -> 0.0
            cmp(cMax, tmpR) -> ((tmpG - tmpB) / delta) % 6
            cmp(cMax, tmpG) -> ((tmpB - tmpR) / delta) + 2
            else -> ((tmpR - tmpG) / delta) + 4
        })).roundToInt()

        HSL.h = if (tmpH < 0) tmpH + 360 else tmpH
        HSL.s = (tmpS * 100).roundToInt()
        HSL.l = (tmpL * 100).roundToInt()
    }

    private fun rgb2hex(a: Int, r: Int, g: Int, b: Int): String {
        a.check(0..100)
        checkRGB(r, g, b)
        return String.format(
            "#%2s%2s%2s%2s", (a * 2.55).roundToInt().toString(16),
            r.toString(16), g.toString(16), b.toString(16)
        )
            .replace(" ", "0")
    }

    override fun toString(): String =
        "Palette: ahex $ahex, r $r, g $g, b $b, a $a, h $h, s $s, l $l"
}