package com.moriak.schednote.activities

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import androidx.lifecycle.Lifecycle
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.dialogs.InfoFragment
import com.moriak.schednote.settings.AlarmTone
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.activity_alarm_tune.*
import android.provider.MediaStore.Audio.Media as Tune

/**
 * Aktivita slúži na výber tónu budenia. Užívateľ je v nej žiadaný o prístup k súborom.
 * Bez tohoto povolenia bude obmedzený výber tónov budenia.
 */
class AlarmToneActivity : ShakeCompatActivity() {
    private companion object {
        private const val INFO = "INFO"
        private const val ACCESS_MUSIC = 1

        private var audio = MediaPlayer.create(App.ctx, Prefs.settings.alarmTone.uri)
        private var audioPos = audio?.currentPosition ?: 0

        // priznak toho, ci aktivita zanika na pokyn uzivatela alebo systemu
        private var destroyedByUser: Boolean = true
    }

    private var tunes = ArrayList<AlarmTone>()

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.just_info, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) inform()
        else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_tune)

        if (audioPos > 0 && !destroyedByUser) {
            music_on_off.isChecked = true
            audio.seekTo(audioPos)
            audio.start()
        }

        destroyedByUser = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        audio_group.setOnCheckedChangeListener { group, checkedId ->
            if (lifecycle.currentState == Lifecycle.State.RESUMED && checkedId > -1) {
                music_on_off.isChecked = false
                Prefs.settings.alarmTone =
                    group.findViewById<RadioButton>(checkedId).tag as AlarmTone
                music_on_off.isChecked = true
            }
        }

        music_on_off.setOnCheckedChangeListener { _, ch ->
            audio?.stop()
            if (ch) {
                audio = MediaPlayer.create(this, Prefs.settings.alarmTone.uri)
                if (audio == null) {
                    music_on_off.isChecked = false
                    App.toast(R.string.audio_file_removed_or_moved, Gravity.BOTTOM)
                } else audio.start()
            }
        }

        request_permission_btn.setOnClickListener {
            if (!checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), ACCESS_MUSIC)
        }
    }

    override fun onResume() {
        super.onResume()
        reloadTunes()
        request_permission_btn.visibility =
            if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) View.GONE else View.VISIBLE

        scroll_container.post {
            val view = audio_group.findViewById<RadioButton>(audio_group.checkedRadioButtonId)
                ?: return@post
            scroll_container.scrollTo(0, (view.top - scroll_container.height / 2 + view.height / 2))
            if (Prefs.firstVisit.alarmTune) {
                inform()
                Prefs.firstVisit.alarmTune = false
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), ACCESS_MUSIC)
            }
        }
    }

    override fun onRequestPermissionsResult(
        request: Int,
        permissions: Array<out String>,
        granted: IntArray
    ) {
        when (request) {
            ACCESS_MUSIC -> {
                if (granted.firstOrNull() != PackageManager.PERMISSION_GRANTED)
                    App.toast(R.string.read_storage_permission_for_music)
                else reloadTunes()
            }
            else -> super.onRequestPermissionsResult(request, permissions, granted)
        }
    }

    /**
     * Ak práve hra hudba, po zatrasení zariadenia bude zastavená,
     * inak sa vykoná rodičovská metóda
     */
    override fun onShake() {
        if (audio.isPlaying) {
            music_on_off.isChecked = false
            App.toast(R.string.music_stopped)
        } else super.onShake()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        destroyedByUser = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (destroyedByUser) audio.stop() else audio.pause()
        audioPos = audio.currentPosition
    }

    private fun reloadTunes() {
        tunes.clear()
        tunes.add(AlarmTone())

        if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val ext = Tune.EXTERNAL_CONTENT_URI
            contentResolver.query(
                ext,
                arrayOf(Tune._ID, Tune.DISPLAY_NAME),
                null,
                null,
                Tune.DISPLAY_NAME
            )?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val title = it.getString(1)
                    tunes.add(AlarmTone(id, title, ContentUris.withAppendedId(ext, id), true))
                }
                it.close()
            }
        }

        audio_group.removeAllViews()
        for (tune in tunes) {
            val radio = RadioButton(this)
            audio_group.addView(radio)
            radio.text = tune.label
            radio.tag = tune
            radio.isChecked = Prefs.settings.alarmTone.id == tune.id
            radio.setPadding(App.dp(8), App.dp(12), App.dp(8), App.dp(12))
        }

        if (audio_group.checkedRadioButtonId == -1) {
            Prefs.settings.alarmTone = AlarmTone()
            (audio_group.getChildAt(0) as RadioButton).isChecked = true
        }
    }

    private fun inform() =
        InfoFragment.create(R.string.info_audio_tunes).show(supportFragmentManager, INFO)
}
