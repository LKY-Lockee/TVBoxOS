package com.github.tvbox.osc.ui.activity

import android.content.ClipboardManager
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.server.ControlManager.Companion.instance
import com.github.tvbox.osc.ui.tv.QRCodeGen
import me.jessyan.autosize.utils.AutoSizeUtils

class PushActivity : BaseActivity() {
	private lateinit var ivQRCode: ImageView
	private lateinit var tvAddress: TextView

	override val layoutResID: Int
		get() = R.layout.activity_push

	override fun init() {
		initView()
		initData()
	}

	private fun initView() {
		ivQRCode = findViewById(R.id.ivQRCode)
		tvAddress = findViewById(R.id.tvAddress)
		refreshQRCode()
		findViewById<View>(R.id.pushLocal).setOnClickListener {
			try {
				val manager = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
				val clip = manager?.primaryClip
				if (clip != null && manager.hasPrimaryClip() && clip.itemCount > 0) {
					val addedText = clip.getItemAt(0)
					val clipText = addedText.text.toString().trim { it <= ' ' }
					val newIntent = Intent(mContext, DetailActivity::class.java)
					newIntent.putExtra("id", clipText)
					newIntent.putExtra("sourceKey", "push_agent")
					newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
					startActivity(newIntent)
				}
			} catch (ignored: Throwable) {
			}
		}
	}

	private fun refreshQRCode() {
		val address = instance.getAddress(false)
		tvAddress.text = String.format("扫描上方二维码或访问地址\n%s", address)
		ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address + "push.html", AutoSizeUtils.mm2px(this, 300f), AutoSizeUtils.mm2px(this, 300f), 4))
	}

	private fun initData() {
	}
}
