package com.github.tvbox.osc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.tvbox.osc.ui.activity.MainActivity

/**
 * @author pj567
 * @date 2021/1/5
 */
class SearchReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (ACTION != intent.action || intent.extras == null) return
		val searchTitle = intent.extras?.getString("title") ?: return
		val newIntent = Intent(context, MainActivity::class.java)
		newIntent.putExtra("openSearch", true)
		newIntent.putExtra("searchTitle", searchTitle)
		newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
		context.startActivity(newIntent)
	}

	companion object {
		const val ACTION: String = "android.content.movie.search.Action"
	}
}
