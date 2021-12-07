package com.moriak.schednote.fragments

import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics.DENSITY_DEFAULT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.LinearLayoutManager
import com.moriak.schednote.ItemTopSpacing
import com.moriak.schednote.adapters.CustomAdapter
import com.moriak.schednote.views.RecyclerViewEmptySupport

/**
 * Fragmenty tohto typu obsahujú blok [RecyclerViewEmptySupport].
 * Obsah, pozícia scroll a iné dáta z bloku [RecyclerViewEmptySupport],
 * budú po otočení automaticky obnovené.
 */
abstract class ListSubActivity<T>(
    @LayoutRes private val layout: Int,
    @IdRes private val adapterView: Int,
    @IdRes private val emptyView: Int,
    @IntRange(from = 0, to = Integer.MAX_VALUE.toLong()) space: Int,
    private val canRestore: Boolean = true
): SubActivity() {
    private companion object {
        private const val SCROLL = "CUSTOM_ADAPTER_FRAGMENT_SCROLL"
        private const val DATA = "CUSTOM_ADAPTER_FRAGMENT_DATA"
    }

    private val llm by lazy { LinearLayoutManager(requireContext()) }

    private val itemTopSpacing by lazy { if (space > 0) ItemTopSpacing(dp(space)) else null }

    protected abstract val adapter: CustomAdapter<T>

    private fun dp(px: Int) = px * resources.displayMetrics.densityDpi / DENSITY_DEFAULT

    /**
     * Táto funkcia slúži na získanie dát pre adaptér [CustomAdapter].
     * @return zoznam položiek pre adaptér [CustomAdapter].
     */
    protected abstract fun firstLoad(): List<T>

    /**
     * Činnosť ktorá sa má vykonať pre položku adaptéra na pozícii [pos], s dátami [data] na základe
     * kódu [code].
     */
    protected abstract fun onItemAction(pos: Int, data: Bundle, code: Int)

    final override fun onCreateView(inf: LayoutInflater, par: ViewGroup?, saved: Bundle?) = inf
        .inflate(layout, par, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<RecyclerViewEmptySupport>(adapterView)?.also {
            it.adapter = adapter
            it.layoutManager = llm
            it.setEmptyView(view.findViewById<TextView>(emptyView))
            itemTopSpacing?.let(it::addItemDecoration)
        }
        adapter.onItemAction(this::onItemAction)
        if (!canRestore) adapter.putItems(firstLoad())
        else adapter.restoreOrPutItems(savedInstanceState?.getBundle(DATA), this::firstLoad)
        savedInstanceState?.getParcelable<Parcelable>(SCROLL)?.let(llm::onRestoreInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (canRestore) outState.putBundle(DATA, Bundle().also(adapter::storeItems))
        outState.putParcelable(SCROLL, llm.onSaveInstanceState())
    }
}