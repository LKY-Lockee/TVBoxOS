package com.github.tvbox.osc.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseLazyFragment
import com.github.tvbox.osc.base.ToolbarMenuProvider
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.cache.RoomDataManger.deleteVodRecordAll
import com.github.tvbox.osc.cache.RoomDataManger.getAllVodRecord
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.activity.HomeActivity
import com.github.tvbox.osc.ui.adapter.HistoryAdapter
import com.github.tvbox.osc.ui.tv.widget.AutoFitGridLayoutManager
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * @author pj567
 * @date :2021/1/7
 * @description:
 */
class HistoryFragment : BaseLazyFragment(), ToolbarMenuProvider {
	private var mSwipe: SwipeRefreshLayout? = null

	override val layoutResID: Int
		// --- BaseLazyFragment ---
		get() = R.layout.fragment_grid

	override fun init() {
		initView()
		initData()
	}

	// ----------------
	// --- Fragment ---
	override fun onDestroy() {
		super.onDestroy()
		EventBus.getDefault().unregister(this)
	}

	override val menuResId: Int
		// ----------------
		get() = R.menu.history_fragment_menu

	override val toolbarTitle: String
		get() = "历史记录"

	override fun onMenuItemClick(itemId: Int): Boolean {
		if (itemId == R.id.action_clear) {
			showClearDialog()
			return true
		}
		return false
	}

	// ----------------
	private fun initView() {
		EventBus.getDefault().register(this)

		mSwipe = rootView?.findViewById(R.id.mSwipe)
		val mGridView = rootView?.findViewById<RecyclerView>(R.id.mGridView)
		mGridView?.setLayoutManager(AutoFitGridLayoutManager(mContext, 150))
		historyAdapter = HistoryAdapter()
		mGridView?.setAdapter(historyAdapter)

		setLoadSir2(mGridView)

		mSwipe?.setOnRefreshListener { this.initData() }
		mSwipe?.setOnChildScrollUpCallback { parent: SwipeRefreshLayout?, child: View? -> mGridView?.canScrollVertically(-1) == true }

		historyAdapter?.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			FastClickCheckUtil.check(requireView())
			if (position == -1) return@setOnItemClickListener
			val vodInfo = historyAdapter?.data[position]
			if (vodInfo != null) {
				val bundle = Bundle()
				bundle.putString("id", vodInfo.id)
				bundle.putString("sourceKey", vodInfo.sourceKey)
				val sourceBean: SourceBean? = ApiConfig.instance.getSource(vodInfo.sourceKey)
				if (sourceBean != null) {
					bundle.putString("picture", vodInfo.pic)
					jumpActivity(DetailActivity::class.java, bundle)
				} else {
					(mActivity as? HomeActivity)?.switchToSearchAndSearch(vodInfo.name)
				}
			}
		}
		historyAdapter?.setOnItemLongClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			val vodInfo = historyAdapter?.data[position]
			if (vodInfo != null) {
				showDeleteHistoryItemDialog(vodInfo, position)
			}
			true
		}
	}

	private fun initData() {
		val allVodRecord = getAllVodRecord(100)
		val vodInfoList: MutableList<VodInfo?> = ArrayList()
		for (vodInfo in allVodRecord) {
			if (!vodInfo.playNote.isEmpty()) vodInfo.note = "上次看到" + vodInfo.playNote
			vodInfoList.add(vodInfo)
		}
		historyAdapter?.setNewData(vodInfoList)

		if (vodInfoList.isEmpty()) {
			showEmpty()
		} else {
			showSuccess()
		}

		mSwipe?.isRefreshing = false
	}

	private fun showDeleteHistoryItemDialog(vodInfo: VodInfo?, position: Int) {
		if (activity == null || vodInfo == null) return

		MaterialAlertDialogBuilder(requireActivity())
			.setTitle("删除历史记录")
			.setMessage("确定要删除「" + vodInfo.name + "」的观看记录吗？")
			.setPositiveButton("删除") { dialog: DialogInterface?, which: Int ->
				historyAdapter?.remove(position)
				RoomDataManger.deleteVodRecord(vodInfo.sourceKey ?: return@setPositiveButton, vodInfo)
				if (historyAdapter?.data?.isEmpty() == true) {
					showEmpty()
				}
				Toast.makeText(mContext, "已删除", Toast.LENGTH_SHORT).show()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showClearDialog() {
		if (activity == null) return

		if (historyAdapter?.data?.isEmpty() == true) {
			Toast.makeText(mContext, "暂无历史记录", Toast.LENGTH_SHORT).show()
			return
		}

		MaterialAlertDialogBuilder(requireActivity())
			.setTitle("清空历史记录")
			.setMessage("确定要清空所有观看记录吗？")
			.setPositiveButton("清空") { dialog: DialogInterface?, which: Int ->
				deleteVodRecordAll()
				historyAdapter?.setNewData(ArrayList<VodInfo?>())
				showEmpty()
				Toast.makeText(mContext, "已清空历史记录", Toast.LENGTH_SHORT).show()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	fun refresh(event: RefreshEvent) {
		if (event.type == RefreshEvent.TYPE_HISTORY_REFRESH) {
			initData()
		}
	}

	companion object {
		var historyAdapter: HistoryAdapter? = null
	}
}
