package com.github.tvbox.osc.player.render

import android.content.Context
import xyz.doikki.videoplayer.render.IRenderView
import xyz.doikki.videoplayer.render.RenderViewFactory

class SurfaceRenderViewFactory : RenderViewFactory() {
	override fun createRenderView(context: Context): IRenderView {
		return SurfaceRenderView(context)
	}

	companion object {
		fun create(): SurfaceRenderViewFactory {
			return SurfaceRenderViewFactory()
		}
	}
}
