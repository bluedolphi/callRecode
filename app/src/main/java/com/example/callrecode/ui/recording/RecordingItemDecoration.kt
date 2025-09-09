package com.example.callrecode.ui.recording

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 录音列表项装饰器
 * 为列表项添加间距
 */
class RecordingItemDecoration : RecyclerView.ItemDecoration() {
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        
        // Add vertical spacing between items
        if (position != RecyclerView.NO_POSITION) {
            outRect.bottom = 8 // 8dp spacing between items
        }
    }
}