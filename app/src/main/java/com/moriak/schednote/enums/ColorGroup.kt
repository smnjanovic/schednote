package com.moriak.schednote.enums

import android.content.Context
import androidx.annotation.StringRes
import com.moriak.schednote.Palette
import com.moriak.schednote.storage.Prefs.Settings.getColor
import com.moriak.schednote.R.string.*
import com.moriak.schednote.enums.ColorGroup.*
import com.moriak.schednote.interfaces.IColorGroup
import com.moriak.schednote.views.ScheduleView.Companion.BACKGROUND_COLOR
import com.moriak.schednote.views.ScheduleView.Companion.FREE_COLOR
import com.moriak.schednote.views.ScheduleView.Companion.HEADER_COLOR

/**
 * Pri nastavovaní dizajnu rozvrhu okrem buniek rôznych typov hodín možno upravovať farby aj iných prvkov.
 * [BACKGROUND] pozadie plátna
 * [HEADER] hlavička tabuľky rozvrhu
 * [FREE] bloky voľného času v rozvrhu
 */
enum class ColorGroup(
    override val id: Int,
    override val color: Palette,
    @StringRes private val res: Int
): IColorGroup {
    BACKGROUND(BACKGROUND_COLOR, Palette(), background),
    HEADER(HEADER_COLOR, Palette(((Math.random() * 0xFFFFFFFF).toLong() or 0xB2000000).toInt()), header),
    FREE(FREE_COLOR, Palette(0, 0, 0, 35), free_time);

    init { getColor(this, color) }

    /**
     * získa popis farebnej skupiny
     * @param context
     * @return Popis farebnej skupiny
     */
    fun describe(context: Context): String = context.getString(res)

    operator fun compareTo(other: IColorGroup): Int = if (other is ColorGroup) other.id - id else -1
}