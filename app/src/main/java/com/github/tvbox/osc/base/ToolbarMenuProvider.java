package com.github.tvbox.osc.base;

import androidx.annotation.MenuRes;

public interface ToolbarMenuProvider {
    @MenuRes
    int getMenuResId();

    boolean onMenuItemClick(int itemId);

    default String getToolbarTitle() {
        return null;
    }
}
