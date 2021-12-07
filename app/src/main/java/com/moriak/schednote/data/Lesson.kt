package com.moriak.schednote.data

import com.moriak.schednote.storage.Prefs.Settings.lessonTimeFormat
import com.moriak.schednote.enums.Day
import com.moriak.schednote.enums.Regularity


/**
 * V [reg] týždni, dňa [day] sa v čase [time] vyučuje
 * predmet [sub] v miestnosti [room] vo forme [type].
 *
 * @property reg párny / nepárny / každý týždeň
 * @property day Typ hodiny
 * @property time Typ hodiny
 * @property type Typ hodiny
 * @property sub Predmet
 * @property room Miestnosť
 */
class Lesson(val reg: Regularity, val day: Day, val time: IntRange, val type: Int, val sub: Subject, val room: String?) {
    constructor(l: Lesson, time: IntRange): this(l.reg, l.day, time, l.type, l.sub, l.room)
    override fun toString() = "${sub.abb} — $day $reg ${lessonTimeFormat.rangeFormat(time)}"
}