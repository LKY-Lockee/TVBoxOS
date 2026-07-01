package com.github.tvbox.osc.ui.dialog

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.github.tvbox.osc.R
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.SubtitleHelper
import com.github.tvbox.osc.util.SubtitleHelper.setTextSize
import com.github.tvbox.osc.util.SubtitleHelper.timeDelay

class SubtitleDialog(context: Context) : BaseDialog(context) {
	var selectInternal: TextView? = null
	private var subtitleSizeText: TextView? = null
	private var subtitleTimeText: TextView? = null
	private var mSearchSubtitleListener: SearchSubtitleListener? = null
	private var mLocalFileChooserListener: LocalFileChooserListener? = null
	private var mSubtitleViewListener: SubtitleViewListener? = null

	init {
		if (context is Activity) {
			setOwnerActivity(context)
		}
		setContentView(R.layout.dialog_subtitle)
		initView(context)
	}

	private fun initView(context: Context?) {
		selectInternal = findViewById(R.id.selectInternal)
		val selectLocal = findViewById<TextView>(R.id.selectLocal)
		val selectRemote = findViewById<TextView>(R.id.selectRemote)
		val subtitleSizeMinus = findViewById<TextView>(R.id.subtitleSizeMinus)
		subtitleSizeText = findViewById(R.id.subtitleSizeText)
		val subtitleSizePlus = findViewById<TextView>(R.id.subtitleSizePlus)
		val subtitleTimeMinus = findViewById<TextView>(R.id.subtitleTimeMinus)
		subtitleTimeText = findViewById(R.id.subtitleTimeText)
		val subtitleTimePlus = findViewById<TextView>(R.id.subtitleTimePlus)
		val subtitleStyleOne = findViewById<TextView>(R.id.subtitleStyleOne)
		val subtitleStyleTwo = findViewById<TextView>(R.id.subtitleStyleTwo)

		selectLocal.setOnClickListener { view: View? ->
			FastClickCheckUtil.check(view ?: return@setOnClickListener)
			dismiss()
			mLocalFileChooserListener?.openLocalFileChooserDialog()
		}

		selectRemote.setOnClickListener { view: View? ->
			FastClickCheckUtil.check(view ?: return@setOnClickListener)
			dismiss()
			mSearchSubtitleListener?.openSearchSubtitleDialog()
		}

		val size = SubtitleHelper.getTextSize(ownerActivity ?: return)
		subtitleSizeText?.text = size.toString()

		subtitleSizeMinus.setOnClickListener { view: View? ->
			val sizeStr = subtitleSizeText?.text.toString()
			var curSize = sizeStr.toInt()
			curSize -= 2
			if (curSize <= 12) {
				curSize = 12
			}
			subtitleSizeText?.text = curSize.toString()
			setTextSize(curSize)
			mSubtitleViewListener?.setTextSize(curSize)
		}
		subtitleSizePlus.setOnClickListener { view: View? ->
			val sizeStr = subtitleSizeText?.text.toString()
			var curSize = sizeStr.toInt()
			curSize += 2
			if (curSize >= 60) {
				curSize = 60
			}
			subtitleSizeText?.text = curSize.toString()
			setTextSize(curSize)
			mSubtitleViewListener?.setTextSize(curSize)
		}

		val timeDelay = timeDelay
		var timeStr = "0"
		if (timeDelay != 0) {
			val dbTimeDelay = timeDelay / 1000.0
			timeStr = dbTimeDelay.toString()
		}
		subtitleTimeText?.text = timeStr

		subtitleTimeMinus.setOnClickListener { view: View? ->
			FastClickCheckUtil.check(view ?: return@setOnClickListener)
			var timeStr2 = subtitleTimeText?.text.toString()
			var time = timeStr2.toFloat().toDouble()
			val oneceDelay = -0.5
			time += oneceDelay
			timeStr2 = if (time == 0.0) {
				"0"
			} else {
				time.toString()
			}
			subtitleTimeText?.text = timeStr2
			val mseconds = (oneceDelay * 1000).toInt()
			SubtitleHelper.timeDelay = (time * 1000).toInt()
			mSubtitleViewListener?.setSubtitleDelay(mseconds)
		}
		subtitleTimePlus.setOnClickListener { view: View? ->
			FastClickCheckUtil.check(view ?: return@setOnClickListener)
			var timeStr1 = subtitleTimeText?.text.toString()
			var time = timeStr1.toFloat().toDouble()
			val oneceDelay = 0.5
			time += oneceDelay
			timeStr1 = if (time == 0.0) {
				"0"
			} else {
				time.toString()
			}
			subtitleTimeText?.text = timeStr1
			val mseconds = (oneceDelay * 1000).toInt()
			SubtitleHelper.timeDelay = (time * 1000).toInt()
			mSubtitleViewListener?.setSubtitleDelay(mseconds)
		}
		selectInternal?.setOnClickListener { view: View? ->
			FastClickCheckUtil.check(view ?: return@setOnClickListener)
			dismiss()
			mSubtitleViewListener?.selectInternalSubtitle()
		}

		subtitleStyleOne.setOnClickListener { view: View? ->
			val style = 0
			dismiss()
			mSubtitleViewListener?.setTextStyle(style)
			Toast.makeText(getContext(), "设置样式成功", Toast.LENGTH_SHORT).show()
		}

		subtitleStyleTwo.setOnClickListener { view: View? ->
			val style = 1
			dismiss()
			mSubtitleViewListener?.setTextStyle(style)
			Toast.makeText(getContext(), "设置样式成功", Toast.LENGTH_SHORT).show()
		}
	}

	fun setLocalFileChooserListener(localFileChooserListener: LocalFileChooserListener) {
		mLocalFileChooserListener = localFileChooserListener
	}

	fun setSearchSubtitleListener(searchSubtitleListener: SearchSubtitleListener) {
		mSearchSubtitleListener = searchSubtitleListener
	}

	fun setSubtitleViewListener(subtitleViewListener: SubtitleViewListener) {
		mSubtitleViewListener = subtitleViewListener
	}

	interface LocalFileChooserListener {
		fun openLocalFileChooserDialog()
	}

	interface SearchSubtitleListener {
		fun openSearchSubtitleDialog()
	}

	interface SubtitleViewListener {
		fun setTextSize(size: Int)

		fun setSubtitleDelay(milliseconds: Int)

		fun selectInternalSubtitle()

		fun setTextStyle(style: Int)
	}
}
