package com.moriak.schednote.design

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Trieda rozširuje RecyclerView, ktorý narozdiel od ListView nedokáže nastaviť layout, ktorý
 * by mohol byť zobrazený namiesto prázdneho zoznamu. Tu je tento problém vyriešený
 */
class RecyclerViewEmptySupport : RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var emptyView: View? = null
    private val observer = object : AdapterDataObserver() {
        override fun onChanged() = checkIfEmpty()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = checkIfEmpty()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) =
            checkIfEmpty()

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkIfEmpty()
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) =
            checkIfEmpty()

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkIfEmpty()
    }

    private fun checkIfEmpty() {
        val empty = adapter?.itemCount?.let { it == 0 } ?: true
        emptyView?.visibility = if (empty) VISIBLE else GONE
        visibility = if (empty) GONE else VISIBLE
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        getAdapter()?.unregisterAdapterDataObserver(observer)
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(observer)
        checkIfEmpty()
    }

    /**
     * Nastavenie zástupného layoutu, v prípade, že je zoznam prázdny
     * @param view grafický prvok
     */
    fun setEmptyView(view: View?) {
        emptyView = view
        checkIfEmpty()
    }
}