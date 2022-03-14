package com.moriak.schednote.activities

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
import android.util.DisplayMetrics.DENSITY_DEFAULT
import android.view.Menu
import android.view.MenuItem
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.moriak.schednote.AlarmTone
import com.moriak.schednote.App
import com.moriak.schednote.ItemTopSpacing
import com.moriak.schednote.R
import com.moriak.schednote.adapters.AlarmToneAdapter
import com.moriak.schednote.adapters.AlarmToneAdapter.Companion.ACTION_PAUSE
import com.moriak.schednote.adapters.AlarmToneAdapter.Companion.ACTION_PLAY
import com.moriak.schednote.adapters.AlarmToneAdapter.Companion.ACTION_SET
import com.moriak.schednote.adapters.AlarmToneAdapter.Companion.ACTION_SET_PLAY_PAUSE
import com.moriak.schednote.adapters.AlarmToneAdapter.Companion.PLAYING
import com.moriak.schednote.contracts.PermissionContract
import com.moriak.schednote.contracts.PickContract
import com.moriak.schednote.dialogs.InfoDialog
import com.moriak.schednote.enums.PermissionHandler.AUDIO_ACCESS
import com.moriak.schednote.storage.Prefs
import com.moriak.schednote.storage.Prefs.Settings.alarmTone
import kotlinx.android.synthetic.main.activity_alarm_tune.*

/**
 * Aktivita slúži na výber tónu budenia. Užívateľ je v nej žiadaný o prístup k súborom.
 * Bez tohoto povolenia bude obmedzený výber tónov budenia.
 */
class AlarmToneActivity : ShakeCompatActivity() {
    private companion object {
        private const val INFO = "INFO"
        private const val LLM = "LLM"
    }

    private var tunes = ArrayList<AlarmTone>()
    private lateinit var adapter: AlarmToneAdapter
    private var player: MediaPlayer? = null
    private var lastPlayed: Pair<Uri, Int>? = null

    private val picker = registerForActivityResult(PickContract) {
        it?.let {
            val tone = AlarmTone.seek(it)
            if (it == tone.uri) {
                val pos = adapter.findIndexOf(this::findByUri, alarmTone.uri)
                alarmTone = tone
                if (pos > -1) adapter.notifyItemChanged(pos)
                adapter.insertItem(tone)
            }
        }
    }

    private val requester = registerForActivityResult(PermissionContract) {
        if (it) picker.launch(EXTERNAL_CONTENT_URI)
    }

    private fun inform() = InfoDialog(R.string.info_audio_tunes).show(supportFragmentManager, INFO)

    private fun findByUri(tone: AlarmTone, uri: Any?) = tone.uri == uri

    private fun old2new(old: Uri?, new: Uri) {
        val posOld = old?.let { adapter.findIndexOf(this::findByUri, old) }
        val posNew = adapter.findIndexOf(this::findByUri, new)
        posOld?.let(adapter::notifyItemChanged)
        adapter.notifyItemChanged(posNew)
    }

    private fun play(uri: Uri?) {
        player?.stop()
        player?.release()
        player = uri?.let { MediaPlayer.create(this, uri) }
        if (uri != null && lastPlayed?.first == uri) player!!.seekTo(lastPlayed!!.second)
        player?.isLooping = true
        player?.start()
        lastPlayed = null
    }

    private fun toneAction(pos: Int, data: Bundle, action: Int) {
        when (action) {
            ACTION_PLAY -> {
                val oldPlaying = data.getString(PLAYING)?.toUri()
                val newPlaying = adapter.getItemAt(pos)
                data.putString(PLAYING, newPlaying.uri.toString())
                old2new(oldPlaying, newPlaying.uri)
                play(newPlaying.uri)
            }
            ACTION_PAUSE -> {
                lastPlayed = data.getString(PLAYING)?.toUri()?.to(player?.currentPosition ?: 0)
                play(null)
                data.putString(PLAYING, null)
                adapter.notifyItemChanged(pos)
            }
            ACTION_SET -> {
                val oldChosen = adapter.findIndexOf(this::findByUri, alarmTone.uri)
                val newRingtone = adapter.getItemAt(pos)
                alarmTone = newRingtone
                if (oldChosen > -1) adapter.notifyItemChanged(oldChosen)
                if (alarmTone.uri != newRingtone.uri) {
                    adapter.deleteItem(pos)
                    val newPos = adapter.findIndexOf(this::findByUri, alarmTone.uri)
                    if (newPos > -1) adapter.notifyItemChanged(newPos)
                    App.toast(R.string.audio_file_removed_or_moved)
                }
                else adapter.notifyItemChanged(pos)
            }
            ACTION_SET_PLAY_PAUSE -> {
                val oldChosen = adapter.findIndexOf(this::findByUri, alarmTone.uri)
                val oldPlayed = adapter.findIndexOf(this::findByUri, data.getString(PLAYING)?.toUri())
                if (oldPlayed != -1) adapter.triggerItemAction(oldPlayed, data, ACTION_PAUSE)
                adapter.triggerItemAction(pos, data, ACTION_SET)
                if (pos != oldChosen || pos != oldPlayed) adapter.triggerItemAction(pos, data, ACTION_PLAY)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.ringtone_action_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.help -> inform()
            R.id.external_ringtone -> AUDIO_ACCESS.allowMe(this, requester) {
                picker.launch(EXTERNAL_CONTENT_URI)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_tone)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        adapter = AlarmToneAdapter()
        adapter.onItemAction(this::toneAction)
        AlarmTone.seek(tunes)
        val ringtone = alarmTone
        if (tunes.indexOfFirst { it.uri == ringtone.uri } == -1) tunes.add(ringtone)
        adapter.putItems(tunes)
        alarm_tone_list.adapter = adapter
        alarm_tone_list.layoutManager = LinearLayoutManager(this).also {
            if (savedInstanceState != null)
                it.onRestoreInstanceState(savedInstanceState.getParcelable(LLM))
        }
        val space = 4 * resources.displayMetrics.densityDpi / DENSITY_DEFAULT
        alarm_tone_list.addItemDecoration(ItemTopSpacing(space))

        if (Prefs.FirstVisit.alarmTune) inform()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(LLM, alarm_tone_list.layoutManager?.onSaveInstanceState())
    }

    /**
     * Ak práve hra hudba, po zatrasení zariadenia bude zastavená,
     * inak sa vykoná rodičovská metóda
     */
    override fun onShake() {
        adapter.extras.getString(PLAYING)?.toUri()?.let {
            val pos = adapter.findIndexOf(this::findByUri, it)
            adapter.triggerItemAction(pos, adapter.extras, ACTION_PAUSE)
        } ?: super.onShake()
    }

    override fun onDestroy() {
        adapter.extras.getString(PLAYING)?.toUri()?.let {
            val pos = adapter.findIndexOf(this::findByUri, it)
            adapter.triggerItemAction(pos, adapter.extras, ACTION_PAUSE)
        }
        super.onDestroy()
    }
}
