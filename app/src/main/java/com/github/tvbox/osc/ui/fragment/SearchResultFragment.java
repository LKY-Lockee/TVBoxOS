package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.SearchAdapter;
import com.github.tvbox.osc.ui.tv.widget.AutoFitGridLayoutManager;
import com.github.tvbox.osc.util.FastClickCheckUtil;

import java.util.ArrayList;

public class SearchResultFragment extends BaseLazyFragment {
    private ArrayList<Movie.Video> dataList;

    private SearchAdapter searchAdapter;
    private SwipeRefreshLayout mSwipe;
    private Runnable onRefreshListener;

    public void setOnRefreshListener(Runnable listener) {
        this.onRefreshListener = listener;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_grid;
    }

    @Override
    protected void init() {
        RecyclerView mGridView = rootView.findViewById(R.id.mGridView);
        mGridView.setHasFixedSize(true);

        mSwipe = rootView.findViewById(R.id.mSwipe);
        mSwipe.setOnChildScrollUpCallback((parent, child) -> mGridView.canScrollVertically(-1));
        mSwipe.setOnRefreshListener(() -> {
            if (onRefreshListener != null) {
                onRefreshListener.run();
            }
            mSwipe.setRefreshing(false);
        });

        int minColumnWidthDp = 150;
        mGridView.setLayoutManager(new AutoFitGridLayoutManager(mContext, minColumnWidthDp));

        searchAdapter = new SearchAdapter();
        mGridView.setAdapter(searchAdapter);

        searchAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            Movie.Video video = searchAdapter.getData().get(position);
            if (video != null) {
                Intent intent = new Intent(mContext, DetailActivity.class);
                intent.putExtra("id", video.id);
                intent.putExtra("sourceKey", video.sourceKey);
                startActivity(intent);
            }
        });

        setLoadSir(rootView);

        if (dataList != null && !dataList.isEmpty()) {
            showSuccess();
            searchAdapter.setNewData(dataList);
        } else {
            showEmpty();
        }
    }

    public void updateData(ArrayList<Movie.Video> newData) {
        this.dataList = newData;
        if (searchAdapter != null && newData != null && isAdded()) {
            if (!newData.isEmpty()) {
                showSuccess();
                searchAdapter.setNewData(newData);
            } else {
                showEmpty();
            }
        }
    }
}
