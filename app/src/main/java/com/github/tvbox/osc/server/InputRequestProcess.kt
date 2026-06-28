package com.github.tvbox.osc.server

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession

/**
 * 响应按键和输入
 *
 * @author pj567
 * @date 2021/1/5
 */
class InputRequestProcess(private val remoteServer: RemoteServer) : RequestProcess {
	override fun isRequest(session: IHTTPSession, fileName: String): Boolean {
		return session.method == NanoHTTPD.Method.POST && fileName == "/action"
	}

	override fun doResponse(session: IHTTPSession, fileName: String, params: Map<String, List<String>>, files: Map<String, String>?): NanoHTTPD.Response {
		remoteServer.dataReceiver?.let {
			if (fileName == "/action") {
				when (val action = params["do"]?.firstOrNull()) {
					"search" -> {
						val word = params["word"]?.firstOrNull()?.trim().orEmpty()
						it.onTextReceived(word)
					}

					"api" -> {
						val url = params["url"]?.firstOrNull()?.trim().orEmpty()
						it.onApiReceived(url)
					}

					"push" -> {
						val url = params["url"]?.firstOrNull()?.trim().orEmpty()
						it.onPushReceived(url)
					}
				}
				return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "ok")
			}
		}
		return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.NOT_FOUND, "Error 404, file not found.")
	}
}
