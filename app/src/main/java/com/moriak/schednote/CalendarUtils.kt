package com.moriak.schednote

import androidx.annotation.IntRange
import com.moriak.schednote.enums.Day
import com.moriak.schednote.enums.Regularity
import com.moriak.schednote.enums.WorkWeek
import java.util.*
import java.util.Calendar.*


val Calendar.now: Calendar get() = also { timeInMillis = System.currentTimeMillis() }

var Calendar.dayMinutes get() = get(HOUR_OF_DAY) * 60 + get(MINUTE)
    set(value) {
        set(HOUR_OF_DAY, value / 60)
        setClean(MINUTE, value % 60)
    }

val Calendar.nextMidnight: Long get() {
    setClean(DAY_OF_YEAR, get(DAY_OF_YEAR) + 1)
    return timeInMillis
}

val Calendar.today: LongRange get() {
    val st = timeInMillis
    val en = nextMidnight
    return st..en
}

val Calendar.tomorrow: LongRange get() {
    val st = nextMidnight
    val en = nextMidnight
    return st..en
}

val Calendar.forWeek: LongRange get() {
    val st = timeInMillis
    add(DAY_OF_YEAR, 7)
    return st..timeInMillis
}

val Calendar.forMonth: LongRange get() {
    val st = timeInMillis
    setClean(MONTH, get(MONTH) + 1)
    return st..timeInMillis
}

/**
 * Po nastavení danej časovej jednotky sa všetky menšie
 * časové jednotky nastavia na najmenšiu možnú hodnotu
 * @param field
 * @param value
 */
fun Calendar.setClean(field: Int, value: Int) {
    set(field, value)
    when (field) {
        YEAR -> setClean(DAY_OF_YEAR, 1)
        MONTH -> setClean(DAY_OF_MONTH, 1)
        DAY_OF_YEAR, DAY_OF_MONTH, DAY_OF_WEEK, DAY_OF_WEEK_IN_MONTH -> setClean(HOUR_OF_DAY, 0)
        HOUR_OF_DAY, HOUR -> setClean(MINUTE, 0)
        MINUTE -> setClean(SECOND, 0)
        SECOND -> set(MILLISECOND, 0)
    }
}

/**
 * @see [Calendar.getNextTime]
 * @param day Deň v týždni
 * @param ww Pracovný týždeň. Predvolený je [WorkWeek.MON_FRI].
 */
fun Calendar.getNextDay(day: Day, ww: WorkWeek = WorkWeek.MON_FRI): Long = getNextDay(day, Regularity.EVERY, ww)

/**
 * @see [Calendar.getNextTime]
 * @param day Deň v týždni
 * @param reg Pravidelnosť - Každý týždeň alebo každý druhý týždeň (párny / nepárny)
 * @param ww Pracovný týždeň. Predvolený je [WorkWeek.MON_FRI].
 */
fun Calendar.getNextDay(day: Day, reg: Regularity, ww: WorkWeek): Long = getNextTime(day, reg, ww, dayMinutes)

/**
 * @param day Deň v týždni
 * @param reg Pravidelnosť - Každý týždeň alebo každý druhý týždeň (párny / nepárny)
 * @param ww Pracovný týždeň. Predvolený je [WorkWeek.MON_FRI].
 * @param dMin minuty dňa od 0 do 1439
 */
fun Calendar.getNextTime(day: Day, reg: Regularity, ww: WorkWeek, @IntRange(from = 0, to = 1439) dMin: Int): Long {
    add(DAY_OF_YEAR, Day[get(DAY_OF_WEEK)].untilNextTime(dayMinutes, day, dMin))
    if (reg != Regularity.EVERY && getRegularity(ww) != reg) add(DAY_OF_YEAR, 7)
    dayMinutes = dMin
    return timeInMillis
}

/**
 * Pre 2-týždňový rozvrh je hodnota [dualWeek] nastavená na true a podľa nastavenia
 * pracovného týždňa vráti [Regularity.EVEN], ak je týždeň párny alebo [Regularity.ODD],
 * ak je nepárny. Pre 1-týždňový rozvrh vráti vždy [Regularity.EVERY].
 *
 * @param ww Pracovný týždeň - nie všade začína v Pondelok a končí v Piatok
 * @param dualWeek true - 2-týždňový rozvrh, false 1-týždenný rozvrh
 * @return [Regularity.EVEN], ak je tento týždeň párny, inak [Regularity.ODD]
 */
fun Calendar.getRegularity(ww: WorkWeek, dualWeek: Boolean): Regularity =
    if (dualWeek) getRegularity(ww) else Regularity.EVERY

/**
 * Podľa nastavenia pracovného týždňa vráti [Regularity.EVEN], ak je týždeň párny alebo
 * [Regularity.ODD], ak je nepárny.
 *
 * V tejto aplikácii sa považuje začiatok víkendu za začiatok týždňa.
 *
 * @param ww Pracovný týždeň - nie všade začína v Pondelok a končí v Piatok
 * @return [Regularity.EVEN], ak je tento týždeň párny, inak [Regularity.ODD]
 */
fun Calendar.getRegularity(ww: WorkWeek): Regularity {
    val millis = timeInMillis
    set(DAY_OF_YEAR, 1)
    getNextDay(ww.workDays.first(), ww)
    val first = (get(DAY_OF_YEAR) - ww.weekend.size).let { if (it < 1) it + 7 else it }
    timeInMillis = millis

    return Regularity.values()[((get(DAY_OF_YEAR) - first) / 7) % 2]
}