package com.moriak.schednote.dialogs

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
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.moriak.schednote.R
import com.moriak.schednote.databinding.VoiceBinding
import com.moriak.schednote.enums.Command
import com.moriak.schednote.enums.Command.ADAPT_ALARMS_TO_SCHEDULE
import com.moriak.schednote.enums.Command.CLEAN_UP
import java.util.*

/**
 * Dialóg umožňuje hlasovo zadávať príkazy. Možno v ňom nájsť aj nápovedu, aké príkazy
 * aplikácia pozná.
 */
class VoiceRecognitionDialog : CustomBoundDialog<VoiceBinding>(), View.OnClickListener {
    private companion object {
        private const val BACKUP_HINT = "BACKUP_HINT"
        private const val BACKUP_STATE = "BACKUP_STATE"
        private const val BACKUP_MESSAGE = "BACKUP_MESSAGE"
    }

    override val title: Int = R.string.voice_instructions
    override val negativeButton: ActionButton? by lazy { ActionButton(R.string.abort) {} }
    private var command = CLEAN_UP
    private lateinit var speech: SpeechRecognizer
    private val speechIntent: Intent = Intent(ACTION_RECOGNIZE_SPEECH)
        .putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM)
        .putExtra(EXTRA_LANGUAGE, Locale.getDefault())
    private var state: State = State.READY
        set(value) {
            field = value
            if (isBound) state.apply(binding)
        }

    private fun getSpanned(command: Command): SpannableString {
        val cmdWord = binding.root.context.getString(command.cmdRes)
        val cmdDesc = binding.root.context.getString(command.cmdDescRes)
        val ss = SpannableString("$cmdWord\n$cmdDesc")
        ss.setSpan(StyleSpan(BOLD), 0, cmdWord.length, SPAN_EXCLUSIVE_EXCLUSIVE)
        val fcs = ForegroundColorSpan(binding.root.context.getColor(R.color.textColor))
        ss.setSpan(fcs, cmdWord.length + 1, ss.length, SPAN_EXCLUSIVE_EXCLUSIVE)
        return ss
    }

    override fun setupBinding(inflater: LayoutInflater) = VoiceBinding.inflate(inflater)

    override fun setupContent(saved: Bundle?) {
        saved?.let {
            command = Command.values()[it.getInt(BACKUP_HINT)]
            state = State.values()[it.getInt(BACKUP_STATE)]
            it.getString(BACKUP_MESSAGE)?.let { msg -> binding.message.text = msg }
        }
        speech = createSpeechRecognizer(requireContext()).apply { setRecognitionListener(Voice()) }
        binding.prevCmd.setOnClickListener(this)
        binding.nextCmd.setOnClickListener(this)
        binding.info.setOnClickListener(this)
        binding.hideInfo.setOnClickListener(this)
        binding.mic.setOnClickListener(this)
    }

    override fun onClick(clicked: View?) {
        when (clicked) {
            binding.info -> state = State.HINTED
            binding.hideInfo -> {
                state = State.READY
                binding.message.text = getSpanned(command)
            }
            binding.prevCmd -> {
                command = command.prev
                binding.message.text = getSpanned(command)
            }
            binding.nextCmd -> {
                command = command.next
                binding.message.text = getSpanned(command)
            }
            binding.mic -> {
                if (state == State.RECORDING) speech.stopListening()
                else {
                    speech.startListening(speechIntent)
                    state = State.RECORDING
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(BACKUP_HINT, command.ordinal)
        if (state == State.RECORDING) state = State.READY
        outState.putInt(BACKUP_STATE, (state.ordinal))
        outState.putString(BACKUP_MESSAGE, binding.message.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        speech.destroy()
    }

    private enum class State (
        private val hintViews: Int,
        private val usualViews: Int,
        private val drawable: Int,
        private val caption: Int
    ) {
        READY (View.GONE, View.VISIBLE, android.R.drawable.ic_btn_speak_now, R.string.press_to_command),
        HINTED(View.VISIBLE, View.GONE, android.R.drawable.ic_btn_speak_now, 0),
        RECORDING(View.GONE, View.VISIBLE, android.R.drawable.ic_notification_overlay, R.string.give_order),
        RESULT(View.GONE, View.VISIBLE, android.R.drawable.ic_btn_speak_now, R.string.wait_please);

        open fun apply(vb: VoiceBinding): State {
            vb.prevCmd.visibility = hintViews
            vb.nextCmd.visibility = hintViews
            vb.info.visibility = usualViews
            vb.hideInfo.visibility = hintViews
            vb.message.text = if (caption != 0) vb.root.context.getString(caption) else null
            val res = ResourcesCompat.getDrawable(vb.root.resources, drawable, null)
            vb.mic.setImageDrawable(res)
            return this
        }
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
            state = State.RESULT
            binding.message.setText(when (err) {
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
            state = State.RESULT
            if (isBound) {
                val result = results?.getStringArrayList(RESULTS_RECOGNITION)?.joinToString(" ")?.trim() ?: ""
                val cmd = context?.let { Command.identifyCommand(requireContext(), result) }

                binding.message.text = when (cmd) {
                    CLEAN_UP -> getString(R.string.obsolete_data_gone)
                    ADAPT_ALARMS_TO_SCHEDULE -> getString(R.string.alarm_clocks_set)
                    null -> "${getString(R.string.unspecified_command)}: $result"
                    else -> result
                }

                cmd?.commit(requireActivity())
                if (cmd?.redirection != null) dismiss()
            }
        }
    }
}