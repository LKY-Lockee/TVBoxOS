package com.github.tvbox.osc.ui.tv.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * 瀑布流 ImageView - 根据图片实际宽高比自适应高度
 * 宽度由父容器决定，高度根据图片实际尺寸按比例计算
 */
public class AspectRatioImageView extends AppCompatImageView {
    private float aspectRatio = 0f; // 宽高比 (宽/高)，0表示使用默认或图片实际比例
    private float defaultAspectRatio = 214f / 280f; // 默认宽高比，图片未加载时使用

    public AspectRatioImageView(Context context) {
        super(context);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 设置宽高比 (宽/高)
     *
     * @param ratio 宽高比，设置为0表示使用图片实际比例
     */
    public void setAspectRatio(float ratio) {
        if (this.aspectRatio != ratio) {
            this.aspectRatio = ratio;
            requestLayout();
        }
    }

    /**
     * 设置默认宽高比（图片未加载时使用）
     */
    public void setDefaultAspectRatio(float ratio) {
        if (this.defaultAspectRatio != ratio) {
            this.defaultAspectRatio = ratio;
            if (aspectRatio == 0 && getDrawable() == null) {
                // 只有在使用默认比例且没有图片时才需要重新布局
                requestLayout();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        if (width > 0) {
            int height;

            if (aspectRatio > 0) {
                // 使用指定的宽高比
                height = (int) (width / aspectRatio);
            } else {
                // 尝试从图片获取实际宽高比
                Drawable drawable = getDrawable();
                if (drawable != null && drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0) {
                    float imageRatio = (float) drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight();
                    height = (int) (width / imageRatio);
                } else {
                    // 图片未加载，使用默认宽高比
                    height = (int) (width / defaultAspectRatio);
                }
            }

            setMeasuredDimension(width, height);
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        // 图片改变时重新测量（只在使用自动宽高比时）
        if (aspectRatio == 0) {
            // 使用 post 避免在布局过程中调用 requestLayout
            post(this::requestLayout);
        }
    }
}

