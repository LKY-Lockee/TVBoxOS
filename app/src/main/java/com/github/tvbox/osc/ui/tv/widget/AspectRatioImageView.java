package com.github.tvbox.osc.ui.tv.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class AspectRatioImageView extends AppCompatImageView {
    private float aspectRatio = 0f; // 宽高比 (宽/高)，0表示使用默认或图片实际比例

    public AspectRatioImageView(Context context) {
        super(context);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAspectRatio(float ratio) {
        if (this.aspectRatio != ratio) {
            this.aspectRatio = ratio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        if (width > 0) {
            int height;

            if (aspectRatio > 0) {
                height = (int) (width / aspectRatio);
            } else {
                Drawable drawable = getDrawable();
                if (drawable != null && drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0) {
                    float imageRatio = (float) drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight();
                    height = (int) (width / imageRatio);
                } else {
                    float defaultAspectRatio = 214f / 280f;
                    height = (int) (width / defaultAspectRatio);
                }
            }

            setMeasuredDimension(width, height);
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (aspectRatio == 0) {
            post(this::requestLayout);
        }
    }
}
