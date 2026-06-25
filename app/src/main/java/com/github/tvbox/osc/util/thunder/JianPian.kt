package com.github.tvbox.osc.util.thunder

import android.text.*
import androidx.core.net.*
import com.github.tvbox.osc.base.*
import com.github.tvbox.osc.util.*
import com.p2p.*
import java.io.*
import java.net.*

object JianPian {
	fun jpUrlDec(url: String?): String {
		val p2p = App.getP2P()
		if (p2p != null) {
			try {
				val decode = URLDecoder.decode(url, "UTF-8")
				val split = decode.split("\\|".toRegex()).filter { it.isNotEmpty() }
				var replace = split.firstOrNull()?.replace("xg://", "ftp://") ?: return ""
				if (replace.contains("xgplay://")) {
					replace = replace.replace("xgplay://", "ftp://")
				}
				if (!TextUtils.isEmpty(App.burl)) {
					p2p.P2Pdoxpause(App.burl?.toByteArray(charset("GBK")))
					p2p.P2Pdoxdel(App.burl?.toByteArray(charset("GBK")))
				}
				App.burl = replace
				p2p.P2Pdoxstart(replace.toByteArray(charset("GBK")))
				p2p.P2Pdoxadd(replace.toByteArray(charset("GBK")))
				val segment = replace.toUri().lastPathSegment ?: return ""
				return "http://${LocalIPAddress.getIP(App.instance)}:${P2PClass.port}/${URLEncoder.encode(segment, "GBK")}"
			} catch (e: Exception) {
				return e.localizedMessage ?: ""
			}
		}
		return ""
	}

	fun finish() {
		val p2p = App.getP2P()
		if (!TextUtils.isEmpty(App.burl) && p2p != null) {
			try {
				p2p.P2Pdoxpause(App.burl?.toByteArray(charset("GBK")))
				p2p.P2Pdoxdel(App.burl?.toByteArray(charset("GBK")))
				App.burl = ""
			} catch (e: UnsupportedEncodingException) {
				e.printStackTrace()
			}
		}
	}

	fun isJpUrl(url: String): Boolean {
		return url.startsWith("tvbox-xg:") || (Thunder.isFtp(url) && url.contains("gbl.114s"))
	}
}
