package com.github.tvbox.osc.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.tvbox.osc.base.BaseLazyFragment

/**
 * @author acer
 * @date 2018/12/4
 */
class HomePageAdapter(fragmentActivity: FragmentActivity, var list: MutableList<BaseLazyFragment>) : FragmentStateAdapter(fragmentActivity) {
	override fun createFragment(position: Int): Fragment {
		return list[position]
	}

	override fun getItemCount(): Int {
		return list.size
	}
}
