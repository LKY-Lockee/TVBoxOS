package com.github.tvbox.osc.player

class TrackInfo {
	private val _audio: MutableList<TrackInfoBean> = ArrayList()
	private val _subtitle: MutableList<TrackInfoBean> = ArrayList()

	val audio: List<TrackInfoBean>
		get() = _audio

	val subtitle: List<TrackInfoBean>
		get() = _subtitle

	fun getAudioSelected(track: Boolean): Int {
		return getSelected(_audio, track)
	}

	fun getSubtitleSelected(track: Boolean): Int {
		return getSelected(_subtitle, track)
	}

	private fun getSelected(list: List<TrackInfoBean>, track: Boolean): Int {
		for ((i, trackInfoBean) in list.withIndex()) {
			if (trackInfoBean.selected) return if (track) trackInfoBean.index else i
		}
		return 99999
	}

	fun addAudio(audio: TrackInfoBean) {
		_audio.add(audio)
	}

	fun addSubtitle(subtitle: TrackInfoBean) {
		_subtitle.add(subtitle)
	}
}
