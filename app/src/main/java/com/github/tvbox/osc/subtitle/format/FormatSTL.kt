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
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormatSTL : TimedTextFileFormat {
	override fun parseFile(fileName: String, `is`: InputStream): TimedTextObject {
		val tto = TimedTextObject()
		tto.fileName = fileName

		val gsiBlock = ByteArray(1024)
		val ttiBlock = ByteArray(128)

		try {
			// we read the file, but first we create the possible styles
			createSTLStyles(tto)

			var bytesRead: Int
			// the GSI block is loaded
			bytesRead = `is`.read(gsiBlock)
			if (bytesRead < 1024) //the file must contain at least a GSI block and a TTI block
			//this is a fatal parsing error.
				throw FatalParsingException("The file must contain at least a GSI block")
			// CPC : code page number 0..2
			// DFC : disk format code 3..10
			// save the number of frames per second
			val dfc = byteArrayOf(gsiBlock[6], gsiBlock[7])
			val fps = String(dfc).toInt()
			// DSC : Display Standard Code 11
			// CCT : Character Code Table number 12..13
			val cct = byteArrayOf(gsiBlock[12], gsiBlock[13])
			val table = String(cct).toInt()
			// LC : Language Code 14..15
			// OPT : Original Program Title 16..47
			val opt = ByteArray(32)
			System.arraycopy(gsiBlock, 16, opt, 0, 32)
			val title = String(opt)
			// OEP : Original Episode Title 48..79
			val oet = ByteArray(32)
			System.arraycopy(gsiBlock, 48, oet, 0, 32)
			val episodeTitle = String(oet)
			// TPT : Translated Program Title 80..111
			// TEP : Translated Episode Title 112..143
			// TN : Translator's Name 144..175
			// TCD : Translators Contact Details 176..207
			// SLR : Subtitle List Reference code 208..223
			// CD : Creation Date 224..229
			// RD : Revision Date 230..235
			// RN : Revision Number 236..237
			// TNB : Total Number of TTI Blocks 238..242
			val tnb = byteArrayOf(gsiBlock[238], gsiBlock[239], gsiBlock[240], gsiBlock[241], gsiBlock[242])
			val numberOfTTIBlocks = String(tnb).toInt()
			// TNS : Total Number of Subtitles 243..247
			val tns = byteArrayOf(gsiBlock[243], gsiBlock[244], gsiBlock[245], gsiBlock[246], gsiBlock[247])
			val numberOfSubtitles = String(tns).toInt()

			// TNG : Total Number of Subtitle Groups 248..250
			// MNC : Max Number of characters in row 251..252
			// MNR : Max number of rows 253..254
			// TCS : Time Code: Status 255
			// TCP : Time Code: Start-of-Program 256..263
			// TCF : Time Code: First In-Cue 264..271
			// TND : Total Number of Disks 272
			// DSN : Disk Sequence Number 273
			// CO : Country of Origin 274..276
			// PUB : Publisher 277..308
			// EN : Editor's Name 309..340
			// ECD : Editor's Contact Details 341..372
			// Spare bytes 373..447
			// UDA : User-Defined Area 448..1023

			// we add the title
			tto.title = (title.trim() + " " + episodeTitle.trim()).trim()
			// this checks the reference to the characters coding employed.
			if (table !in 0..4)
				tto.warnings += "Invalid Character Code table number, corrupt data? will try to parse anyways assuming it is Latin.\n\n"
			else if (table != 0)
				tto.warnings += "Only Latin alphabet supported for import from STL, other languages may produce unexpected results.\n\n"

			var subtitleNumber = 0
			var additionalText = false
			var currentCaption: Subtitle? = null
			// the TTI blocks are read
			for (i in 0..<numberOfTTIBlocks) {
				// the TTI block is loaded
				bytesRead = `is`.read(ttiBlock)
				if (bytesRead < 128) {
					//unexpected end of file
					tto.warnings += "Unexpected end of file, $i blocks read, expecting $numberOfTTIBlocks blocks in total.\n\n"
					break
				}

				// if we have additional text pending, we do not create a new caption
				if (!additionalText) currentCaption = Subtitle()

				// SGN : Subtitle group number 0
				// SN : Subtitle Number 1..2
				val currentSubNumber = ttiBlock[1] + 256 * ttiBlock[2]
				if (currentSubNumber != subtitleNumber) // missing subtitle number?
					tto.warnings += "Unexpected subtitle number at TTI block $i. Parsing proceeds...\n\n"
				// EBN : Extension Block Number 3
				val ebn = ttiBlock[3].toInt()
				additionalText = ebn != -1

				// TCI : Time Code In 5..8
				val startTime = "${ttiBlock[5]}:${ttiBlock[6]}:${ttiBlock[7]}:${ttiBlock[8]}"
				// TCO : Time Code Out 9..12
				val endTime = "${ttiBlock[9]}:${ttiBlock[10]}:${ttiBlock[11]}:${ttiBlock[12]}"
				// VP : Vertical Position 13
				// JC : Justification Code 14
				val justification = ttiBlock[14].toInt()
				// 0:none, 1:left, 2:centered, 3:right
				// CF : Comment Flag 15
				if (ttiBlock[15].toInt() == 0) {
					// comments are ignored
					// TF : Text Field 16..112
					val textField = ByteArray(112)
					System.arraycopy(ttiBlock, 16, textField, 0, 112)

					val caption = currentCaption ?: continue
					if (additionalText) { // if it is just additional text for the caption
						parseTextForSTL(caption, textField, justification, tto)
					} else {
						caption.start = Time("h:m:s:f/fps", "$startTime/$fps")
						caption.end = Time("h:m:s:f/fps", "$endTime/$fps")
						parseTextForSTL(caption, textField, justification, tto)
					}
				}
				// we increase the subtitle number
				if (!additionalText) subtitleNumber++
			}
			if (subtitleNumber != numberOfSubtitles)
				tto.warnings += "Number of parsed subtitles ($subtitleNumber) different from expected number of subtitles ($numberOfSubtitles).\n\n"

			//we close the reader
			`is`.close()

			tto.cleanUnusedStyles()
		} catch (e: Exception) {
			//format error
			e.printStackTrace()
			throw FatalParsingException("Format error in the file, might be due to corrupt data.\n" + e.message)
		}

		tto.built = true
		return tto
	}

	override fun toFile(tto: TimedTextObject): Any? {
		// first we check if the TimedTextObject had been built, otherwise...
		if (!tto.built) return null

		var ttiBlock = ByteArray(128)

		// we will store the whole binary file as a unique array
		val file = ByteArray(1024 + 128 * tto.captions.size)

		// we build the GSI block
		val gsiBlock = ByteArray(1024)
		var extra = "850STL25.0110000".toByteArray()
		System.arraycopy(extra, 0, gsiBlock, 0, extra.size)
		// then we add the title and fill the rest with blanks
		extra = tto.title.toByteArray()
		for (i in 0..<224 - 16) {
			if (i < extra.size) gsiBlock[i + 16] = extra[i]
			else gsiBlock[i + 16] = 32
		}
		// other info
		val dateFormat: DateFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())
		val date = Date()
		var aux = dateFormat.format(date)
		aux += aux + "00" // revision number
		val aux2 = tto.captions.size.toString().padStart(5, '0')
		aux += aux2 + aux2 + "0013216100000000"
		// we add the time of first subtitle
		val firstCaption = tto.captions[tto.captions.firstKey()] ?: return null
		val firstStart = firstCaption.start ?: return null
		aux += firstStart.getTime("hhmmssff/25")
		aux += "11OOO"
		extra = aux.toByteArray()
		System.arraycopy(extra, 0, gsiBlock, 224, extra.size)
		// the rest is filled with blanks
		for (i in 277..1023) {
			gsiBlock[i] = 32
		}

		// we add the GSI block to our string representing the file
		System.arraycopy(gsiBlock, 0, file, 0, gsiBlock.size)

		// we iterate over the captions to create the TTI blocks
		for ((subtitleNumber, currentC) in tto.captions.values.withIndex()) {
			// SGN
			ttiBlock[0] = 0
			// SN
			ttiBlock[1] = (subtitleNumber % 256).toByte()
			ttiBlock[2] = (subtitleNumber / 256).toByte()
			// EBN
			ttiBlock[3] = 0xff.toByte()
			// CS
			ttiBlock[4] = 0
			// TCI
			val startTime = currentC.start ?: return null
			var timeCode = startTime.getTime("h:m:s:f/25").split(":")
			ttiBlock[5] = timeCode[0].toByte()
			ttiBlock[6] = timeCode[1].toByte()
			ttiBlock[7] = timeCode[2].toByte()
			ttiBlock[8] = timeCode[3].toByte()
			// TCO
			val endTime = currentC.end ?: return null
			timeCode = endTime.getTime("h:m:s:f/25").split(":")
			ttiBlock[9] = timeCode[0].toByte()
			ttiBlock[10] = timeCode[1].toByte()
			ttiBlock[11] = timeCode[2].toByte()
			ttiBlock[12] = timeCode[3].toByte()
			// VP
			ttiBlock[13] = 18
			// JC
			if (currentC.style != null) {
				val style = currentC.style ?: return null
				val align = style.textAlign ?: return null
				ttiBlock[14] = when {
					align.contains("left") -> 1
					align.contains("right") -> 3
					else -> 2
				}
			} else {
				ttiBlock[14] = 2
			}
			// CF
			ttiBlock[15] = 0
			// TF
			val lines = currentC.content.split("<br />").toMutableList()
			// we clean XML, span would be implemented here
			var pos = 16
			for (i in lines.indices) {
				lines[i] = lines[i].replace("<.*?>".toRegex(), "")
			}
			// we code the style
			if (currentC.style != null) {
				val style = currentC.style ?: return null
				ttiBlock[pos++] = if (style.italic) 0x80.toByte() else 0x81.toByte()
				ttiBlock[pos++] = if (style.underline) 0x82.toByte() else 0x83.toByte()

				// colors
				val styleColor = style.color ?: return null
				val hexColor = styleColor.substring(0, 6)
				ttiBlock[pos++] = when {
					hexColor.equals("000000", ignoreCase = true) -> 0x00.toByte()
					hexColor.equals("0000ff", ignoreCase = true) -> 0x04.toByte()
					hexColor.equals("00ffff", ignoreCase = true) -> 0x06.toByte()
					hexColor.equals("00ff00", ignoreCase = true) -> 0x02.toByte()
					hexColor.equals("ff0000", ignoreCase = true) -> 0x01.toByte()
					hexColor.equals("ffff00", ignoreCase = true) -> 0x03.toByte()
					hexColor.equals("ff00ff", ignoreCase = true) -> 0x05.toByte()
					else -> 0x07.toByte()
				}
			}

			// we code the text
			for (line in lines) {
				val chars = line.toCharArray()
				for (aChar in chars) {
					//check the text is not too long
					if (pos > 126) break
					//check it is a supported char, else it is ignored
					if (aChar.code in 0x20..0x7f) ttiBlock[pos++] = aChar.code.toByte()
				}
			}

			// we fill the rest with end characters
			while (pos < 128) ttiBlock[pos++] = 0x8F.toByte()

			// we add the TTI block to our string representing the file
			System.arraycopy(ttiBlock, 0, file, 1024 + subtitleNumber * 128, ttiBlock.size)
			ttiBlock = ByteArray(128)
		}

		return file
	}

	/**
	 * This method parses the text field taking into account STL control codes
	 */
	private fun parseTextForSTL(currentCaption: Subtitle, textField: ByteArray, justification: Int, tto: TimedTextObject) {
		var italics = false
		var underline = false
		var color = "white"
		var text = StringBuilder()

		// we go around the field in pair of bytes to decode them
		var i = 0
		while (i < textField.size) {
			if (textField[i] < 0) {
				// first byte > 8 (4 bits)
				if (textField[i] <= -113) {
					// we might be with a control code
					if (i + 1 < textField.size && textField[i] == textField[i + 1]) i++ // if repeated skip one

					when (textField[i]) {
						(-128).toByte() -> italics = true
						(-127).toByte() -> italics = false
						(-126).toByte() -> underline = true
						(-125).toByte() -> underline = false
						(-124).toByte(), (-123).toByte() -> {}
						(-118).toByte() -> {
							// line break
							currentCaption.content += "$text<br />" // line could be trimmed here
							text = StringBuilder()
						}

						(-113).toByte() -> {
							// end of caption
							currentCaption.content += text // line could be trimmed here
							text = StringBuilder()
							// we check the style
							var styleId = color
							if (underline) styleId += "U"
							if (italics) styleId += "I"
							var style = tto.styling[styleId]

							if (justification == 1) {
								styleId += "L"
								if (tto.styling[styleId] == null) {
									val baseStyle = style ?: continue
									style = Style(styleId, baseStyle)
									style.textAlign = "bottom-left"
									tto.styling[styleId] = style
								} else {
									style = tto.styling[styleId]
								}
							} else if (justification == 3) {
								styleId += "R"
								if (tto.styling[styleId] == null) {
									val baseStyle = style ?: continue
									style = Style(styleId, baseStyle)
									style.textAlign = "bottom-right"
									tto.styling[styleId] = style
								} else {
									style = tto.styling[styleId]
								}
							}

							// we save the style
							currentCaption.style = style
							// and save the caption
							var key = currentCaption.start?.mSeconds ?: 0
							// in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
							while (tto.captions.containsKey(key)) key++
							tto.captions[key] = currentCaption
							// we end the loop
							i = textField.size
						}

						else -> {}
					}
				}
			} else if (textField[i] < 32) {
				// it is a teletext control code, only colors are supported
				if (i + 1 < textField.size && textField[i] == textField[i + 1]) i++ // if repeated skip one

				color = when (textField[i]) {
					7.toByte() -> "white"
					2.toByte() -> "green"
					4.toByte() -> "blue"
					6.toByte() -> "cyan"
					1.toByte() -> "red"
					3.toByte() -> "yellow"
					5.toByte() -> "magenta"
					0.toByte() -> "black"
					else -> color
				}
			} else {
				// we have a supported character coded in the two bytes, range is from 0x20 to 0x7F
				val x = byteArrayOf(textField[i])
				text.append(String(x))
			}

			i++
		}
	}

	private fun createSTLStyles(tto: TimedTextObject) {
		var style = Style("white")
		style.color = Style.getRGBValue("name", "white")
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

		style = Style("black")
		style.color = Style.getRGBValue("name", "black")
		tto.styling[style.iD] = style

		style = Style("blackU", style)
		style.underline = true
		tto.styling[style.iD] = style

		style = Style("blackUI", style)
		style.italic = true
		tto.styling[style.iD] = style

		style = Style("blackI", style)
		style.underline = false
		tto.styling[style.iD] = style
	}
}
