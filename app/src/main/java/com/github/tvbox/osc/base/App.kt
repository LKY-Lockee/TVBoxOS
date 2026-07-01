package com.github.tvbox.osc.base

import android.app.Activity
import android.app.Application
import androidx.media3.common.util.UnstableApi
import com.github.catvod.crawler.JsLoader
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.callback.EmptyCallback
import com.github.tvbox.osc.callback.LoadingCallback
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.util.AppManager
import com.github.tvbox.osc.util.EpgUtil
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.OkGoHelper
import com.github.tvbox.osc.util.PlayerHelper
import com.github.tvbox.osc.util.TVBoxRuntimeLog
import com.google.android.material.color.DynamicColors
import com.kingja.loadsir.core.LoadSir
import com.p2p.P2PClass
import com.whl.quickjs.android.QuickJSLoader
import me.jessyan.autosize.AutoSizeConfig
import me.jessyan.autosize.unit.Subunits

/**
 * @author pj567
 * @date :2020/12/17
 */
class App : Application() {
	var vodInfo: VodInfo? = null

	val currentActivity: Activity?
		get() = AppManager.instance.currentActivity()

	@UnstableApi
	override fun onCreate() {
		super.onCreate()
		instance = this
		initParams()
		// OKGo
		OkGoHelper.init() //台标获取
		EpgUtil.init()
		// 初始化Web服务器
		ControlManager.init(this)
		LoadSir.beginBuilder()
			.addCallback(EmptyCallback())
			.addCallback(LoadingCallback())
			.commit()
		AutoSizeConfig.getInstance().setCustomFragment(true).unitsManager
			.setSupportDP(false)
			.setSupportSP(false)
			.setSupportSubunits(Subunits.MM)
		PlayerHelper.init()
		QuickJSLoader.init()
		FileUtils.cleanPlayerCache()
		DynamicColors.applyToActivitiesIfAvailable(this)
	}

	override fun onTerminate() {
		super.onTerminate()
		JsLoader.destroy()
	}

	private fun initParams() {
		PreferenceStore.init(this)
		PreferenceStore.put(ConfigKey.DEBUG_OPEN, false)
		if (PreferenceStore.get(ConfigKey.PLAY_TYPE, -1) == -1) {
			PreferenceStore.put(ConfigKey.PLAY_TYPE, 1)
		}
	}

	companion object {
		var burl: String? = null
		var dashData: String? = null
		private var p: P2PClass? = null

		lateinit var instance: App
			private set

		fun getP2P(): P2PClass? {
			try {
				if (p == null) {
					p = P2PClass(FileUtils.externalCachePath)
				}
				return p
			} catch (e: Exception) {
				TVBoxRuntimeLog.e(e.toString())
				return null
			}
		}
	}
}
