package com.moriak.schednote.adapters

import android.os.Bundle
import android.view.View
import com.moriak.schednote.AlarmTone
import com.moriak.schednote.R
import com.moriak.schednote.storage.Prefs.Settings.alarmTone
import kotlinx.android.synthetic.main.alarm_tone_item.view.*

/**
 * Adaptér zobrazuje zoznam tónov budenia
 */
class AlarmToneAdapter: CustomAdapter<AlarmTone>(R.layout.alarm_tone_item) {
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

    override fun instantiateViewHolder(v: View): CustomViewHolder = AlarmToneHolder(v)

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
    inner class AlarmToneHolder(view: View): CustomViewHolder(view) {
        override fun bind(pos: Int) {
            val isPlaying: Boolean = extras.getString(PLAYING) == item?.uri?.toString()
            val isChosen: Boolean = item?.uri == alarmTone.uri

            itemView.ac_play.visibility = if (isPlaying) View.GONE else View.VISIBLE
            itemView.ac_pause.visibility = if (isPlaying) View.VISIBLE else View.GONE
            itemView.ac_choose.visibility = if (isChosen) View.GONE else View.VISIBLE
            itemView.ac_chosen.visibility = if (isChosen) View.VISIBLE else View.GONE
            itemView.ac_title.text = item?.label

            itemView.ac_play.tag = this
            itemView.ac_pause.tag = this
            itemView.ac_choose.tag = this
            itemView.ac_title.tag = this

            itemView.ac_play.setOnClickListener(clickEvent)
            itemView.ac_pause.setOnClickListener(clickEvent)
            itemView.ac_choose.setOnClickListener(clickEvent)
            itemView.ac_title.setOnClickListener(clickEvent)
        }
    }
}