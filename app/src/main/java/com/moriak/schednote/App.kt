package com.moriak.schednote

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import com.moriak.schednote.App.Companion.cal
import com.moriak.schednote.App.Companion.ctx
import com.moriak.schednote.App.Companion.data
import com.moriak.schednote.App.Companion.h
import com.moriak.schednote.App.Companion.now
import com.moriak.schednote.App.Companion.res
import com.moriak.schednote.App.Companion.w
import com.moriak.schednote.database.Database
import java.util.*
import kotlin.math.roundToInt

/**
 * Trieda aplikácie, má za úlohy zjednotušiť prístup k resourcom a kontextu odkiaľkoľvek a
 * poskytuje často žiadané hodnoty, aby sa zabránilo viacnásobnému písaniu rovnakého kódu
 *
 * @property ctx Kontext
 * @property res Zdroje
 * @property data Prístup k SQLite databáze
 * @property cal Znovupoužiteľný kalendár, ktorý je výhodné využiť napr. v cykloch alebo v situáciach,
 * kedy je riziko vytvárania nadbytočného množstva inštancií
 * @property now Vracia atribút [cal], ktorého hodnota bola nastavená na súčasnosť
 * @property w Šírka zariadenia
 * @property h Výška zariadenia
 */
class App : Application() {
    companion object {
        private var context: Context? = null
        private var aToast: Toast? = null
        private val metrics
            get() = Point().also {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= 30) ctx.display?.getRealSize(it)
                else (ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(
                    it
                )
            }

        val ctx: Context get() = context!!
        val res: Resources get() = ctx.resources
        val data: Database by lazy { Database() }
        val cal: Calendar = Calendar.getInstance()
        val now get() = cal.apply { timeInMillis = System.currentTimeMillis() }
        val w: Int get() = metrics.x
        val h: Int get() = metrics.y

        /**
         * Výpis do logu
         * @param msg Vypíše zreťazenú hodnotu do logu
         * @return Vráti to isté čo vracia hodnota [Log.d]
         * @see Log.d
         */
        fun log(msg: Any?) = Log.d("moriak", "$msg")

        /**
         * Zobrazí toast
         * @param msg Textová správa
         * @param gravity Výber umiestnenia Toastu
         * @param duration Trvanie zobrazovania Toastu
         */
        fun toast(msg: String, gravity: Int = Gravity.BOTTOM, duration: Int = Toast.LENGTH_SHORT) {
            if (aToast == null) aToast = Toast.makeText(ctx, msg, duration) else aToast!!.setText(
                msg
            )
            aToast!!.setGravity(gravity, 0, 0)
            aToast!!.show()
        }

        /**
         * Zobrazí toast
         * @param res Zdroj reťazca na zobrazenie
         * @param gravity Výber umiestnenia Toastu
         * @param duration Trvanie zobrazovania Toastu
         */
        fun toast(
            @StringRes res: Int,
            gravity: Int = Gravity.BOTTOM,
            duration: Int = Toast.LENGTH_SHORT
        ) {
            if (aToast == null) aToast = Toast.makeText(ctx, res, duration) else aToast!!.setText(
                res
            )
            aToast!!.setGravity(gravity, 0, 0)
            aToast!!.show()
        }

        /**
         * Získa reťazec zo zdroja
         * @param resId Zdroj
         * @return [String]
         */
        fun str(@StringRes resId: Int) = ctx.getString(resId)

        /**
         * prevedie hodnotu [dp] z jednotiek dp na pixely
         * @param dp
         * @return hodnota v pixeloch
         *
         */
        fun dp(dp: Int) =
            (dp * (res.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()

    }

    override fun onCreate() {
        super.onCreate()
        context = baseContext
        context!!.setTheme(R.style.AppTheme)
    }
}