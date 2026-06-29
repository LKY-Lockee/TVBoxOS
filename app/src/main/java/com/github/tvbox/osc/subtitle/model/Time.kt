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

class Time {
	// in an integer we can store 24 days worth of milliseconds, no need for a long
	var mSeconds: Int = 0

	/**
	 * Constructor to create a time object.
	 * 
	 * @param format Supported formats: "hh:mm:ss,ms", "h:mm:ss.cs" and "h:m:s:f/fps"
	 * @param value  String in the correct format
	 */
	constructor(format: String, value: String) {
		when {
			format.equals("hh:mm:ss,ms", ignoreCase = true) -> {
				// this type of format:  01:02:22,501 (used in .SRT)
				val h = value.substring(0, 2).toInt()
				val m = value.substring(3, 5).toInt()
				val s = value.substring(6, 8).toInt()
				val ms = value.substring(9, 12).toInt()
				mSeconds = ms + s * 1000 + m * 60000 + h * 3600000
			}

			format.equals("h:mm:ss.cs", ignoreCase = true) -> {
				// this type of format:  1:02:22.51 (used in .ASS/.SSA) 
				val h = value.substring(0, 1).toInt()
				val m = value.substring(2, 4).toInt()
				val s = value.substring(5, 7).toInt()
				val cs = value.substring(8, 10).toInt()
				mSeconds = cs * 10 + s * 1000 + m * 60000 + h * 3600000
			}

			format.equals("h:m:s:f/fps", ignoreCase = true) -> {
				val parts = value.split("/")
				val fps = parts[1].toFloat()
				val timeParts = parts[0].split(":")
				val h = timeParts[0].toInt()
				val m = timeParts[1].toInt()
				val s = timeParts[2].toInt()
				val f = timeParts[3].toInt()
				mSeconds = (f * 1000 / fps).toInt() + s * 1000 + m * 60000 + h * 3600000
			}
		}
	}

	/**
	 * Method to return a formatted value of the time stored
	 * 
	 * @param format Supported formats: "hh:mm:ss,ms", "h:mm:ss.cs" and "hhmmssff/fps"
	 * @return Formatted time in a string
	 */
	fun getTime(format: String): String {
		// we use string builder for efficiency
		val time = StringBuilder()
		when {
			format.equals("hh:mm:ss,ms", ignoreCase = true) -> {
				// this type of format: 01:02:22,501 (used in .SRT)
				val h = mSeconds / 3600000
				val m = (mSeconds / 60000) % 60
				val s = (mSeconds / 1000) % 60
				val ms = mSeconds % 1000
				time.append(h.toString().padStart(2, '0'))
				time.append(':')
				time.append(m.toString().padStart(2, '0'))
				time.append(':')
				time.append(s.toString().padStart(2, '0'))
				time.append(',')
				time.append(ms.toString().padStart(3, '0'))
			}

			format.equals("h:mm:ss.cs", ignoreCase = true) -> {
				// this type of format: 1:02:22.51 (used in .ASS/.SSA)
				val h = mSeconds / 3600000
				val m = (mSeconds / 60000) % 60
				val s = (mSeconds / 1000) % 60
				val cs = (mSeconds / 10) % 100
				time.append(h.toString().padStart(2, '0'))
				time.append(':')
				time.append(m.toString().padStart(2, '0'))
				time.append(':')
				time.append(s.toString().padStart(2, '0'))
				time.append('.')
				time.append(cs.toString().padStart(2, '0'))
			}

			format.startsWith("hhmmssff/") -> {
				val fps = format.split("/")[1].toFloat()
				// now we concatenate time
				// this format is used in EBU's STL
				val h = mSeconds / 3600000
				val m = (mSeconds / 60000) % 60
				val s = (mSeconds / 1000) % 60
				val f = (mSeconds % 1000) * fps.toInt() / 1000
				time.append(h.toString().padStart(2, '0'))
				time.append(m.toString().padStart(2, '0'))
				time.append(s.toString().padStart(2, '0'))
				time.append(f.toString().padStart(2, '0'))
			}

			format.startsWith("h:m:s:f/") -> {
				val fps = format.split("/")[1].toFloat()
				// now we concatenate time
				// this format is used in EBU's STL
				val h = mSeconds / 3600000
				val m = (mSeconds / 60000) % 60
				val s = (mSeconds / 1000) % 60
				val f = (mSeconds % 1000) * fps.toInt() / 1000
				time.append(h.toString())
				time.append(':')
				time.append(m.toString())
				time.append(':')
				time.append(s.toString())
				time.append(':')
				time.append(f.toString())
			}

			format.startsWith("hh:mm:ss:ff/") -> {
				val fps = format.split("/")[1].toFloat()
				// now we concatenate time
				// this format is used in SCC
				val h = mSeconds / 3600000
				val m = (mSeconds / 60000) % 60
				val s = (mSeconds / 1000) % 60
				val f = (mSeconds % 1000) * fps.toInt() / 1000
				time.append(h.toString().padStart(2, '0'))
				time.append(':')
				time.append(m.toString().padStart(2, '0'))
				time.append(':')
				time.append(s.toString().padStart(2, '0'))
				time.append(':')
				time.append(f.toString().padStart(2, '0'))
			}
		}
		return time.toString()
	}
}
