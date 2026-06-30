package com.github.tvbox.osc.player.thirdparty

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.github.tvbox.osc.base.App.Companion.instance
import org.json.JSONException
import org.json.JSONObject

object ReexPlayer {
	const val TAG: String = "ThirdParty.Reex"

	private const val PACKAGE_NAME = "xyz.re.player.ex"
	private const val PLAYBACK_ACTIVITY = "xyz.re.player.ex.MainActivity"
	private val PACKAGES = arrayOf(
		ReexPackageInfo(PACKAGE_NAME, PLAYBACK_ACTIVITY),
	)

	val packageInfo: ReexPackageInfo?
		get() {
			for (pkg in PACKAGES) {
				try {
					val info = instance.packageManager.getApplicationInfo(pkg.packageName, 0)
					if (info.enabled) return pkg
					else Log.v(TAG, "Reex Player package `" + pkg.packageName + "` is disabled.")
				} catch (ex: PackageManager.NameNotFoundException) {
					Log.v(TAG, "Reex Player package `" + pkg.packageName + "` does not exist.")
				}
			}
			return null
		}

	fun run(activity: Activity, url: String?, title: String?, subtitle: String?, headers: HashMap<String, String>?): Boolean {
		val packageInfo: ReexPackageInfo = packageInfo ?: return false

		val intent = Intent(Intent.ACTION_VIEW)
		intent.setPackage(packageInfo.packageName)
		intent.component = ComponentName(packageInfo.packageName, packageInfo.activityName)
		intent.data = Uri.parse(url)
		intent.putExtra("title", title)
		intent.putExtra("name", title)
		intent.putExtra("reex.extra.title", title)
		if (!headers.isNullOrEmpty()) {
			try {
				val json = JSONObject()
				for (key in headers.keys) {
					headers[key]?.let { str -> json.put(key, str.trim { it <= ' ' }) }
				}
				intent.putExtra("reex.extra.http_header", json.toString())
			} catch (e: JSONException) {
				e.printStackTrace()
			}
		}
		if (!subtitle.isNullOrEmpty()) {
			intent.putExtra("reex.extra.subtitle", subtitle)
		}
		try {
			activity.startActivity(intent)
			return true
		} catch (ex: ActivityNotFoundException) {
			Log.e(TAG, "Can't run Reex Player(Pro)", ex)
			return false
		}
	}

	class ReexPackageInfo internal constructor(val packageName: String, val activityName: String)
}
