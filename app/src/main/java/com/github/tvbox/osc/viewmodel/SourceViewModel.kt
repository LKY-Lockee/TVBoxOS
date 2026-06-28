package com.github.tvbox.osc.viewmodel

import android.util.Base64
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.bean.AbsJson
import com.github.tvbox.osc.bean.AbsSortJson
import com.github.tvbox.osc.bean.AbsSortXml
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.Movie.Video.UrlBean.UrlInfo.InfoBean
import com.github.tvbox.osc.bean.MovieSort
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.player.thirdparty.RemoteTVBox
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.LOG
import com.github.tvbox.osc.util.MD5
import com.github.tvbox.osc.util.thunder.Thunder
import com.github.tvbox.osc.util.thunder.Thunder.ThunderCallback
import com.github.tvbox.osc.util.urlhttp.OkHttpUtil
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.orhanobut.hawk.Hawk
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * @author pj567
 * @date 2020/12/18
 */
class SourceViewModel : ViewModel() {
	val sortResult: MutableLiveData<AbsSortXml?> = MutableLiveData<AbsSortXml?>()
	val listResult: MutableLiveData<AbsXml?> = MutableLiveData<AbsXml?>()
	val searchResult: MutableLiveData<AbsXml?> = MutableLiveData<AbsXml?>()
	val quickSearchResult: MutableLiveData<AbsXml?> = MutableLiveData<AbsXml?>()
	val detailResult: MutableLiveData<AbsXml?> = MutableLiveData<AbsXml?>()
	val playResult: MutableLiveData<JSONObject?> = MutableLiveData<JSONObject?>()
	val gson: Gson = Gson()

	// homeContent
	fun getSort(sourceKey: String?) {
		LOG.i("echo--getSort-start")
		if (sourceKey == null) {
			sortResult.postValue(null)
			return
		}

		// 优先检查缓存
		val cached: AbsSortXml? = sortCache[sourceKey]
		if (cached != null) {
			LOG.i("echo--getSort-cached--$sourceKey")
			val homeRec = Hawk.get(HawkConfig.HOME_REC, 0)
			val videoList = cached.videoList
			val shouldUseCache = (homeRec != 1) || !videoList.isNullOrEmpty()
			if (shouldUseCache) {
				sortResult.postValue(cached)
				return
			}
		}

		val sourceBean = ApiConfig.instance.getSource(sourceKey) ?: return
		val name = sourceBean.name ?: return
		if (name.length <= 3 && name.endsWith("搜")) {
			sortResult.postValue(null)
			return
		}

		val type = sourceBean.type
		if (type == 3) {
			val waitResponse = Runnable {
				val executor = Executors.newSingleThreadExecutor()
				val future = executor.submit(Callable {
					val sp = ApiConfig.instance.getCSP(sourceBean)
					sp?.homeContent(true)
				})
				var sortJson: String? = null
				try {
					sortJson = future.get(20, TimeUnit.SECONDS)
				} catch (e: TimeoutException) {
					e.printStackTrace()
					future.cancel(true)
				} catch (e: InterruptedException) {
					e.printStackTrace()
				} catch (e: ExecutionException) {
					e.printStackTrace()
				} finally {
					if (sortJson != null) {
						val sortXml = sortJson(sortResult, sortJson)
						if (sortXml != null && Hawk.get(HawkConfig.HOME_REC, 0) == 1) {
							val absXml = json(null, sortJson, sourceBean.key)
							val movie = absXml?.movie
							val videos = movie?.videoList
							if (absXml != null && movie != null && !videos.isNullOrEmpty()) {
								sortXml.videoList = videos
								sortResult.postValue(sortXml)
								sortCache[sourceKey] = sortXml
							} else {
								getHomeRecList(sourceBean, null) { videos2: MutableList<Movie.Video>? ->
									sortXml.videoList = videos2
									sortResult.postValue(sortXml)
									sortCache[sourceKey] = sortXml
								}
							}
						} else {
							sortResult.postValue(sortXml)
							sortCache[sourceKey] = sortXml
						}
					} else {
						sortResult.postValue(null)
					}
					try {
						executor.shutdown()
					} catch (th: Throwable) {
						th.printStackTrace()
					}
				}
			}
			spThreadPool.execute(waitResponse)
		} else if (type == 0 || type == 1) {
			OkGo.get<String?>(sourceBean.api)
				.tag(sourceBean.key + "_sort")
				.execute(object : AbsCallback<String?>() {
					override fun convertResponse(response: Response): String {
						return response.body.string()
					}

					override fun onSuccess(response: com.lzy.okgo.model.Response<String?>?) {
						val sortXml: AbsSortXml?
						if (type == 0) {
							val xml = response?.body()
							sortXml = sortXml(sortResult, xml)
						} else {
							val json = response?.body()
							sortXml = sortJson(sortResult, json ?: return)
						}
						if (sortXml != null && Hawk.get(HawkConfig.HOME_REC, 0) == 1) {
							val list = sortXml.list
							val videos = list?.videoList
							if (list != null && !videos.isNullOrEmpty()) {
								val ids = videos.map { it.id }.toMutableList()
								getHomeRecList(sourceBean, ids) { videos2: MutableList<Movie.Video>? ->
									sortXml.videoList = videos2
									sortResult.postValue(sortXml)
									sortCache[sourceKey] = sortXml
								}
							} else {
								sortResult.postValue(sortXml)
								sortCache[sourceKey] = sortXml
							}
						} else {
							sortResult.postValue(sortXml)
							sortCache[sourceKey] = sortXml
						}
					}

					override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
						super.onError(response)
						sortResult.postValue(null)
					}
				})
		} else if (type == 4) {
			var extend = sourceBean.ext
			extend = getFixUrl(extend ?: return)
			if (URLEncoder.encode(extend, "UTF-8").length < 1000) {
				val request = OkGo.get<String?>(sourceBean.api)
					.tag(sourceBean.key + "_sort")
					.params("filter", "true")
				// 当 extend 不为空且非空字符串时添加参数
				if (extend.isNotEmpty()) {
					request.params("extend", extend)
				}
				request.execute(object : AbsCallback<String?>() {
					override fun convertResponse(response: Response): String {
						return response.body.string()
					}

					override fun onSuccess(response: com.lzy.okgo.model.Response<String?>) {
						val sortJson = response.body()
						if (sortJson != null) {
							val sortXml = sortJson(sortResult, sortJson)
							if (sortXml != null && Hawk.get(HawkConfig.HOME_REC, 0) == 1) {
								val absXml = json(null, sortJson, sourceBean.key)
								val movie = absXml?.movie
								val videos = movie?.videoList
								if (absXml != null && movie != null && !videos.isNullOrEmpty()) {
									sortXml.videoList = videos
									sortResult.postValue(sortXml)
									sortCache[sourceKey] = sortXml
								} else {
									getHomeRecList(sourceBean, null) { videos2: MutableList<Movie.Video>? ->
										sortXml.videoList = videos2
										sortResult.postValue(sortXml)
										sortCache[sourceKey] = sortXml
									}
								}
							} else {
								sortResult.postValue(sortXml)
								sortCache[sourceKey] = sortXml
							}
						} else {
							sortResult.postValue(null)
						}
					}

					override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
						super.onError(response)
						sortResult.postValue(null)
					}
				})
			} else {
				try {
					val params: MutableMap<String, String> = mutableMapOf()
					params["filter"] = "true"
					if (extend.isNotEmpty()) {
						params["extend"] = extend
					}
					RemoteTVBox.post(sourceBean.api, params, object : Callback {
						override fun onFailure(call: Call, e: okio.IOException) {
							sortResult.postValue(null)
						}

						override fun onResponse(call: Call, response: Response) {
							val body = response.body
							val sortJson = body.string()
							val sortXml = sortJson(sortResult, sortJson)
							if (sortXml != null && Hawk.get(HawkConfig.HOME_REC, 0) == 1) {
								val absXml = json(null, sortJson, sourceBean.key)
								val movie = absXml?.movie
								val videos = movie?.videoList
								if (absXml != null && movie != null && !videos.isNullOrEmpty()) {
									sortXml.videoList = videos
									sortResult.postValue(sortXml)
									sortCache[sourceKey] = sortXml
								}
							} else {
								sortResult.postValue(sortXml)
								sortCache[sourceKey] = sortXml
							}
						}
					})
				} catch (ignored: Exception) {
					sortResult.postValue(null)
				}
			}
		} else {
			sortResult.postValue(null)
		}
	}

	// categoryContent
	fun getList(sortData: SortData, page: Int) {
		LOG.i("echo-getList:")
		val homeSourceBean = ApiConfig.instance.homeSourceBean
		when (val type = homeSourceBean.type) {
			3 -> {
				spThreadPool.execute {
					try {
						val sp = ApiConfig.instance.getCSP(homeSourceBean) ?: return@execute
						val json = sp.categoryContent(sortData.id, page.toString() + "", true, sortData.filterSelect)
						LOG.i("echo-categoryContent:$json")
						json(listResult, json, homeSourceBean.key)
					} catch (th: Throwable) {
						th.printStackTrace()
					}
				}
			}

			0, 1 -> {
				try {
					OkGo.get<String?>(homeSourceBean.api)
						.tag(homeSourceBean.api)
						.params("ac", if (type == 0) "videolist" else "detail")
						.params("t", sortData.id)
						.params("pg", page)
						.params(sortData.filterSelect)
						.params("f", if (sortData.filterSelect.isEmpty()) "" else JSONObject(sortData.filterSelect).toString())
						.execute(object : AbsCallback<String?>() {
							override fun convertResponse(response: Response): String {
								return response.body.string()
							}

							override fun onSuccess(response: com.lzy.okgo.model.Response<String?>?) {
								if (type == 0) {
									val xml = response?.body()
									xml(listResult, xml ?: return, homeSourceBean.key)
								} else {
									val json = response?.body()
									json(listResult, json, homeSourceBean.key)
								}
							}

							override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
								super.onError(response)
								listResult.postValue(null)
							}
						})
				} catch (ignored: Exception) {
				}
			}

			4 -> {
				val ext: String?
				var extend = homeSourceBean.ext
				extend = getFixUrl(extend ?: return)
				if (sortData.filterSelect.isNotEmpty()) {
					val selectExt = JSONObject(sortData.filterSelect).toString()
					ext = Base64.encodeToString(selectExt.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT or Base64.NO_WRAP)
				} else {
					ext = Base64.encodeToString("{}".toByteArray(), Base64.DEFAULT or Base64.NO_WRAP)
				}

				val request = OkGo.get<String?>(homeSourceBean.api)
					.tag(homeSourceBean.api)
					.params("ac", "detail")
					.params("filter", "true")
					.params("t", sortData.id)
					.params("pg", page)
					.params("ext", ext)
				// 当 extend 不为空且非空字符串时添加参数
				if (extend.isNotEmpty()) {
					request.params("extend", extend)
				}
				request.execute(object : AbsCallback<String?>() {
					override fun convertResponse(response: Response): String {
						try {
							return response.body.string()
						} catch (e: Exception) {
							LOG.i("echo-list: convertResponse error" + e.message)
							throw e // 重新抛出异常
						}
					}

					override fun onSuccess(response: com.lzy.okgo.model.Response<String?>) {
						val json = response.body()
						LOG.i("echo-list: $json")
						json(listResult, json, homeSourceBean.key)
					}

					override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
						super.onError(response)
						listResult.postValue(null)
					}
				})
			}

			else -> {
				listResult.postValue(null)
			}
		}
	}

	// homeVideoContent
	fun getHomeRecList(sourceBean: SourceBean, ids: List<String?>?, callback: HomeRecCallback) {
		when (val type = sourceBean.type) {
			3 -> {
				val waitResponse = Runnable {
					val executor = Executors.newSingleThreadExecutor()
					val future = executor.submit(Callable {
						val sp = ApiConfig.instance.getCSP(sourceBean)
						sp?.homeVideoContent()
					})
					var sortJson: String? = null
					try {
						sortJson = future.get(10, TimeUnit.SECONDS)
					} catch (e: TimeoutException) {
						e.printStackTrace()
						future.cancel(true)
					} catch (e: InterruptedException) {
						e.printStackTrace()
					} catch (e: ExecutionException) {
						e.printStackTrace()
					} finally {
						if (sortJson != null) {
							val absXml = json(null, sortJson, sourceBean.key)
							val movie = absXml?.movie
							if (absXml != null && movie != null && movie.videoList != null) {
								callback.done(movie.videoList)
							} else {
								callback.done(null)
							}
						} else {
							callback.done(null)
						}
						try {
							executor.shutdown()
						} catch (th: Throwable) {
							th.printStackTrace()
						}
					}
				}
				spThreadPool.execute(waitResponse)
			}

			0, 1 -> {
				OkGo.get<String?>(sourceBean.api)
					.tag("detail")
					.params("ac", if (sourceBean.type == 0) "videolist" else "detail")
					.params("ids", ids?.joinToString(",").orEmpty())
					.execute(object : AbsCallback<String?>() {
						override fun convertResponse(response: Response): String {
							return response.body.string()
						}

						override fun onSuccess(response: com.lzy.okgo.model.Response<String?>?) {
							val absXml: AbsXml?
							if (sourceBean.type == 0) {
								val xml = response?.body()
								absXml = xml(null, xml ?: return, sourceBean.key)
							} else {
								val json = response?.body()
								absXml = json(null, json, sourceBean.key)
							}
							val movie = absXml?.movie
							if (absXml != null && movie != null && movie.videoList != null) {
								callback.done(movie.videoList)
							} else {
								callback.done(null)
							}
						}

						override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
							super.onError(response)
							callback.done(null)
						}
					})
			}

			else -> {
				callback.done(null)
			}
		}
	}

	// detailContent
	fun getDetail(sourceKey: String?, urlid: String) {
		var sourceKey = sourceKey
		var urlid = urlid
		if (urlid.startsWith("push://") && ApiConfig.instance.getSource("push_agent") != null) {
			var pushUrl = urlid.substring(7)
			if (pushUrl.startsWith("b64:")) {
				pushUrl = String(Base64.decode(pushUrl.substring(4), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
			} else {
				pushUrl = URLDecoder.decode(pushUrl, "UTF-8")
			}
			sourceKey = "push_agent"
			urlid = pushUrl
		}
		val id: String = urlid

		val sourceBean = ApiConfig.instance.getSource(sourceKey) ?: return
		when (val type = sourceBean.type) {
			3 -> {
				spThreadPool.execute {
					val executor = Executors.newSingleThreadExecutor()
					val future = executor.submit<String?> {
						val sp = ApiConfig.instance.getCSP(sourceBean)
						val ids: MutableList<String> = mutableListOf()
						ids.add(id)
						try {
							return@submit sp?.detailContent(ids)
						} catch (e: Exception) {
							LOG.i("echo--getDetail--error: " + e.message)
							return@submit ""
						}
					}

					var json: String? = null
					try {
						json = future.get(15, TimeUnit.SECONDS)
						LOG.i("echo--getDetail--result:$json")
					} catch (e: TimeoutException) {
						LOG.i("echo--getDetail--timeout")
						future.cancel(true)
					} catch (e: Exception) {
						LOG.i("echo--getDetail--error: " + e.message)
					} finally {
						json(detailResult, json, sourceBean.key)
						executor.shutdown()
					}
				}
			}

			0, 1, 4 -> {
				var extend = sourceBean.ext
				extend = getFixUrl(extend ?: return)

				val request = OkGo.get<String?>(sourceBean.api)
					.tag("detail")
					.params("ac", if (type == 0) "videolist" else "detail")
					.params("ids", id)
				// 当 extend 不为空且非空字符串时添加参数
				if (extend.isNotEmpty()) {
					request.params("extend", extend)
				}
				request.execute(object : AbsCallback<String?>() {
					override fun convertResponse(response: Response): String {
						return response.body.string()
					}

					override fun onSuccess(response: com.lzy.okgo.model.Response<String?>?) {
						if (type == 0) {
							val xml = response?.body()
							xml(detailResult, xml ?: return, sourceBean.key)
						} else {
							val json = response?.body()
							LOG.i(json)
							json(detailResult, json, sourceBean.key)
						}
					}

					override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
						super.onError(response)
						json(detailResult, "", sourceBean.key)
					}
				})
			}

			else -> {
				detailResult.postValue(null)
			}
		}
	}

	// searchContent
	fun getSearch(sourceKey: String?, wd: String?) {
		var wd = wd
		val sourceBean = ApiConfig.instance.getSource(sourceKey) ?: return
		when (val type = sourceBean.type) {
			3 -> {
				try {
					val sp = ApiConfig.instance.getCSP(sourceBean) ?: return
					val search = sp.searchContent(wd, false)
					if (search.isNotEmpty()) {
						json(searchResult, search, sourceBean.key)
					} else {
						json(searchResult, "", sourceBean.key)
					}
				} catch (th: Throwable) {
					th.printStackTrace()
					json(searchResult, "", sourceBean.key)
				}
			}

			0, 1 -> {
				OkGo.get<String?>(sourceBean.api)
					.params("wd", wd)
					.params(if (type == 1) "ac" else null, if (type == 1) "detail" else null)
					.tag("search")
					.execute(object : AbsCallback<String?>() {
						override fun convertResponse(response: Response): String {
							return response.body.string()
						}

						override fun onSuccess(response: com.lzy.okgo.model.Response<String?>?) {
							if (type == 0) {
								val xml = response?.body()
								xml(searchResult, xml ?: return, sourceBean.key)
							} else {
								val json = response?.body()
								json(searchResult, json, sourceBean.key)
							}
						}

						override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
							super.onError(response)
							EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SEARCH_RESULT, null))
						}
					})
			}

			4 -> {
				var extend = sourceBean.ext
				extend = getFixUrl(extend ?: return)
				try {
					wd = URLEncoder.encode(wd, "UTF-8")
				} catch (e: UnsupportedEncodingException) {
					e.printStackTrace()
				}

				val request = OkGo.get<String?>(sourceBean.api)
					.tag("search")
					.params("wd", wd)
					.params("ac", "detail")
					.params("quick", "false")
				// 当 extend 不为空且非空字符串时添加参数
				if (extend.isNotEmpty()) {
					request.params("extend", extend)
				}
				request.execute(object : AbsCallback<String?>() {
					override fun convertResponse(response: Response): String {
						return response.body.string()
					}

					override fun onSuccess(response: com.lzy.okgo.model.Response<String?>) {
						val json = response.body()
						LOG.i("echo-t4 search onSuccess$json")
						json(searchResult, json, sourceBean.key)
					}

					override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
						LOG.i("echo-t4 search-onError")
						super.onError(response)
						EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SEARCH_RESULT, null))
					}
				})
			}

			else -> {
				searchResult.postValue(null)
			}
		}
	}

	// searchContent
	fun getQuickSearch(sourceKey: String?, wd: String?) {
		val sourceBean = ApiConfig.instance.getSource(sourceKey) ?: return
		when (val type = sourceBean.type) {
			3 -> {
				try {
					val sp = ApiConfig.instance.getCSP(sourceBean) ?: return
					json(quickSearchResult, sp.searchContent(wd, true), sourceBean.key)
				} catch (th: Throwable) {
					th.printStackTrace()
				}
			}

			0, 1 -> {
				OkGo.get<String?>(sourceBean.api)
					.params("wd", wd)
					.params(if (type == 1) "ac" else null, if (type == 1) "detail" else null)
					.tag("quick_search")
					.execute(object : AbsCallback<String?>() {
						override fun convertResponse(response: Response): String {
							return response.body.string()
						}

						override fun onSuccess(response: com.lzy.okgo.model.Response<String?>?) {
							if (type == 0) {
								val xml = response?.body()
								xml(quickSearchResult, xml ?: return, sourceBean.key)
							} else {
								val json = response?.body()
								json(quickSearchResult, json, sourceBean.key)
							}
						}

						override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
							super.onError(response)
							EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_RESULT, null))
						}
					})
			}

			4 -> {
				var extend = sourceBean.ext
				extend = getFixUrl(extend ?: return)

				val request = OkGo.get<String?>(sourceBean.api)
					.tag("search")
					.params("wd", wd)
					.params("ac", "detail")
					.params("quick", "true")
				// 当 extend 不为空且非空字符串时添加参数
				if (extend.isNotEmpty()) {
					request.params("extend", extend)
				}
				request.execute(object : AbsCallback<String?>() {
					override fun convertResponse(response: Response): String {
						return response.body.string()
					}

					override fun onSuccess(response: com.lzy.okgo.model.Response<String?>) {
						val json = response.body()
						LOG.i(json)
						json(quickSearchResult, json, sourceBean.key)
					}

					override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
						super.onError(response)
						EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SEARCH_RESULT, null))
					}
				})
			}

			else -> {
				quickSearchResult.postValue(null)
			}
		}
	}

	// playerContent
	fun getPlay(sourceKey: String?, playFlag: String?, progressKey: String?, url: String?, subtitleKey: String?) {
		val sourceBean = ApiConfig.instance.getSource(sourceKey) ?: return
		val type = sourceBean.type
		when (type) {
			3 -> {
				spThreadPool.execute {
					val executor = Executors.newSingleThreadExecutor()
					val future = executor.submit<String?> {
						val sp = ApiConfig.instance.getCSP(sourceBean)
						if (url.isNullOrEmpty()) return@submit ""
						try {
							return@submit sp?.playerContent(playFlag, url, ApiConfig.instance.vipParseFlags)
						} catch (e: Exception) {
							LOG.i("echo--getPlay--error: " + e.message)
							return@submit ""
						}
					}
					try {
						val json = future.get(10, TimeUnit.SECONDS)
						LOG.i("echo--getPlay--result:$json")
						// 处理返回的 JSON
						if (!json.isNullOrEmpty()) {
							val result = JSONObject(json)
							result.put("key", url)
							result.put("proKey", progressKey)
							result.put("subtKey", subtitleKey)
							if (!result.has("flag")) result.put("flag", playFlag)
							playResult.postValue(result)
						} else {
							playResult.postValue(null)
						}
					} catch (e: TimeoutException) {
						// 如果超时了，处理超时逻辑
						LOG.i("echo--getPlay--timeout")
						future.cancel(true)
						playResult.postValue(null)
					} catch (e: Exception) {
						// 捕获其他异常
						LOG.i("echo--getPlay--error: " + e.message)
						playResult.postValue(null)
					} finally {
						executor.shutdown()
					}
				}
			}

			0, 1 -> {
				val result = JSONObject()
				try {
					result.put("key", url)
					val playUrl = (sourceBean.playerUrl ?: return).trim { it <= ' ' }
					if (DefaultConfig.isVideoFormat(url) && playUrl.isEmpty()) {
						result.put("parse", 0)
						result.put("url", url)
					} else {
						result.put("parse", 1)
						result.put("url", url)
					}
					result.put("proKey", progressKey)
					result.put("subtKey", subtitleKey)
					result.put("playUrl", playUrl)
					result.put("flag", playFlag)
					playResult.postValue(result)
				} catch (th: Throwable) {
					th.printStackTrace()
					playResult.postValue(null)
				}
			}

			4 -> {
				var extend = sourceBean.ext
				extend = getFixUrl(extend ?: return)

				val request = OkGo.get<String?>(sourceBean.api)
					.tag("play")
					.params("play", url)
					.params("flag", playFlag)
				// 当 extend 不为空且非空字符串时添加参数
				if (extend.isNotEmpty()) {
					request.params("extend", extend)
				}
				request.execute(object : AbsCallback<String?>() {
					override fun convertResponse(response: Response): String {
						return response.body.string()
					}

					override fun onSuccess(response: com.lzy.okgo.model.Response<String?>?) {
						val json = response?.body()
						LOG.i(json)
						try {
							val result = JSONObject(json ?: return)
							result.put("key", url)
							result.put("proKey", progressKey)
							result.put("subtKey", subtitleKey)
							if (!result.has("flag")) result.put("flag", playFlag)
							playResult.postValue(result)
						} catch (th: Throwable) {
							th.printStackTrace()
							playResult.postValue(null)
						}
					}

					override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
						super.onError(response)
						playResult.postValue(null)
					}
				})
			}

			else -> {
				playResult.postValue(null)
			}
		}
	}

	private fun getFixUrl(extend: String): String {
		if (extend.isEmpty()) return ""
		if (!extend.startsWith("http")) return extend
		val key = MD5.string2MD5(extend)
		if (extendCache.containsKey(key)) {
			LOG.i("echo-getFixUrl Cache")
			return extendCache.getValue(key)
		}
		val future: Future<String> = spThreadPool.submit<String> {
			var result = extend
			if (extend.startsWith("http://127.0.0.1")) {
				var path = extend.replace("^http.+/file/".toRegex(), FileUtils.getRootPath() + "/")
				path = path.replace("localhost/".toRegex(), "/")
				result = FileUtils.readFileToString(path, "UTF-8")
				result = tryMinifyJson(result)
				extendCache.putIfAbsent(key, result)
			} else if (extend.startsWith("http")) {
				result = OkHttpUtil.string(extend, null)
				if (result.isNotEmpty()) {
					result = tryMinifyJson(result)
					if (result.length > 2500) result = extend
					extendCache.putIfAbsent(key, result)
				}
			}
			result
		}

		try {
			return future.get(5, TimeUnit.SECONDS) ?: extend
		} catch (te: TimeoutException) {
			te.printStackTrace()
			future.cancel(true)
			return extend
		} catch (e: Exception) {
			e.printStackTrace()
			return extend
		}
	}

	private fun tryMinifyJson(raw: String): String {
		var raw = raw
		try {
			raw = raw.trim { it <= ' ' }
			val jsonElement = JsonParser.parseString(raw)
			return gson.toJson(jsonElement)
		} catch (e: Exception) {
			return raw
		}
	}

	private fun getSortFilter(obj: JsonObject): MovieSort.SortFilter {
		val key = obj.get("key").asString
		val name = obj.get("name").asString
		val kv = obj.getAsJsonArray("value")
		val values = linkedMapOf<String, String>()
		for (ele in kv) {
			val eleObj = ele.asJsonObject
			val valuesKey = if (eleObj.has("n")) eleObj.get("n").asString else ""
			val valuesValue = if (eleObj.has("v")) eleObj.get("v").asString else ""
			values[valuesKey] = valuesValue
		}
		val filter = MovieSort.SortFilter()
		filter.key = key
		filter.name = name
		filter.values = values
		return filter
	}

	private fun sortJson(result: MutableLiveData<AbsSortXml?>?, json: String): AbsSortXml? {
		try {
			val obj = JsonParser.parseString(json).asJsonObject
			val sortJson = gson.fromJson<AbsSortJson>(obj, object : TypeToken<AbsSortJson?>() {
			}.type)
			val data = sortJson.toAbsSortXml()
			try {
				if (obj.has("filters")) {
					val sortFilters = linkedMapOf<String, ArrayList<MovieSort.SortFilter>?>()
					val filters = obj.getAsJsonObject("filters")
					for (key in filters.keySet()) {
						val sortFilter = ArrayList<MovieSort.SortFilter>()
						val one = filters.get(key)
						if (one.isJsonObject) {
							sortFilter.add(getSortFilter(one.asJsonObject))
						} else {
							for (ele in one.asJsonArray) {
								sortFilter.add(getSortFilter(ele.asJsonObject))
							}
						}
						sortFilters[key] = sortFilter
					}
					val classes = data.classes ?: return null
					val sortList = classes.sortList ?: return null
					for (sort in sortList) {
						val filters = sortFilters[sort.id] ?: continue
						sort.filters = filters
					}
				}
			} catch (ignored: Throwable) {
			}
			return data
		} catch (e: Exception) {
			return null
		}
	}

	private fun sortXml(result: MutableLiveData<AbsSortXml?>?, xml: String?): AbsSortXml? {
		try {
			val xstream = XStream(DomDriver()) // 创建Xstram对象
			xstream.autodetectAnnotations(true)
			xstream.processAnnotations(AbsSortXml::class.java)
			xstream.ignoreUnknownElements()
			val data = xstream.fromXML(xml) as AbsSortXml
			return data
		} catch (e: Exception) {
			return null
		}
	}

	private fun absXml(data: AbsXml, sourceKey: String?) {
		val movie = data.movie ?: return
		val videoList = movie.videoList ?: return
		for (video in videoList) {
			val urlBean = video.urlBean ?: continue
			val infoList = urlBean.infoList ?: continue
			for (urlInfo in infoList) {
				val urls = urlInfo.urls ?: continue
				val str = if (urls.contains("#")) {
					urls.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				} else {
					arrayOf(urls)
				}
				val infoBeanList: MutableList<InfoBean> = mutableListOf()
				for (s in str) {
					val ss = s.split("\\$".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
					if (ss.isNotEmpty()) {
						if (ss.size >= 2) {
							infoBeanList.add(InfoBean(ss[0], ss[1]))
						} else {
							infoBeanList.add(InfoBean((infoBeanList.size + 1).toString() + "", ss[0]))
						}
					}
				}
				urlInfo.beanList = infoBeanList
			}
			video.sourceKey = sourceKey
		}
	}

	private fun checkPush(data: AbsXml): AbsXml {
		val movie = data.movie ?: return data
		val videoList = movie.videoList ?: return data
		if (videoList.isEmpty()) return data
		val video = videoList[0]
		val urlBean = video.urlBean ?: return data
		val infoList = urlBean.infoList ?: return data
		if (infoList.isEmpty()) return data

		for (i in infoList.indices) {
			val urlinfo = infoList[i]
			val beanList = urlinfo.beanList ?: continue
			if (beanList.isEmpty()) continue
			for (infoBean in beanList) {
				val infoBeanUrl = infoBean.url ?: continue
				if (infoBeanUrl.startsWith("push://")) {
					var pushUrl = infoBeanUrl.substring(7)
					if (pushUrl.startsWith("b64:")) {
						pushUrl = String(Base64.decode(pushUrl.substring(4), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
					} else {
						pushUrl = URLDecoder.decode(pushUrl, "UTF-8")
					}

					val resData = arrayOf<AbsXml?>(null)

					val countDownLatch = CountDownLatch(1)
					val threadPool = Executors.newSingleThreadExecutor()
					val finalPushUrl: String = pushUrl
					threadPool.execute(object : Runnable {
						override fun run() {
							val sb = ApiConfig.instance.getSource("push_agent")
							if (sb == null) {
								countDownLatch.countDown()
								return
							}
							if (sb.type == 4) {
								OkGo.get<String?>(sb.api)
									.tag("detail")
									.params("ac", "detail")
									.params("ids", finalPushUrl)
									.execute(object : AbsCallback<String?>() {
										override fun convertResponse(response: Response): String {
											return response.body.string()
										}

										override fun onSuccess(response: com.lzy.okgo.model.Response<String?>) {
											val res = response.body()
											if (!res.isNullOrEmpty()) {
												try {
													val absJson = gson.fromJson<AbsJson>(res, object : TypeToken<AbsJson?>() {
													}.type)
													val absXmlData = absJson.toAbsXml()
													resData[0] = absXmlData
													absXml(absXmlData, sb.key)
												} catch (e: Exception) {
													e.printStackTrace()
												}
											}
											countDownLatch.countDown()
										}

										override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
											super.onError(response)
											countDownLatch.countDown()
										}
									})
							} else {
								try {
									val sp = ApiConfig.instance.getCSP(sb)
									val ids: MutableList<String> = mutableListOf()
									ids.add(finalPushUrl)
									val res = sp?.detailContent(ids)
									if (!res.isNullOrEmpty()) {
										try {
											val absJson = gson.fromJson<AbsJson>(res, object : TypeToken<AbsJson?>() {
											}.type)
											val absXmlData = absJson.toAbsXml()
											resData[0] = absXmlData
											absXml(absXmlData, sb.key)
										} catch (e: Exception) {
											e.printStackTrace()
										}
									}
								} catch (th: Throwable) {
									th.printStackTrace()
								}
								countDownLatch.countDown()
							}
						}
					})
					try {
						countDownLatch.await(15, TimeUnit.SECONDS)
						threadPool.shutdown()
					} catch (e: InterruptedException) {
						e.printStackTrace()
					}
					val res = resData[0]
					if (res != null) {
						val resMovie = res.movie
						val resVideoList = resMovie?.videoList
						if (resMovie != null && !resVideoList.isNullOrEmpty()) {
							val resVideo = resVideoList[0]
							val resUrlBean = resVideo.urlBean
							val resInfoList = resUrlBean?.infoList
							if (resUrlBean != null && !resInfoList.isNullOrEmpty()) {
								if (beanList.size == 1) {
									video.urlBean?.infoList?.removeAt(i)
								} else {
									beanList.remove(infoBean)
								}
								for (resUrlinfo in resInfoList) {
									val resBeanList = resUrlinfo.beanList
									if (!resBeanList.isNullOrEmpty()) {
										video.urlBean?.infoList?.add(resUrlinfo)
									}
								}
								video.sourceKey = "push_agent"
								return data
							}
						}
					}
					infoBean.name = "解析失败 >>> " + infoBean.name
				}
			}
		}
		return data
	}

	fun checkThunder(data: AbsXml, index: Int) {
		var thunderParse = false
		val movie = data.movie ?: return
		val videoList = movie.videoList ?: return
		if (videoList.size == 1) {
			val video = videoList[0]
			val urlBean = video.urlBean ?: return
			val infoList = urlBean.infoList ?: return

			var hasThunder = false
			thunderLoop@ for (idx in infoList.indices) {
				val urlInfo = infoList[idx]
				for (infoBean in urlInfo.beanList ?: continue) {
					if (Thunder.isSupportUrl(infoBean.url ?: continue)) {
						hasThunder = true
						break@thunderLoop
					}
				}
			}
			if (hasThunder) {
				thunderParse = true
				Thunder.parse(App.instance, urlBean, object : ThunderCallback {
					override fun status(code: Int, info: String) {
						if (code >= 0) {
							LOG.i(info)
						} else {
							(infoList[0].beanList ?: return)[0].name = info
							detailResult.postValue(data)
						}
					}

					override fun list(urlMap: Map<Int, String>) {
						for (key in urlMap.keys) {
							val playList = urlMap[key]
							infoList[key].urls = playList
							val str = (playList ?: return).split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
							val infoBeanList: MutableList<InfoBean> = mutableListOf()
							for (s in str) {
								if (s.contains("$")) {
									val ss = s.split("\\$".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

									if (ss.isNotEmpty()) {
										if (ss.size >= 2) {
											infoBeanList.add(InfoBean(ss[0], ss[1]))
										} else {
											infoBeanList.add(InfoBean((infoBeanList.size + 1).toString() + "", ss[0]))
										}
									}
								}
							}
							infoList[key].beanList = infoBeanList
						}
						detailResult.postValue(data)
					}

					override fun play(url: String) {
					}
				})
			}
		}
		if (!thunderParse && index == 0) {
			detailResult.postValue(data)
		}
	}

	private fun xml(result: MutableLiveData<AbsXml?>?, xml: String, sourceKey: String?): AbsXml? {
		var xml = xml
		try {
			val xstream = XStream(DomDriver()) // 创建Xstram对象
			xstream.autodetectAnnotations(true)
			xstream.processAnnotations(AbsXml::class.java)
			xstream.ignoreUnknownElements()
			if (xml.contains("<year></year>")) {
				xml = xml.replace("<year></year>", "<year>0</year>")
			}
			if (xml.contains("<state></state>")) {
				xml = xml.replace("<state></state>", "<state>0</state>")
			}
			var data = xstream.fromXML(xml) as AbsXml
			absXml(data, sourceKey)
			if (searchResult === result) {
				EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SEARCH_RESULT, data))
			} else if (quickSearchResult === result) {
				EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_RESULT, data))
			} else if (result != null) {
				if (result === detailResult) {
					data = checkPush(data)
					checkThunder(data, 0)
				} else {
					result.postValue(data)
				}
			}
			return data
		} catch (e: Exception) {
			if (searchResult === result) {
				EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SEARCH_RESULT, null))
			} else if (quickSearchResult === result) {
				EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_RESULT, null))
			} else result?.postValue(null)
			return null
		}
	}

	private fun json(result: MutableLiveData<AbsXml?>?, json: String?, sourceKey: String?): AbsXml? {
		try {
			val absJson = gson.fromJson<AbsJson>(json, object : TypeToken<AbsJson?>() {
			}.type)
			var data = absJson.toAbsXml()
			absXml(data, sourceKey)
			if (searchResult === result) {
				EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SEARCH_RESULT, data))
			} else if (quickSearchResult === result) {
				EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_RESULT, data))
			} else if (result != null) {
				if (result === detailResult) {
					data = checkPush(data)
					checkThunder(data, 0)
				} else {
					result.postValue(data)
				}
			}
			return data
		} catch (e: Exception) {
			if (searchResult === result) {
				EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SEARCH_RESULT, null))
			} else if (quickSearchResult === result) {
				EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_RESULT, null))
			} else result?.postValue(null)
			return null
		}
	}

	fun interface HomeRecCallback {
		fun done(videos: MutableList<Movie.Video>?)
	}

	companion object {
		val spThreadPool: ExecutorService = Executors.newSingleThreadExecutor()

		// homeContent缓存，最多存储5个sourceKey的AbsSortXml对象
		private val sortCache: MutableMap<String, AbsSortXml?> = object : LinkedHashMap<String, AbsSortXml?>(5, 0.75f, true) {
			override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AbsSortXml?>?): Boolean {
				return size > 5
			}
		}
		private val extendCache = ConcurrentHashMap<String, String>()
	}
}
