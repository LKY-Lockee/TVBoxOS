package com.github.tvbox.osc.ui.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;

import java.util.ArrayList;

public class PinyinAdapter extends BaseQuickAdapter<PinyinAdapter.SearchItem, BaseViewHolder> {
    private OnItemLongClickListener onItemLongClickListener;

    public PinyinAdapter() {
        super(R.layout.item_search_word, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, SearchItem item) {
        helper.setText(R.id.tvSearchWord, item.title);
        int iconRes = switch (item.type) {
            case 0 -> R.drawable.icon_history;
            case 1 -> R.drawable.icon_hot;
            default -> R.drawable.icon_search;
        };
        helper.setImageResource(R.id.iv_icon, iconRes);

        // 设置长按监听器（仅对历史记录生效）
        if (item.type == 0) {
            helper.itemView.setOnLongClickListener(v -> {
                if (onItemLongClickListener != null) {
                    onItemLongClickListener.onItemLongClick(helper.getLayoutPosition(), item);
                    return true;
                }
                return false;
            });
        } else {
            helper.itemView.setOnLongClickListener(null);
        }
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, SearchItem item);
    }

    public static class SearchItem {
        public String title;
        public int type; // 0: 历史, 1: 热搜, 2: 搜索建议

        public SearchItem(String title, int type) {
            this.title = title;
            this.type = type;
        }
    }
}