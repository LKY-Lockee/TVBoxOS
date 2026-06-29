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

import com.github.tvbox.osc.subtitle.model.Subtitle
import xyz.doikki.videoplayer.player.AbstractPlayer

/**
 * @author AveryZhong.
 */
interface SubtitleEngine {
	/**
	 * 设置字幕路径，加载字幕
	 * 
	 * @param path 字幕路径（本地路径或者是远程路径）
	 */
	fun setSubtitlePath(path: String)

	/**
	 * 字幕延时
	 */
	fun setSubtitleDelay(milliseconds: Int)

	var playSubtitleCacheKey: String?

	/**
	 * 开启字幕刷新任务
	 */
	fun start()

	/**
	 * 暂停
	 */
	fun pause()

	/**
	 * 恢复
	 */
	fun resume()

	/**
	 * 停止字幕刷新任务
	 */
	fun stop()

	/**
	 * 重置
	 */
	fun reset()

	/**
	 * 销毁字幕
	 */
	fun destroy()

	/**
	 * 绑定AbstractPlayer
	 */
	fun bindToMediaPlayer(mediaPlayer: AbstractPlayer)

	/**
	 * 设置字幕准备完成监接口
	 */
	fun setOnSubtitlePreparedListener(listener: OnSubtitlePreparedListener)

	/**
	 * 设置字幕改变监听接口
	 */
	fun setOnSubtitleChangeListener(listener: OnSubtitleChangeListener)

	/**
	 * 幕准备完成监接口
	 */
	interface OnSubtitlePreparedListener {
		fun onSubtitlePrepared(subtitles: List<Subtitle>?)
	}

	/**
	 * 字幕改变监听接口
	 */
	interface OnSubtitleChangeListener {
		fun onSubtitleChanged(subtitle: Subtitle?)
	}
}
