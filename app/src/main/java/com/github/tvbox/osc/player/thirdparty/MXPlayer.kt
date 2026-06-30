package com.github.tvbox.osc.player.thirdparty

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.core.net.toUri
import com.github.tvbox.osc.base.App.Companion.instance
import java.net.URLEncoder

object MXPlayer {
	const val TAG: String = "ThirdParty.MXPlayer"

	private const val PACKAGE_NAME_PRO = "com.mxtech.videoplayer.pro"
	private const val PACKAGE_NAME_AD = "com.mxtech.videoplayer.ad"
	private const val PLAYBACK_ACTIVITY_PRO = "com.mxtech.videoplayer.ActivityScreen"
	private const val PLAYBACK_ACTIVITY_AD = "com.mxtech.videoplayer.ad.ActivityScreen"
	private val PACKAGES = arrayOf(
		MXPackageInfo(PACKAGE_NAME_PRO, PLAYBACK_ACTIVITY_PRO),
		MXPackageInfo(PACKAGE_NAME_AD, PLAYBACK_ACTIVITY_AD),
	)

	val packageInfo: MXPackageInfo?
		/**
		 * @return null if any MX Player packages not exist.
		 */
		get() {
			for (pkg in PACKAGES) {
				try {
					val info = instance.packageManager.getApplicationInfo(pkg.packageName, 0)
					if (info.enabled) return pkg
					else Log.v(TAG, "MX Player package `" + pkg.packageName + "` is disabled.")
				} catch (ex: PackageManager.NameNotFoundException) {
					Log.v(TAG, "MX Player package `" + pkg.packageName + "` does not exist.")
				}
			}
			return null
		}

	fun run(activity: Activity, url: String?, title: String?, subtitle: String?, headers: HashMap<String, String>?): Boolean {
		var resolvedUrl = url ?: return false
		val packageInfo: MXPackageInfo = packageInfo ?: return false

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

			if (!subtitle.isNullOrEmpty()) {
				val parcels = arrayOfNulls<Parcelable>(1)
				parcels[0] = subtitle.toUri()
				intent.putExtra("subs", parcels)
				intent.putExtra("subs.enable", parcels)
			}
			activity.startActivity(intent)
			return true
		} catch (ex: Exception) {
			Log.e(TAG, "Can't run MX Player(Pro)", ex)
			return false
		}
	}

	class MXPackageInfo internal constructor(val packageName: String, val activityName: String)

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
