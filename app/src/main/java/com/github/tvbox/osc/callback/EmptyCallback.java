package com.github.tvbox.osc.callback;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.github.tvbox.osc.R;
import com.kingja.loadsir.callback.Callback;

/**
 * @author pj567
 * @date :2020/12/24
 * @description:
 */
public class EmptyCallback extends Callback {
    @Override
    protected int onCreateView() {
        return R.layout.loadsir_empty_layout;
    }

    @Override
    public void onAttach(Context context, View view) {
        super.onAttach(context, view);
        LinearLayout contentLayout = view.findViewById(R.id.empty_content);
        if (contentLayout != null) {
            Runnable updatePosition = () -> {
                if (contentLayout.getHeight() == 0 || !contentLayout.isAttachedToWindow()) {
                    return;
                }

                int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
                int contentHeight = contentLayout.getHeight();

                int[] location = new int[2];
                view.getLocationOnScreen(location);
                int viewTop = location[1];

                int screenCenter = screenHeight / 2;

                int targetTop = screenCenter - viewTop - contentHeight / 2;

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) contentLayout.getLayoutParams();
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                layoutParams.topMargin = targetTop;
                contentLayout.setLayoutParams(layoutParams);
            };

            contentLayout.getViewTreeObserver().addOnGlobalLayoutListener(updatePosition::run);
        }
    }
}