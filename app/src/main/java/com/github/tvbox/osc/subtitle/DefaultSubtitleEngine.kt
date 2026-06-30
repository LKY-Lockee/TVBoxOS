/*
 *                       Copyright (C) of Avery
 *
 *                              _ooOoo_
 *                             o8888888o
 *                             88" . "88
 *                             (| -_- |)
 *                             O\  =  /O
 *                          ____/`- -'\____
 *                        .'  \\|     |//  `.
 *                       /  \\|||  :  |||//  \
 *                      /  _||||| -:- |||||-  \
 *                      |   | \\\  -  /// |   |
 *                      | \_|  ''\- -/''  |   |
 *                      \  .-\__  `-`  ___/-. /
 *                    ___`. .' /- -.- -\  `. . __
 *                 ."" '<  `.___\_<|>_/___.'  >'"".
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /
 *           ======`-.____`-.___\_____/___.-`____.-'======
 *                              `=- -='
 *           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *              Buddha bless, there will never be bug!!!
 */

package com.github.tvbox.osc.subtitle

import android.os.Handler
import android.os.HandlerThread
import android.text.TextUtils
import android.util.Log
import com.github.tvbox.osc.base.App.Companion.instance
import com.github.tvbox.osc.cache.CacheManager.save
import com.github.tvbox.osc.subtitle.SubtitleEngine.OnSubtitleChangeListener
import com.github.tvbox.osc.subtitle.SubtitleEngine.OnSubtitlePreparedListener
import com.github.tvbox.osc.subtitle.model.Subtitle
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.MD5
import com.github.tvbox.osc.util.SubtitleHelper
import xyz.doikki.videoplayer.player.AbstractPlayer
import java.io.File

/**
 * @author AveryZhong.
 */
class DefaultSubtitleEngine : SubtitleEngine {
	override var playSubtitleCacheKey: String? = null
	private var mHandlerThread: HandlerThread? = null
	private var mWorkHandler: Handler? = null
	private var mSubtitles: List<Subtitle>? = null
	private var mUIRenderTask: UIRenderTask? = null
	private var mMediaPlayer: AbstractPlayer? = null
	private var mOnSubtitlePreparedListener: OnSubtitlePreparedListener? = null
	private var mOnSubtitleChangeListener: OnSubtitleChangeListener? = null

	override fun bindToMediaPlayer(mediaPlayer: AbstractPlayer) {
		mMediaPlayer = mediaPlayer
	}

	override fun setSubtitlePath(path: String) {
		initWorkThread()
		reset()
		if (TextUtils.isEmpty(path)) {
			Log.w(TAG, "loadSubtitleFromRemote: path is null.")
			return
		}

		SubtitleLoader.loadSubtitle(path, object : SubtitleLoader.Callback {
			override fun onSuccess(subtitleLoadSuccessResult: SubtitleLoadSuccessResult?) {
				val successResult = subtitleLoadSuccessResult ?: run {
					Log.d(TAG, "onSuccess: subtitleLoadSuccessResult is null.")
					return
				}
				val timedTextObject = successResult.timedTextObject ?: run {
					Log.d(TAG, "onSuccess: timedTextObject is null.")
					return
				}
				mSubtitles = ArrayList(timedTextObject.captions.values)
				setSubtitleDelay(SubtitleHelper.timeDelay)
				notifyPrepared()

				val subtitlePath = successResult.subtitlePath
				if (subtitlePath.startsWith("http://") || subtitlePath.startsWith("https://")) {
					val subtitleFileCacheDir = instance.cacheDir.absolutePath + "/zimu/"
					val cacheDir = File(subtitleFileCacheDir)
					if (!cacheDir.exists()) {
						cacheDir.mkdirs()
					}
					val subtitleFile = subtitleFileCacheDir + successResult.fileName
					val cacheSubtitleFile = File(subtitleFile)
					val writeResult = FileUtils.writeSimple(subtitleLoadSuccessResult.content.toByteArray(), cacheSubtitleFile)
					if (writeResult && Companion.playSubtitleCacheKey != null) {
						save(MD5.string2MD5(playSubtitleCacheKey), subtitleFile)
					}
				} else {
					save(MD5.string2MD5(playSubtitleCacheKey), path)
				}
			}

			override fun onError(exception: Exception) {
				Log.e(TAG, "onError: " + exception.message)
			}
		})
	}

	override fun setSubtitleDelay(milliseconds: Int) {
		if (milliseconds == 0) {
			return
		}
		val thisSubtitles = mSubtitles ?: return
		if (thisSubtitles.isEmpty()) {
			return
		}
		mSubtitles = null
		for (subtitle in thisSubtitles) {
			val start = subtitle.start ?: return
			val end = subtitle.end ?: return
			start.mSeconds += milliseconds
			end.mSeconds += milliseconds
			if (start.mSeconds <= 0) {
				start.mSeconds = 0
			}
			if (end.mSeconds <= 0) {
				end.mSeconds = 0
			}
			subtitle.start = start
			subtitle.end = end
		}
		mSubtitles = thisSubtitles
	}

	override fun reset() {
		stop()
		mSubtitles = null
		mUIRenderTask = null
	}

	override fun start() {
		Log.d(TAG, "start: ")
		if (mMediaPlayer == null) {
			Log.w(
				TAG, ("MediaPlayer is not bind, You must bind MediaPlayer to ${SubtitleEngine::class.java.simpleName} before start() method be called, you can do this by call bindToMediaPlayer(MediaPlayer mediaPlayer) method.")
			)
			return
		}
		stop()
		mWorkHandler?.sendEmptyMessageDelayed(MSG_REFRESH, REFRESH_INTERVAL.toLong())
	}

	override fun pause() {
		stop()
	}

	override fun resume() {
		start()
	}

	override fun stop() {
		mWorkHandler?.removeMessages(MSG_REFRESH)
	}

	override fun destroy() {
		Log.d(TAG, "destroy: ")
		stopWorkThread()
		reset()
	}

	private fun initWorkThread() {
		stopWorkThread()
		mHandlerThread = HandlerThread("SubtitleFindThread").also { it.start() }
		val looper = mHandlerThread?.looper ?: return
		mWorkHandler = Handler(looper) { msg ->
			try {
				var delay = REFRESH_INTERVAL.toLong()
				val player = mMediaPlayer
				if (player != null && player.isPlaying) {
					val position = player.currentPosition
					val subtitle = SubtitleFinder.find(position, mSubtitles)
					notifyRefreshUI(subtitle)
					subtitle?.end?.let { delay = it.mSeconds - position }
				}
				mWorkHandler?.sendEmptyMessageDelayed(MSG_REFRESH, delay)
			} catch (e: Exception) {
				// ignored
			}
			true
		}
	}

	private fun stopWorkThread() {
		mHandlerThread?.quit()
		mHandlerThread = null
		mWorkHandler?.removeCallbacksAndMessages(null)
		mWorkHandler = null
	}

	private fun notifyRefreshUI(subtitle: Subtitle?) {
		val task = mUIRenderTask ?: UIRenderTask(
			mOnSubtitleChangeListener ?: return
		).also { mUIRenderTask = it }
		task.execute(subtitle)
	}

	private fun notifyPrepared() {
		mOnSubtitlePreparedListener?.onSubtitlePrepared(mSubtitles)
	}

	override fun setOnSubtitlePreparedListener(listener: OnSubtitlePreparedListener) {
		mOnSubtitlePreparedListener = listener
	}

	override fun setOnSubtitleChangeListener(listener: OnSubtitleChangeListener) {
		mOnSubtitleChangeListener = listener
	}

	companion object {
		private val TAG: String = DefaultSubtitleEngine::class.java.simpleName
		private const val MSG_REFRESH = 0x888
		private const val REFRESH_INTERVAL = 100
		private var playSubtitleCacheKey: String? = null
	}
}
