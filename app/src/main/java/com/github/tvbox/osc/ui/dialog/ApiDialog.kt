package com.github.tvbox.osc.ui.dialog

import android.app.Dialog
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.github.tvbox.osc.R
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.server.ControlManager.Companion.instance
import com.github.tvbox.osc.ui.tv.QRCodeGen
import com.github.tvbox.osc.util.HistoryHelper.setApiHistory
import com.github.tvbox.osc.util.HistoryHelper.setLiveApiHistory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.jessyan.autosize.utils.AutoSizeUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 描述
 * 
 * @author pj567
 * @since 2020/12/27
 */
class ApiDialog(private val context: Context) {
	private val dialog: Dialog
	private val ivQRCode: ImageView
	private val tvAddress: TextView
	private val inputApi: EditText
	private val inputApiLive: EditText
	private var listener: OnListener? = null

	init {
		val view = LayoutInflater.from(context).inflate(R.layout.dialog_api, null)

		ivQRCode = view.findViewById(R.id.ivQRCode)
		tvAddress = view.findViewById(R.id.tvAddress)
		inputApi = view.findViewById(R.id.input)
		inputApiLive = view.findViewById(R.id.inputLive)

		dialog = MaterialAlertDialogBuilder(context)
			.setView(view)
			.create()

		// 初始化数据
		inputApi.setText(PreferenceStore.get(ConfigKey.API_URL, ""))
		inputApiLive.setText(PreferenceStore.get(ConfigKey.LIVE_API_URL, PreferenceStore.get(ConfigKey.API_URL, "")))

		view.findViewById<View>(R.id.inputSubmit).setOnClickListener { v: View? ->
			val newApi = inputApi.text.toString().trim { it <= ' ' }
			val newLiveApi = inputApiLive.text.toString().trim { it <= ' ' }

			// 保存点播配置
			if (!newApi.isEmpty()) {
				setApiHistory(newApi)
				PreferenceStore.put(ConfigKey.API_URL, newApi)
			}

			// 保存直播配置
			if (!newLiveApi.isEmpty()) {
				setLiveApiHistory(newLiveApi)
				PreferenceStore.put(ConfigKey.LIVE_API_URL, newLiveApi)
			} else if (!newApi.isEmpty()) {
				// 如果直播配置为空，使用点播配置
				PreferenceStore.put(ConfigKey.LIVE_API_URL, newApi)
			}

			if (listener != null) {
				(listener ?: return@setOnClickListener).onchange(newApi)
			}
			dialog.dismiss()
		}

		inputApi.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
			if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
				view.findViewById<View>(R.id.inputSubmit).performClick()
				return@setOnEditorActionListener true
			}
			false
		}

		inputApiLive.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
			if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
				view.findViewById<View>(R.id.inputSubmit).performClick()
				return@setOnEditorActionListener true
			}
			false
		}

		refreshQRCode()
		EventBus.getDefault().register(this)
	}

	fun show() {
		dialog.show()
	}

	fun dismiss() {
		EventBus.getDefault().unregister(this)
		dialog.dismiss()
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	fun refresh(event: RefreshEvent) {
		if (event.type == RefreshEvent.TYPE_API_URL_CHANGE) {
			inputApi.setText(event.obj as String?)
			inputApiLive.setText(event.obj as String?)
		}
	}

	private fun refreshQRCode() {
		val address = instance.getAddress(false)
		tvAddress.text = String.format("扫描上方二维码或访问地址\n%s", address)
		ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address + "api.html", AutoSizeUtils.mm2px(context, 300f), AutoSizeUtils.mm2px(context, 300f)))
	}

	fun setOnListener(listener: OnListener?) {
		this.listener = listener
	}

	fun interface OnListener {
		fun onchange(api: String?)
	}
}
