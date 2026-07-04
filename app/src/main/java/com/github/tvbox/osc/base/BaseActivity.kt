package com.github.tvbox.osc.base

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.github.tvbox.osc.R
import com.github.tvbox.osc.callback.EmptyCallback
import com.github.tvbox.osc.callback.LoadingCallback
import com.github.tvbox.osc.util.AppManager
import com.kingja.loadsir.core.LoadService
import com.kingja.loadsir.core.LoadSir
import me.jessyan.autosize.AutoSizeCompat
import me.jessyan.autosize.internal.CustomAdapt
import kotlin.math.max
import kotlin.math.min

/**
 * @author pj567
 * @date :2020/12/17
 */
abstract class BaseActivity : AppCompatActivity(), CustomAdapt {
	protected lateinit var mContext: Context
	protected open val layoutResID: Int = 0
	private var mLoadService: LoadService<*>? = null

	fun jumpActivity(clazz: Class<out BaseActivity>) {
		val intent = Intent(mContext, clazz)
		startActivity(intent)
	}

	fun jumpActivity(clazz: Class<out BaseActivity>, bundle: Bundle) {
		val intent = Intent(mContext, clazz)
		intent.putExtras(bundle)
		startActivity(intent)
	}

	fun hasPermission(permission: String): Boolean {
		var has = true
		try {
			has = PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return has
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		try {
			if (screenRatio < 0) {
				val dm = resources.displayMetrics
				val screenWidth = dm.widthPixels
				val screenHeight = dm.heightPixels
				screenRatio = max(screenWidth, screenHeight).toFloat() / min(screenWidth, screenHeight).toFloat()
			}
		} catch (th: Throwable) {
			th.printStackTrace()
		}
		super.onCreate(savedInstanceState)
		if (layoutResID != 0) setContentView(this.layoutResID)
		mContext = this
		AppManager.instance.addActivity(this)
		init()
	}

	override fun onResume() {
		super.onResume()
	}

	override fun getResources(): Resources {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			AutoSizeCompat.autoConvertDensityOfCustomAdapt(super.getResources(), this)
		}
		return super.getResources()
	}

	override fun getSizeInDp(): Float {
		return (if (isBaseOnWidth) 1280 else 720).toFloat()
	}

	override fun isBaseOnWidth(): Boolean {
		return !(screenRatio >= 4.0f)
	}

	override fun onDestroy() {
		super.onDestroy()
		AppManager.instance.finishActivity(this)
	}

	protected abstract fun init()

	protected fun setLoadSir(view: View?) {
		if (mLoadService == null) {
			mLoadService = LoadSir.getDefault().register(view) { v: View? -> }
		}
	}

	protected fun showLoading() {
		mLoadService?.showCallback(LoadingCallback::class.java)
	}

	protected fun showEmpty() {
		mLoadService?.showCallback(EmptyCallback::class.java)
	}

	protected fun showSuccess() {
		mLoadService?.showSuccess()
	}

	/**
	 * 自动为 AppBarLayout 设置透明度渐变效果
	 * 当标题栏向上收起时，透明度会逐渐降低，避免与状态栏内容重叠
	 */
	companion object {
		private var screenRatio = -100.0f
	}
}