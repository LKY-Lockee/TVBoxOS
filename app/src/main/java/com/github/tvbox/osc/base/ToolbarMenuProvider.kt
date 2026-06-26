package com.github.tvbox.osc.base;

import androidx.annotation.MenuRes;

public interface ToolbarMenuProvider {
    @MenuRes
    default int getMenuResId() {
        return 0;
    }

    default boolean onMenuItemClick(int itemId) {
        return false;
    }

    default String getToolbarTitle() {
        return null;
    }

    default boolean enableAppBarScroll() {
        return false;
    }
}
