package com.github.tvbox.osc.ui.tv.widget;

import android.content.Context;
import android.util.TypedValue;

import androidx.recyclerview.widget.RecyclerView;

import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

/**
 * 自适应网格布局管理器
 * 根据屏幕宽度和最小卡片宽度自动计算列数
 */
public class AutoFitGridLayoutManager extends V7GridLayoutManager {
    private int columnWidth;
    private boolean isColumnWidthChanged = true;
    private int lastWidth = 0;
    private int lastHeight = 0;
    private final Context context;

    /**
     * @param context       上下文
     * @param columnWidthDp 每列的最小宽度（单位：dp）
     */
    public AutoFitGridLayoutManager(Context context, int columnWidthDp) {
        super(context, 1);
        this.context = context;
        setColumnWidth(checkedColumnWidth(context, columnWidthDp));
    }

    /**
     * 检查并转换列宽为像素值
     */
    private int checkedColumnWidth(Context context, int columnWidthDp) {
        if (columnWidthDp <= 0) {
            columnWidthDp = 150; // 默认最小宽度 150dp（确保竖屏至少2列）
        }
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                columnWidthDp,
                context.getResources().getDisplayMetrics()
        );
    }

    /**
     * 设置列宽
     */
    public void setColumnWidth(int newColumnWidth) {
        if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
            columnWidth = newColumnWidth;
            isColumnWidthChanged = true;
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int width = getWidth();
        int height = getHeight();

        if (width > 0 && height > 0 && (isColumnWidthChanged || lastWidth != width || lastHeight != height)) {
            int totalSpace;
            if (getOrientation() == VERTICAL) {
                totalSpace = width - getPaddingRight() - getPaddingLeft();
            } else {
                totalSpace = height - getPaddingTop() - getPaddingBottom();
            }

            // 计算最佳列数
            // 考虑到每个卡片有 margin（@dimen/vs_4），所以实际占用空间略大于 columnWidth
            // 每个卡片的实际宽度 = columnWidth + (左右margin) ≈ columnWidth + 8dp
            int cardMarginPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    8, // vs_4 * 2 = 8dp
                    context.getResources().getDisplayMetrics()
            );
            int effectiveCardWidth = columnWidth + cardMarginPx;
            int spanCount = Math.max(2, totalSpace / effectiveCardWidth); // 最小2列
            
            setSpanCount(spanCount);

            isColumnWidthChanged = false;
            lastWidth = width;
            lastHeight = height;
        }

        super.onLayoutChildren(recycler, state);
    }
}

