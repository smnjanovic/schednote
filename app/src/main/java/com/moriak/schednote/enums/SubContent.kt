package com.moriak.schednote.enums

import androidx.annotation.IdRes
import com.moriak.schednote.R
import com.moriak.schednote.enums.SubContent.*
import com.moriak.schednote.fragments.SubActivity
import com.moriak.schednote.fragments.of_main.*
import com.moriak.schednote.interfaces.ISubContent
import com.moriak.schednote.storage.Prefs

/**
 * Inštancie trieda reprezentujú jednotlivé položky hlavného menu.
 *
 * [SCHEDULE] Zobrazenie možností práce manipulácie s rozvrhom
 * [SUBJECTS] Zobrazenie upraviteľného abecedného zoznamu predmetov
 * [NOTES] Zobrazenie zoznamu úloh
 * [ALARMS] Zobrazenie možností k upozorneniam
 * [SEMESTER] Nastavenie semestra
 */
enum class SubContent(
    @IdRes override val button: Int,
    override val fragmentClass: Class<out SubActivity<*>>
): ISubContent {
    SCHEDULE(R.id.schedule, ScheduleContent::class.java),
    SUBJECTS(R.id.subjects, SubjectList::class.java),
    NOTES(R.id.notes, NoteList::class.java),
    ALARMS(R.id.alarms, NotificationSettings::class.java),
    SEMESTER(R.id.semester, SemesterSettings::class.java);
    override val parent: ISubContent? get() = null
    override fun remember() { Prefs.States.lastMenuChoice = this }
    companion object: ISubContent.ISubContentCompanion {
        @IdRes override val container: Int = R.id.content
        override val values: Array<out ISubContent> = values()
        override val lastSet: ISubContent get() = Prefs.States.lastMenuChoice
    }
}