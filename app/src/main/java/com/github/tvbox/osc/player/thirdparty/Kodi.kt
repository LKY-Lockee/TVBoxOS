package com.github.tvbox.osc.player.thirdparty

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.github.tvbox.osc.base.App.Companion.instance
import java.net.URLEncoder

object Kodi {
	const val TAG: String = "ThirdParty.Kodi"

	private const val PACKAGE_NAME = "org.xbmc.kodi"
	private const val PLAYBACK_ACTIVITY = "org.xbmc.kodi.Splash"
	private val PACKAGES = arrayOf(
		KodiPackageInfo(PACKAGE_NAME, PLAYBACK_ACTIVITY),
	)

	val packageInfo: KodiPackageInfo?
		/**
		 * @return null if any Kodi packages not exist.
		 */
		get() {
			for (pkg in PACKAGES) {
				try {
					val info = instance.packageManager.getApplicationInfo(pkg.packageName, 0)
					if (info.enabled) return pkg
					else Log.v(TAG, "Kodi package `" + pkg.packageName + "` is disabled.")
				} catch (ex: PackageManager.NameNotFoundException) {
					Log.v(TAG, "Kodi package `" + pkg.packageName + "` does not exist.")
				}
			}
			return null
		}

	fun run(activity: Activity, url: String?, title: String?, subtitle: String?, headers: HashMap<String, String>?): Boolean {
		var resolvedUrl = url ?: return false
		val packageInfo: KodiPackageInfo = packageInfo ?: return false

		try {
			val intent = Intent(Intent.ACTION_VIEW)
			intent.setPackage(packageInfo.packageName)
			intent.setClassName(packageInfo.packageName, packageInfo.activityName)
			if (!headers.isNullOrEmpty()) {
				resolvedUrl = "$resolvedUrl|"
				val urlBuilder = StringBuilder(resolvedUrl)
				for ((idx, hk) in headers.keys.withIndex()) {
					urlBuilder.append(hk).append("=").append(URLEncoder.encode(headers[hk], "UTF-8"))
					if (idx < headers.size - 1) {
						urlBuilder.append("&")
					}
				}
				resolvedUrl = urlBuilder.toString()
			}
			intent.data = Uri.parse(resolvedUrl)
			intent.putExtra("title", title)
			intent.putExtra("name", title)

			if (!subtitle.isNullOrEmpty()) {
				intent.putExtra("subs", subtitle)
			}
			activity.startActivity(intent)
			return true
		} catch (ex: Exception) {
			Log.e(TAG, "Can't run Kodi", ex)
			return false
		}
	}

	class KodiPackageInfo internal constructor(val packageName: String, val activityName: String)

	private class Subtitle(uri: Uri) {
		val uri: Uri
		var name: String? = null
		var filename: String? = null

		init {
			checkNotNull(uri.scheme) { "Scheme is missed for subtitle URI $uri" }

			this.uri = uri
		}

		constructor(uriStr: String?) : this(Uri.parse(uriStr))
	}
}
