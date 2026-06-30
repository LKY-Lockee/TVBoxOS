package com.github.tvbox.osc.util

import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackInputStream
import java.io.Reader
import java.nio.file.Files

open class UnicodeReader(input: InputStream?, defaultEncoding: String? = null) : Reader() {
	private var internalIn: InputStreamReader? = null
	var encoding: String? = null
		private set

	constructor(file: String) : this(File(file))

	constructor(file: File) : this(Files.newInputStream(file.toPath()))

	constructor(file: File, defaultEncoding: String?) : this(Files.newInputStream(file.toPath()), defaultEncoding)

	init {
		init(input, defaultEncoding)
	}

	override fun close() {
		internalIn?.close()
	}

	private fun init(input: InputStream?, defaultEncoding: String?) {
		val tempIn = PushbackInputStream(input, 4)

		val bom = ByteArray(4)

		val n = tempIn.read(bom, 0, bom.size)
		val unread: Int
		when {
			bom[0].toInt() == 0 && bom[1].toInt() == 0 &&
					bom[2].toInt() == -2 && bom[3].toInt() == -1 -> {
				encoding = "UTF-32BE"
				unread = n - 4
			}

			bom[0].toInt() == -17 && bom[1].toInt() == -69 &&
					bom[2].toInt() == -65 -> {
				encoding = "UTF-8"
				unread = n - 3
			}

			bom[0].toInt() == -2 && bom[1].toInt() == -1 -> {
				encoding = "UTF-16BE"
				unread = n - 2
			}

			bom[0].toInt() == -1 && bom[1].toInt() == -2 -> {
				encoding = "UTF-16LE"
				unread = n - 2
			}

			else -> {
				encoding = defaultEncoding
				unread = n
			}
		}
		if (unread > 0) tempIn.unread(bom, n - unread, unread)
		else if (unread < -1) {
			tempIn.unread(bom, 0, 0)
		}

		internalIn = if (encoding == null) {
			val reader = InputStreamReader(tempIn)
			encoding = reader.encoding
			reader
		} else {
			InputStreamReader(tempIn, encoding)
		}
	}

	override fun read(cbuf: CharArray, off: Int, len: Int): Int {
		return internalIn?.read(cbuf, off, len) ?: -1
	}

	companion object {
		private const val BOM_SIZE = 4
	}
}
