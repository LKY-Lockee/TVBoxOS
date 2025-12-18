package com.github.tvbox.osc.callback;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.github.tvbox.osc.R;
import com.kingja.loadsir.callback.Callback;

/**
 * @author pj567
 * @date :2020/12/24
 * @description:
 */
public class LoadingCallback extends Callback {
    @Override
    protected int onCreateView() {
        return R.layout.loadsir_loading_layout;
    }

    @Override
    public void onAttach(Context context, View view) {
        super.onAttach(context, view);
        View loadingIndicator = view.findViewById(R.id.loading_indicator);
        if (loadingIndicator != null) {
            Runnable updatePosition = () -> {
                if (loadingIndicator.getHeight() == 0 || !loadingIndicator.isAttachedToWindow()) {
                    return;
                }

                int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
                int indicatorHeight = loadingIndicator.getHeight();

                int[] location = new int[2];
                view.getLocationOnScreen(location);
                int viewTop = location[1];

                int screenCenter = screenHeight / 2;

                int targetTop = screenCenter - viewTop - indicatorHeight / 2;

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) loadingIndicator.getLayoutParams();
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                layoutParams.topMargin = targetTop;
                loadingIndicator.setLayoutParams(layoutParams);
            };

            loadingIndicator.getViewTreeObserver().addOnGlobalLayoutListener(updatePosition::run);
        }
    }
}