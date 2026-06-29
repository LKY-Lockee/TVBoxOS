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

package com.github.tvbox.osc.subtitle.model

import com.github.tvbox.osc.subtitle.format.FormatASS
import com.github.tvbox.osc.subtitle.format.FormatSCC
import com.github.tvbox.osc.subtitle.format.FormatSRT
import com.github.tvbox.osc.subtitle.format.FormatSTL
import com.github.tvbox.osc.subtitle.format.FormatTTML
import java.util.TreeMap

class TimedTextObject {
	val language: String = ""

	// list of layouts (id, reference)
	val layout = HashMap<String, Region?>()

	// list of captions (begin time, reference)
	// represented by a tree map to maintain order
	val captions = TreeMap<Int, Subtitle>()

	// to know whether file should be saved as .ASS or .SSA
	val useASSInsteadOfSSA: Boolean = true

	// to delay or advance the subtitles, parsed into +/- milliseconds
	val offset: Int = 0

	// meta info
	var title: String = ""
	var description: String = ""
	var copyright: String = ""
	var author: String = ""
	var fileName: String = ""

	// list of styles (id, reference)
	var styling = HashMap<String, Style?>()

	// to store non-fatal errors produced during parsing
	var warnings: String = "List of non fatal errors produced during parsing:\n\n"

	// to know if a parsing method has been applied
	var built: Boolean = false

	/**
	 * Method to generate the .SRT file
	 * 
	 * @return An array of strings where each String represents a line
	 */
	fun toSRT(): Array<String?>? {
		return FormatSRT().toFile(this) as? Array<String?>
	}

	/**
	 * Method to generate the .ASS file
	 * 
	 * @return An array of strings where each String represents a line
	 */
	fun toASS(): Array<String?>? {
		return FormatASS().toFile(this) as? Array<String?>
	}

	/**
	 * Method to generate the .STL file
	 */
	fun toSTL(): ByteArray? {
		return FormatSTL().toFile(this) as? ByteArray
	}

	/**
	 * Method to generate the .SCC file
	 */
	fun toSCC(): Array<String?>? {
		return FormatSCC().toFile(this) as? Array<String?>
	}

	/**
	 * Method to generate the .XML file
	 */
	fun toTTML(): Array<String?>? {
		return FormatTTML().toFile(this) as? Array<String?>
	}

	/**
	 * This method simply checks the style list and eliminate any style not referenced by any caption
	 * This might come useful when default styles get created and cover too much.
	 * It requires a unique iteration through all captions.
	 */
	fun cleanUnusedStyles() {
		// here all used styles will be stored
		val usedStyles = HashMap<String, Style?>()
		// we iterate over the captions
		for (current in captions.values) {
			// new caption
			// if it has a style
			val style = current.style ?: continue
			val id = style.iD
			// if we haven't saved it yet
			if (!usedStyles.containsKey(id)) {
				usedStyles[id] = style
			}
		}
		// we saved the used styles
		this.styling = usedStyles
	}
}
