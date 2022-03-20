package com.moriak.schednote.fragments

import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics.DENSITY_DEFAULT
import android.view.View
import androidx.annotation.IntRange
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.moriak.schednote.ItemTopSpacing
import com.moriak.schednote.adapters.CustomAdapter
import com.moriak.schednote.views.RecyclerViewEmptySupport

/**
 * Fragmenty tohto typu obsahujú blok [RecyclerViewEmptySupport].
 * Obsah, pozícia scroll a iné dáta z bloku [RecyclerViewEmptySupport],
 * budú po otočení automaticky obnovené.
 */
abstract class ListSubActivity<I, A: CustomAdapter<I, *>, B: ViewBinding>(
    @IntRange(from = 0, to = Integer.MAX_VALUE.toLong()) space: Int,
    private val canRestore: Boolean = true
): SubActivity<B>() {
    private companion object {
        private const val SCROLL = "CUSTOM_ADAPTER_FRAGMENT_SCROLL"
        private const val DATA = "CUSTOM_ADAPTER_FRAGMENT_DATA"
    }

    protected abstract val adapterView: RecyclerView
    open val emptyView: View? = null

    private val llm by lazy { LinearLayoutManager(requireContext()) }

    private val itemTopSpacing by lazy { if (space > 0) ItemTopSpacing(dp(space)) else null }

    protected abstract val adapter: A

    private fun dp(px: Int) = px * resources.displayMetrics.densityDpi / DENSITY_DEFAULT

    /**
     * Táto funkcia slúži na získanie dát pre adaptér [CustomAdapter].
     * @return zoznam položiek pre adaptér [CustomAdapter].
     */
    protected abstract fun firstLoad(): List<I>

    /**
     * Činnosť ktorá sa má vykonať pre položku adaptéra na pozícii [pos], s dátami [data] na základe
     * kódu [code].
     */
    protected abstract fun onItemAction(pos: Int, data: Bundle, code: Int)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapterView.also {
            adapterView.adapter = adapter
            adapterView.layoutManager = llm
            if (adapterView is RecyclerViewEmptySupport)
                    (adapterView as RecyclerViewEmptySupport).setEmptyView(emptyView)
            itemTopSpacing?.let(adapterView::addItemDecoration)
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