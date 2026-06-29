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
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class FormatSCC : TimedTextFileFormat {
	override fun parseFile(fileName: String, `is`: InputStream): TimedTextObject {
		val tto = TimedTextObject()
		var newCaption: Subtitle? = null

		// variables to represent a decoder
		var textBuffer = ""
		var isChannel1 = false
		var isBuffered = true

		// to store current style
		var underlined = false
		var italics = false
		var color: String? = null

		// first lets load the file
		val br = BufferedReader(InputStreamReader(`is`))

		// the file name is saved
		tto.fileName = fileName
		tto.title = fileName

		var line: String?
		var lineCounter = 0
		try {
			lineCounter++
			// the file must start with the type declaration
			if (!br.readLine().trim().equals("Scenarist_SCC V1.0", ignoreCase = true)) {
				// this is a fatal parsing error.
				throw FatalParsingException("The fist line should define the file type: \"Scenarist_SCC V1.0\"")
			}
			createSCCStyles(tto)

			tto.warnings += "Only data from CC channel 1 will be extracted.\n\n"
			line = br.readLine()

			while (line != null) {
				line = line.trim()
				lineCounter++
				// if it's not an empty line
				if (line.isNotEmpty()) {
					// we separate the time code from the VANC data
					val data = line.split("\t")
					val currentTime = Time("h:m:s:f/fps", data[0] + "/29.97")
					// we separate the words
					val words = data[1].split(" ")
					var j = 0
					while (j < words.size) {
						// we get its hex value stored in an int
						var word = words[j].toInt(16)

						// we eliminate the parity bits before decoding
						word = word and 0x7f7f

						// if it is a char:
						if ((word and 0x6000) != 0) {
							// if we are in the right channel (1)
							if (isChannel1) {
								// we extract the two chars
								val c1 = ((word and 0xff00) ushr 8).toByte()
								val c2 = (word and 0x00ff).toByte()

								if (isBuffered) {
									// we decode the byte and add it to the text buffer
									textBuffer += decodeChar(c1)
									// we decode the second char and add it, this one can be empty.
									textBuffer += decodeChar(c2)
								} else {
									val caption = newCaption ?: continue
									// we decode the byte and add it to the text screen
									caption.content += decodeChar(c1)
									// we decode the second char and add it, this one can be empty.
									caption.content += decodeChar(c2)
								}
							}
						} else if (word == 0x0000) {
							// word 8080 is filler to add frames
							currentTime.mSeconds += (1000 / 29.97).toInt()
						} else {
							// it is a control code
							if (j + 1 < words.size && words[j] == words[j + 1]) {
								// if code is repeated, skip one.
								j++
							}

							// we check the channel
							if ((word and 0x0800) == 0) {
								// we are on channel 1 or 3

								// we parse the code
								if ((word and 0x1670) == 0x1420) {
									// it is a command code
									// we check the channel
									if ((word and 0x0100) == 0) {
										// it is channel 1
										isChannel1 = true
										// the command is decoded
										word = word and 0x000f
										when (word) {
											0 -> {
												// Resume Caption Loading: start pop on captions
												isBuffered = true
												textBuffer = ""
											}

											5, 6, 7 -> {
												// roll-up caption by number of rows, effect not supported
												// clear text buffer
												textBuffer = ""
												// clear screen text
												val caption = newCaption
												if (caption != null) {
													caption.end = currentTime
													var styleId = ""
													if (color != null) styleId += color
													if (underlined) styleId += "U"
													if (italics) styleId += "I"
													caption.style = tto.styling[styleId]
													caption.start?.mSeconds?.let { tto.captions[it] = caption }
												}
												// new caption starts with roll up style
												newCaption = Subtitle()
												newCaption.start = currentTime
												// all characters and codes will be applied directly to the screen
												isBuffered = false
											}

											9 -> {
												// Resume Direct Captioning: start paint-on captions
												isBuffered = false
												newCaption = Subtitle()
												newCaption.start = currentTime
											}

											12 -> {
												// Erase Displayed Memory: clear screen text
												val caption = newCaption
												if (caption != null) {
													caption.end = currentTime
													caption.start?.let { start ->
														// we save the caption
														var key = start.mSeconds
														// in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
														while (tto.captions.containsKey(key)) key++
														// we save the caption
														tto.captions[key] = caption
														// and reset the caption builder
														newCaption = Subtitle()
													}
												}
											}

											14 -> {
												// Erase Non-Displayed Memory: clear the text buffer
												textBuffer = ""
											}

											15 -> {
												// End of caption: Swap off-screen buffer with caption screen.
												val caption = Subtitle()
												caption.start = currentTime
												caption.content = textBuffer
												newCaption = caption
											}
										}
									} else {
										isChannel1 = false
									}
								} else if (isChannel1) {
									when {
										(word and 0x1040) == 0x1040 -> {
											// it is a preamble code, format is removed
											color = "white"
											underlined = false
											italics = false
											// it is a new line
											if (isBuffered && textBuffer.isNotEmpty()) textBuffer += "<br />"
											if (!isBuffered) {
												val caption = newCaption
												if (caption != null && caption.content.isNotEmpty()) {
													caption.content += "<br />"
												}
											}
											if ((word and 0x0001) == 1) // it is underlined
												underlined = true
											// positioning is not supported, rows and columns are ignored
											if ((word and 0x0010) != 0x0010) {
												// setting style for following text
												word = word and 0x000e
												word = (word shr 1)
												when (word) {
													0 -> color = "white"
													1 -> color = "green"
													2 -> color = "blue"
													3 -> color = "cyan"
													4 -> color = "red"
													5 -> color = "yellow"
													6 -> color = "magenta"
													7 -> italics = true
												}
											} else {
												color = "white"
											}
										}

										(word and 0x1770) == 0x1120 -> {
											// it is a midrow style code
											// it is underlined
											underlined = (word and 0x001) == 1
											// setting style for text
											word = word and 0x000e
											word = (word shr 1)
											when (word) {
												0 -> {
													color = "white"
													italics = false
												}

												1 -> {
													color = "green"
													italics = false
												}

												2 -> {
													color = "blue"
													italics = false
												}

												3 -> {
													color = "cyan"
													italics = false
												}

												4 -> {
													color = "red"
													italics = false
												}

												5 -> {
													color = "yellow"
													italics = false
												}

												6 -> {
													color = "magenta"
													italics = false
												}

												7 -> italics = true
											}
										}

										(word and 0x1770) == 0x1130 -> {
											// it is a special character code
											word = word and 0x000f
											val specialChar = decodeSpecialChar(word)
											// coded value is extracted
											if (isBuffered) { // we decode the special char
												// and add it to the text buffer
												textBuffer += specialChar
											} else { // we decode the special char
												// and add it to the text
												newCaption?.content += specialChar
											}
										}
									}
								}
							} else {
								// we are on channel 2 or 4
								isChannel1 = false
							}
						}
						j++
					}
				}
				// end of while
				line = br.readLine()
			}

			// we save any last shown caption
			newCaption?.let { finalCaption ->
				finalCaption.end = Time("h:m:s:f/fps", "99:59:59:29/29.97")
				finalCaption.start?.let { start ->
					// we save the caption
					var key = start.mSeconds
					// in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
					while (tto.captions.containsKey(key)) key++
					// we save the caption
					tto.captions[key] = finalCaption
				}
			}
			tto.cleanUnusedStyles()
		} catch (e: NullPointerException) {
			tto.warnings += ("unexpected end of file at line $lineCounter, maybe last caption is not complete.\n\n")
		} finally {
			// we close the reader
			`is`.close()
		}

		tto.built = true
		return tto
	}

	override fun toFile(tto: TimedTextObject): Any? {
		// first we check if the TimedTextObject had been built, otherwise...
		if (!tto.built) return null

		// we will write the lines in an ArrayList
		var index = 0
		// the minimum size of the file is double the number of captions since lines are double-spaced.
		val file = ArrayList<String>(20 + 2 * tto.captions.size)

		// first we add the header
		file.add(index++, "Scenarist_SCC V1.0\n")

		var oldC: Subtitle?
		var newC = Subtitle().apply {
			content = ""
			end = Time("hh:mm:ss.cs", "0:00:00.00")
		}

		// Next we iterate over the captions
		for (subtitle in tto.captions.values) {
			var line = ""
			oldC = newC
			newC = subtitle
			val oldEnd = oldC.end ?: return null
			val newStart = newC.start ?: return null

			// if old caption ends after new caption starts
			if (oldEnd.mSeconds > newStart.mSeconds) {
				// captions overlap
				newC.content += ("<br />" + oldC.content)
				// we add the time to the new line, and clear old caption so both can now appear
				newStart.mSeconds -= (1000 / 29.97).toInt()
				// we correct the frame delay (8080 8080)
				line += newStart.getTime("hh:mm:ss:ff/29.97") + "\t942c 942c "
				newStart.mSeconds += (1000 / 29.97).toInt()
				// we clear the buffer and start new pop-on caption
				line += "94ae 94ae 9420 9420 "
			} else if (oldEnd.mSeconds < newStart.mSeconds) {
				// we clear the screen for new caption
				line += oldEnd.getTime("hh:mm:ss:ff/29.97") + "\t942c 942c\n\n"
				// we add the time to the new line, we clear buffer and start new caption
				newStart.mSeconds -= (1000 / 29.97).toInt()
				line += newStart.getTime("hh:mm:ss:ff/29.97") + "\t94ae 94ae 9420 9420 "
				newStart.mSeconds += (1000 / 29.97).toInt()
			} else {
				// we add the time to the new line, we clear screen and buffer and start new caption
				newStart.mSeconds -= (1000 / 29.97).toInt()
				// we correct the frame delay (8080 8080)
				line += newStart.getTime("hh:mm:ss:ff/29.97") + "\t942c 942c 94ae 94ae 9420 9420 "
				newStart.mSeconds += (1000 / 29.97).toInt()
			}

			// we add the coded caption text along with any styles to the off-screen buffer
			line += codeText(newC)
			// lastly we display the caption
			line += "8080 8080 942f 942f\n"

			// we add it to the "file"
			file.add(index++, line)
		}

		// an empty line is added
		file.add(index, "")

		// we return the expected file as an array of String
		return file.toTypedArray()
	}

	/**
	 * INCOMPLETE METHOD: does not tab to correct position or applies styles
	 */
	private fun codeText(newC: Subtitle): String {
		var toReturn = ""

		val lines = newC.content.split("<br />").toMutableList()

		var i = 0
		// max 32 chars
		if (lines[i].length > 32) lines[i] = lines[i].substring(0, 32)
		// we calculate tabs to center the text
		lines[i].length

		// we position the cursor with a preamble code
		// the row should be chosen according to how many lines left...
		toReturn += "1340 1340 "

		// we tab over to the correct spot

		// we add the caption style using midrow codes

		// we code the caption text
		toReturn += codeChar(lines[i].toCharArray())

		if (lines.size > 1) {
			// and next line
			i++

			// max 32 chars
			if (lines[i].length > 32) lines[i] = lines[i].substring(0, 32)
			// we calculate tabs to center the text
			lines[i].length

			// we position the cursor with a preamble code
			// the row should be chosen according to how many lines left...
			toReturn += "13e0 13e0 "

			// we tab over to the correct spot

			// we add the caption style using midrow codes

			// we code the caption text
			toReturn += codeChar(lines[i].toCharArray())

			if (lines.size > 2) {
				// and next line
				i++

				// max 32 chars
				if (lines[i].length > 32) lines[i] = lines[i].substring(0, 32)
				// we calculate tabs to center the text
				lines[i].length

				// we position the cursor with a preamble code
				toReturn += "9440 9440 "

				// we tab over to the correct spot
				// we add the caption style using midrow codes

				// we code the caption text
				toReturn += codeChar(lines[i].toCharArray())

				if (lines.size > 3) {
					// and next line
					i++

					// max 32 chars
					if (lines[i].length > 32) lines[i] = lines[i].substring(0, 32)
					// we calculate tabs to center the text
					lines[i].length

					// we position the cursor with a preamble code
					toReturn += "94e0 94e0 "

					// we tab over to the correct spot
					// we add the caption style using midrow codes

					// we code the caption text
					toReturn += codeChar(lines[i].toCharArray())
				}
			}
		}

		return toReturn
	}

	/**
	 * INCOMPLETE METHOD, does not consider special or extended chars
	 */
	private fun codeChar(chars: CharArray): String {
		val toReturn = StringBuilder()
		var i = 0
		while (i < chars.size) {
			when (chars[i]) {
				' ' -> toReturn.append("20")
				'!' -> toReturn.append("a1")
				'"' -> toReturn.append("a2")
				'#' -> toReturn.append("23")
				'$' -> toReturn.append("a4")
				'%' -> toReturn.append("25")
				'&' -> toReturn.append("26")
				'\'' -> toReturn.append("a7")
				'(' -> toReturn.append("a8")
				')' -> toReturn.append("29")
				'�' -> toReturn.append("2a")
				'+' -> toReturn.append("ab")
				',' -> toReturn.append("2c")
				'-' -> toReturn.append("ad")
				'.' -> toReturn.append("ae")
				'/' -> toReturn.append("2f")
				'0' -> toReturn.append("b0")
				'1' -> toReturn.append("31")
				'2' -> toReturn.append("32")
				'3' -> toReturn.append("b3")
				'4' -> toReturn.append("34")
				'5' -> toReturn.append("b5")
				'6' -> toReturn.append("b6")
				'7' -> toReturn.append("37")
				'8' -> toReturn.append("38")
				'9' -> toReturn.append("b9")
				':' -> toReturn.append("ba")
				';' -> toReturn.append("3b")
				'<' -> toReturn.append("bc")
				'=' -> toReturn.append("3d")
				'>' -> toReturn.append("3e")
				'?' -> toReturn.append("bf")
				'@' -> toReturn.append("40")
				'A' -> toReturn.append("c1")
				'B' -> toReturn.append("c2")
				'C' -> toReturn.append("43")
				'D' -> toReturn.append("c4")
				'E' -> toReturn.append("45")
				'F' -> toReturn.append("46")
				'G' -> toReturn.append("c7")
				'H' -> toReturn.append("c8")
				'I' -> toReturn.append("49")
				'J' -> toReturn.append("4a")
				'K' -> toReturn.append("cb")
				'L' -> toReturn.append("4c")
				'M' -> toReturn.append("cd")
				'N' -> toReturn.append("ce")
				'O' -> toReturn.append("4f")
				'P' -> toReturn.append("d0")
				'Q' -> toReturn.append("51")
				'R' -> toReturn.append("52")
				'S' -> toReturn.append("d3")
				'T' -> toReturn.append("54")
				'U' -> toReturn.append("d5")
				'V' -> toReturn.append("d6")
				'W' -> toReturn.append("57")
				'X' -> toReturn.append("58")
				'Y' -> toReturn.append("d9")
				'Z' -> toReturn.append("da")
				'[' -> toReturn.append("5b")
				'a' -> toReturn.append("61")
				'b' -> toReturn.append("62")
				'c' -> toReturn.append("e3")
				'd' -> toReturn.append("64")
				'e' -> toReturn.append("e5")
				'f' -> toReturn.append("e6")
				'g' -> toReturn.append("67")
				'h' -> toReturn.append("68")
				'i' -> toReturn.append("e9")
				'j' -> toReturn.append("ea")
				'k' -> toReturn.append("6b")
				'l' -> toReturn.append("ec")
				'm' -> toReturn.append("6d")
				'n' -> toReturn.append("6e")
				'o' -> toReturn.append("ef")
				'p' -> toReturn.append("70")
				'q' -> toReturn.append("f1")
				'r' -> toReturn.append("f2")
				's' -> toReturn.append("73")
				't' -> toReturn.append("f4")
				'u' -> toReturn.append("75")
				'v' -> toReturn.append("76")
				'w' -> toReturn.append("f7")
				'x' -> toReturn.append("f8")
				'y' -> toReturn.append("79")
				'z' -> toReturn.append("7a")
				'|' -> toReturn.append("7f")
				// error: it happens for strange chars, since it is not complete, they are replaced by spaces
				else -> toReturn.append("7f")
			}
			if (i % 2 == 1) toReturn.append(" ")
			i++
		}
		if (i % 2 == 1) toReturn.append("80 ")

		return toReturn.toString()
	}

	private fun decodeChar(c: Byte): String {
		return when (c) {
			42.toByte(), 124.toByte() -> "�"
			92.toByte() -> "é"
			94.toByte() -> "í"
			95.toByte() -> "ó"
			96.toByte() -> "ú"
			123.toByte() -> "ç"
			125.toByte() -> "Ñ"
			126.toByte() -> "ñ"
			127.toByte() -> "|"
			0.toByte() -> ""  // filler code
			else -> Char(c.toUShort()).toString()
		}
	}

	private fun decodeSpecialChar(word: Int): String {
		return when (word) {
			15, 0, 1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 13, 14 -> "�"
			9 -> "\u00A0"
			7 -> "♪"
			else -> "" // unrecognized code
		}
	}

	private fun createSCCStyles(tto: TimedTextObject) {

		var style = Style("white")
		tto.styling[style.iD] = style

		style = Style("whiteU", style)
		style.underline = true
		tto.styling[style.iD] = style

		style = Style("whiteUI", style)
		style.italic = true
		tto.styling[style.iD] = style

		style = Style("whiteI", style)
		style.underline = false
		tto.styling[style.iD] = style

		style = Style("green")
		style.color = Style.getRGBValue("name", "green")
		tto.styling[style.iD] = style

		style = Style("greenU", style)
		style.underline = true
		tto.styling[style.iD] = style

		style = Style("greenUI", style)
		style.italic = true
		tto.styling[style.iD] = style

		style = Style("greenI", style)
		style.underline = false
		tto.styling[style.iD] = style

		style = Style("blue")
		style.color = Style.getRGBValue("name", "blue")
		tto.styling[style.iD] = style

		style = Style("blueU", style)
		style.underline = true
		tto.styling[style.iD] = style

		style = Style("blueUI", style)
		style.italic = true
		tto.styling[style.iD] = style

		style = Style("blueI", style)
		style.underline = false
		tto.styling[style.iD] = style

		style = Style("cyan")
		style.color = Style.getRGBValue("name", "cyan")
		tto.styling[style.iD] = style

		style = Style("cyanU", style)
		style.underline = true
		tto.styling[style.iD] = style

		style = Style("cyanUI", style)
		style.italic = true
		tto.styling[style.iD] = style

		style = Style("cyanI", style)
		style.underline = false
		tto.styling[style.iD] = style

		style = Style("red")
		style.color = Style.getRGBValue("name", "red")
		tto.styling[style.iD] = style

		style = Style("redU", style)
		style.underline = true
		tto.styling[style.iD] = style

		style = Style("redUI", style)
		style.italic = true
		tto.styling[style.iD] = style

		style = Style("redI", style)
		style.underline = false
		tto.styling[style.iD] = style

		style = Style("yellow")
		style.color = Style.getRGBValue("name", "yellow")
		tto.styling[style.iD] = style

		style = Style("yellowU", style)
		style.underline = true
		tto.styling[style.iD] = style

		style = Style("yellowUI", style)
		style.italic = true
		tto.styling[style.iD] = style

		style = Style("yellowI", style)
		style.underline = false
		tto.styling[style.iD] = style

		style = Style("magenta")
		style.color = Style.getRGBValue("name", "magenta")
		tto.styling[style.iD] = style

		style = Style("magentaU", style)
		style.underline = true
		tto.styling[style.iD] = style

		style = Style("magentaUI", style)
		style.italic = true
		tto.styling[style.iD] = style

		style = Style("magentaI", style)
		style.underline = false
		tto.styling[style.iD] = style
	}
}
