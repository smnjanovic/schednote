package com.moriak.schednote.enums

import androidx.annotation.StringRes
import com.moriak.schednote.storage.Prefs.Settings.earliestMinute
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import com.moriak.schednote.R
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.enums.LessonTimeFormat.*

/**
 * Trieda určuje ako sa budú jednotlivé časové bloky prezentované.
 * [ORDER_FROM_0] Poradové číslo od 0.
 * [ORDER_FROM_1] Poradové číslo od 1.
 * [START_TIME] Uvedenie času začiatia časového bloku
 * @property res Odkaz na reťazec popisujúci spôsom označovania hodín rozvrhu
 */
enum class LessonTimeFormat(@StringRes val res: Int) {
    ORDER_FROM_0(R.string.start_lessons_from_0) {
        override fun startFormat(lessonOrder: Int): String = "${lessonOrder - 1}."
        override fun rangeFormat(range: IntRange): String = "${range.first - 1}.  — ${range.last - 1}."
    }, ORDER_FROM_1(R.string.start_lessons_from_1){
        override fun startFormat(lessonOrder: Int): String = "$lessonOrder."
        override fun rangeFormat(range: IntRange): String = "${range.first}.  — ${range.last}."
    }, START_TIME(R.string.show_start_time_of_lessons) {
        override fun startFormat(lessonOrder: Int) =
            timeFormat.getFormat(earliestMinute + SQLite.lessonStart(lessonOrder))
        override fun rangeFormat(range: IntRange): String {
            val min = SQLite.scheduleRangeToMinuteRange(range) ?: 0..0
            val st = timeFormat.getFormat(earliestMinute + min.first)
            val en = timeFormat.getFormat(earliestMinute + min.last)
            return "$st — $en"
        }
    };

    /**
     * Získanie popisu [lessonOrder]-tej hodiny podľa osobitného predpisu inštancie [LessonTimeFormat]
     * @return Textový popis začiatku [lessonOrder]-tej hodiny
     */
    abstract fun startFormat(lessonOrder: Int): String

    /**
     * Výpis časového úseku rozvrhu vo formáte predpísanom inštanciami [LessonTimeFormat].
     * @param range Obsahuje rozsah hodín.
     * @return Textový popis časového úseku rozvrhu
     */
    abstract fun rangeFormat(range: IntRange): String
}
