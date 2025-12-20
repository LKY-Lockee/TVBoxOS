package com.github.tvbox.osc.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VodInfo;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
public class SeriesAdapter extends BaseQuickAdapter<VodInfo.VodSeries, BaseViewHolder> {
    private final GridLayoutManager mGridLayoutManager;

    public SeriesAdapter(GridLayoutManager gridLayoutManager) {
        super(R.layout.item_series, new ArrayList<>());
        this.mGridLayoutManager = gridLayoutManager;
    }

    private Activity getActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Override
    protected void convert(BaseViewHolder helper, VodInfo.VodSeries item) {
        Chip chipSeries = (Chip) helper.itemView;
        chipSeries.setText(item.name);

        chipSeries.setSelected(item.selected);

        if (getData().size() == 1 && helper.getLayoutPosition() == 0) {
            helper.itemView.setNextFocusUpId(R.id.mGridViewFlag);
        }

        Activity activity = getActivityFromContext(helper.itemView.getContext());
        if (activity != null) {
            View mSeriesGroupTv = activity.findViewById(R.id.mSeriesGroupTv);
            if (getData().size() > 1 && mSeriesGroupTv != null && mSeriesGroupTv.getVisibility() == View.VISIBLE) {
                int spanCount = mGridLayoutManager.getSpanCount();
                int position = helper.getLayoutPosition();
                if (position < spanCount) {
                    helper.itemView.setNextFocusUpId(R.id.mSeriesSortTv);
                }
            }
        }
    }
}