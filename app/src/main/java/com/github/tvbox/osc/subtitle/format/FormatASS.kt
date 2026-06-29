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

import com.github.tvbox.osc.subtitle.model.Style
import com.github.tvbox.osc.subtitle.model.Subtitle
import com.github.tvbox.osc.subtitle.model.Time
import com.github.tvbox.osc.subtitle.model.TimedTextObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class FormatASS : TimedTextFileFormat {
	override fun parseFile(fileName: String, `is`: InputStream): TimedTextObject {
		val tto = TimedTextObject()
		tto.fileName = fileName

		// for the clock timer
		var timer = 100f

		// if the file is .SSA or .ASS
		var isASS = false

		// variables to store the formats
		var styleFormat: Array<String>?
		var dialogueFormat: Array<String>?

		// first lets load the file
		val reader = InputStreamReader(`is`)
		val br = BufferedReader(reader)

		var line: String?
		var lineCounter = 0
		try {
			`is`.use {
				// we scour the file
				line = br.readLine()
				lineCounter++
				while (line != null) {
					line = line.trim()
					// we skip any line until we find a section [section name]
					if (line.startsWith("[")) {
						// now we must identify the section
						when {
							line.equals("[Script info]", ignoreCase = true) -> {
								// it's the script info section
								lineCounter++
								line = br.readLine()?.trim()
								// Each line is scanned for useful info until a new section is detected
								while (line != null && !line.startsWith("[")) {
									when {
										line.startsWith("Title:") -> { // 标题信息非必要
											val titleArr = line.split(":")
											// We have found the title
											tto.title = if (titleArr.size > 1) titleArr[1].trim() else ""
										}

										line.startsWith("Original Script:") -> { // 作者信息非必要
											val authorArr = line.split(":")
											// We have found the author
											tto.author = if (authorArr.size > 1) authorArr[1].trim() else ""
										}

										line.startsWith("Script Type:") -> {
											// we have found the version
											val scriptType = line.split(":")[1].trim()
											if (scriptType.equals("v4.00+", ignoreCase = true)) isASS = true
											else if (!scriptType.equals("v4.00", ignoreCase = true))
												tto.warnings += "Script version is older than 4.00, it may produce parsing errors."
										}

										line.startsWith("Timer:") -> {
											timer = line.split(":")[1].trim().replace(',', '.').toFloat()
										}
									}
									// we go to the next line
									lineCounter++
									line = br.readLine()?.trim()
								}
							}

							line.equals("[v4 Styles]", ignoreCase = true)
									|| line.equals("[v4 Styles+]", ignoreCase = true)
									|| line.equals("[v4+ Styles]", ignoreCase = true) -> {
								// it's the Styles description section
								if (line.contains("+") && !isASS) {
									// its ASS and it had not been noted
									isASS = true
									tto.warnings += "ScriptType should be set to v4:00+ in the [Script Info] section.\n\n"
								}
								lineCounter++
								line = br.readLine()
								// the first line should define the format
								if (!line.startsWith("Format:")) {
									// if not, we scan for the format.
									tto.warnings += "Format: (format definition) expected at line $line for the styles section\n\n"
									while (line != null && !line.startsWith("Format:")) {
										lineCounter++
										line = br.readLine()
									}
									if (line == null) return@use
								}
								// we recover the format's fields
								styleFormat = line.split(":")[1].trim().split(",").toTypedArray()
								lineCounter++
								line = br.readLine()
								// we parse each style until we reach a new section
								while (line != null && !line.startsWith("Style:")) {
									tto.warnings += "Style: (format definition) expected at line $line for the styles section\n\n"
									// next line
									lineCounter++
									line = br.readLine()
								}
								if (line == null) return@use
								// we parse the style
								val styleFormatNonNull = styleFormat
								val style = parseStyleForASS(
									line.split(":")[1].trim().split(","),
									styleFormatNonNull,
									lineCounter,
									isASS,
									tto.warnings
								)
								// and save the style
								tto.styling[style.iD] = style
							}

							line.equals("[Events]", ignoreCase = true) -> {
								// it's the events specification section
								lineCounter++
								line = br.readLine()
								tto.warnings += "Only dialogue events are considered, all other events are ignored.\n\n"
								// the first line should define the format of the dialogues
								if (!line.startsWith("Format:")) {
									// if not, we scan for the format.
									tto.warnings += "Format: (format definition) expected at line $line for the events section\n\n"
									while (line != null && !line.startsWith("Format:")) {
										lineCounter++
										line = br.readLine()
									}
									if (line == null) return@use
								}
								// we recover the format's fields
								dialogueFormat = line.split(":")[1].trim().split(",").toTypedArray()
								// next line
								lineCounter++
								line = br.readLine()
								// we parse each style until we reach a new section
								while (line != null && !line.startsWith("[")) {
									// we check it is a dialogue
									if (line.startsWith("Dialogue:")) {
										val dialogueFormatNonNull = dialogueFormat
										// we parse the dialogue
										val caption = parseDialogueForASS(
											line.split(":", limit = 2)[1].trim().split(",", limit = 10),
											dialogueFormatNonNull,
											timer,
											tto
										)
										// and save the caption
										var key = caption.start?.mSeconds ?: 0
										// in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
										while (tto.captions.containsKey(key)) key++
										tto.captions[key] = caption
									}
									// next line
									lineCounter++
									line = br.readLine()
								}
							}

							line.equals("[Fonts]", ignoreCase = true) || line.equals("[Graphics]", ignoreCase = true) -> {
								// it's the custom fonts or embedded graphics section
								// these are not supported
								tto.warnings += "The section " + line.trim() + " is not supported for conversion, all information there will be lost.\n\n"
							}

							else -> {
								tto.warnings += "Unrecognized section: " + line.trim() + " all information there is ignored."
							}
						}
					}
					line = br.readLine()
					lineCounter++
				}
				// parsed styles that are not used should be eliminated
				tto.cleanUnusedStyles()
			}
		} catch (e: NullPointerException) {
			tto.warnings += "unexpected end of file, maybe last caption is not complete.\n\n"
		}

		// we close the reader
		tto.built = true
		return tto
	}

	override fun toFile(tto: TimedTextObject): Any? {
		// first we check if the TimedTextObject had been built, otherwise...

		if (!tto.built) return null

		// we will write the lines in an ArrayList 
		var index = 0
		// the minimum size of the file is the number of captions and styles + lines for sections and formats and the script info, so we'll take some extra space.
		val file = ArrayList<String>(30 + tto.styling.size + tto.captions.size)

		// header is placed
		file.add(index++, "[Script Info]")
		// title next
		file.add(index++, "Title: ${tto.title.ifEmpty { tto.fileName }}")
		// author next
		file.add(index++, "Original Script: ${tto.author.ifEmpty { "Unknown" }}")
		// additional info
		if (tto.copyright.isNotEmpty()) file.add(index++, "; " + tto.copyright)
		if (tto.description.isNotEmpty()) file.add(index++, "; " + tto.description)
		file.add(index++, "; Converted by the Online Subtitle Converter developed by J. David Requejo")
		// mandatory info
		if (tto.useASSInsteadOfSSA) file.add(index++, "Script Type: V4.00+")
		else file.add(index++, "Script Type: V4.00")
		file.add(index++, "Collisions: Normal")
		file.add(index++, "Timer: 100,0000")
		if (tto.useASSInsteadOfSSA) file.add(index++, "WrapStyle: 1")
		// an empty line is added
		file.add(index++, "")

		// Styles section
		if (tto.useASSInsteadOfSSA) file.add(index++, "[V4+ Styles]")
		else file.add(index++, "[V4 Styles]")
		// define the format
		if (tto.useASSInsteadOfSSA)
			file.add(index++, "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding")
		else
			file.add(index++, "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, TertiaryColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, AlphaLevel, Encoding")
		// Next we iterate over the styles
		for (style in tto.styling.values) {
			if (style == null) continue
			var styleLine = "Style: "
			// new style
			// name
			styleLine += style.iD + ","
			styleLine += style.font + ","
			styleLine += style.fontSize + ","
			styleLine += getColorsForASS(tto.useASSInsteadOfSSA, style)
			styleLine += getOptionsForASS(tto.useASSInsteadOfSSA, style)
			// BorderStyle, Outline, Shadow
			styleLine += "1,2,2,"
			styleLine += getAlignForASS(tto.useASSInsteadOfSSA, style.textAlign)
			// MarginL, MarginR, MarginV
			styleLine += ",0,0,0,"
			// AlphaLevel
			if (!tto.useASSInsteadOfSSA) styleLine += "0,"
			// Encoding
			styleLine += "0"

			// and we add the style definition line
			file.add(index++, styleLine)
		}
		// an empty line is added
		file.add(index++, "")

		// Events section
		file.add(index++, "[Events]")
		// define the format
		if (tto.useASSInsteadOfSSA)
			file.add(index++, "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")
		else
			file.add(index++, "Format: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")
		// Next we iterate over the captions
		for (subtitle in tto.captions.values) {
			var line = "Dialogue: 0,"
			// start time
			line += subtitle.start?.getTime("h:mm:ss.cs") + ","
			// end time
			line += subtitle.end?.getTime("h:mm:ss.cs") + ","
			// style
			line += subtitle.style?.iD ?: "Default"
			// default margins are used, no name or effect is recognized
			line += ",,0000,0000,0000,,"
			// we add the caption text with \N as line breaks and clean of XML
			line += subtitle.content.replace("<br />", "ÀN").replace("<.*?>".toRegex(), "").replace('À', '\\')
			// and we add the caption line
			file.add(index++, line)
		}
		// an empty line is added
		file.add(index, "")

		return file.toTypedArray()
	}

	/**
	 * These methods transform a format line from ASS according to a format definition into a Style object.
	 * 
	 * @param line The format line without its declaration
	 * @param styleFormat The list of attributes in this format line
	 * @return A new Style object.
	 */
	private fun parseStyleForASS(
		line: List<String>,
		styleFormat: Array<String>,
		index: Int,
		isASS: Boolean,
		warnings: String
	): Style {
		val newStyle = Style(Style.defaultID())
		if (line.size == styleFormat.size) {
			val warningsBuilder = StringBuilder(warnings)
			for (i in styleFormat.indices) {
				val formatKey = styleFormat[i].trim()
				val lineValue = line[i].trim()
				when {
					formatKey.equals("Name", ignoreCase = true) -> newStyle.iD = lineValue
					formatKey.equals("Fontname", ignoreCase = true) -> newStyle.font = lineValue
					formatKey.equals("Fontsize", ignoreCase = true) -> newStyle.fontSize = lineValue
					formatKey.equals("PrimaryColour", ignoreCase = true) -> {
						newStyle.color = if (isASS) {
							if (lineValue.startsWith("&H")) Style.getRGBValue("&HAABBGGRR", lineValue)
							else Style.getRGBValue("decimalCodedAABBGGRR", lineValue)
						} else {
							if (lineValue.startsWith("&H")) Style.getRGBValue("&HBBGGRR", lineValue)
							else Style.getRGBValue("decimalCodedBBGGRR", lineValue)
						}
					}

					formatKey.equals("BackColour", ignoreCase = true) -> {
						newStyle.backgroundColor = if (isASS) {
							if (lineValue.startsWith("&H")) Style.getRGBValue("&HAABBGGRR", lineValue)
							else Style.getRGBValue("decimalCodedAABBGGRR", lineValue)
						} else {
							if (lineValue.startsWith("&H")) Style.getRGBValue("&HBBGGRR", lineValue)
							else Style.getRGBValue("decimalCodedBBGGRR", lineValue)
						}
					}

					formatKey.equals("Bold", ignoreCase = true) -> newStyle.bold = lineValue.toBoolean()
					formatKey.equals("Italic", ignoreCase = true) -> newStyle.italic = lineValue.toBoolean()
					formatKey.equals("Underline", ignoreCase = true) -> newStyle.underline = lineValue.toBoolean()
					formatKey.equals("Alignment", ignoreCase = true) -> {
						val placement = lineValue.toInt()
						if (isASS) {
							newStyle.textAlign = when (placement) {
								1 -> "bottom-left"
								2 -> "bottom-center"
								3 -> "bottom-right"
								4 -> "mid-left"
								5 -> "mid-center"
								6 -> "mid-right"
								7 -> "top-left"
								8 -> "top-center"
								9 -> "top-right"
								else -> {
									warningsBuilder.append("undefined alignment for style at line $index\n\n")
									null
								}
							}
						} else {
							newStyle.textAlign = when (placement) {
								9 -> "bottom-left"
								10 -> "bottom-center"
								11 -> "bottom-right"
								1 -> "mid-left"
								2 -> "mid-center"
								3 -> "mid-right"
								5 -> "top-left"
								6 -> "top-center"
								7 -> "top-right"
								else -> {
									warningsBuilder.append("undefined alignment for style at line $index\n\n")
									null
								}
							}
						}
					}
				}
			}
			warningsBuilder.toString()
		}

		return newStyle
	}

	/**
	 * These methods transform a dialogue line from ASS according to a format definition into a Caption object.
	 * 
	 * @param line The dialogue line without its declaration
	 * @param dialogueFormat The list of attributes in this dialogue line
	 * @param timer % to speed or slow the clock, above 100% span of the subtitles is reduced.
	 * @return A new Caption object
	 */
	private fun parseDialogueForASS(
		line: List<String>,
		dialogueFormat: Array<String>,
		timer: Float,
		tto: TimedTextObject
	): Subtitle {
		val newCaption = Subtitle()

		// all information from fields 10 onwards are the caption text therefore needn't be split
		val captionText = line[9]
		// text is cleaned before being inserted into the caption
		newCaption.content = captionText.replace("\\{.*?\\}".toRegex(), "").replace("\n", "<br />").replace("\\N", "<br />")

		for (i in dialogueFormat.indices) {
			val formatKey = dialogueFormat[i].trim()
			when {
				formatKey.equals("Style", ignoreCase = true) -> {
					val style = tto.styling[line[i].trim()]
					if (style != null) newCaption.style = style
					else tto.warnings += "undefined style: " + line[i].trim() + "\n\n"
				}

				formatKey.equals("Start", ignoreCase = true) -> {
					newCaption.start = Time("h:mm:ss.cs", line[i].trim())
				}

				formatKey.equals("End", ignoreCase = true) -> {
					newCaption.end = Time("h:mm:ss.cs", line[i].trim())
				}
			}
		}

		// timer is applied
		if (timer != 100f) {
			val startMs = newCaption.start?.mSeconds ?: return newCaption
			val endMs = newCaption.end?.mSeconds ?: return newCaption
			newCaption.start?.mSeconds = (startMs / (timer / 100)).toInt()
			newCaption.end?.mSeconds = (endMs / (timer / 100)).toInt()
		}
		return newCaption
	}

	/**
	 * Returns a string with the correctly formated colors
	 * 
	 * @param useASSInsteadOfSSA true if formated for ASS
	 * @return The colors in the decimal format
	 */
	private fun getColorsForASS(useASSInsteadOfSSA: Boolean, style: Style): String {
		val colors: String
		val color = style.color.orEmpty()
		if (useASSInsteadOfSSA) {
			colors = ("00" + color.substring(4, 6) + color.substring(2, 4) + color.substring(0, 2)).toInt(16).toString() +
					",16777215,0," +
					("80" + (style.backgroundColor?.substring(4, 6).orEmpty()) +
							(style.backgroundColor?.substring(2, 4).orEmpty()) +
							(style.backgroundColor?.substring(0, 2).orEmpty())).toLong(16) + ","
		} else {
			val bgcolor = (style.backgroundColor?.substring(4, 6).orEmpty()) +
					(style.backgroundColor?.substring(2, 4).orEmpty()) +
					(style.backgroundColor?.substring(0, 2).orEmpty())
			colors = (color.substring(4, 6) + color.substring(2, 4) + color.substring(0, 2)).toLong(16).toString() +
					",16777215,0," + bgcolor.toLong(16) + ","
		}
		return colors
	}

	/**
	 * @return A string with the correctly formated options
	 */
	private fun getOptionsForASS(useASSInsteadOfSSA: Boolean, style: Style): String {
		var options = if (style.bold) "-1," else "0,"
		options += if (style.italic) "-1," else "0,"
		if (useASSInsteadOfSSA) {
			options += if (style.underline) "-1," else "0,"
			options += "0,100,100,0,0,"
		}
		return options
	}

	/**
	 * Converts the string explaining the alignment into the ASS equivalent integer offering bottom-center as default value
	 */
	private fun getAlignForASS(useASSInsteadOfSSA: Boolean, align: String?): Int {
		return if (useASSInsteadOfSSA) {
			when (align) {
				"bottom-left" -> 1
				"bottom-center" -> 2
				"bottom-right" -> 3
				"mid-left" -> 4
				"mid-center" -> 5
				"mid-right" -> 6
				"top-left" -> 7
				"top-center" -> 8
				"top-right" -> 9
				else -> 2
			}
		} else {
			when (align) {
				"bottom-left" -> 9
				"bottom-center" -> 10
				"bottom-right" -> 11
				"mid-left" -> 1
				"mid-center" -> 2
				"mid-right" -> 3
				"top-left" -> 5
				"top-center" -> 6
				"top-right" -> 7
				else -> 10
			}
		}
	}
}
