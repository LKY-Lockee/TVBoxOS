package com.github.tvbox.osc.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.github.tvbox.osc.R
import com.github.tvbox.osc.callback.EmptyCallback
import com.github.tvbox.osc.callback.LoadingCallback
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.player.thirdparty.RemoteTVBox.available
import com.github.tvbox.osc.ui.activity.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kingja.loadsir.core.LoadService
import com.kingja.loadsir.core.LoadSir
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

open class SearchRemoteTvDialog(private val context: Context) {
	private val dialog: Dialog
	private val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_search_remotetv, null)
	private var mLoadService: LoadService<*>? = null

	init {
		dialog = MaterialAlertDialogBuilder(context)
			.setView(view)
			.create()
		EventBus.getDefault().register(this)

		val btnCancel = view.findViewById<Button>(R.id.btnCancel)
		btnCancel.setOnClickListener { v: View? -> dismiss() }
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
		if (event.type == RefreshEvent.TYPE_SETTING_SEARCH_TV) {
			showRemoteTvDialog(SettingsActivity.foundRemoteTv)
		}
	}

	fun setTip(tip: String?) {
		(view.findViewById<View?>(R.id.title) as TextView).text = tip
		setLoadSir(view.findViewById(R.id.list))
		showLoading()
	}

	private fun showRemoteTvDialog(found: Boolean) {
		if (!found) {
			SettingsActivity.loadingSearchRemoteTvDialog?.showEmpty()
			Toast.makeText(context, "未找到附近TVBox", Toast.LENGTH_SHORT).show()
			return
		}
		SettingsActivity.loadingSearchRemoteTvDialog?.dismiss()
		available = SettingsActivity.remoteTvHostList[0]

		val hosts = SettingsActivity.remoteTvHostList.toTypedArray<String?>()
		MaterialAlertDialogBuilder(context)
			.setTitle("附近TVBox")
			.setSingleChoiceItems(hosts, 0) { dlg: DialogInterface?, which: Int ->
				available = hosts[which]
				Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show()
				dlg?.dismiss()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	protected fun setLoadSir(view: View?) {
		if (mLoadService == null) {
			mLoadService = LoadSir.getDefault().register(view) { v: View? -> }
		}
	}

	fun showLoading() {
		if (mLoadService != null) {
			(mLoadService ?: return).showCallback(LoadingCallback::class.java)
		}
	}

	fun showEmpty() {
		if (null != mLoadService) {
			(mLoadService ?: return).showCallback(EmptyCallback::class.java)
		}
	}

	fun showSuccess() {
		if (null != mLoadService) {
			(mLoadService ?: return).showSuccess()
		}
	}
}
