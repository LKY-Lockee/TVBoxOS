package com.github.tvbox.osc.util.js

import android.text.TextUtils
import com.github.tvbox.osc.util.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.net.MalformedURLException
import java.net.URL
import java.util.Locale
import java.util.regex.Pattern

object HtmlParser {
	private val p: Pattern = Pattern.compile("url\\((.*?)\\)", Pattern.MULTILINE or Pattern.DOTALL)
	private val NOADD_INDEX: Pattern = Pattern.compile(":eq|:lt|:gt|:first|:last|^body$|^#") // 不自动加eq下标索引
	private val URLJOIN_ATTR: Pattern = Pattern.compile("(url|src|href|-original|-src|-play|-url|style)$", Pattern.MULTILINE or Pattern.CASE_INSENSITIVE) // 需要自动urljoin的属性
	private val SPECIAL_URL: Pattern = Pattern.compile("^(ftp|magnet|thunder|ws):", Pattern.MULTILINE or Pattern.CASE_INSENSITIVE) // 过滤特殊链接,不走urlJoin
	private var pdfh_html = ""
	private var pdfa_html = ""
	private var pdfh_doc: Document? = null
	private var pdfa_doc: Document? = null

	fun joinUrl(parent: String?, child: String?): String? {
		if (StringUtils.isEmpty(parent)) {
			return child
		}

		val url: URL?
		var q = parent
		try {
			url = URL(URL(parent), child)
			q = url.toExternalForm()
		} catch (e: MalformedURLException) {
			e.printStackTrace()
		}
		//        if (q.contains("#")) {
		//            q = q.replaceAll("^(.+?)#.*?$", "$1");
		//        }
		return q
	}

	/**
	 * 根据传入的单规则获取 parse规则，索引位置,排除列表 -- 可以用于剔除元素,支持多个，按标签剔除，按id剔除等操作
	 */
	private fun getParseInfo(nParse: String): ParseInfo {

		val parseInfo = ParseInfo()
		parseInfo.nParseRule = nParse //定义规则默认值为本身
		if (nParse.contains(":eq")) {
			parseInfo.nParseRule = nParse.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
			var nParsePos = nParse.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]

			if (parseInfo.nParseRule.contains("--")) {
				val rules = parseInfo.nParseRule.split("--".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				val excludes = ArrayList(listOf(*rules))
				parseInfo.excludes = excludes
				excludes.removeAt(0)
				parseInfo.nParseRule = rules[0]
			} else if (nParsePos.contains("--")) {
				val rules = nParsePos.split("--".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				val excludes = ArrayList(listOf(*rules))
				parseInfo.excludes = excludes
				excludes.removeAt(0)
				nParsePos = rules[0]
			}

			try {
				parseInfo.nParseIndex = nParsePos.replace("eq(", "").replace(")", "").toInt()
			} catch (e1: Exception) {
				parseInfo.nParseIndex = 0
			}
		} else {
			if (nParse.contains("--")) {
				val rules = parseInfo.nParseRule.split("--".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				val excludes = ArrayList(listOf(*rules))
				parseInfo.excludes = excludes
				excludes.removeAt(0)
				parseInfo.nParseRule = rules[0]
			}
		}
		return parseInfo
	}

	fun isIndex(str: String?): Boolean {
		if (StringUtils.isEmpty(str)) {
			return false
		}
		val s = str ?: return false
		for (str2 in arrayOf(":eq", ":lt", ":gt", ":first", ":last", "body", "#")) {
			if (s.contains(str2)) {
				if (str2 == "body" || str2 == "#") {
					return s.startsWith(str2)
				}
				return true
			}
		}
		return false
	}

	fun isUrl(str: String?): Boolean {
		if (StringUtils.isEmpty(str)) {
			return false
		}
		val s = str ?: return false
		for (str2 in arrayOf("url", "src", "href", "-original", "-play")) {
			if (s.contains(str2)) {
				return true
			}
		}
		return false
	}

	private fun parseHikerToJq(parse: String, first: Boolean): String {
		/*
         海阔解析表达式转原生表达式,自动补eq,如果传了first就最后一个也取eq(0)
        :param parse:
        :param first:
        :return:
        */
		// 不自动加eq下标索引
		var parse = parse
		if (parse.contains("&&")) {
			val parses = parse.split("&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() //带&&的重新拼接
			val newParses: MutableList<String?> = ArrayList() //构造新的解析表达式列表
			for (i in parses.indices) {
				val pss = parses[i].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				val ps = pss[pss.size - 1] //如果分割&&后带空格就取最后一个元素
				val m = NOADD_INDEX.matcher(ps)
				//if (!isIndex(ps)) {
				if (!m.find()) {
					if (!first && i >= parses.size - 1) { //不传first且遇到最后一个,不用补eq(0)
						newParses.add(parses[i])
					} else {
						newParses.add(parses[i] + ":eq(0)")
					}
				} else {
					newParses.add(parses[i])
				}
			}
			parse = TextUtils.join(" ", newParses)
		} else {
			val pss = parse.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			val ps = pss[pss.size - 1] //如果分割&&后带空格就取最后一个元素
			val m = NOADD_INDEX.matcher(ps)
			//if (!isIndex(ps) && first) {
			if (!m.find() && first) {
				parse = "$parse:eq(0)"
			}
		}
		return parse
	}

	fun parseDomForUrl(html: String, rule: String, addUrl: String?): String? {
		var rule = rule
		if (pdfh_html != html) {
			pdfh_html = html
			pdfh_doc = Jsoup.parse(html)
		}
		val doc = pdfh_doc
		if (rule == "body&&Text" || rule == "Text") {
			return (doc ?: return null).text()
		} else if (rule == "body&&Html" || rule == "Html") {
			return (doc ?: return null).html()
		}
		var option: String? = ""
		if (rule.contains("&&")) {
			val rs = rule.split("&&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			option = rs[rs.size - 1]
			val excludes: MutableList<String> = ArrayList(listOf(*rs))
			excludes.removeAt(rs.size - 1)
			rule = TextUtils.join("&&", excludes)
		}
		rule = parseHikerToJq(rule, true)
		val parses = rule.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		var ret = Elements()
		for (nParse in parses) {
			ret = parseOneRule(doc ?: return null, nParse, ret)
			if (ret.isEmpty()) {
				return ""
			}
		}
		var result: String?
		if (StringUtils.isNotEmpty(option)) {
			if (option == "Text") {
				result = ret.text()
			} else if (option == "Html") {
				result = ret.html()
			} else {
				val opt = option ?: return null
				result = ret.attr(opt)
				if (opt.lowercase(Locale.getDefault())
						.contains("style") && result.contains("url(")
				) {
					val m = p.matcher(result)
					if (m.find()) {
						result = m.group(1)
					}
					if (StringUtils.isNotEmpty(result)) {
						result = result.replace("^['|\"](.*)['|\"]$".toRegex(), "$1")
					}
				}
				if (StringUtils.isNotEmpty(result) && StringUtils.isNotEmpty(addUrl)) {
					// 需要自动urljoin的属性
					val m = URLJOIN_ATTR.matcher(opt)
					val n = SPECIAL_URL.matcher(result)
					//if (isUrl(opt)) {
					if (m.find() && !n.find()) {
						result = if (result.contains("http")) {
							result.substring(result.indexOf("http"))
						} else {
							joinUrl(addUrl, result)
						}
					}
				}
			}
		} else {
			result = ret.outerHtml()
		}
		return result
	}

	fun parseDomForArray(html: String, rule: String): List<String> {
		var rule = rule
		if (pdfa_html != html) {
			pdfa_html = html
			pdfa_doc = Jsoup.parse(html)
		}
		val doc = pdfa_doc ?: return emptyList()
		rule = parseHikerToJq(rule, false)
		val parses = rule.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		var ret = Elements()
		for (pars in parses) {
			ret = parseOneRule(doc, pars, ret)
			if (ret.isEmpty()) {
				return emptyList()
			}
		}

		val eleHtml: MutableList<String> = ArrayList()
		for (i in ret.indices) {
			val element1 = ret[i]
			eleHtml.add(element1.outerHtml())
		}
		return eleHtml
	}

	private fun parseOneRule(doc: Document, nParse: String, ret: Elements): Elements {
		var ret = ret
		val parseInfo = getParseInfo(nParse)
		ret = if (ret.isEmpty()) {
			doc.select(parseInfo.nParseRule)
		} else {
			ret.select(parseInfo.nParseRule)
		}

		if (nParse.contains(":eq")) {
			ret = if (parseInfo.nParseIndex < 0) {
				ret.eq(ret.size + parseInfo.nParseIndex)
			} else {
				ret.eq(parseInfo.nParseIndex)
			}
		}

		val excludes = parseInfo.excludes
		if (excludes != null && !ret.isEmpty()) {
			ret = ret.clone() //克隆一个, 免得直接remove会影响doc的缓存
			for (i in excludes.indices) {
				ret.select(excludes[i])
					.remove()
			}
		}
		return ret
	}

	fun parseDomForList(html: String, p1: String, listText: String, listUrl: String, addUrl: String?): List<String> {
		var p1 = p1
		if (pdfa_html != html) {
			pdfa_html = html
			pdfa_doc = Jsoup.parse(html)
		}
		val doc = pdfa_doc ?: return emptyList()
		p1 = parseHikerToJq(p1, false)
		val parses = p1.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		var ret = Elements()
		for (pars in parses) {
			ret = parseOneRule(doc, pars, ret)
			if (ret.isEmpty()) {
				return emptyList()
			}
		}
		val newVodList: MutableList<String> = ArrayList()
		for (i in ret.indices) {
			val it = ret[i]
				.outerHtml()
			newVodList.add(
				(parseDomForUrl(it, listText, "").orEmpty())
					.trim { it <= ' ' } + '$' + parseDomForUrl(it, listUrl, addUrl))
		}
		return newVodList
	}

	private class ParseInfo {
		var nParseRule: String = ""
		var nParseIndex: Int = 0
		var excludes: MutableList<String>? = null
	}
}
