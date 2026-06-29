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

import com.github.tvbox.osc.subtitle.model.Subtitle
import com.github.tvbox.osc.subtitle.model.Time
import com.github.tvbox.osc.subtitle.model.TimedTextObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class FormatSRT : TimedTextFileFormat {
	override fun parseFile(fileName: String, `is`: InputStream): TimedTextObject {
		val tto = TimedTextObject()
		var caption = Subtitle()
		var captionNumber = 1

		// first lets load the file
		val reader = InputStreamReader(`is`)
		val br = BufferedReader(reader)

		// the file name is saved
		tto.fileName = fileName

		var line = br.readLine()
		var lineCounter = 0
		try {
			`is`.use {
				while (line != null) {
					line = line.trim()
					lineCounter++
					// if it's a blank line, ignore it, otherwise...
					if (line.isNotEmpty()) {
						var allGood = false
						// the first thing should be an increasing number
						try {
							val num = line.toInt()
							if (num != captionNumber) throw Exception()
							else {
								captionNumber++
								allGood = true
							}
						} catch (e: Exception) {
							tto.warnings += "$captionNumber expected at line $lineCounter"
							tto.warnings += "\n skipping to next line\n\n"
						}
						if (allGood) {
							// we go to next line, here the beginning and end time should be found
							try {
								lineCounter++
								line = br.readLine().trim()
								val start = line.substring(0, 12)
								val end = line.substring(line.length - 12)
								var time = Time("hh:mm:ss,ms", start)
								caption.start = time
								time = Time("hh:mm:ss,ms", end)
								caption.end = time
							} catch (e: Exception) {
								tto.warnings += "incorrect time format at line $lineCounter"
								allGood = false
							}
						}
						if (allGood) {
							// we go to next line where the caption text starts
							lineCounter++
							line = br.readLine().trim()
							val text = StringBuilder()
							while (line.isNotEmpty()) {
								text.append(line).append("<br />")
								line = br.readLine().trim()
								lineCounter++
							}
							caption.content = text.toString()
							var key = caption.start?.mSeconds ?: 0
							// in case the key is already there, we increase it by a millisecond, since no duplicates are allowed
							while (tto.captions.containsKey(key)) key++
							if (key != caption.start?.mSeconds) tto.warnings += "caption with same start time found...\n\n"
							// we add the caption.
							tto.captions[key] = caption
						}
						// we go to next blank
						while (line.isNotEmpty()) {
							line = br.readLine().trim()
							lineCounter++
						}
						caption = Subtitle()
					}
					line = br.readLine()
				}
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

		// we will write the lines in an ArrayList,
		var index = 0
		// the minimum size of the file is 4*number of captions, so we'll take some extra space.
		val file = ArrayList<String?>(5 * tto.captions.size)
		// we iterate over our captions collection, they are ordered since they come from a TreeMap
		var captionNumber = 1

		for (current in tto.captions.values) {
			// number is written
			file.add(index++, (captionNumber++).toString())
			// time is written
			val startTime = current.start ?: return null
			val endTime = current.end ?: return null
			file.add(index++, startTime.getTime("hh:mm:ss,ms") + " --> " + endTime.getTime("hh:mm:ss,ms"))
			// offset is undone
			// text is added
			val lines = cleanTextForSRT(current)
			for (line in lines) {
				file.add(index++, line)
			}
			// we add the next blank line
			file.add(index++, "")
		}

		return file.toTypedArray()
	}

	/**
	 * This method cleans `caption.content` of XML and parses line breaks.
	 */
	private fun cleanTextForSRT(current: Subtitle): List<String> {
		val text = current.content
		// add line breaks and clean XML
		return text.split("<br />")
			.map { it.replace("<.*?>".toRegex(), "") }
	}
}
