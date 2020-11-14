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
import com.moriak.schednote.database.Database
import java.util.*
import kotlin.math.roundToInt

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

        fun log(msg: Any?) = Log.d("moriak", "$msg")
        fun toast(res: String, gravity: Int = Gravity.BOTTOM, duration: Int = Toast.LENGTH_SHORT) {
            if (aToast == null) aToast = Toast.makeText(ctx, res, duration) else aToast!!.setText(
                res
            )
            aToast!!.setGravity(gravity, 0, 0)
            aToast!!.show()
        }

        fun toast(res: Int, gravity: Int = Gravity.BOTTOM, duration: Int = Toast.LENGTH_SHORT) {
            if (aToast == null) aToast = Toast.makeText(ctx, res, duration) else aToast!!.setText(
                res
            )
            aToast!!.setGravity(gravity, 0, 0)
            aToast!!.show()
        }

        fun str(resId: Int) = ctx.getString(resId)

        // premeni zadanu jednotku dp na pixely
        fun dp(dp: Int) =
            (dp * (res.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()

    }

    override fun onCreate() {
        super.onCreate()
        context = baseContext
        context!!.setTheme(R.style.AppTheme)
    }
}