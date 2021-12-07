package com.moriak.schednote.enums

/**
 * Inštancie tejto triedy reprezentujú jednotlivé budíky.
 * @property day deň budenia
 * @property reg týždeň budenia
 * @property id bitová reprezentácia budíka
 */
enum class AlarmClockBit {
    B00, B01, B02, B03, B04, B05, B06,
    B07, B08, B09, B10, B11, B12, B13,
    B14, B15, B16, B17, B18, B19, B20;
    val id = 1 shl ordinal
    val day = Day[ordinal % 7 + 1]
    val reg = Regularity.values()[ordinal / 7]
    fun toString(tf: TimeFormat, dMin: Int) = "${tf.getFormat(dMin)} | $day | $reg"
    companion object {
        /**
         * Získanie inštancie podľa dňa a týždňa budenia
         * @param day deň
         * @param reg týždeň
         * @return konkrétny budík
         */
        fun getBit(day: Day, reg: Regularity) = values()[reg.ordinal * 7 + day.value - 1]

        /**
         * Získanie inštancie podľa názvu inštancie [AlarmClockBit.name]
         * @param str hľadaný názov
         * @return budík so zhodujúcim sa názvom alebo null
         */
        operator fun get(str: String): AlarmClockBit? {
            if (!str.matches("^B[0-9]{2}$".toRegex())) return null
            val num = str.substring(1).toInt()
            return if (num <= 20) values()[num] else null
        }
    }
}