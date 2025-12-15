package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.widget.TextView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;

public class LiveFragment extends BaseLazyFragment {
    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_live;
    }

    @Override
    protected void init() {
        // 不在init中直接跳转，而是等待用户操作
        TextView tvLive = rootView.findViewById(R.id.tvLive);
        if (tvLive != null) {
            tvLive.setOnClickListener(v -> {
                // 点击时跳转到直播Activity
                Intent intent = new Intent(mContext, LivePlayActivity.class);
                startActivity(intent);
            });
        }
    }
}

