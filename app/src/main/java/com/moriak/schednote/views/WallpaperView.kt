package com.moriak.schednote.views

import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_LOCK
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import androidx.annotation.FloatRange
import com.moriak.schednote.views.WallpaperView.ImageAngle.*
import com.moriak.schednote.views.WallpaperView.ImageFit.*
import com.moriak.schednote.views.WallpaperView.OverSize.*
import com.moriak.schednote.views.WallpaperView.Touch.Moving.*
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Úlohou tohto bloku je poskytnúť náhľad, ako bude vyzerať tapeta v mobilnom zariadení, pokiaľ
 * sa ju užívateľ rozhodne nastaviť.
 *
 * Pozadie ktoré nastaví táto aplikácia a zobrazuje tento blok sa skladá z niekoľkých vrstiev:
 * 1. Farba pozadia (najspodnejšia vrstva) - pôvodne biela
 * 2. Obrázok pozadia (voliteľný) - možno ho posúvať buď vertikálne alebo horizontálne, podľa
 * nastavenia prispôsobenia obrázka plátnu a ich pomerov strán.
 * 3. Rozvrh hodín - možno ho posúvať iba vertikálne
 * 4. Okolie plátna (najvrchnejšia vrstva) - plátno má rovnaký pomer strán ako displej zariadenia
 * a jeho okolie je priestvitná bielá farba ktorej účelom je čiastočne prekryť to, čo na pozadí
 * tapete vidno nebude.
 *
 * Blok bude vždy orientovaný na výšku. Je interaktívny tým, že používateľ môže posúvať obrázok
 * pozadia a rozvrh.
 *
 * @property schPercent - percentuálna pozícia rozvrhu hodín (vertikálna pozícia)
 * @property imgPercent - percentuálna pozícia obrázku pozadia (vertikálna pozícia | horizontálna pozícia)
 *
 */
class WallpaperView : ScheduleView {
    private enum class OverSize { WIDTH, HEIGHT, NONE }

    /**
     * Označenie pre rozloženie obrázku na plátne.
     * [COVER] - orezanie obrázku (dodržanie pomeru strán orázku)
     * [CONTAIN] - zmestenie obrázku (dodržanie pomeru strán orázku)
     * [FILL] - roztiahnutie obrázku (transformácia pomeru strán orázku)
     */
    enum class ImageFit { COVER, CONTAIN, FILL }

    /**
     * Označenie rotácie obrázku na plátne.
     * [ANGLE_360] žiadna rotácia obrázku
     * [ANGLE_90] obrázok otočený vpravo
     * [ANGLE_180] obrázok hore nohami
     * [ANGLE_270] obrázok otočený vľavo
     * @property left Uhol obrázku po otočení obrázku o 90 stupňov doľava
     * @property right Uhol obrázku po otočení obrázku o 90 stupňov doprava
     */
    enum class ImageAngle {
        ANGLE_360, ANGLE_90, ANGLE_180, ANGLE_270;
        val left get() = when(this) {
            ANGLE_360 -> ANGLE_270
            ANGLE_270 -> ANGLE_180
            ANGLE_180 -> ANGLE_90
            ANGLE_90 -> ANGLE_360
        }
        val right get() = when(this) {
            ANGLE_360 -> ANGLE_90
            ANGLE_90 -> ANGLE_180
            ANGLE_180 -> ANGLE_270
            ANGLE_270 -> ANGLE_360
        }
    }

    private class ImageIllustrator(
        private val context: Context,
        private var frmW: Int,
        private var frmH: Int
    ) {
        private var imgUri: Uri? = null
        private var bmp: Bitmap? = null
        private var percent = 0F

        private var boundsX = 0..0
        private var boundsY = 0..0
        private var imgX = 0
        private var imgY = 0
        private var imgW = 0
        private var imgH = 0
        private val rect by lazy { Rect() }

        private val rotator by lazy { Matrix() }
        private val bmpOpt by lazy { BitmapFactory.Options() }
        private val bmpW get() = bmp?.width ?: 0
        private val bmpH get() = bmp?.height ?: 0

        var rotation = ANGLE_360; set(value) {
            field = value
            if (imgUri != null) {
                bmp = uriToBitmap(imgUri)
                customizeImage()
            }
        }
        var fit: ImageFit = COVER; set(value) {
            val old = field
            field = value
            if (old != value) customizeImage()
        }
        var uri get() = imgUri; set(value) {
            if (value != uri) {
                bmp?.recycle()
                bmp = uriToBitmap(value)
                imgUri = bmp?.let { value }
                if (bmp != null) customizeImage()
            }
        }

        private fun customizeImage() {
            val overSize = if (bmpW < 1 || bmpH < 1 || frmW < 1 || frmH < 1) NONE
            else {
                val ratioW = bmpW.toFloat() / frmW
                val ratioH = bmpH.toFloat() / frmH
                when {
                    abs(ratioW - ratioH) < 0.001F -> NONE
                    ratioW > ratioH -> WIDTH
                    else -> HEIGHT
                }
            }

            boundsX = 0..0
            boundsY = 0..0
            imgX = 0
            imgY = 0
            imgW = frmW
            imgH = frmH

            // umoznit posun po sirke
            if (fit == COVER && overSize == WIDTH || fit == CONTAIN && overSize == HEIGHT) {
                imgW = bmpW * frmH / bmpH
                boundsX = (if (fit == COVER) frmW - imgW .. 0 else 0 .. frmW - imgW)
                imgX = ((boundsX.count() - 1) * percent + boundsX.first).roundToInt()
            }
            // umoznit posun po vyske
            else if (fit == CONTAIN && overSize == WIDTH || fit == COVER && overSize == HEIGHT) {
                imgH = bmpH * frmW / bmpW
                boundsY = (if (fit == COVER) frmH - imgH .. 0 else 0 .. frmH - imgH)
                imgY = ((boundsY.count() - 1) * percent + boundsY.first).roundToInt()
            }

            if (bmp != null && (imgW > bmpW && bmpW < bmpOpt.outWidth || imgH > bmpH
                        && bmpH < bmpOpt.outHeight)) {
                bmp = uriToBitmap(imgUri)
            }
        }

        private fun stream(uri: Uri?) = try {
            uri?.let(context.contentResolver::openInputStream)
        } catch (e: Exception) { null }

        private fun decode(stream: InputStream) = try {
            BitmapFactory.decodeStream(stream, null, bmpOpt)
        } catch (e: Exception) { null }

        fun uriToBitmap(uri: Uri?): Bitmap? {
            if (uri == null) return null
            var input = stream(uri) ?: return null
            bmpOpt.inSampleSize = 1
            bmpOpt.inJustDecodeBounds = true
            decode(input)
            bmpOpt.inSampleSize = if (frmW >= 1 && frmH >= 1) {
                val imgW = if (rotation.ordinal % 2 == 0) bmpOpt.outWidth else bmpOpt.outHeight
                val imgH = if (rotation.ordinal % 2 == 1) bmpOpt.outWidth else bmpOpt.outHeight
                val real = imgW / imgH.toFloat()
                val frm = frmW / frmH.toFloat()
                when (fit) {
                    COVER -> if (real > frm) imgH / frmH else imgW / frmW
                    CONTAIN -> if (real > frm) imgW / frmW else imgH / frmH
                    FILL -> imgW / frmW
                }
            } else bmpOpt.outWidth.coerceAtLeast(bmpOpt.outHeight)
            bmpOpt.inJustDecodeBounds = false
            input.close()
            input = stream(uri) ?: return null
            val bmp = decode(input)
            input.close()
            return rotate(bmp)
        }

        private fun rotate(bmp: Bitmap?): Bitmap? {
            if (bmp == null || rotation == ANGLE_360) return bmp
            rotator.setRotate(rotation.ordinal * 90F)
            return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, rotator, true)
                .also { if (bmp !== it) bmp.recycle() }
        }

        fun resize(width: Int, height: Int) {
            frmW = width
            frmH = height
            bmp = imgUri?.let(this::uriToBitmap)
            customizeImage()
        }

        fun setPercent(@FloatRange(from = 0.0, to = 1.0) p: Float) {
            percent = p
            imgX = ((boundsX.count() - 1) * percent).roundToInt() + boundsX.first
            imgY = ((boundsY.count() - 1) * percent).roundToInt() + boundsY.first
        }

        fun getPercent() = percent

        fun moveImage(dx: Float, dy: Float): Float {
            if (boundsX != 0..0) {
                imgX = (imgX + dx.roundToInt()).coerceIn(boundsX)
                percent = (imgX - boundsX.first).toFloat() / (boundsX.last - boundsX.first)
            }
            else if (boundsY != 0..0) {
                imgY = (imgY + dy.roundToInt()).coerceIn(boundsY)
                percent = (imgY - boundsY.first).toFloat() / (boundsY.last - boundsY.first)
            }
            return percent
        }

        fun draw(canvas: Canvas, x: Int, y: Int) {
            bmp?.let {
                rect.left = x + imgX
                rect.top = y + imgY
                rect.right = rect.left + imgW
                rect.bottom = rect.top + imgH
                canvas.drawBitmap(it, null, rect,null)
            }
        }

        fun copy(other: ImageIllustrator) {
            uri = null
            fit = other.fit
            rotation = other.rotation
            percent = other.percent
            uri = other.uri
        }
    }

    private class WallpaperIllustrator(
        context: Context,
        private var scrW: Int,
        private var scrH: Int,
        private var sch: ScheduleIllustrator
    ) {
        private val rect by lazy { Rect() }
        private val img = ImageIllustrator(context, scrW, scrH)
        private val bgColor = Paint()
        private var percent = 0F
            set(value) { field = if (value.isFinite()) value.coerceIn(0F..1F) else 0F }

        private var bounds = 0..0
        val frmW get() = scrW
        val frmH get() = scrH
        var uri: Uri? get() = img.uri
            set(value) { img.uri = value }
        var fit: ImageFit get() = img.fit
            set(value) { img.fit = value }
        var imgRotation get() = img.rotation
            set(value) { img.rotation = value }

        init {
            resize(scrW, scrH)
        }

        fun resize(width: Int, height: Int) {
            scrW = width
            scrH = height
            img.resize(width, height)
            sch.scaleScheduleByWidth((width * 0.9F).roundToInt())
            bounds = (scrW * 0.15F).toInt().let { it .. scrH - it - sch.schH }
            sch.schL = (scrW - sch.schW) / 2
            sch.schT = ((bounds.count() - 1) * percent).roundToInt() + bounds.first
        }

        fun setSchPercent(@FloatRange(from = 0.0, to = 1.0) p: Float) {
            percent = p
            sch.schT = ((bounds.count() - 1) * p).roundToInt() + bounds.first
        }

        fun getSchPercent() = percent

        fun setImgPercent(@FloatRange(from = 0.0, to = 1.0) p: Float) = img.setPercent(p)

        fun getImgPercent() = img.getPercent()

        fun moveSchedule(dy: Float): Float {
            sch.schT = (sch.schT + dy.roundToInt()).coerceIn(bounds)
            val count = bounds.count()
            percent = if (count > 1) (sch.schT - bounds.first) / (count - 1F) else 0F
            return percent
        }

        fun moveImage(dx: Float, dy: Float) = img.moveImage(dx, dy)

        fun r(x: Int, y: Int, w: Int, h: Int): Rect = rect.apply {
            left = x
            top = y
            right = x + w
            bottom = y + h
        }

        fun draw(canvas: Canvas, x: Int = 0, y: Int = 0) {
            bgColor.color = sch.getTypeColor(BACKGROUND_COLOR)?.first ?: Color.WHITE
            canvas.drawRect(r(x, y, scrW, scrH), bgColor)
            img.draw(canvas, x, y)
            sch.schL += x
            sch.schT += y
            sch.drawSchedule(canvas)
            sch.schL -= x
            sch.schT -= y
        }

        fun drawAround(canvas: Canvas, x: Int, y: Int, viewW: Int, viewH: Int) {
            bgColor.color = Color.argb(197, 255, 255, 255)
            canvas.drawRect(r(0, y, x, scrH), bgColor)
            canvas.drawRect(r(x, 0, scrW, y), bgColor)
            canvas.drawRect(r(x + scrW, y, viewW - (x + scrW), scrH), bgColor)
            canvas.drawRect(r(x, y + scrH, scrW, viewH - (y + scrH)), bgColor)

        }

        fun copy (other: WallpaperIllustrator) {
            img.copy(other.img)
            sch.copy(other.sch)
            sch.schT = sch.schH * other.sch.schT / other.sch.schH
        }
    }

    private object Touch: OnTouchListener {
        private enum class Moving { SCHEDULE, IMAGE, NOTHING }
        var oldX = 0F
        var oldY = 0F
        var moving = NOTHING

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            v as WallpaperView
            when (event!!.action) {
                ACTION_DOWN -> {
                    moving = NOTHING
                    oldX = event.x
                    oldY = event.y
                }
                ACTION_MOVE -> {
                    if (moving == NOTHING) moving = if (v.schedule.isSchedule(oldX, oldY,
                            (v.width - v.viewIllustrator.frmW) / 2)) SCHEDULE else IMAGE
                    // posuvanie rozvrhu
                    if (moving == SCHEDULE) v.viewIllustrator.moveSchedule(event.y - oldY)
                    else if (moving == IMAGE) v.viewIllustrator.moveImage(event.x - oldX, event.y - oldY)
                    if (moving != NOTHING) v.invalidate()
                    oldX = event.x
                    oldY = event.y
                }
                ACTION_UP -> {
                    when (moving) {
                        NOTHING -> v.performClick()
                        SCHEDULE -> v.screenWatcher?.onScheduleMoved(v.viewIllustrator.getSchPercent())
                        IMAGE -> v.screenWatcher?.onImageMoved(v.viewIllustrator.getImgPercent())
                    }
                    moving = NOTHING
                }
            }
            return true
        }

    }

    /**
     * Poslúchač špeciálnych udalostí bloku [WallpaperView]
     */
    interface ScheduleScreenWatcher {
        /**
         * Funkcia sa vykoná po každej zmene pozície rozvrhu
         * @param percent nová pozícia rozvrhu vyjadrená v percentách od 0 po 1.
         */
        fun onScheduleMoved(percent: Float)

        /**
         * Funkcia sa vykoná po každej zmene pozície obrázku
         * @param percent nová pozícia rozvrhu vyjadrená v percentách od 0 po 1.
         */
        fun onImageMoved(percent: Float)

        /**
         * Funkcia sa vykoná po každom neúspešnom pokuse o nastavenie obrázku na pozadie plátna.
         */
        fun onImageLoadError()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    private val viewIllustrator = WallpaperIllustrator(context, 0, 0, schedule)
    private val screenIllustrator by lazy {
        val sch = ScheduleIllustrator(schedule, dp(1F))
        sch.scaleScheduleByWidth((dimShorter * 0.15F).roundToInt())
        WallpaperIllustrator(context, dimShorter, dimLonger, sch)
    }

    private val destRect = Rect()
    private val border = Paint()

    var schPercent = 0F; set(value) {
        field = if (value.isFinite()) value.coerceIn(0F..1F) else 0F
        viewIllustrator.setSchPercent(value)
        invalidate()
    }
    var imgPercent = 0.5F; set(value) {
        field = if (value.isFinite()) value.coerceIn(0F..1F) else 0F
        viewIllustrator.setImgPercent(value)
        invalidate()
    }

    private var screenWatcher: ScheduleScreenWatcher? = null

    init {
        setOnTouchListener(Touch)
        border.color = Color.BLACK
        border.style = Paint.Style.STROKE
        border.strokeWidth = dp(0.5F)
        border.strokeJoin = Paint.Join.ROUND
        border.strokeCap = Paint.Cap.ROUND
    }

    /**
     * Nastaví objekt ktorého úlohou bude reagovať na interakcie užívateľa
     * s obrázkom a rozvrhom v bloku [WallpaperView].
     * @param l pozorovateľ interakcií užívateľa s časťami bloku [WallpaperView]
     */
    fun addScreenWatcher(l: ScheduleScreenWatcher?) { screenWatcher = l }

    /**
     * Nastaví obrázok pozadia z URI adresy. Pre správne fungovanie musí URI adresa
     * ukazovať na obrázok.
     * @param uri Adresa obrázku
     */
    fun setUri(uri: Uri?) {
        viewIllustrator.uri = uri
        invalidate()
    }

    /**
     * Nastaví a vráti uhol obrázku na pozadí
     * @param rotation uhol (násobky 90 deg)
     * @return nastavená rotácia
     */
    fun setBgRotation(rotation: ImageAngle) = rotation.also{
        viewIllustrator.imgRotation = rotation
        invalidate()
    }

    /**
     * Obrázok na pozadí sa otočí doľava
     */
    fun bgRotateLeft() = setBgRotation(viewIllustrator.imgRotation.left)

    /**
     * Obrázok na pozadí sa otočí doprava
     */
    fun bgRotateRight() = setBgRotation(viewIllustrator.imgRotation.right)

    /**
     * nastaví rozloženie obrázu
     * @param pFit rozloženie obrázku na plátne
     */
    fun setFit(pFit: ImageFit) {
        viewIllustrator.fit = pFit
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val realW = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd
        val realH = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
        setMeasuredDimension(realW, realH)
        val ratio = (realW / dimShorter.toFloat()).coerceAtMost(realH / dimLonger.toFloat())
        val w = (dimShorter * ratio).toInt()
        val h = (dimLonger * ratio).toInt()
        viewIllustrator.resize(w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas ?: return
        val x = (width - viewIllustrator.frmW) / 2
        val y = (height - viewIllustrator.frmH) / 2
        viewIllustrator.draw(canvas, x, y)
        viewIllustrator.drawAround(canvas, x, y, width, height)
        destRect.left = x
        destRect.top = y
        destRect.right = x + viewIllustrator.frmW
        destRect.bottom = y + viewIllustrator.frmH
        canvas.drawRect(destRect, border)
    }

    /**
     * Nakreslenie bitmapy, ktorá bude použiteľná ako pozadie
     */
    fun getWallpaperBitmap(): Bitmap {
        screenIllustrator.copy(viewIllustrator)
        val map = Bitmap.createBitmap(dimShorter, dimLonger, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(map)
        screenIllustrator.draw(canvas, 0, 0)
        return map
    }

    /**
     * Nastaví bitmapu ako tapetu
     * @param bmp bitmapa
     */
    fun setAsWallpaper(bmp: Bitmap, flag: Int = FLAG_LOCK or FLAG_SYSTEM) =
        WallpaperManager.getInstance(context).setBitmap(bmp, null, true, flag)
}