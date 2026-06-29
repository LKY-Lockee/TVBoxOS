package com.github.tvbox.osc.subtitle

import android.util.Log
import androidx.core.net.toUri
import com.github.tvbox.osc.subtitle.format.FormatASS
import com.github.tvbox.osc.subtitle.format.FormatSRT
import com.github.tvbox.osc.subtitle.format.FormatSTL
import com.github.tvbox.osc.subtitle.model.TimedTextObject
import com.github.tvbox.osc.subtitle.runtime.AppTaskExecutor.Companion.deskIO
import com.github.tvbox.osc.subtitle.runtime.AppTaskExecutor.Companion.mainThread
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.UnicodeReader
import com.lzy.okgo.OkGo
import org.apache.commons.io.input.ReaderInputStream
import org.mozilla.universalchardet.UniversalDetector
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * @author AveryZhong.
 */
object SubtitleLoader {
	private val TAG: String = SubtitleLoader::class.java.simpleName

	fun loadSubtitle(path: String?, callback: Callback?) {
		if (path.isNullOrEmpty()) {
			return
		}
		if (path.startsWith("http://") || path.startsWith("https://")) {
			loadFromRemoteAsync(path, callback)
		} else {
			loadFromLocalAsync(path, callback)
		}
	}

	private fun loadFromRemoteAsync(remoteSubtitlePath: String, callback: Callback?) {
		deskIO().execute {
			try {
				val subtitleLoadSuccessResult = loadFromRemote(remoteSubtitlePath)
				callback?.let { cb ->
					mainThread().execute { cb.onSuccess(subtitleLoadSuccessResult) }
				}
			} catch (e: Exception) {
				e.printStackTrace()
				callback?.let { cb ->
					mainThread().execute { cb.onError(e) }
				}
			}
		}
	}

	private fun loadFromLocalAsync(localSubtitlePath: String, callback: Callback?) {
		deskIO().execute {
			try {
				val subtitleLoadSuccessResult = loadFromLocal(localSubtitlePath)
				callback?.let { cb ->
					mainThread().execute { cb.onSuccess(subtitleLoadSuccessResult) }
				}
			} catch (e: Exception) {
				e.printStackTrace()
				callback?.let { cb ->
					mainThread().execute { cb.onError(e) }
				}
			}
		}
	}

	private fun loadFromRemote(remoteSubtitlePath: String): SubtitleLoadSuccessResult {
		Log.d(TAG, "parseRemote: remoteSubtitlePath = $remoteSubtitlePath")
		var referer = ""
		if (remoteSubtitlePath.contains("alicloud") || remoteSubtitlePath.contains("aliyundrive")) {
			referer = "https://www.aliyundrive.com/"
		} else if (remoteSubtitlePath.contains("assrt.net")) {
			referer = "https://secure.assrt.net/"
		}
		val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.54 Safari/537.36"
		val response = OkGo.get<String>(remoteSubtitlePath.split("#")[0])
			.headers("Referer", referer)
			.headers("User-Agent", ua)
			.execute()
		val bytes = response.body.bytes()
		val detector = UniversalDetector(null)
		detector.handleData(bytes, 0, bytes.size)
		detector.dataEnd()
		var encoding = detector.detectedCharset
		if (encoding.isEmpty()) encoding = "UTF-8"
		val content = String(bytes, charset(encoding))
		val inputStream: InputStream = ByteArrayInputStream(content.toByteArray())
		var filename = ""
		val contentDisposition = response.header("content-disposition", "")
		val cd = contentDisposition?.split(";")
		cd?.size?.let {
			if (it > 1) {
				var filenameInfo = cd[1]
				filenameInfo = filenameInfo.trim()
				if (filenameInfo.startsWith("filename=")) {
					filename = filenameInfo.replace("filename=", "")
					filename = filename.replace("\"", "")
				} else if (filenameInfo.startsWith("filename*=")) {
					filename = filenameInfo.substring(filenameInfo.lastIndexOf("''") + 2)
				}
				filename = filename.trim()
				filename = URLDecoder.decode(filename)
			}
		}
		var filePath = filename
		if (filename.isEmpty()) {
			val uri = remoteSubtitlePath.toUri()
			filePath = uri.path.orEmpty()
		}
		if (!filePath.contains(".") && remoteSubtitlePath.contains("#")) {
			filePath = remoteSubtitlePath.split("#")[1]
			filePath = URLDecoder.decode(filePath)
		}
		val subtitleLoadSuccessResult = SubtitleLoadSuccessResult()
		subtitleLoadSuccessResult.timedTextObject = loadAndParse(inputStream, filePath)
		subtitleLoadSuccessResult.fileName = filePath
		subtitleLoadSuccessResult.content = content
		subtitleLoadSuccessResult.subtitlePath = remoteSubtitlePath
		return subtitleLoadSuccessResult
	}

	private fun loadFromLocal(localSubtitlePath: String): SubtitleLoadSuccessResult? {
		Log.d(TAG, "parseLocal: localSubtitlePath = $localSubtitlePath")
		val file = File(localSubtitlePath)
		if (!file.exists()) {
			Log.d(TAG, "parseLocal: localSubtitlePath = $localSubtitlePath file not exists")
			return null
		}
		val bytes = FileUtils.readSimple(file) ?: return null
		val detector = UniversalDetector(null)
		detector.handleData(bytes, 0, bytes.size)
		detector.dataEnd()
		val encoding = detector.detectedCharset
		val content = String(bytes, charset(encoding))
		val inputStream: InputStream = ByteArrayInputStream(content.toByteArray())
		val filePath = file.path
		val subtitleLoadSuccessResult = SubtitleLoadSuccessResult()
		subtitleLoadSuccessResult.timedTextObject = loadAndParse(inputStream, filePath)
		subtitleLoadSuccessResult.fileName = filePath.substring(filePath.lastIndexOf("/") + 1)
		subtitleLoadSuccessResult.subtitlePath = localSubtitlePath
		return subtitleLoadSuccessResult
	}

	private fun loadAndParse(inputStream: InputStream?, filePath: String): TimedTextObject? {
		val nonNullIs = inputStream ?: return null
		val fileName = filePath.substring(filePath.lastIndexOf("/") + 1)
		var ext = ""
		if (fileName.lastIndexOf(".") > 0) {
			ext = fileName.substring(fileName.lastIndexOf("."))
		}
		Log.d(TAG, "parse: name = $fileName, ext = $ext")
		val reader: Reader = UnicodeReader(nonNullIs)
		val newInputStream: InputStream = ReaderInputStream(reader, Charset.defaultCharset())
		when {
			ext.equals(".srt", ignoreCase = true) -> return FormatSRT().parseFile(fileName, newInputStream)
			ext.equals(".ass", ignoreCase = true) -> return FormatASS().parseFile(fileName, newInputStream)
			ext.equals(".stl", ignoreCase = true) -> return FormatSTL().parseFile(fileName, newInputStream)
			ext.equals(".ttml", ignoreCase = true) -> return FormatSTL().parseFile(fileName, newInputStream)
		}
		val arr = arrayOf(FormatSRT(), FormatASS(), FormatSTL(), FormatSTL())
		for (oneFormat in arr) {
			try {
				return oneFormat.parseFile(fileName, newInputStream)
			} catch (ignored: Exception) {
			}
		}
		return null
	}

	interface Callback {
		fun onSuccess(subtitleLoadSuccessResult: SubtitleLoadSuccessResult?)

		fun onError(exception: Exception)
	}
}
