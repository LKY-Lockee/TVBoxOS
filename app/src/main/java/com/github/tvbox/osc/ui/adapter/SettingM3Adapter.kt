package com.github.tvbox.osc.ui.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.SettingItem
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Material 3 风格设置适配器
 */
class SettingM3Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
	private var items: MutableList<SettingItem> = ArrayList()

	fun setItems(items: MutableList<SettingItem>) {
		this.items = items
		notifyDataSetChanged()
	}

	fun updateItem(position: Int) {
		if (position >= 0 && position < items.size) {
			notifyItemChanged(position)
		}
	}

	override fun getItemViewType(position: Int): Int {
		return items[position].type
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		return when (viewType) {
			SettingItem.TYPE_CATEGORY -> CategoryViewHolder(inflater.inflate(R.layout.item_setting_category, parent, false))
			SettingItem.TYPE_SWITCH -> SwitchViewHolder(inflater.inflate(R.layout.item_setting_switch, parent, false))
			SettingItem.TYPE_PREFERENCE -> PreferenceViewHolder(inflater.inflate(R.layout.item_setting_preference, parent, false))
			else -> PreferenceViewHolder(inflater.inflate(R.layout.item_setting_preference, parent, false))
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]

		when (holder) {
			is CategoryViewHolder -> {
				holder.bind(item)
			}

			is PreferenceViewHolder -> {
				holder.bind(item)
			}

			is SwitchViewHolder -> {
				holder.bind(item)
			}
		}
	}

	override fun getItemCount(): Int {
		return items.size
	}

	// 分类标题 ViewHolder
	internal class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		var tvCategoryTitle: TextView = itemView.findViewById(R.id.tvCategoryTitle)

		fun bind(item: SettingItem) {
			tvCategoryTitle.text = item.title
		}
	}

	// 普通设置项 ViewHolder
	internal class PreferenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
		var tvSummary: TextView = itemView.findViewById(R.id.tvSummary)
		var tvValue: TextView = itemView.findViewById(R.id.tvValue)

		fun bind(item: SettingItem) {
			tvTitle.text = item.title

			if (!TextUtils.isEmpty(item.summary)) {
				tvSummary.visibility = View.VISIBLE
				tvSummary.text = item.summary
			} else {
				tvSummary.visibility = View.GONE
			}

			if (!TextUtils.isEmpty(item.value)) {
				tvValue.visibility = View.VISIBLE
				tvValue.text = item.value
			} else {
				tvValue.visibility = View.GONE
			}

			itemView.setOnClickListener(View.OnClickListener { v: View? ->
				if (item.onClickListener != null) {
					(item.onClickListener ?: return@OnClickListener).onClick(item)
				}
			})
		}
	}

	// 开关设置项 ViewHolder
	internal class SwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
		var tvSummary: TextView = itemView.findViewById(R.id.tvSummary)
		var switchWidget: MaterialSwitch = itemView.findViewById(R.id.switchWidget)

		fun bind(item: SettingItem) {
			tvTitle.text = item.title

			if (!TextUtils.isEmpty(item.summary)) {
				tvSummary.visibility = View.VISIBLE
				tvSummary.text = item.summary
			} else {
				tvSummary.visibility = View.GONE
			}

			switchWidget.setChecked(item.switchState)

			itemView.setOnClickListener(View.OnClickListener { v: View? ->
				val newState = !item.switchState
				item.switchState = newState
				switchWidget.setChecked(newState)
				if (item.onClickListener != null) {
					(item.onClickListener ?: return@OnClickListener).onClick(item)
				}
			})
		}
	}
}
