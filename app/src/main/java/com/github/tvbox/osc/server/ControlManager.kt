package com.github.tvbox.osc.server

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.receiver.SearchReceiver
import org.greenrobot.eventbus.EventBus
import java.io.IOException

/**
 * @author pj567
 * @date 2021/1/4
 */
class ControlManager private constructor() {
	private lateinit var mContext: Context
	private var mServer: RemoteServer? = null

	fun getAddress(local: Boolean): String? {
		val server = mServer ?: return null
		return if (local) server.loadAddress else server.serverAddress
	}

	fun startServer() {
		if (mServer != null) {
			return
		}
		do {
			mServer = RemoteServer(RemoteServer.serverPort, mContext)
			val server = mServer ?: return
			server.dataReceiver = object : DataReceiver {
				override fun onTextReceived(text: String?) {
					if (text.isNullOrEmpty()) return
					val intent = Intent()
					val bundle = Bundle()
					bundle.putString("title", text)
					intent.action = SearchReceiver.ACTION
					intent.setPackage(mContext.packageName)
					intent.component = ComponentName(mContext, SearchReceiver::class.java)
					intent.putExtras(bundle)
					mContext.sendBroadcast(intent)
				}

				override fun onApiReceived(url: String?) {
					EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_API_URL_CHANGE, url))
				}

				override fun onPushReceived(url: String?) {
					EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_PUSH_URL, url))
				}
			}
			try {
				server.start()
				break
			} catch (ex: IOException) {
				RemoteServer.serverPort++
				server.stop()
			}
		} while (RemoteServer.serverPort < 9999)
	}

	fun stopServer() {
		val server = mServer ?: return
		if (server.isStarting) {
			server.stop()
		}
	}

	companion object {
		val instance: ControlManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
			ControlManager()
		}

		fun init(context: Context) {
			instance.mContext = context
		}
	}
}
