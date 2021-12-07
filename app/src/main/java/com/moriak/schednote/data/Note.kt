package com.moriak.schednote.data

/**
 * Trieda na uchovávanie dát o úlohach
 * @property id Id úlohy
 * @property sub Predmet, ktorému úloha patrí
 * @property info Popis úlohy
 * @property deadline Dátum vypršania úlohy (v milisekundách). Môže byť null
 */
data class Note(val id: Long, val sub: Subject, val info: String, val deadline: Long? = null)