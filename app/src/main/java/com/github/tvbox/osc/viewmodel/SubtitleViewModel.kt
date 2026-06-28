package com.github.tvbox.osc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.tvbox.osc.bean.Subtitle
import com.github.tvbox.osc.bean.SubtitleData
import com.github.tvbox.osc.ui.dialog.SearchSubtitleDialog
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class SubtitleViewModel : ViewModel() {
	val searchResult: MutableLiveData<SubtitleData?> = MutableLiveData<SubtitleData?>()
	val regexShooterFileOnclick: Pattern = Pattern.compile("onthefly\\(\"(\\d+)\",\"(\\d+)\",\"([\\s\\S]*)\"\\)")
	private var pagesTotal = -1

	fun searchResult(title: String?, page: Int) {
		searchResultFromAssrt(title, page)
	}

	fun getSearchResultSubtitleUrls(subtitle: Subtitle) {
		getSearchResultSubtitleUrlsFromAssrt(subtitle)
	}

	fun getSubtitleUrl(subtitle: Subtitle, subtitleLoader: SearchSubtitleDialog.SubtitleLoader) {
		getSubtitleUrlFromAssrt(subtitle, subtitleLoader)
	}

	private fun setSearchListData(data: List<Subtitle>?, isNew: Boolean, isZip: Boolean) {
		try {
			val subtitleData = SubtitleData()
			subtitleData.subtitleList = data
			subtitleData.isNew = isNew
			subtitleData.isZip = isZip
			searchResult.postValue(subtitleData)
		} catch (e: Throwable) {
			e.printStackTrace()
			searchResult.postValue(null)
		}
	}

	private fun searchResultFromAssrt(title: String?, page: Int) {
		try {
			if (pagesTotal in 1..<page) {
				setSearchListData(emptyList(), isNew = false, isZip = true)
				return
			}
			if (page == 1) pagesTotal = -1 // 第一页时 重置页大小

			val searchApiUrl = "https://secure.assrt.net/sub/"
			OkGo.get<String?>(searchApiUrl)
				.params("searchword", title)
				.params("sort", "rank")
				.params("page", page)
				.params("no_redir", "1")
				.execute(object : AbsCallback<String?>() {
					override fun onSuccess(response: Response<String?>?) {
						try {
							val content = response?.body()
							val doc = Jsoup.parse(content ?: return)
							val items = doc.select(".resultcard .sublist_box_title a.introtitle")
							val data = mutableListOf<Subtitle>()
							for (item in items) {
								val title = item.attr("title")
								val href = item.attr("href")
								if (href.isEmpty()) continue
								val one = Subtitle()
								one.name = title
								one.url = "https://assrt.net$href"
								one.isZip = true
								data.add(one)
							}
							setSearchListData(data, page <= 1, true)
							val pages = doc.select(".pagelinkcard a")
							if (!pages.isEmpty()) {
								val lastPage = pages.last() ?: return
								val ps = lastPage.text().split("/", limit = 2)
								if (ps.size == 2 && ps[1].isNotEmpty()) {
									pagesTotal = ps[1].trim { it <= ' ' }.toInt()
								}
							}
						} catch (th: Throwable) {
							th.printStackTrace()
						}
					}

					override fun convertResponse(response: okhttp3.Response): String {
						return response.body.string()
					}

					override fun onError(response: Response<String?>?) {
						super.onError(response)
						setSearchListData(null, page <= 1, true)
					}
				})
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	private fun getSearchResultSubtitleUrlsFromAssrt(subtitle: Subtitle) {
		try {
			val url = subtitle.url
			OkGo.get<String?>(url).execute(object : AbsCallback<String?>() {
				override fun onSuccess(response: Response<String?>?) {
					try {
						val content = response?.body()
						val data = mutableListOf<Subtitle>()
						val doc = Jsoup.parse(content ?: return)
						val items = doc.select("#detail-filelist .waves-effect")
						if (!items.isEmpty()) { // 压缩包里面的字幕
							for (item in items) {
								val onclick = item.attr("onclick")
								if (onclick.isEmpty()) continue
								val matcher = regexShooterFileOnclick.matcher(onclick)
								if (matcher.find()) {
									val url = "https://secure.assrt.net/download/${matcher.group(1)}/-/${matcher.group(2)}/${matcher.group(3)}"
									val one = Subtitle()
									val name = item.selectFirst("#filelist-name")
									one.name = name?.text() ?: matcher.group(3)
									one.url = url
									one.isZip = false
									data.add(one)
								}
							}
							setSearchListData(data, isNew = true, isZip = false)
						} else { // 有的字幕 不一定是压缩包
							val item = doc.selectFirst(".download a#btn_download") ?: return
							val href = item.attr("href")
							if (href.isEmpty()) {
								setSearchListData(null, isNew = true, isZip = false)
								return
							}
							val h2 = href.lowercase(Locale.getDefault())
							if (h2.endsWith("srt") || h2.endsWith("ass") || h2.endsWith("scc") || h2.endsWith("ttml")) {
								val downloadUrl = "https://assrt.net$href"
								val one = Subtitle()
								val title = href.substring(href.lastIndexOf("/") + 1)
								one.name = URLDecoder.decode(title, "UTF-8")
								one.url = downloadUrl
								one.isZip = false
								data.add(one)
								setSearchListData(data, isNew = true, isZip = false)
							} else {
								setSearchListData(null, isNew = true, isZip = false)
							}
						}
					} catch (th: Throwable) {
						th.printStackTrace()
					}
				}

				override fun convertResponse(response: okhttp3.Response): String {
					return response.body.string()
				}

				override fun onError(response: Response<String?>?) {
					super.onError(response)
					setSearchListData(null, isNew = true, isZip = true)
				}
			})
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	private fun getSubtitleUrlFromAssrt(subtitle: Subtitle, subtitleLoader: SearchSubtitleDialog.SubtitleLoader) {
		val subtitleUrl = subtitle.url ?: return
		val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.54 Safari/537.36"
		val request = Request.Builder()
			.url(subtitleUrl)
			.get()
			.addHeader("Referer", "https://secure.assrt.net")
			.addHeader("User-Agent", ua)
			.build()
		val builder = OkHttpClient.Builder()
			.readTimeout(15, TimeUnit.SECONDS)
			.writeTimeout(15, TimeUnit.SECONDS)
			.connectTimeout(15, TimeUnit.SECONDS)
			.followRedirects(false)
			.followSslRedirects(false)
			.retryOnConnectionFailure(true)
		val client = builder.build()
		client.newCall(request).enqueue(object : Callback {
			override fun onFailure(call: Call, e: okio.IOException) {
				e.printStackTrace()
			}

			override fun onResponse(call: Call, response: okhttp3.Response) {
				subtitle.url = response.header("location")
				subtitleLoader.loadSubtitle(subtitle)
			}
		})
	}
}
