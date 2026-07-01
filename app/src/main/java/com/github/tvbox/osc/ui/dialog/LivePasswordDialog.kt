package com.github.tvbox.osc.ui.dialog

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.EditText
import com.github.tvbox.osc.R

/**
 * 描述
 * 
 * @author pj567
 * @since 2020/12/27
 */
class LivePasswordDialog(context: Context) : BaseDialog(context) {
	private val inputPassword: EditText
	var listener: OnListener? = null

	init {
		setOwnerActivity(context as Activity)
		setContentView(R.layout.dialog_live_password)
		inputPassword = findViewById(R.id.input)
		findViewById<View>(R.id.inputSubmit).setOnClickListener { v: View? ->
			val password = inputPassword.text.toString().trim { it <= ' ' }
			if (!password.isEmpty()) {
				listener?.onChange(password)
				dismiss()
			}
		}
	}

	override fun onBackPressed() {
		super.onBackPressed()
		listener?.onCancel()
		dismiss()
	}

	fun setOnListener(listener: OnListener?) {
		this.listener = listener
	}

	interface OnListener {
		fun onChange(password: String?)

		fun onCancel()
	}
}
