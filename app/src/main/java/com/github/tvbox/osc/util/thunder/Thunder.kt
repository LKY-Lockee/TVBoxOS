package com.github.tvbox.osc.util.thunder

import android.content.*
import android.text.*
import androidx.core.content.*
import com.github.tvbox.osc.base.*
import com.github.tvbox.osc.bean.Movie.Video.*
import com.github.tvbox.osc.util.*
import com.xunlei.downloadlib.*
import com.xunlei.downloadlib.android.*
import com.xunlei.downloadlib.parameter.*
import java.io.*
import java.util.*
import java.util.concurrent.*

object Thunder {
	private var cacheRoot = ""
	private var localPath = ""
	private var name = ""
	private var task_url = ""
	private var currentTask = 0L
	private var torrentFileInfoArrayList: ArrayList<TorrentFileInfo>? = null
	private var threadPool: ExecutorService? = null
	private var playList: ArrayList<String>? = null
	private var ed2kList: ArrayList<String>? = null
	private val formats: ArrayList<String> by lazy {
		arrayListOf(
			".rmvb", ".avi", ".mkv", ".flv", ".mp4", ".rm",
			".vob", ".wmv", ".mov", ".3gp", ".asf", ".mpg", ".mpeg", ".mpe"
		)
	}

	val taskInfo: XLTaskInfo?
		get() = XLTaskHelper.instance().getTaskInfo(currentTask)

	val playUrl: String?
		get() {
			if (currentTask != 0L) {
				if (isNetworkDownloadTask(task_url)) {
					return XLTaskHelper.instance().getLoclUrl(localPath + name)
				}
			}
			return null
		}

	fun stop(bool: Boolean) {
		if (currentTask > 0) {
			XLTaskHelper.instance().stopTask(currentTask)
			currentTask = 0L
		}
		if (bool) {
			torrentFileInfoArrayList = null
			// del cache file
			val cache = File(if (task_url.isEmpty()) cacheRoot else localPath)
			recursiveDelete(cache)
			if (!cache.exists()) cache.mkdirs()
			threadPool?.let {
				try {
					it.shutdownNow()
					threadPool = null
				} catch (_: Throwable) {
				}
			}
		}
	}

	fun parse(context: Context, urlBean: UrlBean, callback: ThunderCallback) {
		init(context)
		stop(true)
		threadPool = Executors.newSingleThreadExecutor()
		torrentFileInfoArrayList = ArrayList()
		playList = ArrayList()
		ed2kList = ArrayList()
		val urlMap = mutableMapOf<Int, String>()
		threadPool?.execute {
			for (idx in urlBean.infoList.indices) {
				val urlInfo = urlBean.infoList[idx] ?: continue
				for (infoBean in urlInfo.beanList) {
					var isParse = false
					var url = infoBean.url
					if (isMagnet(url) || isThunder(url) || isTorrent(url)) {
						if (isThunder(url)) url = XLDownloadManager.getInstance().parserThunderUrl(url)
						val link = if (isThunder(url)) XLDownloadManager.getInstance().parserThunderUrl(url) else url
						val fileName = XLTaskHelper.instance().getFileName(link)
						val cache = File(cacheRoot + File.separator + fileName)
						try {
							if (currentTask > 0) {
								XLTaskHelper.instance().stopTask(currentTask)
								currentTask = 0L
							}
							currentTask = if (isMagnet(url)) {
								XLTaskHelper.instance().addMagentTask(url, cacheRoot, fileName)
							} else {
								XLTaskHelper.instance().addThunderTask(url, cacheRoot, fileName)
							}
						} catch (exception: Exception) {
							exception.printStackTrace()
							currentTask = 0
						}
						if (currentTask <= 0) {
							continue
						}
						var count = 30
						outerLoop@ while (true) {
							count--
							if (count <= 0) {
								break
							}
							val taskInfo = XLTaskHelper.instance().getTaskInfo(currentTask)
							if (taskInfo != null) {
								when (taskInfo.mTaskStatus) {
									2 -> {
										run {
											try {
												val torrentInfo = XLTaskHelper.instance().getTorrentInfo(cache.absolutePath)
												if (torrentInfo != null && !TextUtils.isEmpty(torrentInfo.mInfoHash)) {
													val mSubFileInfo = torrentInfo.mSubFileInfo
													if (mSubFileInfo != null) {
														for (sub in mSubFileInfo) {
															if (isMedia(sub.mFileName) && sub.mFileSize > 1048576L * 30) {
																sub.torrentPath = cache.absolutePath
																playList?.add($$"$${sub.mFileName}$tvbox-torrent:$${Thunder.torrentFileInfoArrayList?.size}")
																torrentFileInfoArrayList?.add(sub)
															}
														}
														isParse = true
														break@outerLoop
													}
												}
											} catch (throwable: Throwable) {
												throwable.printStackTrace()
											}
										}
										run {
											break@outerLoop
										}
									}

									3 -> {
										break@outerLoop
									}

									else -> {}
								}
							}
							try {
								Thread.sleep(100)
							} catch (e: InterruptedException) {
								e.printStackTrace()
							}
						}
					} else {
						url = infoBean.url
						if (isThunder(url)) url = XLDownloadManager.getInstance().parserThunderUrl(url)
						if (isNetworkDownloadTask(url)) {
							task_url = url
							if (TextUtils.isEmpty(task_url)) {
								continue
							}
							name = XLTaskHelper.instance().getFileName(task_url)
							playList?.add($$"${$$name}$tvbox-oth:${$${ed2kList?.size}}")
							ed2kList?.add(task_url)
							isParse = true
						}
					}
					if (!isParse) playList?.add($$"$${infoBean.name}$$${infoBean.url}")
				}
				val currentPlayList = playList
				if (!currentPlayList.isNullOrEmpty()) {
					urlMap[idx] = TextUtils.join("#", currentPlayList)
					currentPlayList.clear()
				}
			}
			if (urlMap.isNotEmpty()) {
				callback.list(urlMap)
			} else {
				callback.status(-1, "解析异常")
			}
		}
	}

	fun play(url: String, callback: ThunderCallback): Boolean {
		if (url.startsWith("tvbox-torrent:")) {
			val idx = url.substring(14).toInt()
			val info = torrentFileInfoArrayList?.get(idx)
			if (currentTask > 0) {
				XLTaskHelper.instance().stopTask(currentTask)
				currentTask = 0L
			}
			if (info == null) {
				return false
			}
			threadPool?.execute execute@{
				val torrentName = File(info.torrentPath).name
				val cache = cacheRoot + File.separator + torrentName.substringBeforeLast(".")
				currentTask = XLTaskHelper.instance().addTorrentTask(info.torrentPath, cache, info.mFileIndex)
				if (currentTask < 0) callback.status(-1, "下载出错")
				var count = 30
				while (true) {
					count--
					if (count <= 0) {
						callback.status(-1, "解析下载超时")
						break
					}
					val taskInfo = XLTaskHelper.instance().getBtSubTaskInfo(currentTask, info.mFileIndex).mTaskInfo
					when (taskInfo.mTaskStatus) {
						3 -> {
							callback.status(-1, errorInfo(taskInfo.mErrorCode))
							return@execute
						}

						1, 4, 2 -> {
							// 下载完成
							val pUrl = XLTaskHelper.instance().getLoclUrl(cache + File.separator + info.mFileName)
							callback.play(pUrl)
							return@execute
						}
					}
					try {
						Thread.sleep(1000)
					} catch (e: InterruptedException) {
						e.printStackTrace()
					}
				}
			}
			return true
		}
		if (url.startsWith("tvbox-oth:")) {
			stop(false)
			val idx = url.substring(10).toInt()
			task_url = ed2kList?.get(idx) ?: ""
			name = XLTaskHelper.instance().getFileName(task_url)
			localPath = (File(cacheRoot + File.separator + "temp", FileUtils.getFileNameWithoutExt(name))).toString() + "/"
			currentTask = XLTaskHelper.instance().addThunderTask(task_url, localPath, null)

			threadPool?.execute execute@{
				var count = 20
				while (true) {
					count--
					if (count <= 0) {
						callback.status(-1, "解析下载超时")
						break
					}
					val playUrl = this@Thunder.playUrl
					if (!playUrl.isNullOrEmpty()) {
						callback.play(playUrl)
						return@execute
					}
					try {
						Thread.sleep(1000)
					} catch (e: InterruptedException) {
						e.printStackTrace()
					}
				}
			}
			return true
		}
		if (isEd2k(url) || isFtp(url)) {
			if (threadPool == null) {
				init(App.instance)
				threadPool = Executors.newSingleThreadExecutor()
			}
			if (currentTask > 0) {
				XLTaskHelper.instance().stopTask(currentTask)
				currentTask = 0L
			}
			task_url = url
			name = XLTaskHelper.instance().getFileName(task_url)
			localPath = (File(cacheRoot + File.separator + "temp", FileUtils.getFileNameWithoutExt(name))).toString() + "/"
			currentTask = XLTaskHelper.instance().addThunderTask(task_url, localPath, null)

			threadPool?.execute {
				var count = 20
				while (true) {
					count--
					if (count <= 0) {
						callback.status(-1, "解析下载超时")
						break
					}
					val playUrl = this@Thunder.playUrl
					if (!TextUtils.isEmpty(playUrl)) {
						callback.play(playUrl)
						return@execute
					}
					try {
						Thread.sleep(1000)
					} catch (e: InterruptedException) {
						e.printStackTrace()
					}
				}
			}
			return true
		}
		return false
	}

	fun isSupportUrl(url: String): Boolean {
		return isMagnet(url) || isThunder(url) || isTorrent(url)
	}

	fun isFtp(url: String): Boolean {
		return url.lowercase(Locale.getDefault()).startsWith("ftp://")
	}

	fun recursiveDelete(file: File) {
		if (!file.exists()) return
		if (file.isDirectory) {
			file.listFiles()?.forEach { recursiveDelete(it) }
		}
		file.delete()
	}

	fun isMedia(name: String): Boolean {
		return formats.any { name.lowercase(Locale.getDefault()).endsWith(it) }
	}

	fun randomImei(): String {
		return randomString("0123456", 15)
	}

	fun randomMac(): String {
		@Suppress("SpellCheckingInspection")
		return randomString("ABCDEF0123456", 12).uppercase(Locale.getDefault())
	}

	fun randomString(base: String, length: Int): String {
		val random = Random()
		return buildString {
			repeat(length) {
				val number = random.nextInt(base.length)
				append(base[number])
			}
		}
	}

	fun isNetworkDownloadTask(url: String): Boolean {
		if (TextUtils.isEmpty(url)) return false
		return isFtp(url) || isEd2k(url)
	}

	fun stopTask() {
		if (currentTask != 0L) {
			XLTaskHelper.instance().deleteTask(currentTask, if (task_url.isEmpty()) cacheRoot else localPath)
			currentTask = 0L
		}
	}

	private fun init(context: Context) {
		// fake deviceId and Mac
		val sharedPreferences = context.getSharedPreferences("rand_thunder_id", Context.MODE_PRIVATE)
		var imei = sharedPreferences.getString("imei", null)
		var mac = sharedPreferences.getString("mac", null)
		if (imei == null) {
			imei = randomImei()
			sharedPreferences.edit(commit = true) { putString("imei", imei) }
		}
		if (mac == null) {
			mac = randomMac()
			sharedPreferences.edit(commit = true) { putString("mac", mac) }
		}

		XLUtil.mIMEI = imei
		XLUtil.isGetIMEI = true
		XLUtil.mMAC = mac
		XLUtil.isGetMAC = true
		val cd3 = "cee25055f125a2fde0"

		@Suppress("SpellCheckingInspection")
		val base64Decode = "axzNjAwMQ^^yb==0^852^083dbcff^"

		val substring = base64Decode.substring(1)
		val substring2 = cd3.substring(0, cd3.length - 1)
		val cd = substring + substring2
		XLTaskHelper.init(context, cd, "21.01.07.800002")
		cacheRoot = context.cacheDir.absolutePath + File.separator + "thunder"
	}

	private fun errorInfo(code: Int): String {
		return when (code) {
			9125 -> "文件名太长"
			111120 -> "文件路径太长"
			111142 -> "文件太小"
			111085 -> "磁盘空间不足"
			111171 -> "拒绝的网络连接"
			9301 -> "缓冲区不足"
			114001, 114004, 114005, 114006, 114007, 114011, 9304, 111154 -> "版权限制：无权下载"
			114101 -> "无效链接"
			else -> "ErrorCode=$code"
		}
	}

	private fun isMagnet(url: String): Boolean {
		return url.lowercase(Locale.getDefault()).startsWith("magnet:")
	}

	private fun isThunder(url: String): Boolean {
		return url.lowercase(Locale.getDefault()).startsWith("thunder")
	}

	private fun isTorrent(url: String): Boolean {
		return url.lowercase(Locale.getDefault())
			.split(";")
			.first()
			.endsWith(".torrent")
	}

	private fun isEd2k(url: String): Boolean {
		return url.lowercase(Locale.getDefault()).startsWith("ed2k:")
	}

	interface ThunderCallback {
		fun status(code: Int, info: String?)

		fun list(urlMap: Map<Int, String>?)

		fun play(url: String?)
	}
}
