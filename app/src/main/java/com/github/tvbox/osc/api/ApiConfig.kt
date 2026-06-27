package com.github.tvbox.osc.api

import android.app.Activity
import android.text.TextUtils
import android.util.Base64
import androidx.core.net.toUri
import com.github.catvod.crawler.JarLoader
import com.github.catvod.crawler.JsLoader
import com.github.catvod.crawler.PyLoader
import com.github.catvod.crawler.Spider
import com.github.catvod.crawler.python.IPyLoader
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.bean.IJKCode
import com.github.tvbox.osc.bean.LiveChannelGroup
import com.github.tvbox.osc.bean.LiveChannelItem
import com.github.tvbox.osc.bean.LiveSettingGroup
import com.github.tvbox.osc.bean.LiveSettingItem
import com.github.tvbox.osc.bean.ParseBean
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.util.AES
import com.github.tvbox.osc.util.AdBlocker
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.LOG
import com.github.tvbox.osc.util.M3u8
import com.github.tvbox.osc.util.MD5
import com.github.tvbox.osc.util.OkGoHelper
import com.github.tvbox.osc.util.RegexUtils
import com.github.tvbox.osc.util.VideoParseRuler
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import com.orhanobut.hawk.Hawk
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Locale
import kotlin.concurrent.Volatile

/**
 * @author pj567
 * @date :2020/12/18
 */
class ApiConfig private constructor() {
	private val sourceBeanList = LinkedHashMap<String, SourceBean>()
	val channelGroupList: MutableList<LiveChannelGroup>

	@JvmField
	val parseBeanList: MutableList<ParseBean>
	private val emptyHome = SourceBean()
	private val jarLoader = JarLoader()
	private val jsLoader = JsLoader()
	private val pyLoader: IPyLoader = PyLoader()
	private val gson: Gson
	private val userAgent = "okhttp/3.15"
	private val requestAccept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"

	@JvmField
	val liveSettingGroupList: MutableList<LiveSettingGroup> = ArrayList()
	private var mHomeSource: SourceBean? = null
	private var mDefaultParse: ParseBean? = null
	var vipParseFlags: MutableList<String> = ArrayList()
		private set
	private var myHosts: MutableMap<String, String> = HashMap()
	var ijkCodes: MutableList<IJKCode> = ArrayList()
		private set
	var spider: String? = null
		private set
	private var defaultLiveObjString = "{\"lives\":[{\"name\":\"txt_m3u\",\"type\":0,\"url\":\"txt_m3u_url\"}]}"
	private var tempKey: String? = null
	private var liveSpider = ""
	private var currentLiveSpider: String? = null
	private var searchSourceBeanList: MutableList<SourceBean>

	init {
		clearLoader()
		this.channelGroupList = ArrayList()
		parseBeanList = ArrayList()
		searchSourceBeanList = ArrayList()
		gson = Gson()
		Hawk.put(HawkConfig.LIVE_GROUP_LIST, JsonArray())
		loadDefaultConfig()
	}

	private fun configUrl(apiUrl: String): String {
		var apiUrl = apiUrl
		val configUrl: String
		val pk = ";pk;"
		apiUrl = apiUrl.replace("file://", "clan://localhost/")
		if (apiUrl.contains(pk)) {
			val a = apiUrl.split(pk.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			tempKey = a[1]
			configUrl = if (apiUrl.startsWith("clan")) {
				clanToAddress(a[0])
			} else if (apiUrl.startsWith("http")) {
				a[0]
			} else {
				"http://" + a[0]
			}
		} else if (apiUrl.startsWith("clan")) {
			configUrl = clanToAddress(apiUrl)
		} else if (!apiUrl.startsWith("http")) {
			configUrl = "http://$apiUrl"
		} else {
			configUrl = apiUrl
		}
		return configUrl
	}

	fun loadConfig(useCache: Boolean, callback: LoadConfigCallback, activity: Activity?) {
		val apiUrl = Hawk.get(HawkConfig.API_URL, "")
		//独立加载直播配置
		val liveApiUrl = Hawk.get(HawkConfig.LIVE_API_URL, "")
		val liveApiConfigUrl = configUrl(liveApiUrl)
		if (!liveApiUrl.isEmpty() && liveApiUrl != apiUrl) {
			if (liveApiUrl.contains(".txt") || liveApiUrl.contains(".m3u") || liveApiUrl.contains("=txt") || liveApiUrl.contains("=m3u")) {
				initLiveSettings()
				defaultLiveObjString = defaultLiveObjString.replace("txt_m3u_url", liveApiConfigUrl)
				parseLiveJson(liveApiUrl, defaultLiveObjString)
			} else {
				val liveCache = File(App.instance.filesDir.absolutePath + "/" + MD5.encode(liveApiUrl))
				LOG.i("echo-加载独立直播")
				if (useCache && liveCache.exists()) {
					try {
						parseLiveJson(liveApiUrl, liveCache)
					} catch (th: Throwable) {
						th.printStackTrace()
					}
				} else {
					OkGo.get<String?>(liveApiConfigUrl)
						.headers("User-Agent", userAgent)
						.headers("Accept", requestAccept)
						.execute(object : AbsCallback<String?>() {
							override fun onSuccess(response: Response<String?>) {
								try {
									val json = response.body()
									parseLiveJson(liveApiUrl, json)
									FileUtils.saveCache(liveCache, json)
								} catch (th: Throwable) {
									th.printStackTrace()
									callback.notice("解析直播配置失败")
								}
							}

							override fun onError(response: Response<String?>?) {
								super.onError(response)
								if (liveCache.exists()) {
									try {
										parseLiveJson(liveApiUrl, liveCache)
										callback.success()
										return
									} catch (th: Throwable) {
										th.printStackTrace()
									}
								}
								callback.notice("直播配置拉取失败")
							}

							@Throws(Throwable::class)
							override fun convertResponse(response: okhttp3.Response): String {
								var result: String?
								result = findResult(response.body.string(), tempKey)
								if (liveApiUrl.startsWith("clan")) {
									result = clanContentFix(clanToAddress(liveApiUrl), result)
								}
								//假相對路徑
								result = fixContentPath(liveApiUrl, result)
								return result
							}
						})
				}
			}
		}

		if (apiUrl.isEmpty()) {
			callback.error("-1")
			return
		}
		val cache = File(App.instance.filesDir.absolutePath + "/" + MD5.encode(apiUrl))
		if (useCache && cache.exists()) {
			try {
				parseJson(apiUrl, cache)
				callback.success()
				return
			} catch (th: Throwable) {
				th.printStackTrace()
			}
		}
		val configUrl = configUrl(apiUrl)
		// 使用内部存储，将当前配置地址写入到应用的私有目录中
		val configUrlFile = File(App.instance.filesDir.absolutePath + "/config_url")
		FileUtils.saveCache(configUrlFile, configUrl)

		OkGo.get<String?>(configUrl)
			.headers("User-Agent", userAgent)
			.headers("Accept", requestAccept)
			.execute(object : AbsCallback<String?>() {
				override fun onSuccess(response: Response<String?>) {
					try {
						val json = response.body()
						//                            LOG.i("echo-ConfigJson"+json);
						parseJson(apiUrl, json)
						FileUtils.saveCache(cache, json)
						callback.success()
					} catch (th: Throwable) {
						th.printStackTrace()
						callback.error("解析配置失败")
					}
				}

				override fun onError(response: Response<String?>) {
					super.onError(response)
					if (cache.exists()) {
						try {
							parseJson(apiUrl, cache)
							callback.success()
							return
						} catch (th: Throwable) {
							th.printStackTrace()
						}
					}
					callback.error("拉取配置失败\n" + (if (response.exception != null) response.exception.message else ""))
				}

				@Throws(Throwable::class)
				override fun convertResponse(response: okhttp3.Response): String {
					var result: String?
					result = findResult(response.body.string(), tempKey)

					if (apiUrl.startsWith("clan")) {
						result = clanContentFix(clanToAddress(apiUrl), result)
					}
					//假相對路徑
					result = fixContentPath(apiUrl, result)
					return result
				}
			})
	}

	fun loadJar(useCache: Boolean, spider: String, callback: LoadConfigCallback) {
		val urls = spider.split(";md5;".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		var jarUrl = urls[0]
		val md5 = if (urls.size > 1) urls[1].trim { it <= ' ' } else ""
		val cache = File(App.instance.filesDir.absolutePath + "/csp/" + MD5.string2MD5(jarUrl) + ".jar")

		if (!md5.isEmpty() || useCache) {
			if (cache.exists() && (useCache || MD5.getFileMd5(cache).equals(md5, ignoreCase = true))) {
				if (jarLoader.load(cache.absolutePath)) {
					callback.success()
				} else {
					callback.error("md5缓存失效")
				}
				return
			}
		} else {
			if (jarCache.toBoolean() && cache.exists() && !FileUtils.isWeekAgo(cache)) {
				LOG.i("echo-load jar jarCache:$jarUrl")
				if (jarLoader.load(cache.absolutePath)) {
					callback.success()
					return
				}
			}
		}

		val isJarInImg = jarUrl.startsWith("img+")
		jarUrl = jarUrl.replace("img+", "")
		LOG.i("echo-load jar start:$jarUrl")
		OkGo.get<File?>(jarUrl)
			.headers("User-Agent", userAgent)
			.headers("Accept", requestAccept)
			.execute(object : AbsCallback<File?>() {
				override fun convertResponse(response: okhttp3.Response): File? {
					val cacheDir: File = checkNotNull(cache.parentFile)
					if (!cacheDir.exists()) cacheDir.mkdirs()
					if (cache.exists()) cache.delete()
					// 3. 使用 try-with-resources 确保流关闭
					try {
						FileOutputStream(cache).use { fos ->
							if (isJarInImg) {
								val respData = response.body.string()
								LOG.i("echo---jar Response: $respData")
								val imgJar: ByteArray? = getImgJar(respData)
								if (imgJar == null || imgJar.isEmpty()) {
									LOG.e("echo---Generated JAR data is empty")
									callback.error("JAR 是空的")
								}
								fos.write(imgJar)
							} else {
								// 使用流式传输避免内存溢出
								val inputStream = response.body.byteStream()
								val buffer = ByteArray(4096)
								var bytesRead: Int
								while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
									fos.write(buffer, 0, bytesRead)
								}
							}
							fos.flush()
						}
					} catch (e: IOException) {
						return null
					}
					return cache
				}

				override fun onSuccess(response: Response<File?>) {
					val file = response.body()
					if (file != null && file.exists()) {
						try {
							if (jarLoader.load(file.absolutePath)) {
								LOG.i("echo---load-jar-success")
								callback.success()
							} else {
								LOG.e("echo---jar Loader returned false")
								callback.error("JAR加载失败")
							}
						} catch (e: Exception) {
							LOG.e("echo---jar Loader threw exception: " + e.message)
							callback.error("JAR加载异常: ")
						}
					} else {
						LOG.e("echo---jar File not found")
						callback.error("JAR文件不存在")
					}
				}

				override fun onError(response: Response<File?>) {
					val ex = response.exception
					if (ex != null) {
						LOG.i("echo---jar Request failed: " + ex.message)
					}
					if (cache.exists()) jarLoader.load(cache.absolutePath)
					callback.error("网络错误")
				}
			})
	}

	@Throws(Throwable::class)
	private fun parseJson(apiUrl: String, f: File) {
		val bReader = BufferedReader(InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8))
		val sb = StringBuilder()
		var s: String?
		while ((bReader.readLine().also { s = it }) != null) {
			sb.append(s).append("\n")
		}
		bReader.close()
		parseJson(apiUrl, sb.toString())
	}

	private fun parseJson(apiUrl: String, jsonStr: String?) {
//        pyLoader.setConfig(jsonStr);
		val infoJson = gson.fromJson(jsonStr, JsonObject::class.java)
		// spider
		spider = DefaultConfig.safeJsonString(infoJson, "spider", "")
		jarCache = DefaultConfig.safeJsonString(infoJson, "jarCache", "true")
		// 远端站点源
		var firstSite: SourceBean? = null
		for (opt in infoJson.get("sites").getAsJsonArray()) {
			val obj = opt as JsonObject
			val sb = SourceBean()
			val siteKey = obj.get("key").asString.trim { it <= ' ' }
			sb.key = siteKey
			sb.name = if (obj.has("name")) obj.get("name").asString.trim { it <= ' ' } else siteKey
			sb.type = obj.get("type").asInt
			sb.api = obj.get("api").asString.trim { it <= ' ' }
			sb.setSearchable(DefaultConfig.safeJsonInt(obj, "searchable", 1))
			sb.setQuickSearch(DefaultConfig.safeJsonInt(obj, "quickSearch", 1))
			if (siteKey.startsWith("py_")) {
				sb.filterable = 1
			} else {
				sb.filterable = DefaultConfig.safeJsonInt(obj, "filterable", 1)
			}
			sb.playerUrl = DefaultConfig.safeJsonString(obj, "playUrl", "")
			sb.ext = DefaultConfig.safeJsonString(obj, "ext", "")
			sb.jar = DefaultConfig.safeJsonString(obj, "jar", "")
			sb.playerType = DefaultConfig.safeJsonInt(obj, "playerType", -1)
			sb.categories = DefaultConfig.safeJsonStringList(obj, "categories")
			sb.clickSelector = DefaultConfig.safeJsonString(obj, "click", "")
			sb.style = DefaultConfig.safeJsonString(obj, "style", "")
			if (firstSite == null && sb.filterable == 1) firstSite = sb
			sourceBeanList[siteKey] = sb
		}
		if (sourceBeanList.isNotEmpty()) {
			val home = Hawk.get(HawkConfig.HOME_API, "")
			val sh = getSource(home)
			if (sh == null) {
				setSourceBean(requireNotNull(firstSite))
			} else setSourceBean(sh)
		}
		// 需要使用vip解析的flag
		vipParseFlags = DefaultConfig.safeJsonStringList(infoJson, "flags")
		// 解析地址
		parseBeanList.clear()
		if (infoJson.has("parses")) {
			val parses = infoJson.get("parses").getAsJsonArray()
			for (opt in parses) {
				val obj = opt as JsonObject
				val pb = ParseBean()
				pb.name = obj.get("name").asString.trim { it <= ' ' }
				pb.url = obj.get("url").asString.trim { it <= ' ' }
				val ext = if (obj.has("ext")) obj.get("ext").getAsJsonObject().toString() else ""
				pb.ext = ext
				pb.type = DefaultConfig.safeJsonInt(obj, "type", 0)
				parseBeanList.add(pb)
			}
			if (!parseBeanList.isEmpty()) addSuperParse()
		}
		// 获取默认解析
		if (!parseBeanList.isEmpty()) {
			val defaultParse = Hawk.get(HawkConfig.DEFAULT_PARSE, "")
			if (!TextUtils.isEmpty(defaultParse)) for (pb in parseBeanList) {
				if (pb.name == defaultParse) this.defaultParse = pb
			}
			if (mDefaultParse == null) this.defaultParse = parseBeanList[0]
		}

		// 直播源
		val liveApiUrl = Hawk.get(HawkConfig.LIVE_API_URL, "")
		if (liveApiUrl.isEmpty() || apiUrl == liveApiUrl) {
			LOG.i("echo-load-config_live")
			initLiveSettings()
			if (infoJson.has("lives")) {
				val livesGroups = infoJson.get("lives").getAsJsonArray()
				val liveGroupIndex = Hawk.get(HawkConfig.LIVE_GROUP_INDEX, 0)
				if (liveGroupIndex > livesGroups.size() - 1) Hawk.put(HawkConfig.LIVE_GROUP_INDEX, 0)
				Hawk.put(HawkConfig.LIVE_GROUP_LIST, livesGroups)
				//加载多源配置
				try {
					val liveSettingItemList = ArrayList<LiveSettingItem>()
					for (i in 0..<livesGroups.size()) {
						val jsonObject = livesGroups.get(i).getAsJsonObject()
						val name = if (jsonObject.has("name")) jsonObject.get("name").asString else "线路" + (i + 1)
						val liveSettingItem = LiveSettingItem()
						liveSettingItem.itemIndex = i
						liveSettingItem.itemName = name
						liveSettingItemList.add(liveSettingItem)
					}
					liveSettingGroupList.getOrNull(5)?.liveSettingItems = liveSettingItemList
				} catch (e: Exception) {
					// 捕获任何可能发生的异常
					e.printStackTrace()
				}

				val livesOBJ = livesGroups.get(liveGroupIndex).getAsJsonObject()
				loadLiveApi(livesOBJ)
			}
		}

		myHosts = HashMap()
		if (infoJson.has("hosts")) {
			val hostsArray = infoJson.getAsJsonArray("hosts")
			for (i in 0..<hostsArray.size()) {
				val entry = hostsArray.get(i).asString
				val parts = entry.split("=", limit = 2) // 只分割一次，防止 value 里有 =
				if (parts.size == 2) {
					myHosts[parts[0]] = parts[1]
				}
			}
		}

		//video parse rule for host
		if (infoJson.has("rules")) {
			VideoParseRuler.clearRule()
			VideoParseRuler.clearRule()
			for (oneHostRule in infoJson.getAsJsonArray("rules")) {
				val obj = oneHostRule as JsonObject
				//嗅探过滤规则
				if (obj.has("host")) {
					val host = obj.get("host").asString
					if (obj.has("rule")) {
						val ruleJsonArr = obj.getAsJsonArray("rule")
						val rule = ArrayList<String>()
						for (one in ruleJsonArr) {
							rule.add(one.asString)
						}
						if (rule.isNotEmpty()) {
							VideoParseRuler.addHostRule(host, rule)
						}
					}
					if (obj.has("filter")) {
						val filterJsonArr = obj.getAsJsonArray("filter")
						val filter = ArrayList<String>()
						for (one in filterJsonArr) {
							filter.add(one.asString)
						}
						if (filter.isNotEmpty()) {
							VideoParseRuler.addHostFilter(host, filter)
						}
					}
				}
				//广告过滤规则
				if (obj.has("hosts") && obj.has("regex")) {
					val rule = ArrayList<String>()
					val ads = ArrayList<String>()
					val regexArray = obj.getAsJsonArray("regex")
					for (one in regexArray) {
						val regex = one.asString
						if (M3u8.isAd(regex)) ads.add(regex)
						else rule.add(regex)
					}
					val array = obj.getAsJsonArray("hosts")
					for (one in array) {
						val host = one.asString
						VideoParseRuler.addHostRule(host, rule)
						VideoParseRuler.addHostRegex(host, ads)
					}
				}
				//嗅探脚本规则 如 click
				if (obj.has("hosts") && obj.has("script")) {
					val scripts = ArrayList<String>()
					val scriptArray = obj.getAsJsonArray("script")
					for (one in scriptArray) {
						scripts.add(one.asString)
					}
					val array = obj.getAsJsonArray("hosts")
					for (one in array) {
						val host = one.asString
						VideoParseRuler.addHostScript(host, scripts)
					}
				}
			}
		}

		if (infoJson.has("doh")) {
			val dohJson = infoJson.getAsJsonArray("doh").toString()
			if (Hawk.get(HawkConfig.DOH_JSON, "") != dohJson) {
				Hawk.put(HawkConfig.DOH_URL, 0)
				Hawk.put(HawkConfig.DOH_JSON, dohJson)
			}
		} else {
			Hawk.put(HawkConfig.DOH_JSON, "")
		}
		OkGoHelper.setDnsList()
		LOG.i("echo-api-config-----------load")
		//追加的广告拦截
		if (infoJson.has("ads")) {
			for (host in infoJson.getAsJsonArray("ads")) {
				if (!AdBlocker.hasHost(host.asString)) {
					AdBlocker.addAdHost(host.asString)
				}
			}
		}
	}

	private fun loadDefaultConfig() {
		val defaultIJKADS =
			"{\"ijk\":[{\"options\":[{\"name\":\"opensles\",\"category\":4,\"value\":\"0\"},{\"name\":\"framedrop\",\"category\":4,\"value\":\"1\"},{\"name\":\"soundtouch\",\"category\":4,\"value\":\"1\"},{\"name\":\"start-on-prepared\",\"category\":4,\"value\":\"1\"},{\"name\":\"http-detect-rangeupport\",\"category\":1,\"value\":\"0\"},{\"name\":\"fflags\",\"category\":1,\"value\":\"fastseek\"},{\"name\":\"skip_loop_filter\",\"category\":2,\"value\":\"48\"},{\"name\":\"reconnect\",\"category\":4,\"value\":\"1\"},{\"name\":\"enable-accurate-seek\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec-all-videos\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec-auto-rotate\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec-handle-resolution-change\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec-hevc\",\"category\":4,\"value\":\"0\"},{\"name\":\"max-buffer-size\",\"category\":4,\"value\":\"15728640\"}],\"group\":\"软解码\"},{\"options\":[{\"name\":\"opensles\",\"category\":4,\"value\":\"0\"},{\"name\":\"framedrop\",\"category\":4,\"value\":\"1\"},{\"name\":\"soundtouch\",\"category\":4,\"value\":\"1\"},{\"name\":\"start-on-prepared\",\"category\":4,\"value\":\"1\"},{\"name\":\"http-detect-rangeupport\",\"category\":1,\"value\":\"0\"},{\"name\":\"fflags\",\"category\":1,\"value\":\"fastseek\"},{\"name\":\"skip_loop_filter\",\"category\":2,\"value\":\"48\"},{\"name\":\"reconnect\",\"category\":4,\"value\":\"1\"},{\"name\":\"enable-accurate-seek\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec\",\"category\":4,\"value\":\"1\"},{\"name\":\"mediacodec-all-videos\",\"category\":4,\"value\":\"1\"},{\"name\":\"mediacodec-auto-rotate\",\"category\":4,\"value\":\"1\"},{\"name\":\"mediacodec-handle-resolution-change\",\"category\":4,\"value\":\"1\"},{\"name\":\"mediacodec-hevc\",\"category\":4,\"value\":\"1\"},{\"name\":\"max-buffer-size\",\"category\":4,\"value\":\"15728640\"}],\"group\":\"硬解码\"}],\"ads\":[\"mimg.0c1q0l.cn\",\"www.googletagmanager.com\",\"www.google-analytics.com\",\"mc.usihnbcq.cn\",\"mg.g1mm3d.cn\",\"mscs.svaeuzh.cn\",\"cnzz.hhttm.top\",\"tp.vinuxhome.com\",\"cnzz.mmstat.com\",\"www.baihuillq.com\",\"s23.cnzz.com\",\"z3.cnzz.com\",\"c.cnzz.com\",\"stj.v1vo.top\",\"z12.cnzz.com\",\"img.mosflower.cn\",\"tips.gamevvip.com\",\"ehwe.yhdtns.com\",\"xdn.cqqc3.com\",\"www.jixunkyy.cn\",\"sp.chemacid.cn\",\"hm.baidu.com\",\"s9.cnzz.com\",\"z6.cnzz.com\",\"um.cavuc.com\",\"mav.mavuz.com\",\"wofwk.aoidf3.com\",\"z5.cnzz.com\",\"xc.hubeijieshikj.cn\",\"tj.tianwenhu.com\",\"xg.gars57.cn\",\"k.jinxiuzhilv.com\",\"cdn.bootcss.com\",\"ppl.xunzhuo123.com\",\"xomk.jiangjunmh.top\",\"img.xunzhuo123.com\",\"z1.cnzz.com\",\"s13.cnzz.com\",\"xg.huataisangao.cn\",\"z7.cnzz.com\",\"xg.huataisangao.cn\",\"z2.cnzz.com\",\"s96.cnzz.com\",\"q11.cnzz.com\",\"thy.dacedsfa.cn\",\"xg.whsbpw.cn\",\"s19.cnzz.com\",\"z8.cnzz.com\",\"s4.cnzz.com\",\"f5w.as12df.top\",\"ae01.alicdn.com\",\"www.92424.cn\",\"k.wudejia.com\",\"vivovip.mmszxc.top\",\"qiu.xixiqiu.com\",\"cdnjs.hnfenxun.com\",\"cms.qdwght.com\"]}"
		val defaultJson = gson.fromJson(defaultIJKADS, JsonObject::class.java)
		// 广告地址
		if (AdBlocker.isEmpty()) {
			//默认广告拦截
			for (host in defaultJson.getAsJsonArray("ads")) {
				AdBlocker.addAdHost(host.asString)
			}
		}
		// IJK解码配置
		if (ijkCodes.isEmpty()) {
			ijkCodes = ArrayList()
			var foundOldSelect = false
			var ijkCodec = Hawk.get(HawkConfig.IJK_CODEC, "硬解码")
			val ijkJsonArray = defaultJson.get("ijk").getAsJsonArray()
			for (opt in ijkJsonArray) {
				val obj = opt as JsonObject
				val name = obj.get("group").asString
				val baseOpt = LinkedHashMap<String, String>()
				for (cfg in obj.get("options").getAsJsonArray()) {
					val cObj = cfg as JsonObject
					val key = cObj.get("category").asString + "|" + cObj.get("name").asString
					val `val` = cObj.get("value").asString
					baseOpt[key] = `val`
				}
				val codec = IJKCode()
				codec.name = name
				codec.option = baseOpt
				if (name == ijkCodec || TextUtils.isEmpty(ijkCodec)) {
					codec.selected(true)
					ijkCodec = name
					foundOldSelect = true
				} else {
					codec.selected(false)
				}
				ijkCodes.add(codec)
			}
			if (!foundOldSelect && ijkCodes.isNotEmpty()) {
				ijkCodes[0].selected(true)
			}
		}
		LOG.i("echo-default-config-----------load")
	}

	@Throws(Throwable::class)
	private fun parseLiveJson(apiUrl: String?, f: File) {
		val bReader = BufferedReader(InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8))
		val sb = StringBuilder()
		var s: String?
		while ((bReader.readLine().also { s = it }) != null) {
			sb.append(s).append("\n")
		}
		bReader.close()
		parseLiveJson(apiUrl, sb.toString())
	}

	private fun parseLiveJson(apiUrl: String?, jsonStr: String?) {
		val infoJson = gson.fromJson(jsonStr, JsonObject::class.java)
		// spider
		liveSpider = DefaultConfig.safeJsonString(infoJson, "spider", "")
		// 直播源
		initLiveSettings()
		if (infoJson.has("lives")) {
			val livesGroups = infoJson.get("lives").getAsJsonArray()

			val liveGroupIndex = Hawk.get(HawkConfig.LIVE_GROUP_INDEX, 0)
			if (liveGroupIndex > livesGroups.size() - 1) Hawk.put(HawkConfig.LIVE_GROUP_INDEX, 0)
			Hawk.put(HawkConfig.LIVE_GROUP_LIST, livesGroups)
			//加载多源配置
			try {
				val liveSettingItemList = ArrayList<LiveSettingItem>()
				for (i in 0..<livesGroups.size()) {
					val jsonObject = livesGroups.get(i).getAsJsonObject()
					val name = if (jsonObject.has("name")) jsonObject.get("name").asString else "线路" + (i + 1)
					val liveSettingItem = LiveSettingItem()
					liveSettingItem.itemIndex = i
					liveSettingItem.itemName = name
					liveSettingItemList.add(liveSettingItem)
				}
				liveSettingGroupList.getOrNull(5)?.liveSettingItems = liveSettingItemList
			} catch (e: Exception) {
				// 捕获任何可能发生的异常
				e.printStackTrace()
			}

			val livesOBJ = livesGroups.get(liveGroupIndex).getAsJsonObject()
			loadLiveApi(livesOBJ)
		}

		myHosts = HashMap()
		if (infoJson.has("hosts")) {
			val hostsArray = infoJson.getAsJsonArray("hosts")
			for (i in 0..<hostsArray.size()) {
				val entry = hostsArray.get(i).asString
				val parts = entry.split("=", limit = 2) // 只分割一次，防止 value 里有 =
				if (parts.size == 2) {
					myHosts[parts[0]] = parts[1]
				}
			}
		}
		LOG.i("echo-api-live-config-----------load")
	}

	private fun initLiveSettings() {
		val groupNames = listOf("线路选择", "画面比例", "播放解码", "超时换源", "偏好设置", "多源切换")
		val itemsArrayList = ArrayList<List<String>>()
		val sourceItems = ArrayList<String>()
		val scaleItems = ArrayList(listOf("默认", "16:9", "4:3", "填充", "原始", "裁剪"))
		val playerDecoderItems = ArrayList(listOf("系统", "ijk硬解", "ijk软解", "exo"))
		val timeoutItems = ArrayList(listOf("5s", "10s", "15s", "20s", "25s", "30s"))
		val personalSettingItems = ArrayList(listOf("显示时间", "显示网速", "换台反转", "跨选分类"))
		val yumItems = ArrayList<String>()

		itemsArrayList.add(sourceItems)
		itemsArrayList.add(scaleItems)
		itemsArrayList.add(playerDecoderItems)
		itemsArrayList.add(timeoutItems)
		itemsArrayList.add(personalSettingItems)
		itemsArrayList.add(yumItems)

		liveSettingGroupList.clear()
		for (i in groupNames.indices) {
			val liveSettingGroup = LiveSettingGroup()
			val liveSettingItemList = ArrayList<LiveSettingItem>()
			liveSettingGroup.groupIndex = i
			liveSettingGroup.groupName = groupNames[i]
			for (j in itemsArrayList[i].indices) {
				val liveSettingItem = LiveSettingItem()
				liveSettingItem.itemIndex = j
				liveSettingItem.itemName = itemsArrayList[i][j]
				liveSettingItemList.add(liveSettingItem)
			}
			liveSettingGroup.liveSettingItems = liveSettingItemList
			liveSettingGroupList.add(liveSettingGroup)
		}
	}

	fun loadLives(livesArray: JsonArray) {
		channelGroupList.clear()
		var groupIndex = 0
		var channelIndex: Int
		var channelNum = 0
		for (groupElement in livesArray) {
			val liveChannelGroup = LiveChannelGroup()
			liveChannelGroup.liveChannels = ArrayList()
			liveChannelGroup.groupIndex = groupIndex++
			val groupName = (groupElement as JsonObject).get("group").asString.trim { it <= ' ' }
			val splitGroupName = groupName.split("_", limit = 2)
			liveChannelGroup.groupName = splitGroupName[0]
			if (splitGroupName.size > 1) liveChannelGroup.groupPassword = splitGroupName[1]
			else liveChannelGroup.groupPassword = ""
			channelIndex = 0
			for (channelElement in groupElement.get("channels").getAsJsonArray()) {
				val obj = channelElement as JsonObject
				val liveChannelItem = LiveChannelItem()
				liveChannelItem.channelName = obj.get("name").asString.trim { it <= ' ' }
				liveChannelItem.channelIndex = channelIndex++
				liveChannelItem.channelNum = ++channelNum
				val urls = DefaultConfig.safeJsonStringList(obj, "urls")
				val sourceNames = ArrayList<String>()
				val sourceUrls = ArrayList<String>()
				var sourceIndex = 1
				for (url in urls) {
					val splitText = url.split("$", limit = 2)
					sourceUrls.add(splitText[0])
					if (splitText.size > 1) sourceNames.add(splitText[1])
					else sourceNames.add("源$sourceIndex")
					sourceIndex++
				}
				liveChannelItem.channelSourceNames = sourceNames
				liveChannelItem.setChannelUrls(sourceUrls)
				liveChannelGroup.liveChannels.add(liveChannelItem)
			}
			channelGroupList.add(liveChannelGroup)
		}
	}

	fun loadLiveApi(livesOBJ: JsonObject) {
		try {
			LOG.i("echo-loadLiveApi")
			val lives = livesOBJ.toString()
			val index = lives.indexOf("proxy://")
			var url: String
			if (index != -1) {
				val endIndex = lives.lastIndexOf("\"")
				url = lives.substring(index, endIndex)
				url = DefaultConfig.checkReplaceProxy(url)
				val extUrl = url.toUri().getQueryParameter("ext")
				if (!extUrl.isNullOrEmpty()) {
					var extUrlFix = if (extUrl.startsWith("http") || extUrl.startsWith("clan://")) {
						extUrl
					} else {
						String(Base64.decode(extUrl, Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
					}
					extUrlFix = Base64.encodeToString(extUrlFix.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP)
					url = url.replace(extUrl, extUrlFix)
				}
			} else {
				val type = livesOBJ.get("type").asString
				if (type == "0" || type == "3") {
					url = if (livesOBJ.has("url")) livesOBJ.get("url").asString else ""
					if (url.isEmpty()) url = if (livesOBJ.has("api")) livesOBJ.get("api").asString else ""
					LOG.i("echo-liveurl$url")
					if (!url.startsWith("http://127.0.0.1")) {
						if (url.startsWith("http")) {
							url = Base64.encodeToString(url.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP)
						}
						url = "http://127.0.0.1:9978/proxy?do=live&type=txt&ext=$url"
					}
					if (type == "3") {
						val jarUrl = if (livesOBJ.has("jar")) livesOBJ.get("jar").asString.trim { it <= ' ' } else ""
						val pyApi = if (livesOBJ.has("api")) livesOBJ.get("api").asString.trim { it <= ' ' } else ""
						LOG.i("echo-pyApi1$pyApi")
						if (pyApi.contains(".py")) {
							LOG.i("echo-pyLoader.getSpider")
							val ext: String
							if (livesOBJ.has("ext") && (livesOBJ.get("ext").isJsonObject || livesOBJ.get("ext").isJsonArray)) {
								ext = livesOBJ.get("ext").toString()
							} else {
								ext = DefaultConfig.safeJsonString(livesOBJ, "ext", "")
							}

							pyLoader.getSpider(MD5.string2MD5(pyApi), pyApi, ext)
						}
						if (!jarUrl.isEmpty()) {
							jarLoader.loadLiveJar(jarUrl)
						} else if (!liveSpider.isEmpty()) {
							jarLoader.loadLiveJar(liveSpider)
						}
					}
				} else {
					channelGroupList.clear()
					return
				}
			}
			//设置epg
			if (livesOBJ.has("epg")) {
				val epg = livesOBJ.get("epg").asString
				Hawk.put(HawkConfig.EPG_URL, epg)
			} else {
				Hawk.put(HawkConfig.EPG_URL, "")
			}
			//直播播放器类型
			if (livesOBJ.has("playerType")) {
				val livePlayType = livesOBJ.get("playerType").asString
				Hawk.put(HawkConfig.LIVE_PLAY_TYPE, livePlayType)
			} else {
				Hawk.put(HawkConfig.LIVE_PLAY_TYPE, Hawk.get(HawkConfig.PLAY_TYPE, 0))
			}
			//设置UA
			if (livesOBJ.has("header")) {
				val headerObj = livesOBJ.getAsJsonObject("header")
				val liveHeader = HashMap<String, String>()
				for (entry in headerObj.entrySet()) {
					liveHeader[entry.key] = entry.value.asString
				}
				Hawk.put(HawkConfig.LIVE_WEB_HEADER, liveHeader)
			} else if (livesOBJ.has("ua")) {
				val ua = livesOBJ.get("ua").asString
				val liveHeader = HashMap<String, String>()
				liveHeader["User-Agent"] = ua
				Hawk.put(HawkConfig.LIVE_WEB_HEADER, liveHeader)
			} else {
				Hawk.put<Any?>(HawkConfig.LIVE_WEB_HEADER, null)
			}
			val liveChannelGroup = LiveChannelGroup()
			liveChannelGroup.groupName = url
			channelGroupList.clear()
			channelGroupList.add(liveChannelGroup)
		} catch (th: Throwable) {
			th.printStackTrace()
		}
	}

	fun setLiveJar(liveJar: String) {
		if (liveJar.contains(".py")) {
			pyLoader.setRecentPyKey(liveJar)
		} else {
			val jarUrl = if (!liveJar.isEmpty()) liveJar else liveSpider
			jarLoader.setRecentJarKey(MD5.string2MD5(jarUrl))
		}
		currentLiveSpider = liveJar
	}

	fun getCSP(sourceBean: SourceBean): Spider? {
		return if (sourceBean.api.endsWith(".js") || sourceBean.api.contains(".js?")) {
			jsLoader.getSpider(sourceBean.key, sourceBean.api, sourceBean.ext, sourceBean.jar)
		} else if (sourceBean.api.contains(".py")) {
			pyLoader.getSpider(sourceBean.key, sourceBean.api, sourceBean.ext)
		} else jarLoader.getSpider(sourceBean.key, sourceBean.api, sourceBean.ext, sourceBean.jar)
	}

	fun getPyCSP(url: String): Spider {
		return pyLoader.getSpider(MD5.string2MD5(url), url, "")
	}

	fun proxyLocal(param: Map<String, String>): Array<Any?>? {
		if ("js" == param["do"]) {
			return jsLoader.proxyInvoke(param)
		}
		val apiString: String
		if (Hawk.get(HawkConfig.PLAYER_IS_LIVE, false)) {
			apiString = currentLiveSpider ?: ""
		} else {
			val sourceBean: SourceBean = (get() ?: return null).homeSourceBean
			apiString = sourceBean.api
		}
		return if (apiString.contains(".py")) pyLoader.proxyInvoke(param) else jarLoader.proxyInvoke(param)
	}

	fun jsonExt(key: String, jxs: LinkedHashMap<String, String>?, url: String?): JSONObject? {
		return jarLoader.jsonExt(key, jxs, url)
	}

	fun jsonExtMix(flag: String?, key: String, name: String?, jxs: LinkedHashMap<String, HashMap<String, String>?>?, url: String?): JSONObject? {
		return jarLoader.jsonExtMix(flag, key, name, jxs, url)
	}

	fun getSource(key: String?): SourceBean? {
		if (!sourceBeanList.containsKey(key)) return null
		return sourceBeanList[key]
	}

	fun setSourceBean(sourceBean: SourceBean) {
		this.mHomeSource = sourceBean
		Hawk.put(HawkConfig.HOME_API, sourceBean.key)
	}

	var defaultParse: ParseBean?
		get() = mDefaultParse
		set(value) {
			mDefaultParse?.isDefault = false
			mDefaultParse = value
			if (value != null) {
				Hawk.put(HawkConfig.DEFAULT_PARSE, value.name)
				value.setDefault(true)
			}
		}

	fun getSourceBeanList(): List<SourceBean> {
		return sourceBeanList.values.toList()
	}

	val switchSourceBeanList: List<SourceBean>
		get() = sourceBeanList.values.filter { it.filterable == 1 }

	fun getSearchSourceBeanList(): List<SourceBean> {
		if (searchSourceBeanList.isEmpty()) {
			LOG.i("echo-第一次getSearchSourceBeanList")
			searchSourceBeanList = sourceBeanList.values.filter { it.isSearchable }.toMutableList()
		}
		return searchSourceBeanList
	}

	val homeSourceBean: SourceBean
		get() = mHomeSource ?: emptyHome

	val currentIJKCode: IJKCode?
		get() {
			val codeName = Hawk.get(HawkConfig.IJK_CODEC, "硬解码")
			return getIJKCodec(codeName)
		}

	fun getIJKCodec(name: String?): IJKCode? {
		for (code in ijkCodes) {
			if (code.name == name) return code
		}
		return ijkCodes.firstOrNull()
	}

	fun clanToAddress(lanLink: String): String {
		if (lanLink.startsWith("clan://localhost/")) {
			return lanLink.replace("clan://localhost/", ControlManager.get().getAddress(true) + "file/")
		} else {
			val link = lanLink.substring(7)
			val end = link.indexOf('/')
			return "http://" + link.substring(0, end) + "/file/" + link.substring(end + 1)
		}
	}

	fun clanContentFix(lanLink: String, content: String): String {
		val fix = lanLink.substring(0, lanLink.indexOf("/file/") + 6)
		return content.replace("clan://localhost/", fix).replace("file://", fix)
	}

	fun fixContentPath(url: String, content: String): String {
		var url = url
		var content = content
		if (content.contains("\"./")) {
			url = url.replace("file://", "clan://localhost/")
			if (!url.startsWith("http") && !url.startsWith("clan://")) {
				url = "http://$url"
			}
			if (url.startsWith("clan://")) url = clanToAddress(url)
			content = content.replace("./", url.substring(0, url.lastIndexOf("/") + 1))
		}
		return content
	}

	val myHost: MutableMap<String, String>
		get() = myHosts

	fun clearJarLoader() {
		jarLoader.clear()
	}

	private fun addSuperParse() {
		val superPb = ParseBean()
		superPb.name = "超级解析"
		superPb.url = "SuperParse"
		superPb.ext = ""
		superPb.type = 4
		parseBeanList.add(0, superPb)
	}

	fun clearLoader() {
		jarLoader.clear()
		pyLoader.clear()
		jsLoader.clear()
	}

	interface LoadConfigCallback {
		fun success()

		fun error(msg: String?)

		fun notice(msg: String?)
	}

	interface FastParseCallback {
		fun success(parse: Boolean, url: String?, header: Map<String, String>?)

		fun fail(code: Int, msg: String?)
	}

	companion object {
		@Volatile
		private var instance: ApiConfig? = null
		private var jarCache = "true"

		@JvmStatic
		fun get(): ApiConfig? {
			if (instance == null) {
				synchronized(ApiConfig::class.java) {
					if (instance == null) {
						instance = ApiConfig()
					}
				}
			}
			return instance
		}

		fun findResult(json: String, configKey: String?): String {
			var json = json
			var content = json
			try {
				if (AES.isJson(content)) return content
				val pattern = RegexUtils.getPattern("[A-Za-z0]{8}\\*\\*")
				val matcher = pattern.matcher(content)
				if (matcher.find()) {
					content = content.substring(content.indexOf(matcher.group()) + 10)
					content = String(Base64.decode(content, Base64.DEFAULT))
				}
				if (content.startsWith("2423")) {
					val data = content.substring(content.indexOf("2324") + 4, content.length - 26)
					content = String(AES.toBytes(content)).lowercase(Locale.getDefault())
					val key = AES.rightPadding(content.substring(content.indexOf("$#") + 2, content.indexOf("#$")), "0", 16)
					val iv = AES.rightPadding(content.substring(content.length - 13), "0", 16)
					json = AES.CBC(data, key, iv)
				} else if (configKey != null && !AES.isJson(content)) {
					json = AES.ECB(content, configKey)
				} else {
					json = content
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
			return json
		}

		private fun getImgJar(body: String): ByteArray? {
			var body = body
			val pattern = RegexUtils.getPattern("[A-Za-z0]{8}\\*\\*")
			val matcher = pattern.matcher(body)
			if (matcher.find()) {
				body = body.substring(body.indexOf(matcher.group()) + 10)
				return Base64.decode(body, Base64.DEFAULT)
			}
			return "".toByteArray()
		}
	}
}
