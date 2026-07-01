package com.github.tvbox.osc.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.tvbox.osc.R
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter.SelectViewHolder

class SelectDialogAdapter<T>(
	private val dialogInterface: SelectDialogInterface<T>,
	diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, SelectViewHolder>(diffCallback) {
	private val data = ArrayList<T>()
	private var select = 0

	fun setData(newData: MutableList<T>, defaultSelect: Int) {
		data.clear()
		data.addAll(newData)
		select = defaultSelect
		notifyDataSetChanged()
	}

	override fun getItemCount(): Int {
		return data.size
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
		return SelectViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_dialog_select, parent, false))
	}

	override fun onBindViewHolder(holder: SelectViewHolder, @SuppressLint("RecyclerView") position: Int) {
		val value = data[position]
		val name = dialogInterface.getDisplay(value)
		val view = holder.itemView.findViewById<TextView>(R.id.tvName)
		if (position == select) {
			view.setTextColor(-0xfd071f)
			view.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
		} else {
			view.setTextColor(Color.WHITE)
			view.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
		}
		view.text = name
		holder.itemView.setOnClickListener { v: View? ->
			if (position == select) return@setOnClickListener
			notifyItemChanged(select)
			select = position
			notifyItemChanged(select)
			dialogInterface.click(value, position)
		}
	}

	interface SelectDialogInterface<T> {
		fun click(value: T?, pos: Int)

		fun getDisplay(`val`: T?): String?
	}

	class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

	companion object {
		var stringDiff: DiffUtil.ItemCallback<String?> = object : DiffUtil.ItemCallback<String?>() {
			override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
				return oldItem == newItem
			}

			override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
				return oldItem == newItem
			}
		}
	}
}
