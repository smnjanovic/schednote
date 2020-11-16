package com.moriak.schednote.events

import android.view.MotionEvent
import android.view.View

/**
 * Trieda umoznuje presuvanie 2D objektov po obrazovke, kontroluje dodržanie hraníc
 * posunu po vertikálnej a horizontálnej osi
 *
 * @constructor Vytvorenie dotykovej udalosti, ktorá obmedzí pohyb po ploche
 * @param horizontalBounds vodorovná hranica pohybu
 * @param verticalBounds zvislá hranica pohybu
 * @param scaleX Citlivosť na horizontálny posun (čím menšia, tým citlivšia)
 * @param scaleY Citlivosť na vertikálny posun (čím menšia, tým citlivšia)
 */
class Movement(
    horizontalBounds: ClosedFloatingPointRange<Float> = 0F..0F,
    verticalBounds: ClosedFloatingPointRange<Float> = 0F..0F,
    scaleX: Float = 0F,
    scaleY: Float = scaleX
) : View.OnTouchListener {
    companion object {
        private var lastPosX = 0F //posledna pozicia X kurzoru
        private var lastPosY = 0F //posledna pozicia Y kurzoru
        private var lastEventAction = 0 //posledna udalost
    }

    //koli zvacseniu grafiky sa objekt nebude posuvat rovnakym tempom ako prst / kurzor po obrazokve
    private var sclX = scaleX // pomer zvacsenia / zmensenia na osi X rodicovskeho 2D objektu
    private var sclY = scaleY // pomer zvacsenia / zmensenia na osi Y rodicovskeho 2D objektu

    /*
    hranice posunu. Zozaciatku sa objekty nedaju posunut. Odprucaju sa nastavit, inak sa
     objekt moze teleportovat na lavy horny roh rodicovskeho 2D objektu
     */
    private var bdsX = horizontalBounds
    private var bdsY = verticalBounds

    //nastavenie hranic rozsahu a akykolvek uzitocny algoritmus pred pokusom o posun objektu
    private var onMoveStart: () -> Unit = fun() {}
    private var onMoveEnd: () -> Unit = fun() {}

    /**
     * Čo sa má stať, predtým, ako pohyb začne
     * @param fn Čo?
     */
    fun setOnMoveStart(fn: () -> Unit) {
        onMoveStart = fn
    }

    /**
     * Čo sa má stať, po tom, ako pohyb skončí
     * @param fn Čo?
     */
    fun setOnMoveEnd(fn: () -> Unit) {
        onMoveEnd = fn
    }

    /**
     * Zmena hranic posunu po horizontalnej osi
     * @param bounds Hranice posuvania sa po osi X
     */
    fun setBoundsX(bounds: ClosedFloatingPointRange<Float>) {
        bdsX = bounds
    }

    /**
     * Zmena hranic posunu po vertikálnej osi
     * @param bounds Hranice posuvania sa po osi Y
     */
    fun setBoundsY(bounds: ClosedFloatingPointRange<Float>) {
        bdsY = bounds
    }

    /**
     * Nastavenie pomeru zvacsenia / zmensenia rodicovskeho suboru
     * @param scale Pomer zväčšenia
     */
    fun notifyScale(scale: Float) {
        sclX = scale
        sclY = scale
    }

    /**
     * Nastavenie pomeru zvacsenia / zmensenia rodicovskeho suboru na osi X
     * @param scaleX Pomer zväčšenia
     */
    fun notifyScaleX(scaleX: Float) {
        sclX = scaleX
    }

    /**
     * Nastavenie pomeru zvacsenia / zmensenia rodicovskeho suboru na osi Y
     * @param scaleY Pomer zväčšenia
     */
    fun notifyScaleY(scaleY: Float) {
        sclY = scaleY
    }

    /**
     * Samotne posuvanie objektu
     * @param view Objekt dotyku
     * @param motion Detaily o dotyku vrátane správania sa dotyku (posuvanie, pustenie, pridrzanie...)
     * @return [Boolean] Vrati hodnotu true alebo false, podla toho, ci na dany typ dotyku bola nejaka reakcia
     */
    override fun onTouch(view: View, motion: MotionEvent): Boolean {
        val previousEventAction = lastEventAction
        lastEventAction = motion.action

        when (motion.action) {
            MotionEvent.ACTION_DOWN -> {
                lastPosX = motion.rawX
                lastPosY = motion.rawY
                onMoveStart()
            }
            MotionEvent.ACTION_UP -> {
                if (previousEventAction == MotionEvent.ACTION_DOWN)
                    view.performClick()
                onMoveEnd()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (motion.rawX - lastPosX) / sclX
                val dy = (motion.rawY - lastPosY) / sclY
                lastPosX = motion.rawX
                lastPosY = motion.rawY
                view.x = (view.x + dx).coerceIn(bdsX)
                view.y = (view.y + dy).coerceIn(bdsY)
            }
            else -> return false
        }
        return true
    }
}