package com.github.tvbox.osc.subtitle.model

class Style {
	var iD: String
	var font: String? = null
	var fontSize: String? = null

	/**
	 * colors are stored as 8 chars long RGBA
	 */
	var color: String? = null
	var backgroundColor: String? = null
	var textAlign: String? = null
	var italic: Boolean = false
	var bold: Boolean = false
	var underline: Boolean = false

	constructor(styleName: String) {
		this.iD = styleName
	}

	constructor(styleName: String, style: Style) {
		this.iD = styleName
		this.font = style.font
		this.fontSize = style.fontSize
		this.color = style.color
		this.backgroundColor = style.backgroundColor
		this.textAlign = style.textAlign
		this.italic = style.italic
		this.underline = style.underline
		this.bold = style.bold
	}

	companion object {
		private var styleCounter = 0

		fun getRGBValue(format: String, value: String): String? {
			return when {
				format.equals("name", ignoreCase = true) -> {
					when (value) {
						"transparent" -> "00000000"
						"black" -> "000000ff"
						"silver" -> "c0c0c0ff"
						"gray" -> "808080ff"
						"white" -> "ffffffff"
						"maroon" -> "800000ff"
						"red" -> "ff0000ff"
						"purple" -> "800080ff"
						"fuchsia", "magenta" -> "ff00ffff"
						"green" -> "008000ff"
						"lime" -> "00ff00ff"
						"olive" -> "808000ff"
						"yellow" -> "ffff00ff"
						"navy" -> "000080ff"
						"blue" -> "0000ffff"
						"teal" -> "008080ff"
						"aqua", "cyan" -> "00ffffff"
						else -> null
					}
				}

				format.equals("&HBBGGRR", ignoreCase = true) -> {
					value.substring(6) +
							value[4] +
							value[2] +
							"ff"
				}

				format.equals("&HAABBGGRR", ignoreCase = true) -> {
					value.substring(8) +
							value[6] +
							value[4] +
							value[2]
				}

				format.equals("decimalCodedBBGGRR", ignoreCase = true) -> {
					val colorBuilder = StringBuilder(Integer.toHexString(value.toInt()))
					while (colorBuilder.length < 6) colorBuilder.insert(0, "0")
					var color = colorBuilder.toString()
					color = color.substring(4) + color.substring(2, 4) + color.substring(0, 2) + "ff"
					color
				}

				format.equals("decimalCodedAABBGGRR", ignoreCase = true) -> {
					val colorBuilder = StringBuilder(value.toLong().toHexString())
					while (colorBuilder.length < 8) colorBuilder.insert(0, "0")
					var color = colorBuilder.toString()
					color = color.substring(6) + color.substring(4, 6) + color.substring(2, 4) + color.substring(0, 2)
					color
				}

				else -> null
			}
		}

		fun defaultID(): String {
			return "default" + styleCounter++
		}
	}
}
