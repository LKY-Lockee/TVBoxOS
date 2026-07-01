package com.github.tvbox.osc.bean

/**
 * 设置项数据模型 - Material 3风格
 */
class SettingItem(val type: Int, val title: String) {
	var summary: String? = null
	var value: String? = null
	var switchState = false
	var onClickListener: OnClickListener? = null

	fun interface OnClickListener {
		fun onClick(item: SettingItem)
	}

	companion object {
		/**
		 * 分类标题
		 */
		const val TYPE_CATEGORY: Int = 0

		/**
		 * 普通设置项
		 */
		const val TYPE_PREFERENCE: Int = 1

		/**
		 * 开关设置项
		 */
		const val TYPE_SWITCH: Int = 2

		fun createCategory(title: String): SettingItem {
			return SettingItem(TYPE_CATEGORY, title)
		}

		fun createPreference(title: String, value: String?, listener: OnClickListener?): SettingItem {
			return SettingItem(TYPE_PREFERENCE, title).apply {
				this.value = value
				this.onClickListener = listener
			}
		}

		fun createSwitch(title: String, checked: Boolean, listener: OnClickListener?): SettingItem {
			return SettingItem(TYPE_SWITCH, title).apply {
				this.switchState = checked
				this.onClickListener = listener
			}
		}
	}
}
