package com.github.tvbox.osc.util.urlhttp

import java.io.InputStream

/**
 * Created by fighting on 2017/4/24.
 */
class RealResponse {
	var inputStream: InputStream? = null
	var errorStream: InputStream? = null
	var code: Int = 0
	var contentLength: Long = 0
	var exception: Exception? = null
}
