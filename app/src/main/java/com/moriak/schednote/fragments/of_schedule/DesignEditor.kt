package com.moriak.schednote.fragments.of_schedule

import android.app.WallpaperManager.FLAG_LOCK
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.contracts.PermissionContract
import com.moriak.schednote.contracts.PickContract
import com.moriak.schednote.data.Lesson
import com.moriak.schednote.data.LessonType
import com.moriak.schednote.enums.ColorGroup
import com.moriak.schednote.enums.PermissionHandler.IMAGE_ACCESS
import com.moriak.schednote.enums.Redirection
import com.moriak.schednote.enums.Regularity
import com.moriak.schednote.enums.Regularity.*
import com.moriak.schednote.fragments.SubActivity
import com.moriak.schednote.fragments.of_schedule.DesignEditor.SettingWindow.*
import com.moriak.schednote.getRegularity
import com.moriak.schednote.interfaces.IColorGroup
import com.moriak.schednote.storage.Prefs.Settings.dualWeekSchedule
import com.moriak.schednote.storage.Prefs.Settings.lessonTimeFormat
import com.moriak.schednote.storage.Prefs.Settings.setColor
import com.moriak.schednote.storage.Prefs.Settings.workWeek
import com.moriak.schednote.storage.Prefs.States.bgImage
import com.moriak.schednote.storage.Prefs.States.bgImageAngle
import com.moriak.schednote.storage.Prefs.States.bgImageFit
import com.moriak.schednote.storage.Prefs.States.bgImagePos
import com.moriak.schednote.storage.Prefs.States.schedulePos
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.views.OptionStepper
import com.moriak.schednote.views.ScheduleView
import com.moriak.schednote.views.WallpaperView
import com.moriak.schednote.views.WallpaperView.ImageFit
import com.moriak.schednote.views.WallpaperView.ImageFit.*
import com.moriak.schednote.widgets.ScheduleWidget
import kotlinx.android.synthetic.main.style_layout.*
import kotlinx.android.synthetic.main.style_layout.view.*
import kotlinx.android.synthetic.main.wpp_color_editor.*
import kotlinx.android.synthetic.main.wpp_image_editor.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


/**
 * Fragment ponúka možnosť nastaviť rozvrh ako tapetu. Pred tým však sprístupní náhľad, ako to bude
 * vyzerať a poskytne možnosť nastaviť si vlastné farby, príp. zvoliť obrázok na pozadie rozvrhu
 * ktorý možno na plátno rozložiť 3 rôznymi spôsobmi a nastaviť rotáciu
 * na 4 možné uhly (0°, 90°, 180°, 270°).
 */
class DesignEditor : SubActivity() {
    private companion object {
        private const val INT_DATA = "INT_DATA"
    }

    private enum class SettingWindow { NOTHING, BG_IMG, PALETTE }

    private object ScheduleScreenWatcher: WallpaperView.ScheduleScreenWatcher {
        override fun onScheduleMoved(percent: Float) { schedulePos = percent }
        override fun onImageMoved(percent: Float) { bgImagePos = percent }
        override fun onImageLoadError() { App.toast(R.string.error_loading_image) }
    }

    private object ImgFitChange: RadioGroup.OnCheckedChangeListener {
        private fun setFit(screenView: WallpaperView, fit: ImageFit) {
            screenView.setFit(fit)
            bgImageFit = fit
        }

        override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
            when (checkedId) {
                R.id.fill -> setFit(group!!.tag as WallpaperView, FILL)
                R.id.cover -> setFit(group!!.tag as WallpaperView, COVER)
                R.id.contain -> setFit(group!!.tag as WallpaperView, CONTAIN)
            }
        }
    }

    private object SchFormat: ScheduleView.ColumnNameFormat {
        override fun getColumnDescription(col: Int): String = lessonTimeFormat.startFormat(col)
    }

    private object OnCTypeChange: OptionStepper.OnChange {
        override fun onChange(v: View?, item: Any?) {
            val p = (item as IColorGroup).color
            val f = v!!.tag as DesignEditor
            f.palette_h_range.progress = p.hue
            f.palette_s_range.progress = p.saturation
            f.palette_l_range.progress = p.luminance
            f.palette_a_range.progress = p.alpha
            f.palette_h_number.text = "${p.hue}"
            f.palette_s_number.text = "${p.saturation}"
            f.palette_l_number.text = "${p.luminance}"
            f.palette_a_number.text = "${p.alpha}"
        }
    }

    private val pickerLauncher = registerForActivityResult(PickContract) {
        screen_view.setUri(it)
        bgImage = it
    }

    private val permissionLauncher = registerForActivityResult(PermissionContract) {
        if (it) pickerLauncher.launch(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }

    private val clickListener = View.OnClickListener {
        when (it) {
            wallpaper -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.set_as_wallpaper)
                    .setItems(arrayOf(
                        getString(R.string.set_as_wallpaper_on_home_screen),
                        getString(R.string.set_as_wallpaper_on_lock_screen),
                        getString(R.string.set_as_wallpaper_completely),
                        getString(R.string.abort)
                    )) { _, which ->
                        if (which < 3) {
                            val bmp = screen_view.getWallpaperBitmap()
                            CoroutineScope(Default).launch {
                                screen_view.setAsWallpaper(bmp, when (which) {
                                    0 -> FLAG_SYSTEM
                                    1 -> FLAG_LOCK
                                    else -> FLAG_SYSTEM or FLAG_LOCK
                                })
                                withContext(Main) { App.toast(R.string.wallpaper_set_success) }
                            }
                        }
                    }
                    .show()
            }
            odd_btn, even_btn -> {
                regularity = if (it == odd_btn) ODD else EVEN
                odd_btn.alpha = if (regularity == ODD) 1F else 0.5F
                even_btn.alpha = if (regularity == EVEN) 1F else 0.5F
                loadSchedule(screen_view)
            }
            palette_open -> show(PALETTE)
            palette_close -> if (setting == PALETTE) show(NOTHING)
            bg_img_open -> show(BG_IMG)
            bg_img_close -> if (setting == BG_IMG) show(NOTHING)
            bg_img_set -> IMAGE_ACCESS.allowMe(requireContext(), permissionLauncher) {
                pickerLauncher.launch(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            }
            bg_img_remove -> {
                screen_view.setUri(null)
                bgImage = null
            }
            bg_rotate_left -> bgImageAngle = screen_view.bgRotateLeft()
            bg_rotate_right -> bgImageAngle = screen_view.bgRotateRight()
        }
    }

    private val colorChangeListener = object: SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val type = palette_category.option as IColorGroup
            val pair = when (seekBar) {
                palette_h_range -> palette_h_number to type.color::setHue
                palette_s_range -> palette_s_number to type.color::setSaturation
                palette_l_range -> palette_l_number to type.color::setLuminance
                palette_a_range -> palette_a_number to type.color::setAlpha
                else -> return
            }
            pair.first.text = "$progress"
            pair.second(progress)
            screen_view.setTypeColor(type.id, type.color.color, type.color.contrast)
        }
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            when (val opt = palette_category.option) {
                is ColorGroup -> setColor(opt, opt.color)
                is LessonType -> SQLite.setColor(opt.id, opt.color)
            }
            ScheduleWidget.update(requireContext())
        }
    }

    private val colorDescriptor = object: OptionStepper.Format {
        override fun getItemDescription(item: Any?): String = when (item) {
            is ColorGroup -> item.describe(requireContext())
            is LessonType -> item.name
            else -> ""
        }
    }

    private val colorTypes = IColorGroup.getGroups()

    private var setting = NOTHING

    private var regularity: Regularity = Calendar.getInstance()
        .getRegularity(workWeek, dualWeekSchedule)

    private val lessons: TreeMap<Regularity, List<Lesson>> = TreeMap()

    init {
        lessons[EVERY] = SQLite.getLessons(workWeek, EVERY)
        if (dualWeekSchedule) {
            lessons[ODD] = SQLite.getLessons(workWeek, ODD)
            lessons[EVEN] = SQLite.getLessons(workWeek, EVEN)
        }
    }

    private fun show(sw: SettingWindow) {
        palette_open.visibility = if (sw == PALETTE) GONE else VISIBLE
        palette_box.visibility = if (sw == PALETTE) VISIBLE else GONE
        bg_img_open.visibility = if (sw == BG_IMG) GONE else VISIBLE
        bg_img_box.visibility = if (sw == BG_IMG) VISIBLE else GONE
        setting = sw
    }

    private fun loadSchedule(v: ScheduleView) {
        screen_view.clear()
        val fn = fun(l: Lesson) = v.addLesson(l.day, l.time, l.type, l.sub.abb, l.room)
        lessons[regularity]?.forEach(fn)
        if (regularity != EVERY) lessons[EVERY]?.forEach(fn)
    }

    private fun setUpEvents() {
        screen_view.addScreenWatcher(ScheduleScreenWatcher)
        odd_btn.setOnClickListener(clickListener)
        even_btn.setOnClickListener(clickListener)
        bg_img_open.setOnClickListener(clickListener)
        bg_img_close.setOnClickListener(clickListener)
        palette_open.setOnClickListener(clickListener)
        palette_close.setOnClickListener(clickListener)
        wallpaper.setOnClickListener(clickListener)
        bg_img_set.setOnClickListener(clickListener)
        bg_img_remove.setOnClickListener(clickListener)
        bg_rotate_left.setOnClickListener(clickListener)
        bg_rotate_right.setOnClickListener(clickListener)
        palette_category.setOnChange(OnCTypeChange)
        palette_h_range.setOnSeekBarChangeListener(colorChangeListener)
        palette_s_range.setOnSeekBarChangeListener(colorChangeListener)
        palette_l_range.setOnSeekBarChangeListener(colorChangeListener)
        palette_a_range.setOnSeekBarChangeListener(colorChangeListener)
        radios.setOnCheckedChangeListener(ImgFitChange)
    }

    private fun setUpState(saved: Bundle?) {
        radios.tag = screen_view
        palette_category.tag = this
        saved?.getIntArray(INT_DATA)?.let {
            palette_category.index = it[0]
            setting = SettingWindow.values()[it[1]]
            regularity = Regularity.values()[it[2]]
        }
        if (!dualWeekSchedule) odd_btn.visibility = GONE
        if (!dualWeekSchedule) even_btn.visibility = GONE
        if (regularity == ODD) even_btn.alpha = 0.5F
        if (regularity == EVEN) odd_btn.alpha = 0.5F
        if (setting == BG_IMG) bg_img_box.visibility = VISIBLE
        else if (setting == PALETTE) palette_box.visibility = VISIBLE
        val type = palette_category.option as IColorGroup
        palette_h_range.progress = type.color.hue
        palette_s_range.progress = type.color.saturation
        palette_l_range.progress = type.color.luminance
        palette_a_range.progress = type.color.alpha
        palette_h_number.text = "${type.color.hue}"
        palette_s_number.text = "${type.color.saturation}"
        palette_l_number.text = "${type.color.luminance}"
        palette_a_number.text = "${type.color.alpha}"
        val fit = bgImageFit
        fill.isChecked = fit == FILL
        cover.isChecked = fit == COVER
        contain.isChecked = fit == CONTAIN
        (screen_view as ScheduleView).setWorkWeek(workWeek)
        screen_view.setFit(fit)
        screen_view.setFormat(SchFormat)
        screen_view.imgPercent = bgImagePos
        screen_view.schPercent = schedulePos
        screen_view.setBgRotation(bgImageAngle)
        colorTypes.forEach { screen_view.setTypeColor(it.id, it.color.color, it.color.contrast) }
        screen_view.post { screen_view.setUri(bgImage) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.design)
    }

    override fun onCreateView(inf: LayoutInflater, par: ViewGroup?, saved: Bundle?) = inf
        .inflate(R.layout.style_layout, par, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        palette_category.setOptions(colorTypes)
        palette_category.setFormat(colorDescriptor)
        setUpState(savedInstanceState)
        loadSchedule(screen_view)
        setUpEvents()
        Redirection.detectRedirection(activity?.intent)?.let { _ ->
            val index = colorTypes.indexOfFirst {
                it.id == activity?.intent?.extras?.getInt(Redirection.EXTRA_DESIGN_COLOR_GROUP)
            }
            if (index > -1) {
                show(PALETTE)
                palette_category.index = index
                OnCTypeChange.onChange(palette_category, palette_category.option)
            }
            activity?.intent = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray(INT_DATA, intArrayOf(palette_category.index, setting.ordinal, regularity.ordinal))
    }

    override fun onDestroyView() {
        requireView().screen_view.setUri(null)
        super.onDestroyView()
    }
}