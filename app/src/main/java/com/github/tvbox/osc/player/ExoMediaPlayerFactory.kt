package com.github.tvbox.osc.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import xyz.doikki.videoplayer.player.PlayerFactory

class ExoMediaPlayerFactory : PlayerFactory<ExoPlayer>() {
	@UnstableApi
	override fun createPlayer(context: Context): ExoPlayer {
		return ExoPlayer(context)
	}

	companion object {
		fun create(): ExoMediaPlayerFactory {
			return ExoMediaPlayerFactory()
		}
	}
}
