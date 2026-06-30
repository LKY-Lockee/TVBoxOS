package com.github.tvbox.osc.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.regex.Pattern

/**
 * 作者：By hdy
 * 日期：On 2018/11/1
 * 时间：At 19:17
 */
object LocalIPAddress {
	/**
	 * Ipv4 address check.
	 */
	private val IPV4_PATTERN: Pattern = Pattern.compile(
		"^(" + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
				"([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
	)
	private val IPV6_PATTERN: Pattern = Pattern.compile(
		"^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:)(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\\s*$"
	)

	@SuppressLint("DefaultLocale")
	fun getLocalIPAddress(context: Context): String {
		val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
		val ipAddress = wifiManager.connectionInfo.ipAddress
		if (ipAddress == 0) {
			try {
				val enumerationNi = NetworkInterface.getNetworkInterfaces()
				while (enumerationNi.hasMoreElements()) {
					val networkInterface = enumerationNi.nextElement()
					val interfaceName = networkInterface.displayName
					if (interfaceName == "eth0" || interfaceName == "wlan0") {
						val enumIpAddress = networkInterface.inetAddresses
						while (enumIpAddress.hasMoreElements()) {
							val inetAddress = enumIpAddress.nextElement()
							if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
								return inetAddress.hostAddress ?: "127.0.0.1"
							}
						}
					}
				}
			} catch (e: SocketException) {
				e.printStackTrace()
			}
		} else {
			return String.format("%d.%d.%d.%d", (ipAddress and 0xff), (ipAddress shr 8 and 0xff), (ipAddress shr 16 and 0xff), (ipAddress shr 24 and 0xff))
		}
		return "127.0.0.1"
	}

	fun getIP(context: Context): String {
		return try {
			val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
			// 判断wifi是否开启
			val wifiInfo: WifiInfo? = wifiManager?.connectionInfo
			var ipAddress = 0
			if (wifiInfo != null) {
				ipAddress = wifiInfo.ipAddress
			}
			intToIp(ipAddress)
		} catch (e: Exception) {
			e.printStackTrace()
			try {
				localIPAddress
			} catch (e1: Exception) {
				e1.printStackTrace()
				"127.0.0.1"
			}
		}
	}

	val localIPAddress: String
		get() {
			try {
				val mEnumeration = NetworkInterface.getNetworkInterfaces()
				while (mEnumeration.hasMoreElements()) {
					val networkInterface = mEnumeration.nextElement()
					val enumIPAddress = networkInterface.inetAddresses
					while (enumIPAddress.hasMoreElements()) {
						val inetAddress = enumIPAddress.nextElement()
						// 如果不是回环地址
						if (!inetAddress.isLoopbackAddress) {
							// 直接返回本地IP地址
							return inetAddress.hostAddress ?: "127.0.0.1"
						}
					}
				}
			} catch (ex: SocketException) {
				System.err.print("error")
			}
			return "127.0.0.1"
		}

	private fun intToIp(i: Int): String {
		return (i and 0xFF).toString() + "." +
				((i shr 8) and 0xFF) + "." +
				((i shr 16) and 0xFF) + "." +
				(i shr 24 and 0xFF)
	}

	/**
	 * Check if valid IPV4 address.
	 * 
	 * @param input the address string to check for validity.
	 * @return True if the input parameter is a valid IPv4 address.
	 */
	fun isIPv4Address(input: String): Boolean {
		return IPV4_PATTERN.matcher(input).matches()
	}

	fun isIPv6Address(str: String): Boolean {
		return IPV6_PATTERN.matcher(str).matches()
	}

	fun isIPAddress(str: String): Boolean {
		return isIPv4Address(str) || isIPv6Address(str)
	}

	fun isNetworkAvailable(context: Context): Boolean {
		var hasWifiCon = false
		var hasMobileCon = false

		val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		val netInfos = cm.allNetworkInfo
		for (net in netInfos) {
			val type = net.typeName
			if (type.equals("WIFI", ignoreCase = true)) {
				if (net.isConnected) {
					hasWifiCon = true
				}
			}

			if (type.equals("MOBILE", ignoreCase = true)) {
				if (net.isConnected) {
					hasMobileCon = true
				}
			}
		}
		return hasWifiCon || hasMobileCon
	}
}
