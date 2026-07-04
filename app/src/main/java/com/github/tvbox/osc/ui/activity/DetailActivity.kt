package com.github.tvbox.osc.ui.activity

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.App.Companion.instance
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.cache.RoomDataManger.insertVodRecord
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.ui.fragment.DetailTabInfoFragment
import com.github.tvbox.osc.ui.fragment.DetailTabPlaylistFragment
import com.github.tvbox.osc.ui.fragment.PlayFragment
import com.github.tvbox.osc.ui.fragment.SearchFragment
import com.github.tvbox.osc.util.SubtitleHelper.getTextSize
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.lzy.okgo.OkGo
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.math.max

/**
 * @author pj567
 * @date 2020/12/22
 */
class DetailActivity : BaseActivity() {
	var vodId: String = ""
	var sourceKey: String = ""
	var firstSourceKey: String? = null
	var previewVodInfo: VodInfo? = null
	private var isFullscreen = false
	private lateinit var llPlayerFragmentContainer: FragmentContainerView
	private lateinit var topLayout: View
	private lateinit var viewPager: ViewPager2
	private lateinit var playFragment: PlayFragment
	private var sourceViewModel: SourceViewModel? = null
	private var mVideo: Movie.Video? = null
	private var vodInfo: VodInfo? = null
	private var preFlag: String = ""
	private var vodPicture = ""
	private lateinit var tabLayout: TabLayout
	private lateinit var searchFragment: SearchFragment
	private lateinit var tabInfoFragment: DetailTabInfoFragment
	private lateinit var tabPlaylistFragment: DetailTabPlaylistFragment
	private var hasSearchedOnce = false

	private fun refreshFlag(itemView: View?, position: Int) {
		if (vodInfo == null || (vodInfo ?: return).seriesFlags == null || position >= ((vodInfo ?: return).seriesFlags ?: return).size) {
			return
		}

		val newFlag = ((vodInfo ?: return).seriesFlags ?: return)[position].name
		if ((vodInfo ?: return).playFlag != newFlag) {
			for (i in ((vodInfo ?: return).seriesFlags ?: return).indices) {
				val flag = ((vodInfo ?: return).seriesFlags ?: return)[i]
				if (flag.name == (vodInfo ?: return).playFlag) {
					flag.selected = false
					break
				}
			}
			val flag = ((vodInfo ?: return).seriesFlags ?: return)[position]
			flag.selected = true
			if (((vodInfo ?: return).seriesMap ?: return)[(vodInfo ?: return).playFlag] != null &&
				(((vodInfo ?: return).seriesMap ?: return)[(vodInfo ?: return).playFlag] ?: return).size > (vodInfo ?: return).playIndex
			) {
				(((vodInfo ?: return).seriesMap ?: return)[(vodInfo ?: return).playFlag] ?: return)[(vodInfo ?: return).playIndex].selected = false
			}
			(vodInfo ?: return).playFlag = newFlag
			tabPlaylistFragment.refreshList(vodInfo)
		}
	}

	private fun onSeriesSelected(position: Int) {
		if (vodInfo == null || ((vodInfo ?: return).seriesMap ?: return)[(vodInfo ?: return).playFlag] == null) {
			return
		}

		if (position == -1) {
			(vodInfo ?: return).reverseSort = !(vodInfo ?: return).reverseSort
			(vodInfo ?: return).reverse()
			tabPlaylistFragment.refreshList(vodInfo)
			return
		}

		val seriesList = ((vodInfo ?: return).seriesMap ?: return)[(vodInfo ?: return).playFlag]
		if ((seriesList ?: return).isEmpty() || position >= seriesList.size) {
			return
		}

		var reload = false

		val oldIndex = (vodInfo ?: return).playIndex

		if ((vodInfo ?: return).playIndex != position) {
			(vodInfo ?: return).playIndex = position
			reload = true
		}

		if (!preFlag.isEmpty() && (vodInfo ?: return).playFlag != preFlag) {
			reload = true
		}

		tabPlaylistFragment.updateSeriesSelection(oldIndex, position)

		if (reload) {
			jumpToPlay()
		}
	}

	override val layoutResID: Int
		get() = R.layout.activity_detail

	override fun init() {
		EventBus.getDefault().register(this)
		initView()
		initViewModel()
		initData()

		WindowCompat.setDecorFitsSystemWindows(window, false)
		val videoContainer = findViewById<View>(R.id.topLayout)
		ViewCompat.setOnApplyWindowInsetsListener(videoContainer) { v, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(v.paddingLeft, insets.top, v.paddingRight, v.paddingBottom)
			windowInsets
		}

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				if (isFullscreen) {
					if (playFragment.onBackPressed()) return
					requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
					val list = ((vodInfo ?: return).seriesMap ?: return)[(vodInfo ?: return).playFlag]
					if (list != null) {
						tabPlaylistFragment.setSeriesGroupVisibility(if (list.size > 1) View.VISIBLE else View.GONE)
					}
					tabPlaylistFragment.requestGridFocus()
					return
				}
				playFragment.setPlayTitle(false)

				isEnabled = false
				onBackPressedDispatcher.onBackPressed()
				isEnabled = true
			}
		})
	}

	private fun initView() {
		val llLayout = findViewById<ConstraintLayout?>(R.id.llLayout)
		topLayout = findViewById(R.id.topLayout)
		llPlayerFragmentContainer = findViewById(R.id.previewPlayer)

		preFlag = ""
		playFragment = PlayFragment()
		supportFragmentManager.beginTransaction().add(R.id.previewPlayer, playFragment).commit()
		supportFragmentManager.beginTransaction().show(playFragment).commitAllowingStateLoss()

		tabLayout = findViewById(R.id.tabLayout)
		viewPager = findViewById(R.id.viewPager)

		val tabInfoView = layoutInflater.inflate(R.layout.fragment_detail_tab_info, viewPager, false)
		val tabPlaylistView = layoutInflater.inflate(R.layout.fragment_detail_tab_playlist, viewPager, false)

		tabInfoFragment = DetailTabInfoFragment()
		tabPlaylistFragment = DetailTabPlaylistFragment()
		searchFragment = SearchFragment()

		tabInfoFragment.setContentView(tabInfoView)
		tabPlaylistFragment.setContentView(tabPlaylistView)

		tabPlaylistFragment.setOnSeriesFlagSelectedListener { flagName: String?, position: Int -> refreshFlag(null, position) }
		tabPlaylistFragment.setOnSeriesSelectedListener { position: Int -> this.onSeriesSelected(position) }

		val adapter: FragmentStateAdapter = object : FragmentStateAdapter(this) {
			override fun createFragment(position: Int): Fragment {
				return when (position) {
					0 -> tabInfoFragment
					1 -> tabPlaylistFragment
					2 -> searchFragment
					else -> Fragment()
				}
			}

			override fun getItemCount(): Int {
				return tabLayout.tabCount
			}

			override fun getItemViewType(position: Int): Int {
				return position
			}
		}
		viewPager.setAdapter(adapter)
		viewPager.setOffscreenPageLimit(adapter.itemCount)

		tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) {
				viewPager.currentItem = tab.position
				viewPager.post {
					if (tab.position == 2 && mVideo != null && !hasSearchedOnce) {
						searchFragment.search(mVideo?.name.orEmpty())
						hasSearchedOnce = true
					}
				}
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {
			}

			override fun onTabReselected(tab: TabLayout.Tab?) {
			}
		})

		viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				tabLayout.selectTab(tabLayout.getTabAt(position))
			}
		})

		setLoadSir(llLayout)
	}

	private fun jumpToPlay() {
		if (vodInfo != null && !(((vodInfo ?: return).seriesMap ?: return)[(vodInfo ?: return).playFlag] ?: return).isEmpty()) {
			preFlag = (vodInfo ?: return).playFlag
			//更新播放地址
			tabInfoFragment.setPlayUrl((((vodInfo ?: return).seriesMap ?: return)[(vodInfo ?: return).playFlag] ?: return)[(vodInfo ?: return).playIndex].url)
			val bundle = Bundle()
			//保存历史
			insertVod(firstSourceKey ?: return, vodInfo ?: return)
			bundle.putString("sourceKey", sourceKey)
			instance.vodInfo = vodInfo
			if (previewVodInfo == null) {
				try {
					val bos = ByteArrayOutputStream()
					val oos = ObjectOutputStream(bos)
					oos.writeObject(vodInfo)
					oos.flush()
					oos.close()
					val ois = ObjectInputStream(ByteArrayInputStream(bos.toByteArray()))
					previewVodInfo = ois.readObject() as VodInfo?
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			if (previewVodInfo != null) {
				(previewVodInfo ?: return).playerCfg = (vodInfo ?: return).playerCfg
				(previewVodInfo ?: return).playFlag = (vodInfo ?: return).playFlag
				(previewVodInfo ?: return).playIndex = (vodInfo ?: return).playIndex
				(previewVodInfo ?: return).seriesMap = (vodInfo ?: return).seriesMap
				instance.vodInfo = previewVodInfo
			}
			playFragment.setData(bundle)
		}
	}

	private fun initViewModel() {
		sourceViewModel = ViewModelProvider(this)[SourceViewModel::class.java]
		(sourceViewModel ?: return).detailResult.observe(this, Observer { absXml: AbsXml? ->
			if (absXml != null && absXml.movie != null && (absXml.movie ?: return@Observer).videoList != null && !((absXml.movie ?: return@Observer).videoList ?: return@Observer).isEmpty()) {
				showSuccess()

				val controller = WindowInsetsControllerCompat(window, window.decorView)
				controller.isAppearanceLightStatusBars = false

				if (!TextUtils.isEmpty(absXml.msg) && absXml.msg != "数据列表") {
					Toast.makeText(this@DetailActivity, absXml.msg, Toast.LENGTH_SHORT).show()
					showEmpty()
					return@Observer
				}
				mVideo = ((absXml.movie ?: return@Observer).videoList ?: return@Observer)[0]
				(mVideo ?: return@Observer).id = vodId
				hasSearchedOnce = false
				if (TextUtils.isEmpty((mVideo ?: return@Observer).name)) (mVideo ?: return@Observer).name = "TVBox"
				vodInfo = VodInfo()
				if (((mVideo ?: return@Observer).pic == null || ((mVideo ?: return@Observer).pic ?: return@Observer).isEmpty()) && !vodPicture.isEmpty()) {
					(mVideo ?: return@Observer).pic = vodPicture
				}
				(vodInfo ?: return@Observer).setVideo(mVideo ?: return@Observer)
				(vodInfo ?: return@Observer).sourceKey = (mVideo ?: return@Observer).sourceKey
				sourceKey = (mVideo ?: return@Observer).sourceKey

				tabInfoFragment.setVideoInfo(mVideo, sourceKey, firstSourceKey.orEmpty(), vodId)

				if ((vodInfo ?: return@Observer).seriesMap != null && !((vodInfo ?: return@Observer).seriesMap ?: return@Observer).isEmpty()) {
					val vodInfoRecord = RoomDataManger.getVodInfo(sourceKey, vodId)
					// 读取历史记录
					if (vodInfoRecord != null) {
						(vodInfo ?: return@Observer).playIndex = max(vodInfoRecord.playIndex, 0)
						(vodInfo ?: return@Observer).playFlag = vodInfoRecord.playFlag
						(vodInfo ?: return@Observer).playerCfg = vodInfoRecord.playerCfg
						(vodInfo ?: return@Observer).reverseSort = vodInfoRecord.reverseSort
					} else {
						(vodInfo ?: return@Observer).playIndex = 0
						(vodInfo ?: return@Observer).playFlag = ""
						(vodInfo ?: return@Observer).playerCfg = ""
						(vodInfo ?: return@Observer).reverseSort = false
					}

					if ((vodInfo ?: return@Observer).reverseSort) {
						(vodInfo ?: return@Observer).reverse()
					}

					if (!((vodInfo ?: return@Observer).seriesMap ?: return@Observer).containsKey((vodInfo ?: return@Observer).playFlag)) (vodInfo ?: return@Observer).playFlag = ((vodInfo ?: return@Observer).seriesMap ?: return@Observer).keys.toTypedArray()[0]

					//设置播放地址
					tabInfoFragment.setPlayUrl((((vodInfo ?: return@Observer).seriesMap ?: return@Observer)[(vodInfo ?: return@Observer).playFlag] ?: return@Observer)[0].url)

					tabPlaylistFragment.setVodInfo(vodInfo)

					jumpToPlay()
					llPlayerFragmentContainer.visibility = View.VISIBLE
					toggleSubtitleTextSize()
				} else {
					tabPlaylistFragment.setPlaylistVisibility(View.GONE)
				}
			} else {
				showEmpty()
				llPlayerFragmentContainer.visibility = View.GONE
			}
		})
	}

	private fun initData() {
		val intent = getIntent()
		if (intent != null && intent.extras != null) {
			val bundle = intent.extras
			vodPicture = (bundle ?: return).getString("picture", "")
			loadDetail(bundle.getString("id", null), bundle.getString("sourceKey", ""))
		}
	}

	private fun loadDetail(vid: String?, key: String) {
		if (vid != null) {
			vodId = vid
			sourceKey = key
			firstSourceKey = key
			showLoading()
			(sourceViewModel ?: return).getDetail(sourceKey, vodId)
			tabInfoFragment.updateCollectButton()
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	fun refresh(event: RefreshEvent) {
		if (event.type == RefreshEvent.TYPE_REFRESH) {
			if (event.obj != null) {
				when (event.obj) {
					is Int -> {
						val index = event.obj as Int
						val oldIndex = (vodInfo ?: return).playIndex
						(vodInfo ?: return).playIndex = index
						tabPlaylistFragment.updateSeriesSelection(oldIndex, index)
						//保存历史
						insertVod(firstSourceKey ?: return, vodInfo ?: return)
					}

					is JSONObject -> {
						(vodInfo ?: return).playerCfg = event.obj.toString()
						//保存历史
						insertVod(firstSourceKey ?: return, vodInfo ?: return)
					}

					is String -> {
						val url = event.obj.toString()
						//设置更新播放地址
						tabInfoFragment.setPlayUrl(url)
					}
				}
			}
		} else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_SELECT) {
			if (event.obj != null) {
				val video = event.obj as Movie.Video?
				loadDetail((video ?: return).id, video.sourceKey)
			}
		}
	}

	private fun insertVod(sourceKey: String, vodInfo: VodInfo) {
		try {
			vodInfo.playNote = ((vodInfo.seriesMap ?: return)[vodInfo.playFlag] ?: return)[vodInfo.playIndex].name
		} catch (th: Throwable) {
			vodInfo.playNote = ""
		}
		insertVodRecord(sourceKey, vodInfo)
		EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH))
	}

	override fun onDestroy() {
		super.onDestroy()
		OkGo.getInstance().cancelTag("fenci")
		OkGo.getInstance().cancelTag("detail")
		EventBus.getDefault().unregister(this)
	}

	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		val fragment = playFragment
		if (isFullscreen && fragment.dispatchKeyEvent(event)) {
			return true
		}
		return super.dispatchKeyEvent(event)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		val fragment = playFragment
		if (event != null && isFullscreen && fragment.onKeyDown(keyCode, event)) {
			return true
		}
		return super.onKeyDown(keyCode, event)
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		val fragment = playFragment
		if (event != null && isFullscreen && fragment.onKeyUp(keyCode, event)) {
			return true
		}
		return super.onKeyUp(keyCode, event)
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			isFullscreen = true
			hideSystemUI()
			updatePlayerLayoutForFullscreen(true)
			toggleSubtitleTextSize()
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			isFullscreen = false
			showSystemUI()
			updatePlayerLayoutForFullscreen(false)
			toggleSubtitleTextSize()
		}
	}

	private fun updatePlayerLayoutForFullscreen(fullscreen: Boolean) {

		if (fullscreen) {
			// 隐藏所有其他UI元素
			tabLayout.visibility = View.GONE
			viewPager.visibility = View.GONE

			// 修改topLayout的布局参数，使其填充整个屏幕
			val topParams =
				topLayout.layoutParams as ConstraintLayout.LayoutParams
			topParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
			topParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
			topParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
			topParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
			topParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
			topParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
			topLayout.layoutParams = topParams

			// 修改previewPlayer的布局参数，使其填充整个topLayout
			val playerParams =
				llPlayerFragmentContainer.layoutParams as ConstraintLayout.LayoutParams
			playerParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
			playerParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
			playerParams.dimensionRatio = null
			playerParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
			playerParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
			playerParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
			playerParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
			llPlayerFragmentContainer.layoutParams = playerParams
		} else {
			// 恢复UI元素显示
			tabLayout.visibility = View.VISIBLE
			viewPager.visibility = View.VISIBLE

			// 恢复topLayout的布局参数
			val topParams =
				topLayout.layoutParams as ConstraintLayout.LayoutParams
			topParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
			topParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
			topParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
			topParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
			topParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
			topParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
			topLayout.layoutParams = topParams

			// 恢复previewPlayer的布局参数
			val playerParams =
				llPlayerFragmentContainer.layoutParams as ConstraintLayout.LayoutParams
			playerParams.width = 0
			playerParams.height = 0
			playerParams.dimensionRatio = "H,16:9"
			playerParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
			playerParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
			playerParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
			playerParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
			llPlayerFragmentContainer.layoutParams = playerParams
		}

		topLayout.requestLayout()
		llPlayerFragmentContainer.requestLayout()
	}

	private fun hideSystemUI() {
		val controller = WindowInsetsControllerCompat(window, window.decorView)
		controller.hide(WindowInsetsCompat.Type.systemBars())
		controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
	}

	private fun showSystemUI() {
		val controller = WindowInsetsControllerCompat(window, window.decorView)
		controller.show(WindowInsetsCompat.Type.systemBars())
		controller.isAppearanceLightStatusBars = false
	}

	fun toggleSubtitleTextSize() {
		var subtitleTextSize = getTextSize(this)
		if (!isFullscreen) {
			subtitleTextSize = (subtitleTextSize * 0.6).toInt()
		}
		EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE, subtitleTextSize))
	}
}
