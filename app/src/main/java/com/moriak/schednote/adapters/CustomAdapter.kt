package com.moriak.schednote.adapters

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Rozšírený [RecyclerView.Adapter], so zoradenými obnoviteľnými prvkami.
 * @property extras doplňujúce dáta
 * @property items zoznam položiek adaptéra
 */
abstract class CustomAdapter<T, B: ViewBinding> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private companion object { private const val ITEMS = "CUSTOM_ARRAY_ADAPTER_ITEMS" }
    private var itemAction: (Int, Bundle, Int)->Unit = fun(_, _, _) = Unit
    protected val items: ArrayList<T> = ArrayList()
    val extras: Bundle = Bundle()

    private fun shellSort() {
        val n = items.size
        var gap = n / 2
        while (gap > 0) {
            for (i in gap until n) {
                val temp = items[i]
                var j = i
                while (j >= gap && compare(items[j - gap], temp) > 0) {
                    items[j] = items[j - gap]
                    j -= gap
                }
                items[j] = temp
            }
            gap /= 2
        }
    }

    private fun getFittingPosition(item: T): Int {
        when {
            items.isEmpty() -> return items.size
            compare(item, items.first()) < 0 -> return 0
            compare(item, items.last()) >= 0 -> return items.size
            else -> {
                var st = 0
                var en = items.size
                while(st < en) {
                    val pi = (en - st) / 2 + st
                    val cmpL = compare(items[pi - 1], item)
                    val cmpR = compare(items[pi], item)
                    when {
                        cmpL < 0 && cmpR < 0 -> st = pi
                        cmpL > 0 && cmpR > 0 -> en = pi
                        cmpL <= 0 && cmpR >= 0 -> return pi
                        else -> return items.size
                    }
                }
                return items.size
            }
        }
    }

    final override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
            = (holder as CustomAdapter<*, *>.CustomViewHolder).bind(position)

    final override fun getItemCount(): Int = items.size

    /**
     * Porovnávajú sa 2 položky
     * @param a položka A
     * @param b položka B
     * @return rozdiel, resp. výsledok porovnania
     */
    protected open fun compare(a: T, b: T): Int = 0

    /**
     * Z úložiska [bundle] Načíta údaje a vytvorí z nich objekt typu [T].
     * @param bundle
     * @return objekt typu [T]
     */
    abstract fun bundleToItem(bundle: Bundle): T

    /**
     * Odloží objekt [item] alebo jeho množinu atribútov do úložiska [bundle].
     * @param item
     * @param bundle
     */
    abstract fun itemToBundle(item: T, bundle: Bundle)

    /**
     * Odstráni všetky položky
     */
    fun clearItems() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    /**
     * Pridá do zoznamu nové položky. A utriedi
     * @param pItems zoznam položkiek, ktoré treba pridať
     */
    fun putItems(pItems: Collection<T>) {
        val size = items.size
        items.addAll(pItems)
        shellSort()
        notifyItemRangeInserted(size, pItems.size)
    }

    /**
     * Položky zoznamu [items] a doplňujúce dáta [extras] sa uložia zapíšu do inštancie [bundle].
     * @param bundle
     */
    fun storeItems(bundle: Bundle) {
        bundle.putAll(extras)
        bundle.putParcelableArray(ITEMS, items.map {
            Bundle().also { bdl -> itemToBundle(it, bdl) }
        }.toTypedArray())
    }

    /**
     * Položky zoznamu [items] a doplňujúce dáta [extras] sa načítajú z inštancie [bundle].
     * Ak je [bundle] null, tak sa vytvorí nový zoznam pomocou funkcie [getItems].
     * @param bundle
     * @param getItems
     * @return true, ak [bundle] nie je null
     */
    fun restoreOrPutItems(bundle: Bundle?, getItems: ()->List<T>): Boolean {
        bundle?.let { extras.putAll(it) }
        putItems(bundle?.getParcelableArray(ITEMS)?.map{ bundleToItem(it as Bundle) } ?: getItems())
        extras.remove(ITEMS)
        return bundle != null
    }

    /**
     * Získa položku na pozícii [pos].
     */
    fun getItemAt(pos: Int): T = items[pos]

    /**
     * Nájde pozície prvého prvku, ktorý vyhovuje podmienke [fn], teda prvý prvok, pre ktorý funkcia
     * [fn] vráti true.
     * @param fn parameter 1: položka typu [T] zo zoznamu [items], parameter 2: [data].
     * @param data doplňujúci údaj k adaptéru
     * @return vráti pozíciu prvého vyhovujúceho prvku
     */
    fun findIndexOf(fn: (T, Any?)->Boolean, data: Any?): Int = items.indexOfFirst { fn(it, data) }

    /**
     * Pridá sa položka do zoznamu na takú pozíciu, aby bol zoznam stále utriedený.
     * @param item položka
     */
    fun insertItem(item: T): Int {
        val pos = getFittingPosition(item)
        items.add(pos, item)
        notifyItemInserted(pos)
        return pos
    }

    /**
     * Aktualizuje sa položka a presunie sa na takú pozíciu, aby bol zoznam stále utriedený.
     * @param item náhradná položka
     * @param pos pozícia, na ktorej sa úprava vykoný
     * @return pozícia položky po premiestnení
     */
    fun updateItem(item: T, pos: Int): Int {
        if (pos !in items.indices) throw IndexOutOfBoundsException("Cannot update item on index $pos within the range ${items.indices}.")
        items[pos] = item
        notifyItemChanged(pos)
        val cmpL = if (pos == 0) -1 else compare(items[pos-1], items[pos])
        val cmpR = if (pos == items.lastIndex) 1 else compare(items[pos+1], items[pos])
        if (cmpL <= 0 && cmpR >= 0) return pos
        items.removeAt(pos)
        val newPos = getFittingPosition(item)
        items.add(newPos, item)
        notifyItemMoved(pos, newPos)
        return newPos
    }

    /**
     * Odstráni sa položka na pozícii [pos].
     * @param pos
     */
    fun deleteItem(pos: Int) {
        if (pos !in items.indices) throw IndexOutOfBoundsException("Cannot delete item on index $pos within the range ${items.indices}.")
        items.removeAt(pos)
        notifyItemRemoved(pos)
    }

    /**
     * Odstráni sa niekoľko položiek na pozíciach v rozsahu [posRng].
     * @param posRng rozsah pozícii prvov, ktoré zmiznú zo zoznamu.
     */
    fun deleteRange(posRng: IntRange) {
        if (posRng.first < 0 || posRng.last > items.lastIndex || posRng.first > posRng.last)
            throw IndexOutOfBoundsException("Cannot delete some items in range $posRng within the range ${items.indices}.")
        for (i in posRng.reversed()) items.removeAt(i)
        notifyItemRangeRemoved(posRng.first, posRng.count())
    }

    /**
     * Nastaviť funkciu, ktorá sa má vykonať po určitej interakcii s niektorou z položiek.
     * @param fn funkcia:
     * 1. argument: pozícia položky
     * 2. argument: doplňujúce dáta
     * 3. argument: typ interakcie (číselné označenie)
     */
    fun onItemAction(fn: (Int, Bundle, Int)->Unit) { itemAction = fn }

    /**
     * Vykonať funkciu ktorá sa má vykonať po určitej interakcii [key]
     * s položkou na pozícii [position] s doplňujúcimi dátami [data].
     * @param position
     * @param data
     * @param key
     */
    fun triggerItemAction(position: Int, data: Bundle, key: Int) = itemAction(position, data, key)

    /**
     * Rozšírený [RecyclerView.ViewHolder]
     * @property item súvisiaca položka
     */
    abstract inner class CustomViewHolder(protected val binding: B): RecyclerView.ViewHolder(binding.root) {
        val item: T? get() = if (adapterPosition in items.indices) items[adapterPosition] else null

        /**
         * Po pripnutí inštancie [RecyclerView.ViewHolder] k položke typu [T]
         * sa obsah bloku [RecyclerView.ViewHolder.itemView] prispôsobí pripnutej položke.
         * @param pos pozícia položky, ku ktorej bola inštancia [RecyclerView.ViewHolder] pripnutá.
         */
        abstract fun bind(pos: Int)
    }
}