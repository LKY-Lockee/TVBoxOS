/**
 * Class that represents the .ASS and .SSA subtitle file format
 * 
 * Copyright (c) 2012 J. David Requejo
 * j.david.requejo@Gmail
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 * @author J. David REQUEJO
 */

package com.github.tvbox.osc.subtitle.format

import com.github.tvbox.osc.subtitle.exception.FatalParsingException
import com.github.tvbox.osc.subtitle.model.Style
import com.github.tvbox.osc.subtitle.model.Subtitle
import com.github.tvbox.osc.subtitle.model.Time
import com.github.tvbox.osc.subtitle.model.TimedTextObject
import org.w3c.dom.Document
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class FormatTTML : TimedTextFileFormat {
	override fun parseFile(fileName: String, `is`: InputStream): TimedTextObject {
		val tto = TimedTextObject()
		tto.fileName = fileName

		val dbFactory = DocumentBuilderFactory.newInstance()
		try {
			val dBuilder = dbFactory.newDocumentBuilder()
			val doc = dBuilder.parse(`is`)
			doc.documentElement.normalize()

			// we recover the metadata
			var node = doc.getElementsByTagName("ttm:title").item(0)
			if (node != null) tto.title = node.textContent
			node = doc.getElementsByTagName("ttm:copyright").item(0)
			if (node != null) tto.copyright = node.textContent
			node = doc.getElementsByTagName("ttm:desc").item(0)
			if (node != null) tto.description = node.textContent

			// we recover the styles
			val styleN = doc.getElementsByTagName("style")
			// we recover the timed text elements
			val captionsN = doc.getElementsByTagName("p")

			// regions of the layout could also be recovered this way
			tto.warnings += "Styling attributes are only recognized inside a style definition, to be referenced later in the captions.\n\n"
			// we parse the styles
			for (i in 0..<styleN.length) {
				var style = Style(Style.defaultID())
				node = styleN.item(i)
				val attr = node.attributes
				// we get the id
				var currentAtr = attr.getNamedItem("id")
				if (currentAtr != null) style.iD = currentAtr.nodeValue
				currentAtr = attr.getNamedItem("xml:id")
				if (currentAtr != null) style.iD = currentAtr.nodeValue

				// we get the style it may be based upon
				currentAtr = attr.getNamedItem("style")
				if (currentAtr != null) {
					val baseStyle = tto.styling[currentAtr.nodeValue]
					if (baseStyle != null) style = Style(style.iD, baseStyle)
				}

				// we check for background color
				currentAtr = attr.getNamedItem("tts:backgroundColor")
				if (currentAtr != null) style.backgroundColor = parseColor(currentAtr.nodeValue, tto)

				// we check for color
				currentAtr = attr.getNamedItem("tts:color")
				if (currentAtr != null) style.color = parseColor(currentAtr.nodeValue, tto)

				// we check for font family
				currentAtr = attr.getNamedItem("tts:fontFamily")
				if (currentAtr != null) style.font = currentAtr.nodeValue

				// we check for font size
				currentAtr = attr.getNamedItem("tts:fontSize")
				if (currentAtr != null) style.fontSize = currentAtr.nodeValue

				// we check for italics
				currentAtr = attr.getNamedItem("tts:fontStyle")
				if (currentAtr != null) {
					if (currentAtr.nodeValue.equals("italic", ignoreCase = true) || currentAtr.nodeValue.equals("oblique", ignoreCase = true))
						style.italic = true
					else if (currentAtr.nodeValue.equals("normal", ignoreCase = true))
						style.italic = false
				}

				// we check for bold
				currentAtr = attr.getNamedItem("tts:fontWeight")
				if (currentAtr != null) {
					if (currentAtr.nodeValue.equals("bold", ignoreCase = true))
						style.bold = true
					else if (currentAtr.nodeValue.equals("normal", ignoreCase = true))
						style.bold = false
				}

				// we check opacity (to set the alpha)
				currentAtr = attr.getNamedItem("tts:opacity")
				if (currentAtr != null) {
					try {
						// a number between 1.0 and 0
						var alpha = currentAtr.nodeValue.toFloat()
						if (alpha > 1) alpha = 1f
						else if (alpha < 0) alpha = 0f

						var aa = Integer.toHexString((alpha * 255).toInt())
						if (aa.length < 2) aa = "0$aa"

						style.color = style.color?.substring(0, 6) + aa
						style.backgroundColor = style.backgroundColor?.substring(0, 6) + aa
					} catch (e: NumberFormatException) {
						// ignore the alpha
					}
				}

				// we check for text align
				currentAtr = attr.getNamedItem("tts:textAlign")
				if (currentAtr != null) {
					if (currentAtr.nodeValue.equals("left", ignoreCase = true) || currentAtr.nodeValue.equals("start", ignoreCase = true))
						style.textAlign = "bottom-left"
					else if (currentAtr.nodeValue.equals("right", ignoreCase = true) || currentAtr.nodeValue.equals("end", ignoreCase = true))
						style.textAlign = "bottom-right"
				}

				// we check for underline
				currentAtr = attr.getNamedItem("tts:textDecoration")
				if (currentAtr != null) {
					if (currentAtr.nodeValue.equals("underline", ignoreCase = true))
						style.underline = true
					else if (currentAtr.nodeValue.equals("noUnderline", ignoreCase = true))
						style.underline = false
				}

				// we add the style
				tto.styling[style.iD] = style
			}

			// we parse the captions
			for (i in 0..<captionsN.length) {
				val caption = Subtitle()
				caption.content = ""
				var validCaption = true
				node = captionsN.item(i)

				val attr = node.attributes
				// we get the beginning time
				var currentAtr = attr.getNamedItem("begin")
				// if no begin is present, 0 is assumed
				caption.start = Time("", "")
				caption.end = Time("", "")
				if (currentAtr != null) caption.start?.mSeconds = parseTimeExpression(currentAtr.nodeValue, tto, doc)

				// we get the end time, if present, duration is ignored, otherwise end is calculated from duration
				currentAtr = attr.getNamedItem("end")
				if (currentAtr != null) {
					caption.end?.mSeconds = parseTimeExpression(currentAtr.nodeValue, tto, doc)
				} else {
					currentAtr = attr.getNamedItem("dur")
					if (currentAtr != null) {
						caption.start?.let { start ->
							caption.end?.mSeconds = start.mSeconds + parseTimeExpression(currentAtr.nodeValue, tto, doc)
						}
					} else {
						// no end or duration, invalid format, caption is discarded
						validCaption = false
					}
				}

				// we get the style
				currentAtr = attr.getNamedItem("style")
				if (currentAtr != null) {
					val style = tto.styling[currentAtr.nodeValue]
					if (style != null) {
						caption.style = style
					} else { // unrecognized style
						tto.warnings += "unrecognized style referenced: " + currentAtr.nodeValue + "\n\n"
					}
				}

				// we save the text
				val textN = node.childNodes
				for (j in 0..<textN.length) {
					if (textN.item(j).nodeName == "#text")
						caption.content += textN.item(j).textContent.trim()
					else if (textN.item(j).nodeName == "br")
						caption.content += "<br />"
				}
				if (caption.content.replace("<br />", "").trim().isEmpty()) validCaption = false

				// and save the caption
				if (validCaption) {
					var key = caption.start?.mSeconds ?: 0
					// in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
					while (tto.captions.containsKey(key)) key++
					tto.captions[key] = caption
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			// this could be a fatal error...
			throw FatalParsingException("Error during parsing: " + e.message)
		}

		tto.built = true
		return tto
	}

	override fun toFile(tto: TimedTextObject): Any? {
		// first we check if the TimedTextObject had been built, otherwise...
		if (!tto.built) return null

		// we will write the lines in an ArrayList 
		var index = 0
		// the minimum size of the file is the number of captions and styles + lines for sections and formats and the metadata, so we'll take some extra space.
		val file = ArrayList<String>(30 + tto.styling.size + tto.captions.size)

		// identification line is placed
		file.add(index++, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
		// root element is placed
		file.add(index++, "<tt xml:lang=\"" + tto.language + "\" xmlns=\"http://www.w3.org/ns/ttml\" xmlns:tts=\"http://www.w3.org/ns/ttml#styling\">")
		// head
		file.add(index++, "\t<head>")
		// metadata
		file.add(index++, "\t\t<metadata xmlns:ttm=\"http://www.w3.org/ns/ttml#metadata\">")
		// title
		val title: String = tto.title.ifEmpty { tto.fileName }
		file.add(index++, "\t\t\t<ttm:title>$title</ttm:title>")
		// Copyright
		if (tto.copyright.isNotEmpty()) file.add(index++, "\t\t\t<ttm:copyright>" + tto.copyright + "</ttm:copyright>")
		// additional info
		var desc = "Converted by the Online Subtitle Converter developed by J. David Requejo\n"
		if (tto.author.isNotEmpty()) desc += "\n Original file by: " + tto.author + "\n"
		if (tto.description.isNotEmpty()) desc += tto.description + "\n"
		file.add(index++, "\t\t\t<ttm:desc>$desc\t\t\t</ttm:desc>")

		// metadata closes
		file.add(index++, "\t\t</metadata>")
		// styling opens
		file.add(index++, "\t\t<styling>")

		// Next we iterate over the styles
		for (style in tto.styling.values) {
			if (style == null) continue
			// we add the attributes
			var line = "\t\t\t<style xml:id=\"" + style.iD + "\""
			if (style.color != null) line += " tts:color=\"#" + style.color + "\""
			if (style.backgroundColor != null) line += " tts:backgroundColor=\"#" + style.backgroundColor + "\""
			if (style.font != null) line += " tts:fontFamily=\"" + style.font + "\""
			if (style.fontSize != null) line += " tts:fontSize=\"" + style.fontSize + "\""
			if (style.italic) line += " tts:fontStyle=\"italic\""
			if (style.bold) line += " tts:fontWeight=\"bold\""
			line += " tts:textAlign=\""
			line += when {
				style.textAlign?.contains("left") == true -> "left\""
				style.textAlign?.contains("right") == true -> "right\""
				else -> "center\""
			}
			if (style.underline) line += " tts:textDecoration=\"underline\""
			// style is ready, we close it.
			line += " />"
			// we insert it
			file.add(index++, line)
		}

		// styling closes
		file.add(index++, "\t\t</styling>")

		// head closes
		file.add(index++, "\t</head>")
		// body opens
		file.add(index++, "\t<body>")
		// unique div opens
		file.add(index++, "\t\t<div>")

		// Next we iterate over the captions
		for (caption in tto.captions.values) {
			// we open the subtitle line
			var line = "\t\t\t<p begin=\"" + (caption.start?.getTime("hh:mm:ss,ms")?.replace(',', '.').orEmpty()) + "\""
			line += " end=\"" + (caption.end?.getTime("hh:mm:ss,ms")?.replace(',', '.').orEmpty()) + "\""
			caption.style?.let { line += " style=\"" + it.iD + "\"" }
			// attributes are done being inserted, if region was implemented it should be added before this.
			line += " >" + caption.content + "</p>\n"
			// we write the line
			file.add(index++, line)
		}

		// unique div closes
		file.add(index++, "\t\t</div>")
		// body closes
		file.add(index++, "\t</body>")
		// root closes
		file.add(index++, "</tt>")

		// an empty line is added
		file.add(index, "")

		return file.toTypedArray()
	}

	/**
	 * Identifies the color expression and obtains the RGBA equivalent value.
	 */
	private fun parseColor(color: String, tto: TimedTextObject): String {
		if (color.startsWith("#")) {
			return when (color.length) {
				7 -> color.substring(1) + "ff"
				9 -> color.substring(1)
				else -> {
					tto.warnings += "Unrecognized format: $color\n\n"
					// unrecognized format
					"ffffffff"
				}
			}
		}
		if (color.startsWith("rgb")) {
			val alpha = color.startsWith("rgba")
			return try {
				val content = color.split("\\(")[1].split(",")
				val r = content[0].toInt()
				val g = content[1].toInt()
				val b = content[2].substring(0, 2).toInt()
				val a = if (alpha) content[3].substring(0, 2).toInt() else 255

				val hexR = Integer.toHexString(r).padStart(2, '0')
				val hexG = Integer.toHexString(g).padStart(2, '0')
				val hexB = Integer.toHexString(b).padStart(2, '0')
				val hexA = if (alpha) Integer.toHexString(a).padStart(2, '0') else "ff"
				hexR + hexG + hexB + hexA
			} catch (e: Exception) {
				tto.warnings += "Unrecognized color: $color\n\n"
				"ffffffff"
			}
		}
		// it should be a named color so...
		val namedColor = Style.getRGBValue("name", color)
		// if not recognized named color
		if (namedColor != null) {
			return namedColor
		}
		tto.warnings += "Unrecognized color: $color\n\n"
		return "ffffffff"
	}

	/**
	 * Returns the number of milliseconds equivalent to this time expression
	 */
	private fun parseTimeExpression(timeExpression: String, tto: TimedTextObject?, doc: Document): Int {
		var mSeconds = 0
		if (timeExpression.contains(":")) {
			// it is a clock time
			val parts = timeExpression.split(":")
			if (parts.size == 3) {
				val h = parts[0].toInt()
				val m = parts[1].toInt()
				val s = parts[2].toFloat()
				mSeconds = h * 3600000 + m * 60000 + (s * 1000).toInt()
			} else if (parts.size == 4) {
				var frameRate = 25
				// we recover the frame rate
				val n = doc.getElementsByTagName("ttp:frameRate").item(0)
				if (n != null) {
					// used as auxiliary string
					val aux = n.nodeValue
					try {
						frameRate = aux.toInt()
					} catch (e: NumberFormatException) {
						// should not happen, but if it does, use default value...
					}
				}
				// we have h:m:s:f.fraction�
				val h = parts[0].toInt()
				val m = parts[1].toInt()
				val s = parts[2].toInt()
				val f = parts[3].toFloat()
				mSeconds = h * 3600000 + m * 60000 + s * 1000 + (f * 1000 / frameRate).toInt()
			}
		} else {
			// it is an offset - time, this is composed of a number and a metric
			var expr = timeExpression
			val metric = expr.substring(expr.length - 1)
			expr = expr.substring(0, expr.length - 1).replace(',', '.').trim()
			try {
				val time = expr.toDouble()
				when {
					metric.equals("h", ignoreCase = true) -> mSeconds = (time * 3600000).toInt()
					metric.equals("m", ignoreCase = true) -> mSeconds = (time * 60000).toInt()
					metric.equals("s", ignoreCase = true) -> mSeconds = (time * 1000).toInt()
					metric.equals("ms", ignoreCase = true) -> mSeconds = time.toInt()
					metric.equals("f", ignoreCase = true) -> {
						// we recover the frame rate
						val n = doc.getElementsByTagName("ttp:frameRate").item(0)
						if (n != null) {
							// used as auxiliary string
							val frameRate = n.nodeValue.toInt()
							mSeconds = (time * 1000 / frameRate).toInt()
						}
					}

					metric.equals("t", ignoreCase = true) -> {
						// we recover the tick rate
						val n = doc.getElementsByTagName("ttp:tickRate").item(0)
						if (n != null) {
							// used as auxiliary string
							val tickRate = n.nodeValue.toInt()
							mSeconds = (time * 1000 / tickRate).toInt()
						}
					}
				}
			} catch (e: NumberFormatException) {
				// incorrect format for offset time
			}
		}

		return mSeconds
	}
}
