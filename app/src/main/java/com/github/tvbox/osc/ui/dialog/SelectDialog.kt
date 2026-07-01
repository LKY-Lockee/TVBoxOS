package com.github.tvbox.osc.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import com.github.tvbox.osc.R
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter.SelectDialogInterface
import com.owen.tvrecyclerview.widget.TvRecyclerView

class SelectDialog<T> : BaseDialog {
	constructor(context: Context) : super(context) {
		setContentView(R.layout.dialog_select)
	}

	constructor(context: Context, resId: Int) : super(context) {
		setContentView(resId)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
	}

	fun setTip(tip: String?) {
		(findViewById<View?>(R.id.title) as TextView).text = tip
	}

	fun setAdapter(
		sourceBeanSelectDialogInterface: SelectDialogInterface<T?>,
		sourceBeanItemCallback: DiffUtil.ItemCallback<T?>,
		data: MutableList<T?>, select: Int
	) {
		val adapter = SelectDialogAdapter(sourceBeanSelectDialogInterface, sourceBeanItemCallback)
		adapter.setData(data, select)
		val tvRecyclerView = findViewById<TvRecyclerView>(R.id.list)
		tvRecyclerView.adapter = adapter
		tvRecyclerView.selectedPosition = select
		if (select < 10) {
			tvRecyclerView.setSelection(select)
		}
		tvRecyclerView.post {
			if (select >= 10) {
				tvRecyclerView.smoothScrollToPosition(select)
				tvRecyclerView.setSelectionWithSmooth(select)
			}
		}
	}
}
