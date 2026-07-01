package com.github.tvbox.osc.ui.fragment

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BackPressProvider
import com.github.tvbox.osc.base.BaseLazyFragment
import com.github.tvbox.osc.base.ToolbarMenuProvider
import com.github.tvbox.osc.bean.AbsSortXml
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.github.tvbox.osc.ui.activity.HomeActivity
import com.github.tvbox.osc.ui.adapter.HomePageAdapter
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.orhanobut.hawk.Hawk

class HomeFragment : BaseLazyFragment(), BackPressProvider, ToolbarMenuProvider {
	private var mTabLayout: TabLayout? = null
	private var mViewPager: ViewPager2? = null
	private val fragments: MutableList<BaseLazyFragment> = ArrayList()
	private var sortDataList: MutableList<SortData> = ArrayList()
	private var adapter: HomePageAdapter? = null
	private var currentSelected = 0
	private var sourceViewModel: SourceViewModel? = null

	// --- BackPressProvider ---
	override fun handleBackPress(): Boolean {
		if (this.currentFragment is GridFragment) {
			// 如果当前 Fragment 能恢复之前保存的 UI 状态，则直接返回
			if ((currentFragment as GridFragment).restoreView()) {
				return true
			}
			// 如果当前不是第一个界面，则返回到第一项
			if (this.currentPosition != 0) {
				this.currentPosition = 0
				return true
			} else {
				return false
			}
		} else {
			return false
		}
	}

	override val layoutResID: Int
		// ----------------
		get() = R.layout.fragment_home

	override fun init() {
		mTabLayout = rootView?.findViewById(R.id.mTabLayout)
		mViewPager = rootView?.findViewById(R.id.mViewPager)
		mViewPager?.isSaveEnabled = false
		mViewPager?.setUserInputEnabled(false)
		mTabLayout?.visibility = View.GONE

		initViewModel()
		loadData()
	}

	override val menuResId: Int
		// ----------------
		get() = R.menu.home_fragment_menu

	override fun onMenuItemClick(itemId: Int): Boolean {
		if (itemId == R.id.action_switch_site) {
			showSiteSwitch()
			return true
		}
		return false
	}

	override fun enableAppBarScroll(): Boolean {
		return true
	}

	// ----------------
	private fun showSiteSwitch() {
		val sites = ApiConfig.instance.switchSourceBeanList
		if (sites.isEmpty()) return

		var select = sites.indexOf(ApiConfig.instance.homeSourceBean)
		if (select < 0 || select >= sites.size) select = 0

		val siteNames = arrayOfNulls<String>(sites.size)
		for (i in sites.indices) {
			siteNames[i] = sites[i].name
		}

		MaterialAlertDialogBuilder(mContext)
			.setTitle("请选择首页数据源")
			.setSingleChoiceItems(siteNames, select) { dialog: DialogInterface?, which: Int ->
				val selectedSite = sites[which]
				ApiConfig.instance.setSourceBean(selectedSite)
				dialog?.dismiss()
				refreshHome()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun refreshHome() {
		val intent = Intent(context, HomeActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
		val bundle = Bundle()
		bundle.putBoolean("useCache", true)
		intent.putExtras(bundle)
		startActivity(intent)
	}

	private fun initViewModel() {
		sourceViewModel = ViewModelProvider(requireActivity())[SourceViewModel::class.java]
		sourceViewModel?.sortResult?.observe(getViewLifecycleOwner(), Observer { absXml: AbsSortXml? -> this.setDataFromAbsSortXml(absXml) })
	}

	fun loadData() {
		if (sourceViewModel != null) {
			sourceViewModel?.getSort(ApiConfig.instance.homeSourceBean.key)
		}
	}

	fun setDataFromAbsSortXml(absXml: AbsSortXml?) {
		fragments.clear()
		val adjustedSortList: MutableList<SortData> = if (absXml != null && absXml.classes != null && absXml.classes?.sortList != null) {
			DefaultConfig.adjustSort(
				ApiConfig.instance.homeSourceBean.key,
				(absXml.classes ?: return).sortList ?: return,
				true
			)
		} else {
			DefaultConfig.adjustSort(
				ApiConfig.instance.homeSourceBean.key,
				ArrayList(),
				true
			)
		}

		this.sortDataList = adjustedSortList

		if (!sortDataList.isEmpty()) {
			for (data in sortDataList) {
				if (data.id == "my0") {
					if (Hawk.get(HawkConfig.HOME_REC, 0) == 1 && absXml != null && absXml.videoList != null && absXml.videoList?.isEmpty() != true) {
						fragments.add(UserFragment(data))
					} else {
						fragments.add(UserFragment(null))
					}
				} else {
					fragments.add(GridFragment(data))
				}
			}
		}

		if (mViewPager != null && mTabLayout != null) {
			setupViewPager()
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun setupViewPager() {
		if (activity == null) {
			return
		}

		// 只有在适配器为空时才创建新的适配器
		if (mViewPager?.adapter == null) {
			adapter = HomePageAdapter(requireActivity(), fragments)
			mViewPager?.setAdapter(adapter)

			// ViewPager页面改变监听（只设置一次）
			mViewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
				override fun onPageSelected(position: Int) {
					super.onPageSelected(position)
					if (mTabLayout != null && position >= 0 && position < (mTabLayout ?: return).tabCount) {
						val tab = mTabLayout?.getTabAt(position)
						if (tab != null && !tab.isSelected) {
							tab.select()
						}
						currentSelected = position
					}
				}
			})
		} else {
			// 如果适配器已存在，只需通知数据改变
			adapter?.notifyDataSetChanged()
		}

		// 设置当前页面
		mViewPager?.setCurrentItem(currentSelected, false)

		// 设置TabLayout
		updateTabLayout()

		// TabLayout监听（移除旧的监听器，避免重复）
		mTabLayout?.clearOnTabSelectedListeners()
		mTabLayout?.addOnTabSelectedListener(object : OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) {
				val position = tab.position
				currentSelected = position
				mViewPager?.setCurrentItem(position, true)
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {
			}

			override fun onTabReselected(tab: TabLayout.Tab?) {
			}
		})
	}

	private fun updateTabLayout() {
		mTabLayout?.removeAllTabs()
		for (sortData in sortDataList) {
			val tab = mTabLayout?.newTab()
			tab?.let {
				it.text = sortData.name
				(mTabLayout ?: return@let).addTab(it)
			}
		}

		if ((mTabLayout ?: return).tabCount > 0) {
			mTabLayout?.visibility = View.VISIBLE
		} else {
			(mTabLayout ?: return).visibility = View.GONE
		}

		if (currentSelected < (mTabLayout ?: return).tabCount) {
			val tab = mTabLayout?.getTabAt(currentSelected)
			tab?.select()
		}
	}

	var currentPosition: Int
		get() = currentSelected
		set(position) {
			this.currentSelected = position
			if (mViewPager != null) {
				mViewPager?.setCurrentItem(position, false)
			}
		}

	val currentFragment: BaseLazyFragment?
		get() {
			if (currentSelected >= 0 && currentSelected < fragments.size) {
				return fragments[currentSelected]
			}
			return null
		}

	interface OnTabReselectedListener {
		fun onTabReselected(position: Int, sortData: SortData?, fragment: BaseLazyFragment?)
	}
}

