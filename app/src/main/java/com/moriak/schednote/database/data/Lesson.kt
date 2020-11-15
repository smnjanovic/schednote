package com.moriak.schednote.database.data

import android.text.Editable
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity


/**
 * Hodina - udalosť rozvrhu [ScheduleEvent], pri ktorej sa zíde skupina ľudí v určitej miestnosti [room],
 * jej priebeh má nejakú formu [type] a je tematicky zameraná na jeden predmet [sub].
 *
 * @property id
 * @property type Typ hodiny
 * @property sub Predmet
 * @property room Miestnosť
 */
data class Lesson(
    val id: Long,
    override val regularity: Regularity,
    override val day: Day,
    override val time: IntRange,
    val type: Int,
    val sub: Subject,
    val room: String?
) : ScheduleEvent {
    init {
        ScheduleEvent.rangeCheck(time)
    }

    /**
     * @property room_limit maximálny počet znakov v popise miestnosti
     */
    companion object {
        const val room_limit = 20

        /**
         * Kontrola správnosti formátu popisu miestnosti - dodržanie maximálnej dĺžky
         * @param editable Kontrolovaný text
         */
        fun roomValid(editable: Editable?) = editable
            ?.trimStart()
            ?.replace("\\s+".toRegex(), " ")
            ?.let { if (it.length > room_limit) it.substring(0, room_limit) else it }
    }

    override fun isEqual(other: ScheduleEvent?): Boolean =
        other is Lesson && sub == other.sub && type == other.type && room == other.room

    override fun toString(): String = String.format("%s  — %s%s %s",
        sub.abb,
        day.toString(),
        regularity
            .odd?.let { if (it) " I." else " II." } ?: "",
        Prefs.settings.lessonTimeFormat.rangeFormat(time))
}