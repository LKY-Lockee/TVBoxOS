package com.github.tvbox.osc.util

import android.graphics.Bitmap
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.picasso.MyOkhttpDownLoader
import com.github.tvbox.osc.util.ssl.SSLSocketFactoryCompat
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.lzy.okgo.OkGo
import com.lzy.okgo.https.HttpsUtils
import com.lzy.okgo.interceptor.HttpLoggingInterceptor
import com.orhanobut.hawk.Hawk
import com.squareup.picasso.Picasso
import okhttp3.Cache
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import xyz.doikki.videoplayer.exo.ExoMediaSourceHelper
import java.io.File
import java.net.InetAddress
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

object OkGoHelper {
	const val DEFAULT_MILLISECONDS: Long = 10000 // 默认的超时时间
	val dnsHttpsList: ArrayList<String> = ArrayList()

	// 内置doh json
	private const val DNS_CONFIG_JSON = ("""[{"name": "腾讯", "url": "https://doh.pub/dns-query"},{"name": "阿里", "url": "https://dns.alidns.com/dns-query"},{"name": "360", "url": "https://doh.360.cn/dns-query"}]""")
	var dnsOverHttps: DnsOverHttps? = null
	var is_doh: Boolean = false
	var myHosts: MutableMap<String, String>? = null
	var ItvClient: OkHttpClient? = null
	var defaultClient: OkHttpClient? = null
	var noRedirectClient: OkHttpClient? = null

	@UnstableApi
	fun initExoOkHttpClient() {
		val dns = dnsOverHttps ?: return
		val builder = OkHttpClient.Builder()
		val loggingInterceptor = HttpLoggingInterceptor("OkExoPlayer")

		if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
			loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY)
			loggingInterceptor.setColorLevel(Level.INFO)
		} else {
			loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE)
			loggingInterceptor.setColorLevel(Level.OFF)
		}
		builder.addInterceptor(loggingInterceptor)

		builder.retryOnConnectionFailure(true)
		builder.followRedirects(true)
		builder.followSslRedirects(true)

		try {
			setOkHttpSsl(builder)
		} catch (th: Throwable) {
			th.printStackTrace()
		}

		// builder.dns(dnsOverHttps);
		builder.dns(CustomDns(dns))
		val itvClient = builder.build()
		ItvClient = itvClient

		ExoMediaSourceHelper.getInstance(App.instance)
			.setHttpDataSourceFactory(OkHttpDataSource.Factory(itvClient))
	}

	fun getDohUrl(type: Int): String {
		var json = Hawk.get(HawkConfig.DOH_JSON, "")
		if (json.isEmpty()) json = DNS_CONFIG_JSON
		val jsonArray = JsonParser.parseString(json).asJsonArray
		if (type >= 1 && type < dnsHttpsList.size) {
			val dnsConfig = jsonArray.get(type - 1).asJsonObject
			return dnsConfig.get("url").asString // 获取对应的 URL
		}
		return ""
	}

	fun setDnsList() {
		dnsHttpsList.clear()
		var json = Hawk.get(HawkConfig.DOH_JSON, "")
		if (json.isEmpty()) json = DNS_CONFIG_JSON
		val jsonArray = JsonParser.parseString(json).asJsonArray
		dnsHttpsList.add("关闭")
		for (i in 0..<jsonArray.size()) {
			val dnsConfig = jsonArray.get(i).asJsonObject
			val name = if (dnsConfig.has("name")) dnsConfig.get("name").asString else "Unknown Name"
			dnsHttpsList.add(name)
		}
		if (Hawk.get(HawkConfig.DOH_URL, 0) + 1 > dnsHttpsList.size) Hawk.put(HawkConfig.DOH_URL, 0)
	}

	private fun dohIps(ips: JsonArray?): List<InetAddress> {
		val inetAddresses: MutableList<InetAddress> = ArrayList()
		if (ips != null) {
			for (j in 0..<ips.size()) {
				try {
					val inetAddress = InetAddress.getByName(ips.get(j).asString)
					inetAddresses.add(inetAddress) // 添加到 List 中
				} catch (e: Exception) {
					e.printStackTrace() // 处理无效的 IP 字符串
				}
			}
		}
		return inetAddresses
	}

	fun initDnsOverHttps() {
		val dohSelector = Hawk.get(HawkConfig.DOH_URL, 0)
		var ips: JsonArray? = null
		try {
			dnsHttpsList.add("关闭")
			var json = Hawk.get(HawkConfig.DOH_JSON, "")
			if (json.isEmpty()) json = DNS_CONFIG_JSON
			val jsonArray = JsonParser.parseString(json).asJsonArray
			if (dohSelector + 1 > jsonArray.size()) Hawk.put(HawkConfig.DOH_URL, 0)
			for (i in 0..<jsonArray.size()) {
				val dnsConfig = jsonArray.get(i).asJsonObject
				val name = if (dnsConfig.has("name")) dnsConfig.get("name").asString else "Unknown Name"
				dnsHttpsList.add(name)
				if (dohSelector == (i + 1)) ips = if (dnsConfig.has("ips")) dnsConfig.getAsJsonArray("ips") else null
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}

		val builder = OkHttpClient.Builder()
		val loggingInterceptor = HttpLoggingInterceptor("OkExoPlayer")
		if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
			loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY)
			loggingInterceptor.setColorLevel(Level.INFO)
		} else {
			loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE)
			loggingInterceptor.setColorLevel(Level.OFF)
		}
		builder.addInterceptor(loggingInterceptor)
		try {
			setOkHttpSsl(builder)
		} catch (th: Throwable) {
			th.printStackTrace()
		}
		builder.cache(Cache(File(App.instance.cacheDir.absolutePath, "dohcache"), (100 * 1024 * 1024).toLong()))
		val dohClient = builder.build()
		val dohUrl = getDohUrl(Hawk.get(HawkConfig.DOH_URL, 0))
		dnsOverHttps = DnsOverHttps.Builder(dohClient)
			.url(if (dohUrl.isEmpty()) null else dohUrl.toHttpUrl())
			.bootstrapDnsHosts(if (ips != null && dohUrl != "https://doh.pub/dns-query") dohIps(ips) else null)
			.build()
	}

	@UnstableApi
	fun init() {
		initDnsOverHttps()

		val dns = dnsOverHttps ?: return
		val builder = OkHttpClient.Builder()
		val loggingInterceptor = HttpLoggingInterceptor("OkGo")

		if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
			loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY)
			loggingInterceptor.setColorLevel(Level.INFO)
		} else {
			loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE)
			loggingInterceptor.setColorLevel(Level.OFF)
		}

		// builder.retryOnConnectionFailure(false);
		builder.addInterceptor(loggingInterceptor)

		builder.readTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
		builder.writeTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
		builder.connectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)

		builder.dns(dns)
		try {
			setOkHttpSsl(builder)
		} catch (th: Throwable) {
			th.printStackTrace()
		}

		val okHttpClient = builder.build()
		OkGo.getInstance().setOkHttpClient(okHttpClient)

		defaultClient = okHttpClient

		builder.followRedirects(false)
		builder.followSslRedirects(false)
		noRedirectClient = builder.build()

		initExoOkHttpClient()
		initPicasso(okHttpClient)
	}

	fun initPicasso(client: OkHttpClient) {
		client.dispatcher.maxRequestsPerHost = 10
		val downloader = MyOkhttpDownLoader(client)
		val picasso = Picasso.Builder(App.instance)
			.downloader(downloader)
			.defaultBitmapConfig(Bitmap.Config.RGB_565)
			.build()
		Picasso.setSingletonInstance(picasso)
	}

	private fun setOkHttpSsl(builder: OkHttpClient.Builder) {
		try {
			// 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
			val trustAllCert: X509TrustManager = object : X509TrustManager {
				override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
				override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
				override fun getAcceptedIssuers(): Array<X509Certificate> {
					return arrayOf()
				}
			}
			val sslSocketFactory: SSLSocketFactory = SSLSocketFactoryCompat(trustAllCert)
			builder.sslSocketFactory(sslSocketFactory, trustAllCert)
			builder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier)
		} catch (e: Exception) {
			throw RuntimeException(e)
		}
	}

	// 自定义 DNS 解析器
	internal class CustomDns // 接收外部注入的 DoH 实例
		(private val mDnsOverHttps: DnsOverHttps) : Dns {
		override fun lookup(hostname: String): List<InetAddress> {
			var resolvedHost = hostname
			if (myHosts == null) {
				myHosts = ApiConfig.instance.myHost // 确保只获取一次减少消耗
			}
			val hosts = myHosts
			if (!hosts.isNullOrEmpty() && hosts.containsKey(resolvedHost)) {
				resolvedHost = hosts[resolvedHost] ?: resolvedHost
			}
			return if (isValidIpAddress(resolvedHost)) {
				mutableListOf(InetAddress.getByName(resolvedHost))
			} else {
				mDnsOverHttps.lookup(resolvedHost)
			}
		}

		fun mapHosts(hosts: Map<String, String>) {
			val map = ConcurrentHashMap<String, MutableList<InetAddress>>()
			for ((key, value) in hosts) {
				if (isValidIpAddress(value)) {
					map[key] = mutableListOf(InetAddress.getByName(value))
				} else {
					map[key] = getAllByName(value)
				}
			}
		}

		private fun getAllByName(host: String): MutableList<InetAddress> {
			return try {
				// 获取所有与主机名关联的 IP 地址
				val allAddresses = InetAddress.getAllByName(host)
				val excludeIps = "2409:8087:6c02:14:100::14,2409:8087:6c02:14:100::18,39.134.108.253,39.134.108.245"
				// 创建一个列表用于存储有效的 IP 地址
				val validAddresses: MutableList<InetAddress> = ArrayList()
				val excludeIpsSet: MutableSet<String> = HashSet()
				for (ip in excludeIps.split(",")) {
					excludeIpsSet.add(ip.trim()) // 添加到集合，去除多余的空格
				}
				for (address in allAddresses) {
					if (!address.hostAddress.isNullOrEmpty() && !excludeIpsSet.contains(address.hostAddress.orEmpty())) {
						validAddresses.add(address)
					}
				}
				validAddresses
			} catch (e: Exception) {
				ArrayList()
			}
		}

		// 简单判断减少开销
		private fun isValidIpAddress(str: String): Boolean {
			if (str.indexOf('.') > 0) return isValidIPv4(str)
			return str.indexOf(':') > 0
		}

		private fun isValidIPv4(str: String): Boolean {
			val parts = str.split("\\.".toRegex()).toTypedArray()
			if (parts.size != 4) return false
			for (part in parts) {
				try {
					part.toInt()
				} catch (e: NumberFormatException) {
					return false
				}
			}
			return true
		}
	}
}
