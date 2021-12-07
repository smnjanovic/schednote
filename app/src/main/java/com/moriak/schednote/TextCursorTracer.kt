package com.moriak.schednote

import android.text.*
import android.text.Selection.SELECTION_END
import android.text.Selection.SELECTION_START
import com.moriak.schednote.TextCursorTracer.TextAction.*

/**
 * Objekt pozoruje zmeny užívateľom upraviteľného textového poľa vrátane zmien kurzora
 * @constructor pri vzniku sa ako parameter zadá formát, ktorý musí byť dodržaný.
 * Zadaný formát môže byť aj null, čo znamená, že text môže obsahovať ľubovoľné znaky
 * v ľubovoľnom počte.
 *
 * @property format ak nie je null, formát zadávaneho textu musí platiť
 */
abstract class TextCursorTracer(protected val format: Regex? = null): TextWatcher {
    protected enum class TextAction { REMOVED, ADDED, REPLACED }
    private val cursorWatcher = object : SpanWatcher {
        override fun onSpanAdded(text: Spannable?, what: Any?, start: Int, end: Int) {}
        override fun onSpanRemoved(text: Spannable?, what: Any?, start: Int, end: Int) {}
        override fun onSpanChanged(s: Spannable?, o: Any?, os: Int, oe: Int, ns: Int, ne: Int) {
            when (o) {
                SELECTION_START -> {
                    val en = Selection.getSelectionEnd(s)
                    if (en >= ns) onCursorChanged(ns..en)
                }
                SELECTION_END -> {
                    val st = Selection.getSelectionStart(s)
                    if (st <= ne) onCursorChanged(st..ne)
                }
            }
        }
    }
    private var aStart: Int = 0
    private var aEnd: Int = 0
    private var aAction: TextAction = REPLACED
    protected val st: Int get() = aStart
    protected val en: Int get() = aEnd
    protected val action: TextAction get() = aAction

    /**
     * Aplikuje [SpanWatcher] na celý text.
     * @param s editovateľný text
     */
    fun setSpan(s: Editable?) {
        s?.setSpan(cursorWatcher, 0, s.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }

    final override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        aStart = start
        aEnd = start + after
        aAction = when {
            count > 0 && after > 0 -> REPLACED
            count > 0 -> REMOVED
            after > 0 -> ADDED
            else -> REPLACED
        }
    }

    final override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    final override fun afterTextChanged(s: Editable?) {
        if (s != null) {
            if (format == null || s.matches(format)) afterValidTextChange(s)
            else afterInvalidTextChange(s)
            setSpan(s)
        }
    }

    /**
     * Táto funkcia sa vykoná vždy, keď sa v texte presunie kurzor
     * @param range rozsah kurzora
     */
    abstract fun onCursorChanged(range: IntRange)

    /**
     * Táto funkcia sa zavolá, ak dôjde k povolenej zmene
     * @param s
     */
    protected abstract fun afterValidTextChange(s: Editable)

    /**
     * Táto funkcia sa zavolá, ak dôjde k nepovolenej zmene
     * @param s
     */
    protected abstract fun afterInvalidTextChange(s: Editable)
}