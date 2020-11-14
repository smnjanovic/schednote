package com.moriak.schednote.design

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ItemTopSpacing(private val top: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = top
    }
}