package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.api.ApiConfig.LoadConfigCallback
import com.github.tvbox.osc.base.BackPressProvider
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.base.ToolbarMenuProvider
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.ui.dialog.TipDialog
import com.github.tvbox.osc.ui.fragment.CollectFragment
import com.github.tvbox.osc.ui.fragment.HistoryFragment
import com.github.tvbox.osc.ui.fragment.HomeFragment
import com.github.tvbox.osc.ui.fragment.SearchFragment
import com.github.tvbox.osc.util.AppManager
import com.github.tvbox.osc.util.HawkConfig
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.orhanobut.hawk.Hawk
import kotlin.system.exitProcess

internal enum class Page {
	Home,
	History,
	Search,
	Collect,
}

class HomeActivity : BaseActivity() {
	// Fragments
	private var homeFragment: HomeFragment? = null
	private var historyFragment: HistoryFragment? = null
	private var searchFragment: SearchFragment? = null
	private var collectFragment: CollectFragment? = null

	// ----------------
	private val mHandler = Handler(Looper.getMainLooper())
	private var useCacheConfig = false
	private var mBottomNavigation: BottomNavigationView? = null
	private var viewPager: ViewPager2? = null
	private var currentMainPage: Page? = Page.Home
	private var mExitTime: Long = 0
	private var dataInitOk = false
	private var jarInitOk = false
	private var topAppBar: MaterialToolbar? = null
	private var appBarLayout: AppBarLayout? = null

	override val layoutResID: Int
		// --- BaseActivity ---
		get() = R.layout.activity_home

	override fun init() {
		ControlManager.instance.startServer()
		initView()
		useCacheConfig = false
		initData()

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				handleBackPress()
			}
		})
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)

		val bundle = intent.extras ?: return
		if (!bundle.getBoolean("openSearch", false)) return

		val searchTitle = bundle.getString("searchTitle")
		mHandler.postDelayed({
			showFragment(Page.Search, false)
			if (!searchTitle.isNullOrEmpty()) {
				mHandler.postDelayed({ searchWhenReady(searchTitle) }, 500)
			}
		}, 100)
	}

	override fun onDestroy() {
		super.onDestroy()
		AppManager.instance.appExit(0)
		ControlManager.instance.stopServer()
	}

	// ----------------
	// --- FragmentActivity ---
	override fun onPause() {
		super.onPause()
		mHandler.removeCallbacksAndMessages(null)
	}

	// ----------------
	private fun initView() {
		// 菜单
		appBarLayout = findViewById(R.id.appBarLayout)
		val toolbar = findViewById<MaterialToolbar>(R.id.appBar)
		topAppBar = toolbar
		toolbar.inflateMenu(R.menu.home_toolbar_menu)
		toolbar.setOnMenuItemClickListener { item: MenuItem ->
			val itemId = item.itemId
			if (itemId == R.id.action_settings) {
				jumpActivity(SettingsActivity::class.java)
				return@setOnMenuItemClickListener true
			}

			// 将菜单点击事件委托给实现了 ToolbarMenuProvider 的 Fragment
			val currentFragment = this.currentFragment
			if (currentFragment is ToolbarMenuProvider) {
				return@setOnMenuItemClickListener (currentFragment as ToolbarMenuProvider).onMenuItemClick(itemId)
			}
			false
		}

		// 设置 ViewPager2
		val pager = findViewById<ViewPager2>(R.id.contentLayout)
		viewPager = pager
		pager.setUserInputEnabled(false)

		// ViewPager2 页面改变监听
		pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				super.onPageSelected(position)

				val page = Page.entries[position]
				currentMainPage = page

				// 同步底部导航栏
				val bottomNavigation = mBottomNavigation ?: return
				when (page) {
					Page.Home -> bottomNavigation.selectedItemId = R.id.navigation_main
					Page.History -> bottomNavigation.selectedItemId = R.id.navigation_history
					Page.Search -> bottomNavigation.selectedItemId = R.id.navigation_search
					Page.Collect -> bottomNavigation.selectedItemId = R.id.navigation_favourite
				}

				// 强制展开 AppBar 和底部导航
				expandUI()

				mHandler.post { updateAppBarForCurrentPage() }
			}
		})

		// 设置底部导航栏监听器
		val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
		mBottomNavigation = bottomNavigation
		bottomNavigation.setOnItemSelectedListener { item: MenuItem ->
			val itemId = item.itemId
			when (itemId) {
				R.id.navigation_main -> {
					showFragment(Page.Home, true)
					return@setOnItemSelectedListener true
				}

				R.id.navigation_history -> {
					showFragment(Page.History, true)
					return@setOnItemSelectedListener true
				}

				R.id.navigation_search -> {
					showFragment(Page.Search, true)
					return@setOnItemSelectedListener true
				}

				R.id.navigation_favourite -> {
					showFragment(Page.Collect, true)
					return@setOnItemSelectedListener true
				}

				R.id.navigation_live -> {
					jumpActivity(LivePlayActivity::class.java)
					return@setOnItemSelectedListener false
				}

				else -> false
			}
		}

		setLoadSir(viewPager)
	}

	private fun initData() {
		if (dataInitOk && jarInitOk) {
			(viewPager ?: return).setAdapter(HomePagerAdapter(this))
			showFragment(Page.Home, false)
			showSuccess()
			if (!useCacheConfig && Hawk.get(HawkConfig.DEFAULT_LOAD_LIVE, false)) {
				jumpActivity(LivePlayActivity::class.java)
			}
			return
		}
		showLoading()
		if (dataInitOk && !jarInitOk) {
			if (!(ApiConfig.instance.spider ?: return).isEmpty()) {
				ApiConfig.instance.loadJar(useCacheConfig, ApiConfig.instance.spider ?: return, object : LoadConfigCallback {
					override fun success() {
						jarInitOk = true
						mHandler.postDelayed({ initData() }, 50)
					}

					override fun notice(msg: String?) {
						mHandler.post { Toast.makeText(this@HomeActivity, msg, Toast.LENGTH_SHORT).show() }
					}

					override fun error(msg: String?) {
						jarInitOk = true
						dataInitOk = true
						mHandler.postDelayed({
							Toast.makeText(this@HomeActivity, "$msg; 尝试加载最近一次的jar", Toast.LENGTH_SHORT).show()
							initData()
						}, 50)
					}
				})
			}
			return
		}
		ApiConfig.instance.loadConfig(useCacheConfig, object : LoadConfigCallback {
			var dialog: TipDialog? = null

			override fun notice(msg: String?) {
				mHandler.post { Toast.makeText(this@HomeActivity, msg, Toast.LENGTH_SHORT).show() }
			}

			override fun success() {
				dataInitOk = true
				if ((ApiConfig.instance.spider ?: return).isEmpty()) {
					jarInitOk = true
				}
				mHandler.postDelayed({ initData() }, 50)
			}

			override fun error(msg: String?) {
				if (msg.equals("-1", ignoreCase = true)) {
					mHandler.post {
						dataInitOk = true
						jarInitOk = true
						initData()
					}
					return
				}
				mHandler.post {
					if (dialog == null) dialog = TipDialog(this@HomeActivity, msg, "重试", "取消", object : TipDialog.OnListener {
						override fun left() {
							mHandler.post {
								initData()
								(dialog ?: return@post).hide()
							}
						}

						override fun right() {
							dataInitOk = true
							jarInitOk = true
							mHandler.post {
								initData()
								(dialog ?: return@post).hide()
							}
						}

						override fun cancel() {
							dataInitOk = true
							jarInitOk = true
							mHandler.post {
								initData()
								(dialog ?: return@post).hide()
							}
						}
					})
					if (!(dialog ?: return@post).isShowing) (dialog ?: return@post).show()
				}
			}
		}, this)
	}

	private val currentFragment: Fragment?
		get() {
			val position = viewPager?.currentItem ?: return null
			val page: Page = Page.entries[position]
			return when (page) {
				Page.Home -> homeFragment
				Page.History -> historyFragment
				Page.Search -> searchFragment
				Page.Collect -> collectFragment
			}
		}

	private fun showFragment(page: Page, smoothScroll: Boolean) {
		currentMainPage = page
		(viewPager ?: return).setCurrentItem(page.ordinal, smoothScroll)

		// 强制展开 AppBar 和底部导航
		expandUI()
	}

	fun expandUI() {
		expandAppBar()
		expandBottomNav()
	}

	fun expandAppBar() {
		appBarLayout?.setExpanded(true, true)
	}

	fun expandBottomNav() {
		val bottomNavigation = mBottomNavigation ?: return
		bottomNavigation.visibility = View.VISIBLE
		if (bottomNavigation.translationY != 0f) {
			bottomNavigation.animate()
				.translationY(0f)
				.setDuration(300)
				.start()
		}
	}

	fun collapseBottomNav() {
		val bottomNavigation = mBottomNavigation ?: return
		if (!bottomNavigation.isVisible) return

		bottomNavigation.animate()
			.translationY(bottomNavigation.height.toFloat())
			.setDuration(300)
			.withEndAction {
				bottomNavigation.visibility = View.GONE
			}
			.start()
	}

	private fun searchWhenReady(keyword: String, retry: Boolean = true) {
		val fragment = searchFragment
		if (fragment?.isAdded == true && fragment.context != null) {
			fragment.search(keyword)
		} else if (retry) {
			mHandler.postDelayed({ searchWhenReady(keyword, false) }, 500)
		}
	}

	fun switchToSearchAndSearch(keyword: String?) {
		showFragment(Page.Search, false)
		if (!keyword.isNullOrEmpty()) {
			mHandler.postDelayed({ searchWhenReady(keyword) }, 200)
		}
	}

	private fun updateAppBarForCurrentPage() {
		val fragment = this.currentFragment ?: return

		var enableScroll = false
		if (fragment is ToolbarMenuProvider) {
			enableScroll = fragment.enableAppBarScroll()
		}

		// 设置 AppBar 滚动行为
		val params = (topAppBar ?: return).layoutParams as AppBarLayout.LayoutParams
		if (enableScroll) {
			// 允许随滑动收起
			params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP)
		} else {
			// 不允许随滑动收起
			params.setScrollFlags(0)
		}
		(topAppBar ?: return).layoutParams = params

		// 清除菜单
		(topAppBar ?: return).getMenu().clear()

		if (fragment is ToolbarMenuProvider) {
			// 设置标题
			val title = fragment.toolbarTitle
			if (title != null) {
				(topAppBar ?: return).setTitle(title)
			} else {
				(topAppBar ?: return).setTitle(R.string.app_name)
			}

			// 加载 Fragment 特定的菜单（会在设置按钮之前添加）
			val menuResId = fragment.menuResId
			if (menuResId != 0) {
				(topAppBar ?: return).inflateMenu(menuResId)
			}
		} else {
			(topAppBar ?: return).setTitle(R.string.app_name)
		}

		// 加载通用菜单
		(topAppBar ?: return).inflateMenu(R.menu.home_toolbar_menu)
	}

	private fun handleBackPress() {
		// 检查当前页面是否有自定义返回处理
		val currentFragment = this.currentFragment
		if (currentFragment is BackPressProvider) {
			if (currentFragment.handleBackPress()) {
				return
			}
		}

		// 如果不在主页，先返回主页
		if (currentMainPage != Page.Home) {
			// 返回主页
			(mBottomNavigation ?: return).selectedItemId = R.id.navigation_main
			return
		}

		// 如果两次返回间隔小于 2000 毫秒，则退出应用
		if (System.currentTimeMillis() - mExitTime < 2000) {
			AppManager.instance.finishAllActivity()
			ControlManager.instance.stopServer()
			finish()
			Process.killProcess(Process.myPid())
			exitProcess(0)
		} else {
			// 否则仅提示用户，再按一次退出应用
			mExitTime = System.currentTimeMillis()
			Toast.makeText(mContext, "再按一次返回键退出应用", Toast.LENGTH_SHORT).show()
		}
	}

	private inner class HomePagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
		override fun createFragment(position: Int): Fragment {
			val page: Page = Page.entries[position]
			return when (page) {
				Page.Home -> {
					if (homeFragment == null) {
						homeFragment = HomeFragment()
					}
					homeFragment as Fragment
				}

				Page.History -> {
					if (historyFragment == null) {
						historyFragment = HistoryFragment()
					}
					historyFragment as Fragment
				}

				Page.Search -> {
					if (searchFragment == null) {
						searchFragment = SearchFragment()
					}
					searchFragment as Fragment
				}

				Page.Collect -> {
					if (collectFragment == null) {
						collectFragment = CollectFragment()
					}
					collectFragment as Fragment
				}
			}
		}

		override fun getItemCount(): Int {
			return Page.entries.size
		}
	}
}
