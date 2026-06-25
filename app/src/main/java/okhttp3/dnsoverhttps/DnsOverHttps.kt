/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package okhttp3.dnsoverhttps

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.platform.*
import okhttp3.internal.publicsuffix.PublicSuffixDatabase.Companion.get
import java.io.*
import java.net.*
import java.util.concurrent.*

/**
 * DNS over HTTPS implementation.
 *
 * Implementation of [draft-ietf-doh-dns-over-https-13](https://tools.ietf.org/html/draft-ietf-doh-dns-over-https-13)
 *
 * > A DNS API client encodes a single DNS query into an HTTP request
 * using either the HTTP GET or POST method and the other requirements
 * of this section.  The DNS API server defines the URI used by the
 * request through the use of a URI Template.</blockquote>
 *
 * ### Warning: This is a non-final API.
 *
 * **As of OkHttp 3.11, this feature is an unstable preview: the API is subject to change,
 * and the implementation is incomplete. We expect that OkHttp 3.12 or 3.13 will finalize this API.
 * Until then, expect API and behavior changes when you update your OkHttp dependency.**
 */
class DnsOverHttps internal constructor(builder: Builder) : Dns {
	private val client: OkHttpClient = builder.client.newBuilder().dns(buildBootstrapClient(builder)).build()
	private val includeIPv6: Boolean = builder.includeIPv6
	private val post: Boolean = builder.post
	private val resolvePrivateAddresses: Boolean = builder.resolvePrivateAddresses
	private val resolvePublicAddresses: Boolean = builder.resolvePublicAddresses
	private val url: HttpUrl? = builder.url

	fun lookupHttpsForwardSync(hostname: String): ByteArray? {
		val byteArrayOutputStream = ByteArrayOutputStream()
		byteArrayOutputStream.write(executeRequestsSync(hostname, DnsRecordCodec.TYPE_A))
		byteArrayOutputStream.write(executeRequestsSync(hostname, DnsRecordCodec.TYPE_AAAA))
		return byteArrayOutputStream.toByteArray()
	}

	override fun lookup(hostname: String): List<InetAddress> {
		if (this.url == null) return Dns.SYSTEM.lookup(hostname)
		if (!resolvePrivateAddresses || !resolvePublicAddresses) {
			val privateHost: Boolean = isPrivateHost(hostname)

			if (privateHost && !resolvePrivateAddresses) {
				throw UnknownHostException("private hosts not resolved")
			}

			if (!privateHost && !resolvePublicAddresses) {
				throw UnknownHostException("public hosts not resolved")
			}
		}
		return lookupHttps(hostname)
	}

	private fun lookupHttps(hostname: String): List<InetAddress> {
		val networkRequests = mutableListOf<Call>()
		val failures = mutableListOf<Exception>()
		val results = mutableListOf<InetAddress>()

		buildRequest(hostname, networkRequests, results, failures, DnsRecordCodec.TYPE_A)

		if (includeIPv6) {
			buildRequest(hostname, networkRequests, results, failures, DnsRecordCodec.TYPE_AAAA)
		}

		executeRequests(hostname, networkRequests, results, failures)

		if (results.isNotEmpty()) {
			return results
		}

		return Dns.SYSTEM.lookup(hostname)
	}

	private fun buildRequest(
		hostname: String,
		networkRequests: MutableList<Call>,
		results: MutableList<InetAddress>,
		failures: MutableList<Exception>, type: Int
	) {
		val request = buildRequest(hostname, type)
		val response = getCacheOnlyResponse(request)

		if (response != null) {
			processResponse(response, hostname, results, failures)
		} else {
			networkRequests.add(client.newCall(request))
		}
	}

	private fun executeRequestsSync(hostname: String, type: Int): ByteArray {
		val request = buildRequest(hostname, type)
		var response = getCacheOnlyResponse(request)

		if (response == null) {
			response = client.newCall(request).execute()
		}
		return response.body.bytes()
	}

	private fun executeRequests(
		hostname: String,
		networkRequests: MutableList<Call>,
		responses: MutableList<InetAddress>,
		failures: MutableList<Exception>
	) {
		val latch = CountDownLatch(networkRequests.size)

		for (call in networkRequests) {
			call.enqueue(object : Callback {
				override fun onFailure(call: Call, e: okio.IOException) {
					synchronized(failures) {
						failures.add(e)
					}
					latch.countDown()
				}

				override fun onResponse(call: Call, response: Response) {
					processResponse(response, hostname, responses, failures)
					latch.countDown()
				}
			})
		}

		try {
			latch.await()
		} catch (e: InterruptedException) {
			failures.add(e)
		}
	}

	private fun processResponse(
		response: Response,
		hostname: String,
		results: MutableList<InetAddress>,
		failures: MutableList<Exception>
	) {
		try {
			val addresses = readResponse(hostname, response)
			synchronized(results) {
				results.addAll(addresses)
			}
		} catch (e: Exception) {
			synchronized(failures) {
				failures.add(e)
			}
		}
	}

	private fun getCacheOnlyResponse(request: Request): Response? {
		if (!post && client.cache != null) {
			try {
				val cacheRequest = request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()
				val cacheResponse = client.newCall(cacheRequest).execute()

				if (cacheResponse.code != 504) {
					return cacheResponse
				}
			} catch (_: IOException) {
				// Failures are ignored as we can fall back to the network
				// and hopefully repopulate the cache.
			}
		}

		return null
	}

	private fun readResponse(hostname: String, response: Response): List<InetAddress> {
		if (response.cacheResponse == null && response.protocol != Protocol.HTTP_2) {
			Platform.get().log("Incorrect protocol: ${response.protocol}", Platform.WARN, null)
		}

		response.use { resp ->
			if (!resp.isSuccessful) {
				throw IOException("response: ${resp.code} ${resp.message}")
			}

			val body = resp.body

			if (body.contentLength() > MAX_RESPONSE_SIZE) {
				throw IOException("response size exceeds limit ($MAX_RESPONSE_SIZE bytes): ${body.contentLength()} bytes")
			}

			val responseBytes = body.source().readByteString()

			return DnsRecordCodec.decodeAnswers(hostname, responseBytes)
		}
	}

	private fun buildRequest(hostname: String, type: Int): Request {
		var requestBuilder = Request.Builder().header("Accept", DNS_MESSAGE.toString())
		val query = DnsRecordCodec.encodeQuery(hostname, type)

		val reqUrl = url
		if (reqUrl != null) {
			if (post) {
				requestBuilder = requestBuilder.url(reqUrl).post(query.toRequestBody(DNS_MESSAGE))
			} else {
				val encoded = query.base64Url().replace("=", "")
				val requestUrl = reqUrl.newBuilder().addQueryParameter("dns", encoded).build()
				requestBuilder = requestBuilder.url(requestUrl)
			}
		}

		return requestBuilder.build()
	}

	class Builder(var client: OkHttpClient) {
		var url: HttpUrl? = null
		var includeIPv6: Boolean = true
		var post: Boolean = false
		var systemDns: Dns = Dns.SYSTEM
		var bootstrapDnsHosts: List<InetAddress>? = null
		var resolvePrivateAddresses: Boolean = false
		var resolvePublicAddresses: Boolean = true

		fun url(url: HttpUrl?): Builder {
			this.url = url
			return this
		}

		fun post(post: Boolean): Builder {
			this.post = post
			return this
		}

		fun bootstrapDnsHosts(bootstrapDnsHosts: List<InetAddress>?): Builder {
			this.bootstrapDnsHosts = bootstrapDnsHosts
			return this
		}

		fun build(): DnsOverHttps {
			return DnsOverHttps(this)
		}
	}

	companion object {
		val DNS_MESSAGE: MediaType = "application/dns-message".toMediaType()
		const val MAX_RESPONSE_SIZE: Int = 64 * 1024

		fun isPrivateHost(host: String): Boolean {
			return get().getEffectiveTldPlusOne(host) == null
		}

		private fun buildBootstrapClient(builder: Builder): Dns {
			val hosts = builder.bootstrapDnsHosts ?: return builder.systemDns
			return BootstrapDns(builder.url?.host ?: "", hosts)
		}
	}
}
