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
import com.github.tvbox.osc.cache.RoomDataManger.deleteVodCollect
import com.github.tvbox.osc.cache.RoomDataManger.deleteVodCollectAll
import com.github.tvbox.osc.cache.RoomDataManger.getAllVodCollect
import com.github.tvbox.osc.cache.VodCollect
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.activity.HomeActivity
import com.github.tvbox.osc.ui.adapter.CollectAdapter
import com.github.tvbox.osc.ui.tv.widget.AutoFitGridLayoutManager
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class CollectFragment : BaseLazyFragment(), ToolbarMenuProvider {
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
		get() = R.menu.collect_fragment_menu

	override val toolbarTitle: String
		get() = "收藏"

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
		collectAdapter = CollectAdapter()
		mGridView?.setAdapter(collectAdapter)

		setLoadSir2(mGridView)

		mSwipe?.setOnRefreshListener { this.initData() }
		mSwipe?.setOnChildScrollUpCallback { parent: SwipeRefreshLayout?, child: View? -> mGridView?.canScrollVertically(-1) == true }

		collectAdapter?.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			FastClickCheckUtil.check(requireView())
			val vodInfo: VodCollect = (collectAdapter ?: return@setOnItemClickListener).data[position]
			if (ApiConfig.instance.getSource(vodInfo.sourceKey) != null) {
				val bundle = Bundle()
				bundle.putString("id", vodInfo.vodId)
				bundle.putString("sourceKey", vodInfo.sourceKey)
				bundle.putString("picture", vodInfo.pic)
				jumpActivity(DetailActivity::class.java, bundle)
			} else {
				(mActivity as? HomeActivity)?.switchToSearchAndSearch(vodInfo.name)
			}
		}
		collectAdapter?.onItemLongClickListener = BaseQuickAdapter.OnItemLongClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			val vodCollect = collectAdapter?.data[position]
			if (vodCollect != null) {
				showDeleteCollectItemDialog(vodCollect, position)
			}
			true
		}
	}

	private fun initData() {
		val allVodRecord = getAllVodCollect()
		val vodInfoList: MutableList<VodCollect?> = ArrayList(allVodRecord)
		collectAdapter?.setNewData(vodInfoList)

		if (vodInfoList.isEmpty()) {
			showEmpty()
		} else {
			showSuccess()
		}

		mSwipe?.isRefreshing = false
	}

	private fun showDeleteCollectItemDialog(vodCollect: VodCollect?, position: Int) {
		if (activity == null || vodCollect == null) return

		MaterialAlertDialogBuilder(requireActivity())
			.setTitle("取消收藏")
			.setMessage("确定要取消收藏「" + vodCollect.name + "」吗？")
			.setPositiveButton("删除") { dialog: DialogInterface?, which: Int ->
				collectAdapter?.remove(position)
				deleteVodCollect(vodCollect.id)
				if (collectAdapter?.data?.isEmpty() == true) {
					showEmpty()
				}
				Toast.makeText(mContext, "已删除", Toast.LENGTH_SHORT).show()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showClearDialog() {
		if (activity == null) return

		if (collectAdapter?.data?.isEmpty() == true) {
			Toast.makeText(mContext, "暂无收藏", Toast.LENGTH_SHORT).show()
			return
		}

		MaterialAlertDialogBuilder(requireActivity())
			.setTitle("清空收藏")
			.setMessage("确定要清空所有收藏吗？")
			.setPositiveButton("清空") { dialog: DialogInterface?, which: Int ->
				deleteVodCollectAll()
				collectAdapter?.setNewData(ArrayList<VodCollect?>())
				showEmpty()
				Toast.makeText(mContext, "已清空收藏", Toast.LENGTH_SHORT).show()
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
		var collectAdapter: CollectAdapter? = null
	}
}
