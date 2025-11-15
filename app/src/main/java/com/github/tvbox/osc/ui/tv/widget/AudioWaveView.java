package com.github.tvbox.osc.ui.tv.widget;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class AudioWaveView extends View {
    /**
     * 条间距
     */
    private final int space = 8;
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            invalidate();
        }
    };
    private Paint paint;
    private RectF rectF1;
    private RectF rectF2;
    private RectF rectF3;
    private RectF rectF4;
    private RectF rectF5;
    private int viewHeight;
    /**
     * 每个条的宽度
     */
    private int rectWidth;
    private Random random;

    public AudioWaveView(Context context) {
        super(context);
        init();
    }

    public AudioWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        /**
         * 条数
         */
        int columnCount = 7;
        rectWidth = (viewWidth - space * (columnCount - 1)) / columnCount;
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);//字节跳动颜色
        paint.setStyle(Paint.Style.FILL);
        random = new Random();

        initRect();
    }

    private void initRect() {
        rectF1 = new RectF();
        rectF2 = new RectF();
        rectF3 = new RectF();
        rectF4 = new RectF();
        rectF5 = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int left = rectWidth + space;

        //画每个条之前高度都重新随机生成
        /**
         * 条随机高度
         */
        int randomHeight = random.nextInt(viewHeight);
        rectF1.set(0, randomHeight, rectWidth, viewHeight);
        randomHeight = random.nextInt(viewHeight);
        rectF2.set(left, randomHeight, left + rectWidth, viewHeight);
        randomHeight = random.nextInt(viewHeight);
        rectF3.set(left * 2, randomHeight, left * 2 + rectWidth, viewHeight);
        randomHeight = random.nextInt(viewHeight);
        rectF4.set(left * 3, randomHeight, left * 3 + rectWidth, viewHeight);
        randomHeight = random.nextInt(viewHeight);
        rectF5.set(left * 4, randomHeight, left * 4 + rectWidth, viewHeight);

        canvas.drawRect(rectF1, paint);
        canvas.drawRect(rectF2, paint);
        canvas.drawRect(rectF3, paint);
        canvas.drawRect(rectF4, paint);
        canvas.drawRect(rectF5, paint);

        handler.sendEmptyMessageDelayed(0, 200); //每间隔200毫秒发送消息刷新
    }
}
