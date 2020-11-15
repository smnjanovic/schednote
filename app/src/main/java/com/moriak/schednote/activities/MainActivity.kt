package com.moriak.schednote.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.facebook.stetho.Stetho
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.dialogs.InfoFragment
import com.moriak.schednote.fragments.of_main.NotesFragment
import com.moriak.schednote.fragments.of_main.SubActivity
import com.moriak.schednote.fragments.of_main.SubjectsFragment
import com.moriak.schednote.menu.AlarmCategory
import com.moriak.schednote.menu.ScheduleDisplay
import com.moriak.schednote.menu.SubContent
import com.moriak.schednote.other.Redirection
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Hlavná aktivita pozostáva z len menu, pomocou ktorého sa striedajú fragmenty, ktoré načítavajú obsah
 * Fragmenty sú od seba nezávislé a môžu mať ďalšie vnorené fragmenty.
 */
class MainActivity : ShakeCompatActivity() {
    companion object {
        private const val SUB_ACTIVITY = "SUB_ACTIVITY"
        private const val INFO = "INFO"
    }

    private var updated = true

    override fun onBackPressed() {
        supportFragmentManager.findFragmentByTag(SUB_ACTIVITY)?.let { f ->
            supportFragmentManager.beginTransaction().remove(f).commit()
        }
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Pomocou menu môžem zobraziť stručný tutoriál k obsahu načítaného z fragmentu,
     * odstrániť nadbytočné údaje (napr. predmety, ktoré nie sú v rozvrhu a sú bez poznámok)
     * alebo sa presmerovať do vnútorných nastavení aplikácie.
     * @param item Položka menu, na ktorú sa ťuklo
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> true.also {
                startActivity(Intent(this, Settings::class.java))
                updated = false
            }
            R.id.sweep -> {
                App.data.clearGarbageData()
                val fragment = supportFragmentManager.findFragmentByTag(SUB_ACTIVITY)
                if (fragment is SubjectsFragment) forceMenuChoice(SubContent.SUBJECTS)
                else if (fragment is NotesFragment) forceMenuChoice(SubContent.NOTES)
                App.toast(R.string.garbage_data_removed, Gravity.CENTER, Toast.LENGTH_LONG)
            }
            R.id.help -> inform()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Stetho.initializeWithDefaults(this)

        bottomMenu.setOnNavigationItemSelectedListener { m ->
            val choice = SubContent.giveEnum(m.itemId)
            val currFr = supportFragmentManager.findFragmentByTag(SUB_ACTIVITY) as SubActivity?
            if (currFr == null || choice != Prefs.states.lastMenuChoice) {
                currFr?.removeAllSubFragments()
                Prefs.states.lastMenuChoice = choice
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content, choice.fragment, SUB_ACTIVITY).commit()
                true
            } else false
        }
        if (savedInstanceState == null && !intentAction(intent)) menuChoice(Prefs.states.lastMenuChoice)
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        intent = newIntent
        intentAction(newIntent)
    }

    private fun inform() {
        InfoFragment.create(
            when (Prefs.states.lastMenuChoice) {
                SubContent.SCHEDULE -> when (Prefs.states.lastScheduleDisplay) {
                    ScheduleDisplay.DESIGN -> R.string.info_design
                    ScheduleDisplay.LESSON_SCHEDULE -> R.string.info_lesson_schedule
                    ScheduleDisplay.TIME_SCHEDULE -> R.string.info_time_schedule
                    ScheduleDisplay.LESSON_TYPES -> R.string.info_lesson_types
                }
                SubContent.SUBJECTS -> R.string.info_subjects
                SubContent.NOTES -> R.string.info_notes
                SubContent.ALARMS -> when (Prefs.states.lastAlarmCategory) {
                    AlarmCategory.REMINDER -> R.string.info_reminder_advance
                    AlarmCategory.ALARM -> R.string.info_alarm_clocks
                }
                SubContent.SEMESTER -> R.string.info_semester
            }
        ).show(supportFragmentManager, INFO)
    }

    private fun intentAction(intent: Intent?): Boolean {
        if (intent == null) return false
        when (val redirect = Redirection.detectRedirection(intent)) {
            Redirection.LESSON_SCHEDULE, Redirection.LESSON_TYPES, Redirection.TIME_SCHEDULE, Redirection.DESIGN -> {
                Prefs.states.lastScheduleDisplay = ScheduleDisplay[redirect.fragment]!!
                forceMenuChoice(SubContent.SCHEDULE)
            }
            Redirection.REMINDERS, Redirection.ALARM_CLOCKS -> {
                Prefs.states.lastAlarmCategory = AlarmCategory[redirect.fragment!!]!!
                forceMenuChoice(SubContent.ALARMS)
            }
            Redirection.SUBJECTS -> forceMenuChoice(SubContent.SUBJECTS)
            Redirection.NOTES -> forceMenuChoice(SubContent.NOTES)
            Redirection.SEMESTER -> forceMenuChoice(SubContent.SEMESTER)
            else -> return false
        }
        return true
    }

    /**
     * Načítanie iného obsahu v rámci tej istej aktivity. Ak konkrétny je už zobrazený, nedeje sa nič.
     * Táto funkcia môže byť volaná aj z fragmentov.
     * @param subContent Enum objekt pomocou ktorého viem vybrať konkrétny fragment, ktorý chcem načítať
     */
    fun menuChoice(subContent: SubContent) {
        bottomMenu.selectedItemId = subContent.resId
    }

    /**
     * Funkcia podobná [menuChoice] s rozdielom, že zobrazený fragment zanikne a bude nahradený novým bez
     * ohľadu na to, či sa jedná o tú istú triedu alebo nie.
     * Táto funkcia môže byť volaná aj z fragmentov.
     * @param subContent Enum objekt pomocou ktorého viem vybrať konkrétny fragment, ktorý chcem načítať
     */
    fun forceMenuChoice(subContent: SubContent) {
        if (bottomMenu.selectedItemId != subContent.resId) bottomMenu.selectedItemId =
            subContent.resId
        else {
            val currFr = supportFragmentManager.findFragmentByTag(SUB_ACTIVITY) as SubActivity?
            currFr?.removeAllSubFragments()
            Prefs.states.lastMenuChoice = subContent
            supportFragmentManager.beginTransaction()
                .replace(R.id.content, subContent.fragment, SUB_ACTIVITY).commit()
        }
    }

    /**
     * Ak je fragment prvý krát uživateľom zobrazený prvý krát, automaticky sa mu zabrazí dialóg
     * s popisom, aké funkcie daný fragment ponúka. Od toho momentu si to môže kedykoľvek zobraziť
     * kliknutím na určenú položku menu
     */
    fun introduce() {
        val subContent = Prefs.states.lastMenuChoice
        if (Prefs.firstVisit.wasVisited(subContent)) {
            inform()
            Prefs.firstVisit.notifyVisited(subContent)
        }
    }

    @SuppressLint("ResourceType")
    override fun onResume() {
        super.onResume()
        if (!updated) {
            forceMenuChoice(Prefs.states.lastMenuChoice)
            updated = true
        }
    }
}
