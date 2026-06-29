package com.github.tvbox.osc.util.ssl

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.GeneralSecurityException
import java.util.LinkedList
import java.util.Locale
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * 描述
 *
 * @author pj567
 * @since 2021/1/10
 */
class SSLSocketFactoryCompat(tm: X509TrustManager?) : SSLSocketFactory() {
	private val defaultFactory: SSLSocketFactory

	init {
		try {
			val sslContext = SSLContext.getInstance("TLS")
			sslContext.init(null, if (tm != null) arrayOf(tm) else null, null)
			defaultFactory = sslContext.socketFactory
		} catch (e: GeneralSecurityException) {
			throw AssertionError() // The system has no TLS. Just give up.
		}
	}

	private fun upgradeTLS(ssl: SSLSocket) {
		// Android 5.0+ (API level21) provides reasonable default settings, but it still allows SSLv3
		// https://developer.android.com/about/versions/android-5.0-changes.html#ssl
		if (protocols != null) {
			ssl.enabledProtocols = protocols
		}
	}

	override fun getDefaultCipherSuites(): Array<String> {
		return cipherSuites ?: arrayOf()
	}

	override fun getSupportedCipherSuites(): Array<String> {
		return cipherSuites ?: arrayOf()
	}

	override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
		val ssl = defaultFactory.createSocket(s, host, port, autoClose)
		if (ssl is SSLSocket) upgradeTLS(ssl)
		return ssl
	}

	override fun createSocket(host: String?, port: Int): Socket {
		val ssl = defaultFactory.createSocket(host, port)
		if (ssl is SSLSocket) upgradeTLS(ssl)
		return ssl
	}

	override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
		val ssl = defaultFactory.createSocket(host, port, localHost, localPort)
		if (ssl is SSLSocket) upgradeTLS(ssl)
		return ssl
	}

	override fun createSocket(host: InetAddress?, port: Int): Socket {
		val ssl = defaultFactory.createSocket(host, port)
		if (ssl is SSLSocket) upgradeTLS(ssl)
		return ssl
	}

	override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
		val ssl = defaultFactory.createSocket(address, port, localAddress, localPort)
		if (ssl is SSLSocket) upgradeTLS(ssl)
		return ssl
	}

	companion object {
		val cipherSuites: Array<String>? = null

		// Android 5.0+ (API level21) provides reasonable default settings, but it still allows SSLv3
		// https://developer.android.com/about/versions/android-5.0-changes.html#ssl
		var protocols: Array<String>? = null

		init {
			try {
				val socket = getDefault().createSocket() as? SSLSocket
				if (socket != null) {
					/* set reasonable protocol versions */
					// - enable all supported protocols (enables TLSv1.1 and TLSv1.2 on Android <5.0)
					// - remove all SSL versions (especially SSLv3) because they're insecure now
					val protocolsList: MutableList<String> = LinkedList()
					for (protocol in socket.supportedProtocols) if (!protocol.uppercase(Locale.getDefault()).contains("SSL")) protocolsList.add(protocol)
					protocols = protocolsList.toTypedArray()
					/* set up reasonable cipher suites */
				}
			} catch (e: IOException) {
				throw RuntimeException(e)
			}
		}
	}
}
