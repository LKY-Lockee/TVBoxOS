package com.github.tvbox.osc.server

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import android.util.Base64
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.event.ServerEvent
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.OkGoHelper
import com.github.tvbox.osc.util.Proxy
import com.github.tvbox.osc.util.RegexUtils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.IStatus
import org.greenrobot.eventbus.EventBus
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern
import java.util.zip.ZipFile

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
class RemoteServer(port: Int, private val mContext: Context) : NanoHTTPD(port) {
	private val getRequestList = mutableListOf<RequestProcess>()
	private val postRequestList = mutableListOf<RequestProcess>()
	var isStarting: Boolean = false
		private set
	var dataReceiver: DataReceiver? = null

	init {
		addGetRequestProcess()
		addPostRequestProcess()
	}

	private fun addGetRequestProcess() {
		getRequestList.add(RawRequestProcess(this.mContext, "/", R.raw.index, MIME_HTML))
		getRequestList.add(RawRequestProcess(this.mContext, "/index.html", R.raw.index, MIME_HTML))
		getRequestList.add(RawRequestProcess(this.mContext, "/style.css", R.raw.style, "text/css"))
		getRequestList.add(RawRequestProcess(this.mContext, "/ui.css", R.raw.ui, "text/css"))
		getRequestList.add(RawRequestProcess(this.mContext, "/jquery.js", R.raw.jquery, "application/x-javascript"))
		getRequestList.add(RawRequestProcess(this.mContext, "/script.js", R.raw.script, "application/x-javascript"))
		getRequestList.add(RawRequestProcess(this.mContext, "/favicon.ico", R.drawable.app_icon, "image/x-icon"))
	}

	private fun addPostRequestProcess() {
		postRequestList.add(InputRequestProcess(this))
	}

	override fun start(timeout: Int, daemon: Boolean) {
		this.isStarting = true
		super.start(timeout, daemon)
		EventBus.getDefault().post(ServerEvent(ServerEvent.SERVER_SUCCESS))
	}

	override fun stop() {
		super.stop()
		this.isStarting = false
	}

	private fun getProxy(rs: Array<Any?>): Response {
		try {
			if (rs[0] is Response) return rs[0] as Response
			val code = rs[0] as Int
			val mime = rs[1] as String?
			val stream = if (rs[2] != null) rs[2] as InputStream else null
			val response = newChunkedResponse(
				Response.Status.lookup(code),
				mime,
				stream
			)
			// 添加头部信息
			if (rs.size >= 4 && rs[3] is Map<*, *>) {
				val mapHeader = rs[3] as Map<*, *>
				if (mapHeader.isNotEmpty()) {
					for (key in mapHeader.keys) {
						response.addHeader(key as String?, mapHeader[key] as String?)
					}
				}
			}
			return response
		} catch (th: Throwable) {
			return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "500")
		}
	}

	override fun serve(session: IHTTPSession): Response? {
		EventBus.getDefault().post(ServerEvent(ServerEvent.SERVER_CONNECTION))
		if (!session.uri.isEmpty()) {
			var fileName = session.uri.trim { it <= ' ' }
			if (fileName.indexOf('?') >= 0) {
				fileName = fileName.substring(0, fileName.indexOf('?'))
			}
			if (session.method == Method.GET) {
				for (process in getRequestList) {
					if (process.isRequest(session, fileName)) {
						return process.doResponse(session, fileName, session.parameters, null)
					}
				}
				if (fileName == "/proxy") {
					val params = session.parameters
					params.putAll(session.headers.mapValues { listOf(it.value) })
					if (params.containsKey("do")) {
						val rs = ApiConfig.instance.proxyLocal(params)
						return getProxy(rs ?: return null)
					}
					if (params.containsKey("go")) {
						val rs = Proxy.proxy(params)
						return getProxy(rs ?: return null)
					}
				} else if (fileName.startsWith("/file/")) {
					try {
						val f = fileName.substring(6)
						val root = Environment.getExternalStorageDirectory().absolutePath
						val file = "$root/$f"
						val localFile = File(file)
						return if (localFile.exists()) {
							if (localFile.isFile) {
								newChunkedResponse(Response.Status.OK, "application/octet-stream", Files.newInputStream(localFile.toPath()))
							} else {
								newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, fileList(root, f))
							}
						} else {
							newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "File $file not found!")
						}
					} catch (th: Throwable) {
						return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, th.message)
					}
				} else if (fileName == "/dns-query") {
					val name = session.parameters["name"]?.firstOrNull()
					val rs: ByteArray? = try {
						OkGoHelper.dnsOverHttps?.lookupHttpsForwardSync(name ?: return null)
					} catch (th: Throwable) {
						ByteArray(0)
					}
					return newFixedLengthResponse(Response.Status.OK, "application/dns-message", ByteArrayInputStream(rs), (rs ?: return null).size.toLong())
				} else if (fileName.startsWith("/push/")) {
					var url: String? = fileName.substring(6)
					url = if ((url ?: return null).startsWith("b64:")) {
						String(Base64.decode(url.substring(4), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
					} else {
						URLDecoder.decode(url, "UTF-8")
					}
					EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_PUSH_URL, url))
					return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "ok")
				} else if (fileName.startsWith("/proxyM3u8")) {
					return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, m3u8Content)
				} else if (fileName.startsWith("/dash/")) {
					val dashData = App.dashData
					try {
						val data = String(Base64.decode(dashData, Base64.DEFAULT or Base64.NO_WRAP), StandardCharsets.UTF_8)
						return newFixedLengthResponse(
							Response.Status.OK,
							"application/dash+xml",
							data
						)
					} catch (th: Throwable) {
						return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, dashData)
					}
				}
			} else if (session.method == Method.POST) {
				val files: MutableMap<String, String> = mutableMapOf()
				try {
					if (session.headers.containsKey("content-type")) {
						val hd = session.headers["content-type"]
						if (hd != null) {
							// cuke: 修正中文乱码问题
							if (hd.lowercase(Locale.getDefault()).contains("multipart/form-data") && !hd.lowercase(Locale.getDefault()).contains("charset=")) {
								val matcher = RegexUtils.getPattern("[ |\t]*(boundary[ |\t]*=[ |\t]*['|\"]?[^\"^'^;^,]*['|\"]?)", Pattern.CASE_INSENSITIVE).matcher(hd)
								val boundary = if (matcher.find()) matcher.group(1) else null
								if (boundary != null) {
									session.headers["content-type"] = "multipart/form-data; charset=utf-8; $boundary"
								}
							}
						}
					}
					session.parseBody(files)
				} catch (exception: IOException) {
					return createPlainTextResponse(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + exception.message)
				} catch (rex: ResponseException) {
					return createPlainTextResponse(rex.status, rex.message)
				}
				for (process in postRequestList) {
					if (process.isRequest(session, fileName)) {
						return process.doResponse(session, fileName, session.parameters, files)
					}
				}
				try {
					val params = session.parameters
					when (fileName) {
						"/upload" -> {
							val path = params["path"]
							for (k in files.keys) {
								if (k.startsWith("files-")) {
									val fn = params[k]
									val tmpFile = files.getValue(k)
									val tmp = File(tmpFile)
									val root = Environment.getExternalStorageDirectory().absolutePath
									val file = File("$root/$path/$fn")
									if (file.exists()) file.delete()
									if (tmp.exists()) {
										if (fn?.firstOrNull()?.lowercase(Locale.getDefault())?.endsWith(".zip") ?: return null) {
											unzip(tmp, "$root/$path")
										} else {
											FileUtils.copyFile(tmp, file)
										}
									}
									if (tmp.exists()) tmp.delete()
								}
							}
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
						}

						"/newFolder" -> {
							val path = params["path"]
							val name = params["name"]
							val root = Environment.getExternalStorageDirectory().absolutePath
							val file = File("$root/$path/$name")
							if (!file.exists()) {
								file.mkdirs()
								val flag = File("$root/$path/$name/.tvbox_folder")
								if (!flag.exists()) flag.createNewFile()
							}
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
						}

						"/delFolder" -> {
							val path = params["path"]
							val root = Environment.getExternalStorageDirectory().absolutePath
							val file = File("$root/$path")
							if (file.exists()) {
								FileUtils.recursiveDelete(file)
							}
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
						}

						"/delFile" -> {
							val path = params["path"]
							val root = Environment.getExternalStorageDirectory().absolutePath
							val file = File("$root/$path")
							if (file.exists()) {
								file.delete()
							}
							return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
						}
					}
				} catch (th: Throwable) {
					return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
				}
			}
		}
		// default page: index.html
		return getRequestList[0].doResponse(session, "", session.parameters, null)
	}

	val serverAddress: String
		get() {
			val ipAddress: String = getLocalIPAddress(mContext)
			return "http://$ipAddress:$serverPort/"
		}

	val loadAddress: String
		get() = "http://127.0.0.1:$serverPort/"

	fun fileTime(time: Long, fmt: String?): String {
		val calendar = Calendar.getInstance()
		calendar.timeInMillis = time
		val date = calendar.getTime()
		val sdf = SimpleDateFormat(fmt, Locale.getDefault())
		return sdf.format(date)
	}

	fun fileList(root: String, path: String): String {
		val file = File("$root/$path")
		val list = file.listFiles()
		val info = JsonObject()
		info.addProperty("remote", this.serverAddress.replace("http://", "clan://"))
		info.addProperty("del", 0)
		if (path.isEmpty()) {
			info.addProperty("parent", ".")
		} else {
			info.addProperty("parent", (file.parentFile?.absolutePath.orEmpty()).replace("$root/", "").replace(root, ""))
		}
		if (list == null || list.isEmpty()) {
			info.add("files", JsonArray())
			return info.toString()
		}
		list.sortWith(Comparator { o1, o2 ->
			if (o1.isDirectory && o2.isFile) return@Comparator -1
			if (o1.isFile && o2.isDirectory) 1 else o1.name.compareTo(o2.name)
		})
		val result = JsonArray()
		for (f in list) {
			if (f.name.startsWith(".")) {
				if (f.name == ".tvbox_folder") {
					info.addProperty("del", 1)
				}
				continue
			}
			val fileObj = JsonObject()
			fileObj.addProperty("name", f.name)
			fileObj.addProperty("path", f.absolutePath.replace("$root/", ""))
			fileObj.addProperty("time", fileTime(f.lastModified(), "yyyy/MM/dd aHH:mm:ss"))
			fileObj.addProperty("dir", if (f.isDirectory) 1 else 0)
			result.add(fileObj)
		}
		info.add("files", result)
		return info.toString()
	}

	fun unzip(zipFilePath: File?, destDirectory: String) {
		val destDir = File(destDirectory)
		if (!destDir.exists()) {
			destDir.mkdirs()
		}
		val zip = ZipFile(zipFilePath)
		for (entry in zip.entries()) {
			val inputStream = zip.getInputStream(entry)
			val filePath = destDirectory + File.separator + entry.name
			if (!entry.isDirectory) {
				extractFile(inputStream, filePath)
			} else {
				val dir = File(filePath)
				if (!dir.exists()) dir.mkdirs()
				val flag = File("$dir/.tvbox_folder")
				if (!flag.exists()) flag.createNewFile()
			}
		}
	}

	fun extractFile(inputStream: InputStream, destFilePath: String) {
		val dst = File(destFilePath)
		if (dst.exists()) dst.delete()
		val bos = BufferedOutputStream(Files.newOutputStream(Paths.get(destFilePath)))
		val bytesIn = ByteArray(2048)
		var len = inputStream.read(bytesIn)
		while (len > 0) {
			bos.write(bytesIn, 0, len)
			len = inputStream.read(bytesIn)
		}
		bos.close()
	}

	companion object {
		var serverPort: Int = 9978
		var m3u8Content: String? = null
		fun createPlainTextResponse(status: IStatus?, text: String?): Response {
			return newFixedLengthResponse(status, MIME_PLAINTEXT, text)
		}

		fun createJSONResponse(status: IStatus?, text: String?): Response {
			return newFixedLengthResponse(status, "application/json", text)
		}

		@SuppressLint("DefaultLocale")
		@Suppress("DEPRECATION")
		fun getLocalIPAddress(context: Context): String {
			val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
			val ipAddress = wifiManager.connectionInfo.ipAddress
			if (ipAddress == 0) {
				try {
					val enumerationNi = NetworkInterface.getNetworkInterfaces()
					while (enumerationNi.hasMoreElements()) {
						val networkInterface = enumerationNi.nextElement()
						val interfaceName = networkInterface.displayName
						if (interfaceName == "eth0" || interfaceName == "wlan0") {
							val enumIpAddr = networkInterface.inetAddresses
							while (enumIpAddr.hasMoreElements()) {
								val inetAddress = enumIpAddr.nextElement()
								if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
									return inetAddress.hostAddress ?: "0.0.0.0"
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
			return "0.0.0.0"
		}
	}
}
