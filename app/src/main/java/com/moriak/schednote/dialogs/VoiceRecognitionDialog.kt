package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.activities.MainActivity
import com.moriak.schednote.database.data.Subject
import com.moriak.schednote.events.Voice
import com.moriak.schednote.menu.AlarmCategory
import com.moriak.schednote.menu.ScheduleDisplay
import com.moriak.schednote.menu.SubContent
import com.moriak.schednote.other.Command
import com.moriak.schednote.other.Command.*
import com.moriak.schednote.other.Redirection
import com.moriak.schednote.other.TimeCategory
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.voice.view.*
import java.util.*

class VoiceRecognitionDialog : DialogFragment() {
    companion object {
        private const val BACKUP_HINT = "BACKUP_HINT"
        private const val BACKUP_HINT_VISIBILITY = "BACKUP_HINT_VISIBILITY"
        private const val BACKUP_MESSAGE = "BACKUP_MESSAGE"
    }

    private lateinit var customView: View

    //sucasne zobrazeny hlasovy pokyn, pokiak sa nachadzam v rezime zobrazenia napovedy
    private var command = Command[0]!!

    // true ak je viditelna napoveda hlasovych pokynov
    private var hint: Boolean = false
        set(hintVisible) {
            customView.prevCmd.visibility = if (hintVisible) View.VISIBLE else View.GONE
            customView.nextCmd.visibility = if (hintVisible) View.VISIBLE else View.GONE
            customView.info.visibility = if (!hintVisible) View.VISIBLE else View.GONE
            customView.hideInfo.visibility = if (hintVisible) View.VISIBLE else View.GONE
            customView.message.text =
                if (!hintVisible) resources.getString(R.string.press_to_command) else command.spanned
            field = hintVisible
        }

    private var recording = false

    private val voice = Voice()
    private lateinit var speech: SpeechRecognizer
    private lateinit var speechIntent: Intent

    /**
     * Dialóg s rozhraním pre zadanie hlasového pokynu. Je dostupná nápoveda pokynov.
     * @param savedInstanceState zapamatany stav fragmentu pred otocenim
     */
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setUpView(savedInstanceState)
        voiceSetup()
        return AlertDialog.Builder(
            activity ?: throw(NullPointerException("Activity was destroyed!"))
        )
            .setTitle(R.string.voice_instructions)
            .setView(customView)
            .setNegativeButton(R.string.abort, fun(_, _) {})
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(BACKUP_HINT, command.ordinal)
        outState.putBoolean(BACKUP_HINT_VISIBILITY, hint)
        outState.putString(BACKUP_MESSAGE, customView.message.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun notifyResult(str: String) {
        customView.message.text = str
        customView.mic.setImageDrawable(
            resources.getDrawable(
                android.R.drawable.ic_btn_speak_now,
                null
            )
        )
        speech.stopListening()
        recording = false
    }

    private fun voiceSetup() {
        activity ?: return
        speech = SpeechRecognizer.createSpeechRecognizer(activity)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speech.setRecognitionListener(voice)

        voice.setUpCommand { cmd, result ->
            if (activity == null) return@setUpCommand
            when (cmd) {
                TIME_SCHEDULE, LESSON_TYPES, LESSON_SCHEDULE, DESIGN,
                SUBJECTS, NOTES, REMINDERS, ALARM_CLOCKS, SEMESTER,
                SETTINGS, ALARM_TUNE -> {
                    startActivity(cmd.redirection!!.makeIntent(activity!!, false))
                    notifyResult(result)
                }
                LESSON_TYPE -> {
                    startActivity(
                        cmd.redirection!!.makeIntent(activity!!, false)
                            .putExtra(Redirection.EXTRA_DESIGN_COLOR_GROUP, cmd.tag as Int)
                    )
                    notifyResult(result)
                }
                NOTE_CATEGORY -> {
                    startActivity(
                        cmd.redirection!!.makeIntent(activity!!, false).putExtra(
                            Redirection.EXTRA_NOTE_CATEGORY,
                            if (cmd.tag is TimeCategory) -(cmd.tag as TimeCategory).ordinal.toLong()
                            else (cmd.tag as Subject).id
                        )
                    )
                    notifyResult(result)
                }
                CLEAN_UP -> {
                    App.data.clearGarbageData()
                    notifyResult(App.str(R.string.obsolete_data_gone))
                    if (activity is MainActivity)
                        when (Prefs.states.lastMenuChoice) {
                            SubContent.SCHEDULE ->
                                if (Prefs.states.lastScheduleDisplay != ScheduleDisplay.DESIGN) null
                                else Redirection.DESIGN
                            SubContent.SUBJECTS -> Redirection.SUBJECTS
                            SubContent.NOTES -> Redirection.NOTES
                            else -> null
                        }?.let { startActivity(it.makeIntent(activity!!, false)) }
                }
                ADAPT_ALARMS_TO_SCHEDULE -> {
                    Prefs.notifications.setAlarmsBySchedule()
                    notifyResult(App.str(R.string.alarm_clocks_set))
                    if (activity is MainActivity && Prefs.states.lastMenuChoice == SubContent.ALARMS
                        && Prefs.states.lastAlarmCategory == AlarmCategory.ALARM
                    )
                        startActivity(Redirection.ALARM_CLOCKS.makeIntent(activity!!, false))
                }
                null -> notifyResult(App.str(R.string.unspecified_command) + ": $result")
            }
        }

        voice.handleError {
            speech.cancel()
            notifyResult(it)
        }
    }

    @SuppressLint("InflateParams")
    private fun setUpView(saved: Bundle?) {
        customView = activity?.layoutInflater?.inflate(R.layout.voice, null)
            ?: throw Exception("Activity was destroyed")

        val toggleInfo = View.OnClickListener { clicked ->
            when (clicked) {
                customView.info -> hint = true
                customView.hideInfo -> hint = false
            }
        }
        val navigate = View.OnClickListener { clicked ->
            command = when (clicked) {
                customView.prevCmd -> command.prev
                customView.nextCmd -> command.next
                else -> command
            }
            customView.message.text = command.spanned
        }

        customView.prevCmd.setOnClickListener(navigate)
        customView.nextCmd.setOnClickListener(navigate)
        customView.info.setOnClickListener(toggleInfo)
        customView.hideInfo.setOnClickListener(toggleInfo)

        saved?.let {
            command = Command[it.getInt(BACKUP_HINT)]!!
            hint = it.getBoolean(BACKUP_HINT_VISIBILITY)
            it.getString(BACKUP_MESSAGE)?.let { msg -> customView.message.text = msg }
        }

        //vypnut / zapnut nahravanie (Google: ma chybu, ze zastavit ide len na prvý krát 16.5.2020)
        customView.mic.setOnClickListener {
            if (!recording) {
                hint = false
                recording = true
                customView.mic.setImageDrawable(
                    resources.getDrawable(
                        android.R.drawable.ic_notification_overlay,
                        null
                    )
                )
                customView.message.text = resources.getString(R.string.give_order)
                speech.startListening(speechIntent)
            } else {
                recording = false
                speech.stopListening()
                customView.message.text = resources.getString(R.string.wait_please)
                customView.mic.setImageDrawable(
                    resources.getDrawable(
                        android.R.drawable.ic_btn_speak_now,
                        null
                    )
                )
            }
        }
    }
}