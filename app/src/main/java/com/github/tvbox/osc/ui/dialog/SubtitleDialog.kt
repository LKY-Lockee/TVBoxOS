package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.SubtitleHelper;

import org.jetbrains.annotations.NotNull;

public class SubtitleDialog extends BaseDialog {

    public TextView selectInternal;
    private TextView subtitleSizeText;
    private TextView subtitleTimeText;

    private SearchSubtitleListener mSearchSubtitleListener;
    private LocalFileChooserListener mLocalFileChooserListener;
    private SubtitleViewListener mSubtitleViewListener;

    public SubtitleDialog(@NonNull @NotNull Context context) {
        super(context);
        if (context instanceof Activity) {
            setOwnerActivity((Activity) context);
        }
        setContentView(R.layout.dialog_subtitle);
        initView(context);
    }

    private void initView(Context context) {
        selectInternal = findViewById(R.id.selectInternal);
        TextView selectLocal = findViewById(R.id.selectLocal);
        TextView selectRemote = findViewById(R.id.selectRemote);
        TextView subtitleSizeMinus = findViewById(R.id.subtitleSizeMinus);
        subtitleSizeText = findViewById(R.id.subtitleSizeText);
        TextView subtitleSizePlus = findViewById(R.id.subtitleSizePlus);
        TextView subtitleTimeMinus = findViewById(R.id.subtitleTimeMinus);
        subtitleTimeText = findViewById(R.id.subtitleTimeText);
        TextView subtitleTimePlus = findViewById(R.id.subtitleTimePlus);
        TextView subtitleStyleOne = findViewById(R.id.subtitleStyleOne);
        TextView subtitleStyleTwo = findViewById(R.id.subtitleStyleTwo);

        selectLocal.setOnClickListener(view -> {
            FastClickCheckUtil.check(view);
            dismiss();
            mLocalFileChooserListener.openLocalFileChooserDialog();
        });

        selectRemote.setOnClickListener(view -> {
            FastClickCheckUtil.check(view);
            dismiss();
            mSearchSubtitleListener.openSearchSubtitleDialog();
        });

        int size = SubtitleHelper.getTextSize(getOwnerActivity());
        subtitleSizeText.setText(Integer.toString(size));

        subtitleSizeMinus.setOnClickListener(view -> {
            String sizeStr = subtitleSizeText.getText().toString();
            int curSize = Integer.parseInt(sizeStr);
            curSize -= 2;
            if (curSize <= 12) {
                curSize = 12;
            }
            subtitleSizeText.setText(Integer.toString(curSize));
            SubtitleHelper.setTextSize(curSize);
            mSubtitleViewListener.setTextSize(curSize);
        });
        subtitleSizePlus.setOnClickListener(view -> {
            String sizeStr = subtitleSizeText.getText().toString();
            int curSize = Integer.parseInt(sizeStr);
            curSize += 2;
            if (curSize >= 60) {
                curSize = 60;
            }
            subtitleSizeText.setText(Integer.toString(curSize));
            SubtitleHelper.setTextSize(curSize);
            mSubtitleViewListener.setTextSize(curSize);
        });

        int timeDelay = SubtitleHelper.getTimeDelay();
        String timeStr = "0";
        if (timeDelay != 0) {
            double dbTimeDelay = timeDelay / 1000d;
            timeStr = Double.toString(dbTimeDelay);
        }
        subtitleTimeText.setText(timeStr);

        subtitleTimeMinus.setOnClickListener(view -> {
            FastClickCheckUtil.check(view);
            String timeStr2 = subtitleTimeText.getText().toString();
            double time = Float.parseFloat(timeStr2);
            double oneceDelay = -0.5;
            time += oneceDelay;
            if (time == 0.0) {
                timeStr2 = "0";
            } else {
                timeStr2 = Double.toString(time);
            }
            subtitleTimeText.setText(timeStr2);
            int mseconds = (int) (oneceDelay * 1000);
            SubtitleHelper.setTimeDelay((int) (time * 1000));
            mSubtitleViewListener.setSubtitleDelay(mseconds);
        });
        subtitleTimePlus.setOnClickListener(view -> {
            FastClickCheckUtil.check(view);
            String timeStr1 = subtitleTimeText.getText().toString();
            double time = Float.parseFloat(timeStr1);
            double oneceDelay = 0.5;
            time += oneceDelay;
            if (time == 0.0) {
                timeStr1 = "0";
            } else {
                timeStr1 = Double.toString(time);
            }
            subtitleTimeText.setText(timeStr1);
            int mseconds = (int) (oneceDelay * 1000);
            SubtitleHelper.setTimeDelay((int) (time * 1000));
            mSubtitleViewListener.setSubtitleDelay(mseconds);
        });
        selectInternal.setOnClickListener(view -> {
            FastClickCheckUtil.check(view);
            dismiss();
            mSubtitleViewListener.selectInternalSubtitle();
        });

        subtitleStyleOne.setOnClickListener(view -> {
            int style = 0;
            dismiss();
            mSubtitleViewListener.setTextStyle(style);
            Toast.makeText(getContext(), "设置样式成功", Toast.LENGTH_SHORT).show();
        });

        subtitleStyleTwo.setOnClickListener(view -> {
            int style = 1;
            dismiss();
            mSubtitleViewListener.setTextStyle(style);
            Toast.makeText(getContext(), "设置样式成功", Toast.LENGTH_SHORT).show();
        });
    }

    public void setLocalFileChooserListener(LocalFileChooserListener localFileChooserListener) {
        mLocalFileChooserListener = localFileChooserListener;
    }

    public void setSearchSubtitleListener(SearchSubtitleListener searchSubtitleListener) {
        mSearchSubtitleListener = searchSubtitleListener;
    }

    public void setSubtitleViewListener(SubtitleViewListener subtitleViewListener) {
        mSubtitleViewListener = subtitleViewListener;
    }

    public interface LocalFileChooserListener {
        void openLocalFileChooserDialog();
    }

    public interface SearchSubtitleListener {
        void openSearchSubtitleDialog();
    }

    public interface SubtitleViewListener {
        void setTextSize(int size);

        void setSubtitleDelay(int milliseconds);

        void selectInternalSubtitle();

        void setTextStyle(int style);
    }
}
