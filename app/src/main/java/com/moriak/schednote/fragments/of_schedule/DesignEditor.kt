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
import com.moriak.schednote.databinding.StyleLayoutBinding
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
class DesignEditor : SubActivity<StyleLayoutBinding>() {
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
            f.binding.inPalette.paletteHRange.progress = p.hue
            f.binding.inPalette.paletteSRange.progress = p.saturation
            f.binding.inPalette.paletteLRange.progress = p.luminance
            f.binding.inPalette.paletteARange.progress = p.alpha
            f.binding.inPalette.paletteHNumber.text = "${p.hue}"
            f.binding.inPalette.paletteSNumber.text = "${p.saturation}"
            f.binding.inPalette.paletteLNumber.text = "${p.luminance}"
            f.binding.inPalette.paletteANumber.text = "${p.alpha}"
        }
    }

    private val pickerLauncher = registerForActivityResult(PickContract) {
        binding.screenView.setUri(it)
        bgImage = it
    }

    private val permissionLauncher = registerForActivityResult(PermissionContract) {
        if (it) pickerLauncher.launch(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }

    private val clickListener = View.OnClickListener {
        when (it) {
            binding.wallpaper -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.set_as_wallpaper)
                    .setItems(arrayOf(
                        getString(R.string.set_as_wallpaper_on_home_screen),
                        getString(R.string.set_as_wallpaper_on_lock_screen),
                        getString(R.string.set_as_wallpaper_completely),
                        getString(R.string.abort)
                    )) { _, which ->
                        if (which < 3) {
                            val bmp = binding.screenView.getWallpaperBitmap()
                            CoroutineScope(Default).launch {
                                binding.screenView.setAsWallpaper(bmp, when (which) {
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
            binding.oddBtn, binding.evenBtn -> {
                regularity = if (it == binding.oddBtn) ODD else EVEN
                binding.oddBtn.alpha = if (regularity == ODD) 1F else 0.5F
                binding.evenBtn.alpha = if (regularity == EVEN) 1F else 0.5F
                loadSchedule(binding.screenView)
            }
            binding.paletteOpen -> show(PALETTE)
            binding.inPalette.paletteClose -> if (setting == PALETTE) show(NOTHING)
            binding.bgImgOpen -> show(BG_IMG)
            binding.inBgImg.bgImgClose -> if (setting == BG_IMG) show(NOTHING)
            binding.inBgImg.bgImgSet -> IMAGE_ACCESS.allowMe(requireContext(), permissionLauncher) {
                pickerLauncher.launch(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            }
            binding.inBgImg.bgImgRemove -> {
                binding.screenView.setUri(null)
                bgImage = null
            }
            binding.bgRotateLeft -> bgImageAngle = binding.screenView.bgRotateLeft()
            binding.bgRotateRight -> bgImageAngle = binding.screenView.bgRotateRight()
        }
    }

    private val colorChangeListener = object: SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val type = binding.inPalette.paletteCategory.option as IColorGroup
            val pair = when (seekBar) {
                binding.inPalette.paletteHRange -> binding.inPalette.paletteHNumber to type.color::setHue
                binding.inPalette.paletteSRange -> binding.inPalette.paletteSNumber to type.color::setSaturation
                binding.inPalette.paletteLRange -> binding.inPalette.paletteLNumber to type.color::setLuminance
                binding.inPalette.paletteARange -> binding.inPalette.paletteANumber to type.color::setAlpha
                else -> return
            }
            pair.first.text = "$progress"
            pair.second(progress)
            binding.screenView.setTypeColor(type.id, type.color.color, type.color.contrast)
        }
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            when (val opt = binding.inPalette.paletteCategory.option) {
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
        binding.paletteOpen.visibility = if (sw == PALETTE) GONE else VISIBLE
        binding.paletteBox.visibility = if (sw == PALETTE) VISIBLE else GONE
        binding.bgImgOpen.visibility = if (sw == BG_IMG) GONE else VISIBLE
        binding.bgImgBox.visibility = if (sw == BG_IMG) VISIBLE else GONE
        setting = sw
    }

    private fun loadSchedule(v: ScheduleView) {
        binding.screenView.clear()
        val fn = fun(l: Lesson) = v.addLesson(l.day, l.time, l.type, l.sub.abb, l.room)
        lessons[regularity]?.forEach(fn)
        if (regularity != EVERY) lessons[EVERY]?.forEach(fn)
    }

    private fun setUpEvents() {
        binding.screenView.addScreenWatcher(ScheduleScreenWatcher)
        binding.oddBtn.setOnClickListener(clickListener)
        binding.evenBtn.setOnClickListener(clickListener)
        binding.bgImgOpen.setOnClickListener(clickListener)
        binding.inBgImg.bgImgClose.setOnClickListener(clickListener)
        binding.paletteOpen.setOnClickListener(clickListener)
        binding.inPalette.paletteClose.setOnClickListener(clickListener)
        binding.wallpaper.setOnClickListener(clickListener)
        binding.inBgImg.bgImgSet.setOnClickListener(clickListener)
        binding.inBgImg.bgImgRemove.setOnClickListener(clickListener)
        binding.bgRotateLeft.setOnClickListener(clickListener)
        binding.bgRotateRight.setOnClickListener(clickListener)
        binding.inPalette.paletteCategory.setOnChange(OnCTypeChange)
        binding.inPalette.paletteHRange.setOnSeekBarChangeListener(colorChangeListener)
        binding.inPalette.paletteSRange.setOnSeekBarChangeListener(colorChangeListener)
        binding.inPalette.paletteLRange.setOnSeekBarChangeListener(colorChangeListener)
        binding.inPalette.paletteARange.setOnSeekBarChangeListener(colorChangeListener)
        binding.inBgImg.radios.setOnCheckedChangeListener(ImgFitChange)
    }

    private fun setUpState(saved: Bundle?) {
        binding.inBgImg.radios.tag = binding.screenView
        binding.inPalette.paletteCategory.tag = this
        saved?.getIntArray(INT_DATA)?.let {
            binding.inPalette.paletteCategory.index = it[0]
            setting = SettingWindow.values()[it[1]]
            regularity = Regularity.values()[it[2]]
        }
        if (!dualWeekSchedule) binding.oddBtn.visibility = GONE
        if (!dualWeekSchedule) binding.evenBtn.visibility = GONE
        if (regularity == ODD) binding.evenBtn.alpha = 0.5F
        if (regularity == EVEN) binding.oddBtn.alpha = 0.5F
        if (setting == BG_IMG) binding.bgImgBox.visibility = VISIBLE
        else if (setting == PALETTE) binding.paletteBox.visibility = VISIBLE
        val type = binding.inPalette.paletteCategory.option as IColorGroup
        binding.inPalette.paletteHRange.progress = type.color.hue
        binding.inPalette.paletteSRange.progress = type.color.saturation
        binding.inPalette.paletteLRange.progress = type.color.luminance
        binding.inPalette.paletteARange.progress = type.color.alpha
        binding.inPalette.paletteHNumber.text = "${type.color.hue}"
        binding.inPalette.paletteSNumber.text = "${type.color.saturation}"
        binding.inPalette.paletteLNumber.text = "${type.color.luminance}"
        binding.inPalette.paletteANumber.text = "${type.color.alpha}"
        val fit = bgImageFit
        binding.inBgImg.fill.isChecked = fit == FILL
        binding.inBgImg.cover.isChecked = fit == COVER
        binding.inBgImg.contain.isChecked = fit == CONTAIN
        (binding.screenView as ScheduleView).setWorkWeek(workWeek)
        binding.screenView.setFit(fit)
        binding.screenView.setFormat(SchFormat)
        binding.screenView.imgPercent = bgImagePos
        binding.screenView.schPercent = schedulePos
        binding.screenView.setBgRotation(bgImageAngle)
        colorTypes.forEach { binding.screenView.setTypeColor(it.id, it.color.color, it.color.contrast) }
        binding.screenView.post { binding.screenView.setUri(bgImage) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.design)
    }

    override fun makeBinder(inflater: LayoutInflater, container: ViewGroup?): StyleLayoutBinding =
        StyleLayoutBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.inPalette.paletteCategory.setOptions(colorTypes)
        binding.inPalette.paletteCategory.setFormat(colorDescriptor)
        setUpState(savedInstanceState)
        loadSchedule(binding.screenView)
        setUpEvents()
        Redirection.detectRedirection(activity?.intent)?.let { _ ->
            val index = colorTypes.indexOfFirst {
                it.id == activity?.intent?.extras?.getInt(Redirection.EXTRA_DESIGN_COLOR_GROUP)
            }
            if (index > -1) {
                show(PALETTE)
                binding.inPalette.paletteCategory.index = index
                OnCTypeChange.onChange(binding.inPalette.paletteCategory, binding.inPalette.paletteCategory.option)
            }
            activity?.intent = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray(INT_DATA, intArrayOf(binding.inPalette.paletteCategory.index, setting.ordinal, regularity.ordinal))
    }

    override fun onDestroyView() {
        binding.screenView.setUri(null)
        super.onDestroyView()
    }
}