package com.moriak.schednote.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.databinding.ActivityMainBinding
import com.moriak.schednote.dialogs.InfoDialog
import com.moriak.schednote.enums.AlarmCategory
import com.moriak.schednote.enums.Redirection
import com.moriak.schednote.enums.ScheduleDisplay
import com.moriak.schednote.enums.SubContent
import com.moriak.schednote.fragments.SubActivity
import com.moriak.schednote.fragments.of_main.NoteList
import com.moriak.schednote.fragments.of_main.SubjectList
import com.moriak.schednote.storage.Prefs
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.widgets.NoteWidget

/**
 * Hlavná aktivita pozostáva z len menu, pomocou ktorého sa striedajú fragmenty, ktoré načítavajú obsah
 * Fragmenty sú od seba nezávislé a môžu mať ďalšie vnorené fragmenty.
 */
class MainActivity : ShakeCompatActivity<ActivityMainBinding>() {
    private companion object {
        private const val SUB_ACTIVITY = "SUB_ACTIVITY"
        private const val INFO = "INFO"
    }
    private var isRestoring: Boolean = false

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_action_menu, menu)
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
            R.id.settings -> startActivity(Intent(this, Settings::class.java))
            R.id.sweep -> {
                SQLite.clearGarbageData()
                val fragment = getFragment()
                if (fragment is SubjectList) forceMenuChoice(SubContent.SUBJECTS)
                else if (fragment is NoteList) forceMenuChoice(SubContent.NOTES)
                App.toast(R.string.garbage_data_removed, true)
                NoteWidget.update(this)
            }
            R.id.help -> inform()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setMenuVisible(visible: Boolean) {
        binding.leftMenu?.visibility = if (visible) VISIBLE else GONE
        binding.leftMenuShutter?.visibility = if (visible) VISIBLE else GONE
    }

    override fun onCreateBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isRestoring = savedInstanceState != null

        val choiceFn = fun (m: MenuItem) = true.also {
            if (!isRestoring) setSubContent(SubContent.values.find { it.button == m.itemId } as SubContent)
            binding.leftMenu?.let { setMenuVisible(false) }
        }

        binding.bottomMenu?.setOnItemSelectedListener(choiceFn)
        binding.leftMenu?.setNavigationItemSelectedListener(choiceFn)

        val content = intentAction(intent)
        binding.bottomMenu?.selectedItemId = content.button
        if (binding.leftMenu != null) {
            binding.leftMenu!!.setCheckedItem(content.button)
            if (savedInstanceState == null) setSubContent(content)
            binding.leftMenuShutter!!.setOnClickListener { setMenuVisible(false) }
            binding.leftMenuOpenner!!.setOnClickListener { setMenuVisible(true) }
        }
        isRestoring = false
    }

    private fun intentAction(intent: Intent?): SubContent {
        Redirection.detectRedirection(intent)?.let {
            var content = it.iSubContent
            while(content != null) {
                content.remember()
                content = content.parent
            }
        }
        return SubContent.lastSet as SubContent
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        intent = newIntent
        forceMenuChoice(intentAction(intent))
    }

    private fun inform() = when (SubContent.lastSet as SubContent) {
        SubContent.SCHEDULE -> when (ScheduleDisplay.lastSet as ScheduleDisplay) {
            ScheduleDisplay.DESIGN -> R.string.info_design
            ScheduleDisplay.LESSON_SCHEDULE -> R.string.info_lesson_schedule
            ScheduleDisplay.TIME_SCHEDULE -> R.string.info_time_schedule
            ScheduleDisplay.LESSON_TYPES -> R.string.info_lesson_types
        }
        SubContent.SUBJECTS -> R.string.info_subjects
        SubContent.NOTES -> R.string.info_notes
        SubContent.ALARMS -> when (AlarmCategory.lastSet as AlarmCategory) {
            AlarmCategory.REMINDER -> R.string.info_reminder_advance
            AlarmCategory.ALARM -> R.string.info_alarm_clocks
        }
        SubContent.SEMESTER -> R.string.info_semester
    }.let(::InfoDialog).show(supportFragmentManager, INFO)

    private fun forceMenuChoice(content: SubContent) {
        binding.leftMenu?.let {
            it.setCheckedItem(content.button)
            setSubContent(content)
        }
        binding.bottomMenu?.selectedItemId = content.button
    }

    private fun getFragment() = supportFragmentManager.findFragmentByTag(SUB_ACTIVITY) as SubActivity?

    private fun setSubContent(subContent: SubContent) {
        getFragment()?.removeAllSubFragments()
        subContent.remember()
        supportFragmentManager
            .beginTransaction()
            .replace(SubContent.container, subContent.fragmentClass.newInstance(), SUB_ACTIVITY)
            .commit()
    }

    fun refreshWhenCleanedUpAndNecessary() {
        when (val m = SubContent.lastSet) {
            SubContent.SUBJECTS, SubContent.NOTES -> forceMenuChoice(m as SubContent)
            SubContent.SCHEDULE -> if (ScheduleDisplay.lastSet == ScheduleDisplay.LESSON_SCHEDULE)
                forceMenuChoice(m as SubContent)
        }
    }

    fun refreshWhenAlarmClocksSetAndNecessary() {

        when (val m = SubContent.lastSet) {
            SubContent.ALARMS ->
                if (AlarmCategory.lastSet == AlarmCategory.ALARM) forceMenuChoice(m as SubContent)
        }
    }

    /**
     * Ak je fragment prvý krát uživateľom zobrazený prvý krát, automaticky sa mu zabrazí dialóg
     * s popisom, aké funkcie daný fragment ponúka. Od toho momentu si to môže kedykoľvek zobraziť
     * kliknutím na určenú položku menu
     */
    fun introduce() = when(SubContent.lastSet as SubContent) {
        SubContent.SCHEDULE -> when (ScheduleDisplay.lastSet as ScheduleDisplay) {
            ScheduleDisplay.DESIGN -> Prefs.FirstVisit.design
            ScheduleDisplay.LESSON_SCHEDULE -> Prefs.FirstVisit.lessonSchedule
            ScheduleDisplay.TIME_SCHEDULE -> Prefs.FirstVisit.timeSchedule
            ScheduleDisplay.LESSON_TYPES -> Prefs.FirstVisit.lessonTypes
        }
        SubContent.SUBJECTS -> Prefs.FirstVisit.subjects
        SubContent.NOTES -> Prefs.FirstVisit.notes
        SubContent.ALARMS -> when (AlarmCategory.lastSet as AlarmCategory) {
            AlarmCategory.REMINDER -> Prefs.FirstVisit.reminders
            AlarmCategory.ALARM -> Prefs.FirstVisit.alarmClocks
        }
        SubContent.SEMESTER -> Prefs.FirstVisit.semester
    }.let { if (it) inform() else Unit }

    override fun onResume() {
        super.onResume()
        if (Settings.changed) {
            forceMenuChoice(SubContent.lastSet as SubContent)
            Settings.changed = false
        }
    }
}