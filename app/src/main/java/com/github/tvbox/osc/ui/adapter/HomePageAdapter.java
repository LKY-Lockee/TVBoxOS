package com.github.tvbox.osc.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.github.tvbox.osc.base.BaseLazyFragment;

import java.util.List;

/**
 * @author acer
 * @date 2018/12/4
 */
public class HomePageAdapter extends FragmentStateAdapter {
    public List<BaseLazyFragment> list;

    public HomePageAdapter(@NonNull FragmentActivity fragmentActivity, List<BaseLazyFragment> list) {
        super(fragmentActivity);
        this.list = list;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return list.get(position);
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }
}
