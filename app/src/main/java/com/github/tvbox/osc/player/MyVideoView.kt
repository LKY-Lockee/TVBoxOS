package com.github.tvbox.osc.player

import android.content.Context
import android.util.AttributeSet
import xyz.doikki.videoplayer.player.AbstractPlayer
import xyz.doikki.videoplayer.player.VideoView

class MyVideoView : VideoView {
	val mediaPlayer: AbstractPlayer?
		get() = mMediaPlayer

	constructor(context: Context) : super(context, null)

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0)

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
}
