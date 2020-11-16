package com.moriak.schednote.fragments.of_schedule

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.design.PaletteStorage
import com.moriak.schednote.design.ScheduleIllustrator
import com.moriak.schednote.events.Movement
import com.moriak.schednote.fragments.of_main.SubActivity
import com.moriak.schednote.fragments.of_schedule.DesignEditor.ImgFit.*
import com.moriak.schednote.fragments.of_schedule.DesignEditor.OverSize.*
import com.moriak.schednote.other.Redirection
import com.moriak.schednote.settings.ColorGroup.BACKGROUND
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.style_layout.*
import kotlinx.android.synthetic.main.style_layout.view.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * V tomto fragmente sa upravuje dizajn tabuľky rozvrhu a v ponuke je aj možnosť nastavenia rozvrhu
 * s obrázkom ako pozadie. Ak pomery strán obrázku a zariadenia nie sú rovnaké, tak sú 3 spôsoby
 * zobrazenia obrázku na pozadí rozvrhu. Obrázok aj tabuľku možno posúvať a výsledný obrázok sa dá nastaviť
 * ako pozadie
 */
class DesignEditor : SubActivity(), SchedulePart {
    private companion object {
        private const val IMG_REQUEST = 1000
        private const val NEW_IMG_PERMISSION = 1001
    }

    private enum class HSLPartition { HUE, SATURATION, LUMINANCE, ALPHA }

    /**
     * @property COVER obrázok obsadí celú predlohu a bude orezaný (bez straty, iba nebude vidno kraje)
     * @property CONTAIN obrázok sa budé zmestiť celý, na predlohe budú pásiky s farbou pozadia
     * @property FILL obrázok bude roztiahnutý na celú plochu
     */
    enum class ImgFit {
        COVER, CONTAIN, FILL;

        companion object {
            /**
             * Získanie hodnoty o zobrazení z reťazca
             * @param str reťazec ktorý by sa mal zhodovať s niektorým z názvov tohoto enumu.
             */
            operator fun get(str: String?) = when (str) {
                COVER.name -> COVER
                CONTAIN.name -> CONTAIN
                FILL.name -> FILL
                else -> null
            }
        }
    }

    /**
     * @property WIDTH obrázok sa môže posúvať vľavo a vpravo, keď je pri rovnakej šírke predlohy
     * nižší ako predloha alebo pri rovnakej výške predlohy širší ako predloha
     * @property HEIGHT obrázok sa môže posúvať nahor a nadol, keď je pri rovnakej šírke predlohy
     * vyšší ako predloha alebo pri rovnakej výške predlohy užší ako predloha
     * @property NONE Pomery strán predlohy a obrázka sa rovnajú alebo nie je k dispozícii  obrázok
     */
    enum class OverSize {
        WIDTH, HEIGHT, NONE;

        companion object {
            /**
             * Na základe pomeru strán obrázka sa určí či sú pomery strán rovnaké alebo ktorým smerom
             * je obrázok dlhší ako predloha (x alebo y)
             * @param drawable Grafický prvok
             */
            fun getOverFlow(drawable: Drawable?): OverSize {
                if (drawable?.let { it.intrinsicWidth * it.intrinsicHeight > 0 } != true) return NONE

                val screenRatio = App.w.coerceAtMost(App.h).toFloat() / App.w.coerceAtLeast(App.h)
                val drawableRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
                return when {
                    abs(screenRatio - drawableRatio) < 0.001F -> NONE
                    drawableRatio < screenRatio -> HEIGHT
                    else -> WIDTH
                }
            }
        }
    }

    /**
     * Trieda pracuje s obrázkom na pozadí
     */
    inner class IMGCoordinates {
        private var drawable: Drawable? = null
        private var imgFit: ImgFit? = null
        private val frmW = App.w.coerceAtMost(App.h)
        private val frmH = App.w.coerceAtLeast(App.h)
        private val origW get() = drawable?.intrinsicWidth ?: 0
        private val origH get() = drawable?.intrinsicHeight ?: 0
        private var x = 0F
        private var y = 0F
        private var boundsX = 0F..0F
        private var boundsY = 0F..0F
        private var imgW = 0
        private var imgH = 0

        /**
         * Získanie informácie, o tom, či je obrázok, roztiahnutý, orezaný alebo natlačený
         */
        fun getFit() = imgFit

        /**
         * Zmena obrázka
         * @param iv objekt ktorý má obrázok zobraziť
         * @param uri odkaz na obrázok, ktorý má byť zobrazený
         */
        fun setUri(iv: ImageView?, uri: Uri?) {
            iv ?: return
            if (PackageManager.PERMISSION_GRANTED == activity?.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                try {
                    iv.setImageURI(uri)
                } catch (ex: SecurityException) {
                    iv.setImageURI(null)
                    App.toast(R.string.error_loading_image, Gravity.CENTER, Toast.LENGTH_LONG)
                } finally {
                    drawable = iv.drawable
                    Prefs.states.bgImage = uri?.toString()
                }
            } else {
                iv.setImageURI(null)
                drawable = null
                Prefs.states.bgImage = null
            }
            computeCoordinates(imgFit ?: Prefs.states.bgImageFit ?: FILL)
        }

        /**
         * Uložiť pozíciu obrázku
         * @param img objekt ktorého pozíciu si treba zapamätať
         */
        fun rememberCoordinates(img: ImageView?) {
            x = img?.x ?: x
            y = img?.y ?: y
        }

        private fun fill() {
            imgW = frmW
            imgH = frmH
            boundsX = 0F..0F
            boundsY = 0F..0F
        }

        private fun scaleByWidth() {
            imgW = frmW
            imgH = origH * frmW / origW
            boundsX = 0F..0F
            boundsY = when {
                abs(imgH - frmH) < 0.001F -> {
                    imgH = frmH
                    0F..0F
                }
                imgH < frmH -> 0F..(frmH - imgH).toFloat()
                else -> frmH - imgH.toFloat()..0F
            }
        }

        private fun scaleByHeight() {
            imgH = frmH
            imgW = origW * frmH / origH
            boundsX = when {
                abs(imgW - frmW) < 2F -> {
                    imgW = frmW
                    0F..0F
                }
                imgW < frmW -> 0F..(frmW - imgW).toFloat()
                else -> frmW - imgW.toFloat()..0F
            }
            boundsY = 0F..0F
        }

        /**
         * Vypočitanie nových súradníc obrázka po akejsi zmene
         */
        fun computeCoordinates(fit: ImgFit? = imgFit) {
            imgFit = if (fit == null || drawable == null || origW * origH == 0) null else fit
            if (imgFit == null) {
                drawable = null
                Prefs.states.bgImage = null
            }
            Prefs.states.bgImageFit = imgFit

            val overFlow = OverSize.getOverFlow(drawable)
            when (imgFit) {
                COVER -> when (overFlow) {
                    WIDTH -> scaleByHeight()
                    HEIGHT -> scaleByWidth()
                    else -> fill()
                }
                CONTAIN -> when (overFlow) {
                    WIDTH -> scaleByWidth()
                    HEIGHT -> scaleByHeight()
                    else -> fill()
                }
                FILL, null -> fill()
            }

            x = if (imgFit == COVER && overFlow == WIDTH || imgFit == CONTAIN && overFlow == HEIGHT)
                if (Prefs.states.bgImagePos in boundsX) Prefs.states.bgImagePos
                else (boundsX.start + boundsX.endInclusive) / 2
            else ((boundsX.start + boundsX.endInclusive) / 2)

            y = if (imgFit == CONTAIN && overFlow == WIDTH || imgFit == COVER && overFlow == HEIGHT)
                if (Prefs.states.bgImagePos in boundsY) Prefs.states.bgImagePos
                else (boundsY.start + boundsY.endInclusive) / 2
            else ((boundsY.start + boundsY.endInclusive) / 2)
        }

        /**
         * Aplikovať spočítané súradnice na obrázok [imageView]
         * @param imageView
         */
        fun applyCoordinates(imageView: ImageView?) {
            imageView?.x = x
            imageView?.y = y
        }

        /**
         * Aplikovať spočítané rozmery na obrázok [imageView]
         * @param imageView
         */
        fun applySize(imageView: ImageView?) {
            imageView ?: return
            val param = FrameLayout.LayoutParams(imgW, imgH)
            imageView.layoutParams = param
            imageMovement.setBoundsX(boundsX)
            imageMovement.setBoundsY(boundsY)
        }
    }

    private val palette = Prefs.settings.getColor(BACKGROUND)
    private lateinit var illustrator: ScheduleIllustrator

    private val keys = PaletteStorage.keys()
    private var colorKeyIndex = 0

    private val switchColors = View.OnClickListener {
        colorKeyIndex += it.tag as Int
        colorKeyIndex %= keys.size
        if (colorKeyIndex < 0) colorKeyIndex += keys.size
        refreshColorValues()
    }
    private val recolor = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val valueViewer = seekBar!!.tag as TextView
            valueViewer.text = seekBar.progress.toString()
            when (valueViewer.tag as HSLPartition) {
                HSLPartition.HUE -> palette.hue(seekBar.progress)
                HSLPartition.SATURATION -> palette.saturation(seekBar.progress)
                HSLPartition.LUMINANCE -> palette.luminance(seekBar.progress)
                HSLPartition.ALPHA -> palette.alpha(seekBar.progress)
            }
            illustrator.recolor(keys[colorKeyIndex], palette)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) =
            illustrator.storeColor(keys[colorKeyIndex])
    }
    private val changeImgFit = RadioGroup.OnCheckedChangeListener { group, checkedId ->
        val fit = (group.findViewById(checkedId) as RadioButton?)?.tag as ImgFit?
        imageCoordinates.computeCoordinates(fit)
        if (Prefs.states.bgImageFit == null && fit != null) {
            group.clearCheck()
            App.toast(R.string.no_image_present)
        } else view?.bg_image?.let { img ->
            if (fit == null && Prefs.states.bgImageFit != null) imageCoordinates.setUri(img, null)
            img.post {
                imageCoordinates.applySize(img)
                img.post { imageCoordinates.applyCoordinates(img) }
            }
        }
    }
    private val setAsWallpaper = View.OnClickListener {
        view?.screen?.let { v ->
            AlertDialog
                .Builder(requireContext())
                .setTitle(R.string.info)
                .setMessage(R.string.before_set_as_wallpaper)
                .setNegativeButton(R.string.abort, fun(_, _) = Unit)
                .setPositiveButton(R.string.confirm, fun(_, _) {
                    // odlozim sucasne popredie aby nezavadzalo
                    val foreground = v.foreground
                    v.foreground = null

                    // prekreslim obsah predlohy
                    val bitmap = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
                    v.draw(Canvas(bitmap))
                    WallpaperManager.getInstance(v.context).setBitmap(bitmap)
                    App.toast(R.string.wallpaper_set_success)

                    // obnovim pouzite popredie
                    v.foreground = foreground
                })
                .create().show()
        }
    }
    private val setImage = View.OnClickListener {
        if (activity?.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                NEW_IMG_PERMISSION
            )
        else attemptToPickImage()
    }
    private val unsetImage = View.OnClickListener {
        view?.radios?.clearCheck()
        it.visibility = View.GONE
        bg_image.setImageURI(null)
    }

    private val imageCoordinates = IMGCoordinates()
    private val tableMovement: Movement = Movement()
    private val imageMovement: Movement = Movement()

    init {
        tableMovement.setOnMoveEnd { view?.let { Prefs.states.tableY = it.schedule_frame.y } }
        imageMovement.setOnMoveEnd {
            imageCoordinates.rememberCoordinates(view?.bg_image)
            Prefs.states.bgImagePos = view?.bg_image?.let {
                when (imageCoordinates.getFit()) {
                    COVER -> when (OverSize.getOverFlow(it.drawable)) {
                        WIDTH -> it.x
                        HEIGHT -> it.y
                        NONE -> 0F
                    }
                    CONTAIN -> when (OverSize.getOverFlow(it.drawable)) {
                        WIDTH -> it.y
                        HEIGHT -> it.x
                        NONE -> 0F
                    }
                    FILL, null -> 0F
                }
            } ?: 0F
        }
    }

    private fun refreshColorValues() {
        PaletteStorage.getColor(keys[colorKeyIndex], palette)
        view?.color_group?.text = PaletteStorage.getString(keys[colorKeyIndex])
        view?.rangeH?.progress = palette.hue
        view?.rangeS?.progress = palette.saturation
        view?.rangeL?.progress = palette.luminance
        view?.rangeA?.progress = palette.alpha
        view?.hnum?.text = palette.hue.toString()
        view?.snum?.text = palette.saturation.toString()
        view?.lnum?.text = palette.luminance.toString()
        view?.anum?.text = palette.alpha.toString()
    }

    private fun attemptToPickImage() {
        startActivityForResult(Intent(Intent.ACTION_PICK).also { it.type = "image/*" }, IMG_REQUEST)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.design)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        inflater.inflate(R.layout.style_layout, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var key = -BACKGROUND.ordinal
        if (savedInstanceState == null) {
            if (activity?.intent?.action == Redirection.DESIGN.action) {
                key =
                    activity?.intent?.getIntExtra(Redirection.EXTRA_DESIGN_COLOR_GROUP, key) ?: key
                colorKeyIndex = keys.indexOf(key).let { if (it == -1) 0 else it }
                PaletteStorage.getColor(key, palette)
            }
        }

        // pridanie tabulky rozvrhu
        illustrator = ScheduleIllustrator
            .schedule(
                savedInstanceState?.let { if (this::illustrator.isInitialized) illustrator else null },
                false
            )
            .attachTo(view.schedule_frame)
            .involveButtons(view.odd_btn, view.even_btn)
            .background(view.screen)


        //nastavenia tagov
        view.rangeH.tag = view.hnum
        view.rangeS.tag = view.snum
        view.rangeL.tag = view.lnum
        view.rangeA.tag = view.anum
        view.hnum.tag = HSLPartition.HUE
        view.snum.tag = HSLPartition.SATURATION
        view.lnum.tag = HSLPartition.LUMINANCE
        view.anum.tag = HSLPartition.ALPHA
        view.previous_color.tag = -1
        view.next_color.tag = 1
        view.cover.tag = COVER
        view.contain.tag = CONTAIN
        view.fill.tag = FILL

        // nastavenia posluchacov udalosti podla tagov
        view.rangeH.setOnSeekBarChangeListener(recolor)
        view.rangeS.setOnSeekBarChangeListener(recolor)
        view.rangeL.setOnSeekBarChangeListener(recolor)
        view.rangeA.setOnSeekBarChangeListener(recolor)
        view.previous_color.setOnClickListener(switchColors)
        view.next_color.setOnClickListener(switchColors)
        view.wallpaper.setOnClickListener(setAsWallpaper)
        view.radios.setOnCheckedChangeListener(changeImgFit)
        view.schedule_frame.setOnTouchListener(tableMovement)
        view.bg_image.setOnTouchListener(imageMovement)
        view.add_image.setOnClickListener(setImage)
        view.remove_image.setOnClickListener(unsetImage)

        refreshColorValues()

        // nastavenia vzhladu platna
        val w = App.w.coerceAtMost(App.h)
        val h = App.w.coerceAtLeast(App.h)
        view.screen.layoutParams.width = w
        view.screen.layoutParams.height = h
        view.screen.foreground = GradientDrawable().also { it.setStroke(App.dp(4), Color.BLACK) }

        // nastavit obrazok
        imageCoordinates.setUri(view.bg_image, Prefs.states.bgImage?.let { Uri.parse(it) })
        view.remove_image.visibility =
            if (view.bg_image.drawable == null) View.GONE else View.VISIBLE

        // nastavenie obrazku
        imageCoordinates.getFit()
            ?.let { view.radios.findViewWithTag<RadioButton?>(it) }
            ?.let { it.isChecked = true } ?: view.radios.clearCheck()
    }

    override fun onResume() {
        super.onResume()
        // nastavenie rozmeru tabulky

        val v = view ?: return
        val w = App.w.coerceAtMost(App.h)
        val h = App.w.coerceAtLeast(App.h)

        // nastavenia rozmerov a hranic pozicie tabulky s rozvrhom
        val horizontalMargin = w * 20 / 720F // w * 20 / 720
        val verticalMargin = w * 140 / 720F // w * 140 / 720
        v.schedule_frame.layoutParams.width = (w - 2F * horizontalMargin).roundToInt()

        view?.post {
            //nastavenie pomeru zmensenia
            val scale = (v.frame.width.toFloat() / w).coerceAtMost(v.frame.height.toFloat() / h)
            v.screen.scaleX = scale
            v.screen.scaleY = scale
            tableMovement.notifyScale(scale)
            imageMovement.notifyScale(scale)

            // nastavenie hranice posuvania tabulky
            val boundsY = verticalMargin..h - verticalMargin - v.schedule_frame.height
            v.schedule_frame.x = horizontalMargin
            v.schedule_frame.y = Prefs.states.tableY.coerceIn(boundsY)
            tableMovement.setBoundsX(horizontalMargin..horizontalMargin)
            tableMovement.setBoundsY(boundsY)
            illustrator.customizeColumnWidth((w - 2 * horizontalMargin).toInt())
        }
    }

    /**
     * Pytanie si povolenia na pridavanie obrazku
     * @param request Kod ziadosti
     * @param permissions zoznam ziadosti o povolenie
     * @param granted zoznam povoleni
     */
    override fun onRequestPermissionsResult(
        request: Int,
        permissions: Array<out String>,
        granted: IntArray
    ) {
        if (request == NEW_IMG_PERMISSION)
            if (granted.firstOrNull() == PackageManager.PERMISSION_GRANTED) attemptToPickImage()
            else App.toast(R.string.read_storage_permission_for_gallery)
        else super.onRequestPermissionsResult(request, permissions, granted)
    }

    /**
     * Ak fotku nie je mozne pridat, stara zostava
     * @param requestCode kod ziadosti
     * @param resultCode kod vysledku
     * @param result vysledne data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        if (resultCode == Activity.RESULT_OK && requestCode == IMG_REQUEST) {
            imageCoordinates.setUri(view!!.bg_image, result?.data)
            view?.remove_image?.visibility = result?.data?.let { View.VISIBLE } ?: View.GONE
            val fit =
                imageCoordinates.getFit()?.let { view?.radios?.findViewWithTag<RadioButton?>(it) }
            fit?.let { it.isChecked = true } ?: view?.radios?.clearCheck()
        }
    }
}