package com.github.tvbox.osc.bean

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author pj567
 * @date 2020/12/18
 */
class AbsJson : Serializable {
	var code: Int = 0 // : 1
	var limit: String? = null // : "20"
	var list: List<AbsJsonVod>? = null // : [{vod_id: 71930, type_id: 22, type_id_1: 20, group_id: 0, vod_name: "意式情歌",…},…]
	var msg: String? = null // : "提示信息"
	var page: Int = 0 // : "2"

	@SerializedName("pagecount")
	var pageCount: Int = 0 // : 209
	var total: Int = 0 // : 4166

	fun toAbsXml(): AbsXml {
		val movie = Movie()
		movie.page = page
		movie.pageCount = pageCount
		movie.pageSize = limit?.toIntOrNull() ?: 0
		movie.recordCount = total
		movie.videoList = list.orEmpty().mapNotNull { vod ->
			try {
				vod.toXmlVideo()
			} catch (_: Throwable) {
				movie.pageSize = 0
				null
			}
		}.toMutableList()

		return AbsXml().apply {
			this.movie = movie
			msg = this@AbsJson.msg
		}
	}

	class AbsJsonVod : Serializable {
		@SerializedName("group_id")
		var groupId: Int = 0 //: 0

		@SerializedName("type_id")
		var typeId: Int = 0 //: 32

		@SerializedName("type_id_1")
		var typeId1: Int = 0 //: 31

		@SerializedName("type_name")
		var typeName: String? = null //: "国产剧"

		@SerializedName("vod_actor")
		var vodActor: String? = null //: "黄小戈,赵旭东,时男,傅隽,张佳琳"

		@SerializedName("vod_area")
		var vodArea: String? = null //: "中国大陆"

		@SerializedName("vod_author")
		var vodAuthor: String? = null //: ""

		@SerializedName("vod_behind")
		var vodBehind: String? = null //: ""

		@SerializedName("vod_blurb")
		var vodBlurb: String? = null //: "本剧通过讲述咸鱼馆神秘店主灵叔以咸鱼和rose为主角虚构的十六种不同人生故事，展现了在不同故事中的咸鱼和rose犹如千千万万生活在世界上的青年男女一样，拥有着不同的性格和不同背景，怀揣着不同梦想和欲望"

		@SerializedName("vod_class")
		var vodClass: String? = null //: "剧情,爱情,科幻,悬疑,惊悚,国产剧"

		@SerializedName("vod_color")
		var vodColor: String? = null //: ""

		@SerializedName("vod_content")
		var vodContent: String = "" //: "<p>本剧通过讲述咸鱼馆神秘店主灵叔以咸鱼和rose为主角虚构的十六种不同人生故事，展现了在不同故事中的咸鱼和rose犹如千千万万生活在世界上的青年男女一样，拥有着不同的性格和不同背景，怀揣着不同梦想和欲望，在充满机遇挑战又布满荆棘的人生旅途中，积极面对人生困惑，努力走出困惑和绝境的故事。十六个小故事有神奇，有烦恼，有笑声，有感动，对爱情的向外，对亲情的追忆，对人生的徘徊，对未来的恐惧，构成了一幅咸鱼和rose生命多种可能性的美丽画卷，犹如人生百味，柴米油盐酱醋茶，酸甜苦辣咸，喜怒哀乐怨。人生总是在希望中面临困惑走向绝望，又从绝望中坚强不息走向希望，故事中咸鱼和rose心底善良，拼搏向上，于人生困境中搏出一片青天，体验了生命的各种美好，传递了人生应该坚持希望，积极向上拥抱美好的乐观精神，表达了生命不止，自强不息的内涵思想。</p>"

		@SerializedName("vod_copyright")
		var vodCopyright: String? = null //: 0

		@SerializedName("vod_director")
		var vodDirector: String? = null //: "王凯阳,Kaiyang,Wang"

		@SerializedName("vod_douban_id")
		var vodDoubanId: String? = null //: 35373052

		@SerializedName("vod_douban_score")
		var vodDoubanScore: String? = null //: "4.4"

		@SerializedName("vod_down")
		var vodDown: String? = null //: 0

		@SerializedName("vod_down_from")
		var vodDownFrom: String? = null //: ""

		@SerializedName("vod_down_note")
		var vodDownNote: String? = null //: ""

		@SerializedName("vod_down_server")
		var vodDownServer: String? = null //: ""

		@SerializedName("vod_down_url")
		var vodDownUrl: String? = null //: ""

		@SerializedName("vod_duration")
		var vodDuration: String? = null //: "10"

		@SerializedName("vod_en")
		var vodEn: String? = null //: "xianyuxianshengRosexiaojiezhihuixinglailiao"

		@SerializedName("vod_hits")
		var vodHits: String? = null //: 0

		@SerializedName("vod_hits_day")
		var vodHitsDay: String? = null //: 0

		@SerializedName("vod_hits_month")
		var vodHitsMonth: String? = null //: 0

		@SerializedName("vod_hits_week")
		var vodHitsWeek: String? = null //: 0

		@SerializedName("vod_id")
		var vodId: String = "" //: 71989

		@SerializedName("vod_isend")
		var vodIsEnd: String? = null //: 0

		@SerializedName("vod_jumpurl")
		var vodJumpUrl: String? = null //: ""

		@SerializedName("vod_lang")
		var vodLang: String? = null //: "汉语普通话"

		@SerializedName("vod_letter")
		var vodLetter: String? = null //: "X"

		@SerializedName("vod_level")
		var vodLevel: String? = null //: 0

		@SerializedName("vod_lock")
		var vodLock: String? = null //: 0

		@SerializedName("vod_name")
		var vodName: String? = null //: "咸鱼先生，Rose小姐之彗星来了"

		@SerializedName("vod_pic")
		var vodPic: String? = null //: "https://img.52swat.cn/upload/vod/20210410-1/c8a9342fff893c85e4a255da90fdbe3f.jpg"

		@SerializedName("vod_pic_screenshot")
		var vodPicScreenshot: String? = null //: null

		@SerializedName("vod_pic_slide")
		var vodPicSlide: String? = null //: ""

		@SerializedName("vod_pic_thumb")
		var vodPicThumb: String? = null //: ""

		@SerializedName("vod_play_from")
		var vodPlayFrom: String? = null //: "dbyun$$$dbm3u8"

		@SerializedName("vod_play_note")
		var vodPlayNote: String? = null //: "$$$"

		@SerializedName("vod_play_server")
		var vodPlayServer: String? = null //: "no$$$no"

		@SerializedName("vod_play_url")
		var vodPlayUrl: String? =
			null //: "第01集$https://vod3.buycar5.cn/share/dHsXTOBwbaX4idZb#第02集$https://vod3.buycar5.cn/share/qTlFmVkS3ABl7F4v#第03集$https://vod3.buycar5.cn/share/uNAQVhnro4Xnx4Y1#第04集$https://vod3.buycar5.cn/share/EtGK2XPmuzygMFmE#第05集$https://vod3.buycar5.cn/share/MC1U1bcQrGgVxF6h#第06集$https://vod3.buycar5.cn/share/gEtYSq6IX9KWPykl#第07集$https://vod3.buycar5.cn/share/OEMBq5ujsPaq8Sy7#第08集$https://vod3.buycar5.cn/share/bynmQTMBQwsVHtkn#第09集$https://vod3.buycar5.cn/share/Th7aQDVPOT1p6Cib#第10集$https://vod3.buycar5.cn/share/8AaZzRvh3fFk43Mi#第11集$https://vod3.buycar5.cn/share/YzEk819PQphuqDgL#第12集$https://vod3.buycar5.cn/share/vdAGJhlSg0o1yzcA$$$第01集$https://vod3.buycar5.cn/20210410/iWay2ycC/index.m3u8#第02集$https://vod3.buycar5.cn/20210410/5DpcrSCA/index.m3u8#第03集$https://vod3.buycar5.cn/20210410/wVdGBPgj/index.m3u8#第04集$https://vod3.buycar5.cn/20210410/cUVpM93e/index.m3u8#第05集$https://vod3.buycar5.cn/20210410/NWALmXkH/index.m3u8#第06集$https://vod3.buycar5.cn/20210410/lXZKFL7d/index.m3u8#第07集$https://vod3.buycar5.cn/20210411/3gQEOdxL/index.m3u8#第08集$https://vod3.buycar5.cn/20210411/yMLR7Fsz/index.m3u8#第09集$https://vod3.buycar5.cn/20210411/vMtFz4in/index.m3u8#第10集$https://vod3.buycar5.cn/20210412/EOwKfgwt/index.m3u8#第11集$https://vod3.buycar5.cn/20210412/xRT9FEjR/index.m3u8#第12集$https://vod3.buycar5.cn/20210412/Q6krcXYC/index.m3u8"

		@SerializedName("vod_plot")
		var vodPlot: String? = null //: 0

		@SerializedName("vod_plot_detail")
		var vodPlotDetail: String? = null //: ""

		@SerializedName("vod_plot_name")
		var vodPlotName: String? = null //: ""

		@SerializedName("vod_points")
		var vodPoints: String? = null //: 0

		@SerializedName("vod_points_down")
		var vodPointsDown: String? = null //: 0

		@SerializedName("vod_points_play")
		var vodPointsPlay: String? = null //: 0

		@SerializedName("vod_pubdate")
		var vodPubDate: String? = null //: "2021-04-10(中国大陆)"

		@SerializedName("vod_pwd")
		var vodPwd: String? = null //: ""

		@SerializedName("vod_pwd_down")
		var vodPwdDown: String? = null //: ""

		@SerializedName("vod_pwd_down_url")
		var vodPwdDownUrl: String? = null //: ""

		@SerializedName("vod_pwd_play")
		var vodPwdPlay: String? = null //: ""

		@SerializedName("vod_pwd_play_url")
		var vodPwdPlayUrl: String? = null //: ""

		@SerializedName("vod_pwd_url")
		var vodPwdUrl: String? = null //: ""

		@SerializedName("vod_rel_art")
		var vodRelArt: String? = null //: ""

		@SerializedName("vod_rel_vod")
		var vodRelVod: String? = null //: ""

		@SerializedName("vod_remarks")
		var vodRemarks: String? = null //: "共30集,更新至12集"

		@SerializedName("vod_reurl")
		var vodReUrl: String? = null //: ""

		@SerializedName("vod_score")
		var vodScore: String? = null //: "4.4"

		@SerializedName("vod_score_all")
		var vodScoreAll: String? = null //: 460

		@SerializedName("vod_score_num")
		var vodScoreNum: String? = null //: 291

		@SerializedName("vod_serial")
		var vodSerial: String? = null //: "12"

		@SerializedName("vod_state")
		var vodState: String? = null //: ""

		@SerializedName("vod_status")
		var vodStatus: String? = null //: 1

		@SerializedName("vod_sub")
		var vodSub: String? = null //: "Mr.Salted Fish Miss Ross 2,咸鱼先生，Rose小姐 第二季,咸鱼先生，Rose小姐之彗星来了"

		@SerializedName("vod_tag")
		var vodTag: String? = null //: ""

		@SerializedName("vod_time")
		var vodTime: String? = null //: "2021-04-12 19:13:27"

		@SerializedName("vod_time_add")
		var vodTimeAdd: String? = null //: 1618053726

		@SerializedName("vod_time_hits")
		var vodTimeHits: String? = null //: 0

		@SerializedName("vod_time_make")
		var vodTimeMake: String? = null //: 0

		@SerializedName("vod_total")
		var vodTotal: String? = null //: 30

		@SerializedName("vod_tpl")
		var vodTpl: String? = null //: ""

		@SerializedName("vod_tpl_down")
		var vodTplDown: String? = null //: ""

		@SerializedName("vod_tpl_play")
		var vodTplPlay: String? = null //: ""

		@SerializedName("vod_trysee")
		var vodTrySee: String? = null //: 0

		@SerializedName("vod_tv")
		var vodTv: String? = null //: ""

		@SerializedName("vod_up")
		var vodUp: String? = null //: 0

		@SerializedName("vod_version")
		var vodVersion: String? = null //: ""

		@SerializedName("vod_weekday")
		var vodWeekday: String? = null //: ""

		@SerializedName("vod_writer")
		var vodWriter: String? = null //: "周炎青,刘恒,支雅雪,孙露军,李璐,王梦璇"

		@SerializedName("vod_year")
		var vodYear: String? = null //: "2021"

		fun toXmlVideo(): Movie.Video {
			val video = Movie.Video()
			video.tag = vodTag
			video.last = vodTime
			video.id = vodId
			video.tid = typeId
			video.name = vodName
			video.type = typeName
			video.pic = vodPic
			video.lang = vodLang
			video.area = vodArea
			video.year = vodYear?.toIntOrNull() ?: 0
			video.state = vodState
			video.note = vodRemarks
			video.actor = vodActor
			video.director = vodDirector

			val urlBean = Movie.Video.UrlBean()
			val playFrom = vodPlayFrom
			val playUrl = vodPlayUrl
			if (playFrom != null && playUrl != null) {
				val playFlags = playFrom.split("\\$\\$\\$".toRegex()).dropLastWhile { it.isEmpty() }
				val playUrls = playUrl.split("\\$\\$\\$".toRegex()).dropLastWhile { it.isEmpty() }
				urlBean.infoList = playFlags.mapIndexed { i, flag ->
					Movie.Video.UrlBean.UrlInfo().apply {
						this.flag = flag
						urls = playUrls.getOrElse(i) { "" }
					}
				}.toMutableList()
			}
			video.urlBean = urlBean
			video.des = vodContent // <![CDATA[权来]
			return video
		}
	}
}
