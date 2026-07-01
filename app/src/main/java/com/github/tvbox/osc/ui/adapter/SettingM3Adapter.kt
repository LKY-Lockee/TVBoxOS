package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.SettingItem;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

/**
 * Material 3 风格设置适配器
 */
public class SettingM3Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<SettingItem> items = new ArrayList<>();

    public void setItems(List<SettingItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void updateItem(int position) {
        if (position >= 0 && position < items.size()) {
            notifyItemChanged(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case SettingItem.TYPE_CATEGORY:
                return new CategoryViewHolder(inflater.inflate(R.layout.item_setting_category, parent, false));
            case SettingItem.TYPE_SWITCH:
                return new SwitchViewHolder(inflater.inflate(R.layout.item_setting_switch, parent, false));
            case SettingItem.TYPE_PREFERENCE:
            default:
                return new PreferenceViewHolder(inflater.inflate(R.layout.item_setting_preference, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SettingItem item = items.get(position);
        
        if (holder instanceof CategoryViewHolder) {
            ((CategoryViewHolder) holder).bind(item);
        } else if (holder instanceof PreferenceViewHolder) {
            ((PreferenceViewHolder) holder).bind(item);
        } else if (holder instanceof SwitchViewHolder) {
            ((SwitchViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // 分类标题 ViewHolder
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryTitle;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryTitle = itemView.findViewById(R.id.tvCategoryTitle);
        }

        void bind(SettingItem item) {
            tvCategoryTitle.setText(item.getTitle());
        }
    }

    // 普通设置项 ViewHolder
    static class PreferenceViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSummary;
        TextView tvValue;

        PreferenceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSummary = itemView.findViewById(R.id.tvSummary);
            tvValue = itemView.findViewById(R.id.tvValue);
        }

        void bind(SettingItem item) {
            tvTitle.setText(item.getTitle());
            
            if (!TextUtils.isEmpty(item.getSummary())) {
                tvSummary.setVisibility(View.VISIBLE);
                tvSummary.setText(item.getSummary());
            } else {
                tvSummary.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(item.getValue())) {
                tvValue.setVisibility(View.VISIBLE);
                tvValue.setText(item.getValue());
            } else {
                tvValue.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (item.getOnClickListener() != null) {
                    item.getOnClickListener().onClick(item);
                }
            });
        }
    }

    // 开关设置项 ViewHolder
    static class SwitchViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSummary;
        MaterialSwitch switchWidget;

        SwitchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSummary = itemView.findViewById(R.id.tvSummary);
            switchWidget = itemView.findViewById(R.id.switchWidget);
        }

        void bind(SettingItem item) {
            tvTitle.setText(item.getTitle());
            
            if (!TextUtils.isEmpty(item.getSummary())) {
                tvSummary.setVisibility(View.VISIBLE);
                tvSummary.setText(item.getSummary());
            } else {
                tvSummary.setVisibility(View.GONE);
            }

            switchWidget.setChecked(item.isSwitchState());

            itemView.setOnClickListener(v -> {
                boolean newState = !item.isSwitchState();
                item.setSwitchState(newState);
                switchWidget.setChecked(newState);
                if (item.getOnClickListener() != null) {
                    item.getOnClickListener().onClick(item);
                }
            });
        }
    }
}

