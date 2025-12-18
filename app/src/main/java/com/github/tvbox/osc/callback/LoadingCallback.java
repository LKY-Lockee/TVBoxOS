package com.github.tvbox.osc.callback;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
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

                // 获取整个应用的内容区域
                Activity activity = getActivityFromContext(context);
                if (activity == null) return;

                View contentView = activity.findViewById(android.R.id.content);
                if (contentView == null || contentView.getHeight() == 0) return;

                // 获取中心位置
                int[] contentLocation = new int[2];
                contentView.getLocationOnScreen(contentLocation);
                int contentCenterY = contentLocation[1] + contentView.getHeight() / 2;

                int[] viewLocation = new int[2];
                view.getLocationOnScreen(viewLocation);
                int viewTop = viewLocation[1];

                int indicatorHeight = loadingIndicator.getHeight();
                int targetTop = contentCenterY - viewTop - indicatorHeight / 2;

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) loadingIndicator.getLayoutParams();
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                layoutParams.topMargin = Math.max(0, targetTop);
                loadingIndicator.setLayoutParams(layoutParams);
            };

            loadingIndicator.getViewTreeObserver().addOnGlobalLayoutListener(updatePosition::run);
        }
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
}