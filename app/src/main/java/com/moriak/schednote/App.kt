package com.moriak.schednote

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Trieda aplikácie, má za úlohy zjednotušiť prístup k resourcom a kontextu odkiaľkoľvek a
 * poskytuje často žiadané hodnoty, aby sa zabránilo viacnásobnému písaniu rovnakého kódu
 */
class App : Application() {
    /**
     * @property ctx Kontext
     */
    companion object {
        private var aCtx: Context? = null
        private var aToast: Toast? = null
        val ctx: Context get() = aCtx!!

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
         * @param longer Trvanie zobrazovania Toastu. True = dlhsie
         */
        fun toast(msg: CharSequence, longer: Boolean = false) {
            val duration = if (longer) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            if (aToast == null) aToast = Toast.makeText(ctx, msg, duration)
            else {
                aToast!!.setText(msg)
                aToast!!.duration = duration
            }
            aToast!!.show()
        }

        /**
         * Zobrazí toast
         * @param res Zdroj reťazca na zobrazenie
         * @param longer Trvanie zobrazovania Toastu. True = dlhsie
         */
        fun toast(@StringRes res: Int, longer: Boolean = false) {
            val duration = if (longer) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            if (aToast == null) aToast = Toast.makeText(ctx, res, duration)
            else {
                aToast!!.setText(res)
                aToast!!.duration = duration
            }
            aToast!!.show()
        }

    }

    override fun onCreate() {
        super.onCreate()
        aCtx = baseContext
        aCtx!!.setTheme(R.style.AppTheme)
    }
}