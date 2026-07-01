package com.github.tvbox.osc.ui.tv.widget

import android.content.Context
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import kotlin.math.max

/**
 * 自适应网格布局管理器
 * 根据屏幕宽度和最小卡片宽度自动计算列数
 * 
 * @param context       上下文
 * @param columnWidthDp 每列的最小宽度（单位：dp）
 */
class AutoFitGridLayoutManager(private val context: Context, columnWidthDp: Int) : V7GridLayoutManager(context, 1) {
	private var columnWidth = 0
	private var isColumnWidthChanged = true
	private var lastWidth = 0
	private var lastHeight = 0

	/**
	 * 检查并转换列宽为像素值
	 */
	private fun checkedColumnWidth(context: Context, columnWidthDp: Int): Int {
		var columnWidthDp = columnWidthDp
		if (columnWidthDp <= 0) {
			columnWidthDp = 150 // 默认最小宽度 150dp（确保竖屏至少2列）
		}
		return TypedValue.applyDimension(
			TypedValue.COMPLEX_UNIT_DIP,
			columnWidthDp.toFloat(),
			context.resources.displayMetrics
		).toInt()
	}

	/**
	 * 设置列宽
	 */
	fun setColumnWidth(newColumnWidth: Int) {
		if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
			columnWidth = newColumnWidth
			isColumnWidthChanged = true
		}
	}

	override fun onLayoutChildren(recycler: Recycler?, state: RecyclerView.State?) {
		val width = getWidth()
		val height = getHeight()

		if (width > 0 && height > 0 && (isColumnWidthChanged || lastWidth != width || lastHeight != height)) {
			val totalSpace: Int = if (orientation == VERTICAL) {
				width - paddingRight - paddingLeft
			} else {
				height - paddingTop - paddingBottom
			}

			// 计算最佳列数
			// 考虑到每个卡片有 margin（@dimen/vs_4），所以实际占用空间略大于 columnWidth
			// 每个卡片的实际宽度 = columnWidth + (左右margin) ≈ columnWidth + 8dp
			val cardMarginPx = TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP,
				8f,  // vs_4 * 2 = 8dp
				context.resources.displayMetrics
			).toInt()
			val effectiveCardWidth = columnWidth + cardMarginPx
			val spanCount = max(2, totalSpace / effectiveCardWidth) // 最小2列

			setSpanCount(spanCount)

			isColumnWidthChanged = false
			lastWidth = width
			lastHeight = height
		}

		super.onLayoutChildren(recycler, state)
	}
}
