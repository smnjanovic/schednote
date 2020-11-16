package com.moriak.schednote.menu

import com.moriak.schednote.R
import com.moriak.schednote.fragments.of_main.*
import com.moriak.schednote.menu.SubContent.*

/**
 * Trieda reprezentuje položky hlavného menu. Možno na základe nich rozhodnúť ktorý fragment treba zobraziť
 *
 * @property SCHEDULE Zobrazenie možností práce manipulácie s rozvrhom
 * @property SUBJECTS Zobrazenie upraviteľného abecedného zoznamu predmetov
 * @property NOTES Zobrazenie zoznamu úloh
 * @property ALARMS Zobrazenie možností k upozorneniam
 * @property SEMESTER Nastavenie semestra
 *
 * @property resId Odkaz na kontajner, kam sa budú fragmenty vkladať
 * @property fragment Vyberie sa fragment, ktorý bude načítany do kontajnera [resId]
 */
enum class SubContent {
    SCHEDULE, SUBJECTS, NOTES, ALARMS, SEMESTER;

    companion object {

        /**
         * Získanie položky menu podľa názvu enumu
         * @param pName
         * @return Vráti obsah vyplývajúci z [pName] alebo vráti predvolený prvok [SCHEDULE]
         */
        fun giveEnum(pName: String?): SubContent {
            for (e in values())
                if (e.name == pName)
                    return e
            return SCHEDULE
        }

        /**
         * Získanie položky menu podľa odkazu na tlačidlo v menu
         * @param res Odkaz na tlačidlo v menu
         * @return Vráti obsah vyplývajúci z [res] alebo vráti predvolený prvok [SCHEDULE]
         */
        fun giveEnum(res: Int): SubContent {
            for (e in values())
                if (e.resId == res)
                    return e
            return SCHEDULE
        }
    }

    val resId: Int
        get() = when (this) {
            SCHEDULE -> R.id.schedule
            SUBJECTS -> R.id.subjects
            NOTES -> R.id.notes
            ALARMS -> R.id.alarms
            SEMESTER -> R.id.semester
        }
    val fragment
        get() = when (this) {
            SCHEDULE -> ScheduleFragment()
            SUBJECTS -> SubjectsFragment()
            NOTES -> NotesFragment()
            ALARMS -> AlarmsFragment()
            SEMESTER -> SemesterFragment()
        }
}