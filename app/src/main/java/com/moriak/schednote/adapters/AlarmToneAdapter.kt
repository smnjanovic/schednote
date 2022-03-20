package com.moriak.schednote.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.AlarmTone
import com.moriak.schednote.R
import com.moriak.schednote.databinding.AlarmToneItemBinding
import com.moriak.schednote.storage.Prefs.Settings.alarmTone

/**
 * Adaptér zobrazuje zoznam tónov budenia
 */
class AlarmToneAdapter: CustomAdapter<AlarmTone, AlarmToneItemBinding>() {
    /**
     * @property PLAYING Kľúč pod ktorým je uložené ID skladby, ktorá práve hrá
     * @property ACTION_PLAY Označenie pre pokus o prehratie skladby
     * @property ACTION_PAUSE Označenie pre pokus o pozastavenie skladby
     * @property ACTION_SET Označenie pre pokus o nastavenie sladby ako tón budenia
     * @property ACTION_SET_PLAY_PAUSE Označenie pre pokus o prehratie a súčasné nastavenie
     * ako tón budenia, ak je sladba zastavená, inak pozastavenie.
     */
    companion object {
        const val PLAYING = "PLAYING"

        const val ACTION_PLAY = 1
        const val ACTION_PAUSE = 2
        const val ACTION_SET = 3
        const val ACTION_SET_PLAY_PAUSE = 5
    }

    private val clickEvent = View.OnClickListener {
        triggerItemAction((it.tag as AlarmToneHolder).adapterPosition, extras, when (it.id) {
            R.id.ac_play -> ACTION_PLAY
            R.id.ac_pause -> ACTION_PAUSE
            R.id.ac_choose -> ACTION_SET
            R.id.ac_title -> ACTION_SET_PLAY_PAUSE
            else -> 0
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        AlarmToneHolder(AlarmToneItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun bundleToItem(bundle: Bundle): AlarmTone = throw Exception("Unimplemented!!!")
    override fun itemToBundle(item: AlarmTone, bundle: Bundle) { throw Exception("Unimplemented!!!") }

    override fun compare(a: AlarmTone, b: AlarmTone): Int {
        var cmp = a.label.compareTo(b.label)
        if (cmp == 0) cmp = a.id.compareTo(b.id)
        return cmp
    }

    /**
     * Objekt vizualizuje položku zoznamu (tón budenia)
     */
    inner class AlarmToneHolder(b: AlarmToneItemBinding): CustomViewHolder(b) {
        override fun bind(pos: Int) {
            val isPlaying: Boolean = extras.getString(PLAYING) == item?.uri?.toString()
            val isChosen: Boolean = item?.uri == alarmTone.uri

            binding.acPlay.visibility = if (isPlaying) View.GONE else View.VISIBLE
            binding.acPause.visibility = if (isPlaying) View.VISIBLE else View.GONE
            binding.acChoose.visibility = if (isChosen) View.GONE else View.VISIBLE
            binding.acChosen.visibility = if (isChosen) View.VISIBLE else View.GONE
            binding.acTitle.text = item?.label

            binding.acPlay.tag = this
            binding.acPause.tag = this
            binding.acChoose.tag = this
            binding.acTitle.tag = this

            binding.acPlay.setOnClickListener(clickEvent)
            binding.acPause.setOnClickListener(clickEvent)
            binding.acChoose.setOnClickListener(clickEvent)
            binding.acTitle.setOnClickListener(clickEvent)
        }
    }
}