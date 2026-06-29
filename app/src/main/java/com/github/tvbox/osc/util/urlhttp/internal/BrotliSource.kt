package com.github.tvbox.osc.util.urlhttp.internal

import okio.BufferedSource
import okio.Source
import okio.source
import org.brotli.dec.BrotliInputStream

object BrotliSource {
	fun create(source: BufferedSource): Source {
		val brotliInputStream = BrotliInputStream(source.inputStream())
		return brotliInputStream.source()
	}
}
