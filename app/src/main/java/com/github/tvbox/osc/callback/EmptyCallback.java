package com.github.tvbox.osc.callback;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
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

                int contentHeight = contentLayout.getHeight();
                int targetTop = contentCenterY - viewTop - contentHeight / 2;

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) contentLayout.getLayoutParams();
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                layoutParams.topMargin = Math.max(0, targetTop);
                contentLayout.setLayoutParams(layoutParams);
            };

            contentLayout.getViewTreeObserver().addOnGlobalLayoutListener(updatePosition::run);
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