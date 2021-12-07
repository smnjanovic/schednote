package com.moriak.schednote.dialogs

import android.R.drawable.ic_btn_speak_now
import android.R.drawable.ic_notification_overlay
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface.BOLD
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent.*
import android.speech.SpeechRecognizer
import android.speech.SpeechRecognizer.*
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.R
import com.moriak.schednote.R.string.give_order
import com.moriak.schednote.R.string.wait_please
import com.moriak.schednote.enums.Command
import com.moriak.schednote.enums.Command.ADAPT_ALARMS_TO_SCHEDULE
import com.moriak.schednote.enums.Command.CLEAN_UP
import kotlinx.android.synthetic.main.voice.view.*
import java.util.*

/**
 * Dialóg umožňuje hlasovo zadávať príkazy. Možno v ňom nájsť aj nápovedu, aké príkazy
 * aplikácia pozná.
 */
class VoiceRecognitionDialog : DialogFragment() {
    private companion object {
        private const val BACKUP_HINT = "BACKUP_HINT"
        private const val BACKUP_HINT_VISIBILITY = "BACKUP_HINT_VISIBILITY"
        private const val BACKUP_MESSAGE = "BACKUP_MESSAGE"
    }

    private inner class Voice: RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onEndOfSpeech() {}

        override fun onError(err: Int) {
            recording = false
            customView.message.setText(when (err) {
                ERROR_SERVER -> R.string.voice_err_server
                ERROR_AUDIO -> R.string.voice_err_audio
                ERROR_CLIENT -> R.string.voice_err_client
                ERROR_INSUFFICIENT_PERMISSIONS -> R.string.voice_err_permissions
                ERROR_NETWORK -> R.string.voice_err_network
                ERROR_NETWORK_TIMEOUT -> R.string.voice_err_network
                ERROR_NO_MATCH -> R.string.voice_err_no_match
                ERROR_RECOGNIZER_BUSY -> R.string.voice_err_busy
                ERROR_SPEECH_TIMEOUT -> R.string.time_out
                else -> R.string.voice_not_captured
            })
        }

        override fun onResults(results: Bundle?) {
            recording = false
            activity ?: return
            val result = results?.getStringArrayList(RESULTS_RECOGNITION)?.joinToString(" ")?.trim() ?: ""
            val cmd = context?.let { Command.identifyCommand(requireContext(), result) }
            when (cmd) {
                CLEAN_UP -> customView.message.setText(R.string.obsolete_data_gone)
                ADAPT_ALARMS_TO_SCHEDULE -> customView.message.setText(R.string.alarm_clocks_set)
                null -> customView.message.text = ("${getString(R.string.unspecified_command)}: $result")
                else -> customView.message.text = result
            }
            cmd?.commit(requireActivity())
            if (cmd?.redirection != null) dismiss()
        }
    }

    private val clickListener = View.OnClickListener { clicked ->
        when (clicked) {
            customView.info, customView.hideInfo -> hint = clicked == customView.info
            customView.prevCmd, customView.nextCmd -> {
                command = if (clicked == customView.prevCmd) command.prev else command.next
                customView.message.text = getSpanned(command)
            }
            customView.mic -> {
                recording = !recording
                if (recording) speech.startListening(speechIntent) else speech.stopListening()
            }
        }
    }

    private lateinit var customView: View

    private var command = CLEAN_UP

    private var hint: Boolean = false; set(hintVisible) {
        customView.prevCmd.visibility = if (hintVisible) View.VISIBLE else View.GONE
        customView.nextCmd.visibility = if (hintVisible) View.VISIBLE else View.GONE
        customView.info.visibility = if (!hintVisible) View.VISIBLE else View.GONE
        customView.hideInfo.visibility = if (hintVisible) View.VISIBLE else View.GONE
        customView.message.text = if (hintVisible) getSpanned(command)
        else customView.context.getString(R.string.press_to_command)
        field = hintVisible
    }

    private var recording = false; set(value) {
        field = value
        if (value) hint = false
        if (this::customView.isInitialized) {
            val res = if (value) ic_notification_overlay else ic_btn_speak_now
            customView.mic.setImageDrawable(customView.resources.getDrawable(res, null))
            customView.message.text = customView.context.getString(if (value) give_order else wait_please)
        }
    }

    private lateinit var speech: SpeechRecognizer

    private lateinit var speechIntent: Intent

    private fun getSpanned(command: Command): SpannableString {
        val cmdWord = customView.context.getString(command.cmdRes)
        val cmdDesc = customView.context.getString(command.cmdDescRes)
        val ss = SpannableString("$cmdWord\n$cmdDesc")
        ss.setSpan(StyleSpan(BOLD), 0, cmdWord.length, SPAN_EXCLUSIVE_EXCLUSIVE)
        val fcs = ForegroundColorSpan(customView.context.getColor(R.color.textColor))
        ss.setSpan(fcs, cmdWord.length + 1, ss.length, SPAN_EXCLUSIVE_EXCLUSIVE)
        return ss
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        customView = requireActivity().layoutInflater.inflate(R.layout.voice, null)
        customView.prevCmd.setOnClickListener(clickListener)
        customView.nextCmd.setOnClickListener(clickListener)
        customView.info.setOnClickListener(clickListener)
        customView.hideInfo.setOnClickListener(clickListener)
        customView.mic.setOnClickListener(clickListener)

        savedInstanceState?.let {
            command = Command.values()[it.getInt(BACKUP_HINT)]
            hint = it.getBoolean(BACKUP_HINT_VISIBILITY)
            it.getString(BACKUP_MESSAGE)?.let { msg -> customView.message.text = msg }
        }
        speech = createSpeechRecognizer(activity)
        speechIntent = Intent(ACTION_RECOGNIZE_SPEECH)
            .putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM)
            .putExtra(EXTRA_LANGUAGE, Locale.getDefault())
        speech.setRecognitionListener(Voice())
        return AlertDialog.Builder(requireActivity())
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

    override fun onDestroy() {
        super.onDestroy()
        speech.destroy()
    }
}