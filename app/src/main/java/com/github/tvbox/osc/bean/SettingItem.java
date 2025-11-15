package com.github.tvbox.osc.bean;

/**
 * 设置项数据模型 - Material 3风格
 */
public class SettingItem {
    public static final int TYPE_CATEGORY = 0;      // 分类标题
    public static final int TYPE_PREFERENCE = 1;    // 普通设置项
    public static final int TYPE_SWITCH = 2;        // 开关设置项

    private int type;
    private String title;
    private String summary;
    private String value;
    private boolean switchState;
    private OnClickListener onClickListener;

    public SettingItem(int type, String title) {
        this.type = type;
        this.title = title;
    }

    public static SettingItem createCategory(String title) {
        return new SettingItem(TYPE_CATEGORY, title);
    }

    public static SettingItem createPreference(String title, String value, OnClickListener listener) {
        SettingItem item = new SettingItem(TYPE_PREFERENCE, title);
        item.value = value;
        item.onClickListener = listener;
        return item;
    }

    public static SettingItem createSwitch(String title, boolean checked, OnClickListener listener) {
        SettingItem item = new SettingItem(TYPE_SWITCH, title);
        item.switchState = checked;
        item.onClickListener = listener;
        return item;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public SettingItem setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isSwitchState() {
        return switchState;
    }

    public void setSwitchState(boolean switchState) {
        this.switchState = switchState;
    }

    public OnClickListener getOnClickListener() {
        return onClickListener;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public interface OnClickListener {
        void onClick(SettingItem item);
    }
}

