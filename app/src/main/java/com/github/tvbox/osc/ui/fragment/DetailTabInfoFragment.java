package com.github.tvbox.osc.ui.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.MD5;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class DetailTabInfoFragment extends Fragment {
    private View contentView;
    private ImageView ivThumb;
    private TextView tvName;
    private TextView tvYear;
    private TextView tvSite;
    private TextView tvArea;
    private TextView tvLang;
    private TextView tvType;
    private TextView tvActor;
    private TextView tvDirector;
    private TextView tvPlayUrl;
    private TextView tvDes;
    private MaterialButton tvCollect;

    private String sourceKey;
    private String vodId;

    public void setContentView(View view) {
        this.contentView = view;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (contentView != null) {
            if (contentView.getParent() != null) {
                ((ViewGroup) contentView.getParent()).removeView(contentView);
            }
            initViews(contentView);
            return contentView;
        }
        View view = inflater.inflate(R.layout.fragment_detail_tab_info, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        ivThumb = view.findViewById(R.id.ivThumb);
        tvName = view.findViewById(R.id.tvName);
        tvYear = view.findViewById(R.id.tvYear);
        tvSite = view.findViewById(R.id.tvSite);
        tvArea = view.findViewById(R.id.tvArea);
        tvLang = view.findViewById(R.id.tvLang);
        tvType = view.findViewById(R.id.tvType);
        tvActor = view.findViewById(R.id.tvActor);
        tvDirector = view.findViewById(R.id.tvDirector);
        tvPlayUrl = view.findViewById(R.id.tvPlayUrl);
        tvDes = view.findViewById(R.id.tvDes);
        tvCollect = view.findViewById(R.id.tvCollect);

        tvCollect.setOnClickListener(v -> onCollectClick());
        tvPlayUrl.setOnClickListener(v -> onPlayUrlClick());
    }

    public void setVideoInfo(Movie.Video video, String sourceKey, String firstSourceKey, String vodId) {
        if (video == null) return;

        this.sourceKey = sourceKey;
        this.vodId = vodId;

        if (tvName != null) tvName.setText(video.name);
        setTextShow(tvSite, "来源：", ApiConfig.get().getSource(firstSourceKey).getName());
        setTextShow(tvYear, "年份：", video.year == 0 ? "" : String.valueOf(video.year));
        setTextShow(tvArea, "地区：", video.area);
        setTextShow(tvLang, "语言：", video.lang);

        if (!firstSourceKey.equals(sourceKey)) {
            setTextShow(tvType, "类型：", "[" + ApiConfig.get().getSource(sourceKey).getName() + "] 解析");
        } else {
            setTextShow(tvType, "类型：", video.type);
        }

        setTextShow(tvActor, "演员：", video.actor);
        setTextShow(tvDirector, "导演：", video.director);
        setTextShow(tvDes, "简介：", removeHtmlTag(video.des));

        if (!TextUtils.isEmpty(video.pic) && ivThumb != null) {
            Picasso.get()
                    .load(DefaultConfig.checkReplaceProxy(video.pic))
                    .transform(new RoundTransformation(MD5.string2MD5(video.pic))
                            .centerCorp(true)
                            .override(AutoSizeUtils.mm2px(requireContext(), 300), AutoSizeUtils.mm2px(requireContext(), 400))
                            .roundRadius(AutoSizeUtils.mm2px(requireContext(), 10), RoundTransformation.RoundType.ALL))
                    .placeholder(R.drawable.img_loading_placeholder)
                    .noFade()
                    .error(R.drawable.img_loading_placeholder)
                    .into(ivThumb);
        } else if (ivThumb != null) {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }

        updateCollectButton();
    }

    public void setPlayUrl(String url) {
        setTextShow(tvPlayUrl, "地址：", url);
    }

    public void updateCollectButton() {
        if (tvCollect == null || sourceKey == null || vodId == null) return;

        boolean isVodCollect = RoomDataManger.isVodCollect(sourceKey, vodId);
        if (isVodCollect) {
            tvCollect.setIcon(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.icon_collect_filled));
        } else {
            tvCollect.setIcon(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.icon_collect));
        }
    }

    private void onCollectClick() {
        if (sourceKey == null || vodId == null) return;

        boolean isVodCollect = RoomDataManger.isVodCollect(sourceKey, vodId);
        if (isVodCollect) {
            RoomDataManger.deleteVodCollect(sourceKey, null);
            tvCollect.setIcon(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.icon_collect));
            Toast.makeText(requireContext(), "已取消收藏", Toast.LENGTH_SHORT).show();
        } else {
            RoomDataManger.insertVodCollect(sourceKey, null);
            tvCollect.setIcon(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.icon_collect_filled));
            Toast.makeText(requireContext(), "已加入收藏夹", Toast.LENGTH_SHORT).show();
        }
    }

    private void onPlayUrlClick() {
        if (tvPlayUrl == null) return;

        ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(null, tvPlayUrl.getText().toString().replace("播放地址：", "")));
        Toast.makeText(requireContext(), "已复制", Toast.LENGTH_SHORT).show();
    }

    private void setTextShow(TextView view, String tag, String info) {
        if (view == null) return;

        if (info == null || info.trim().isEmpty()) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setText(getHtml(tag, info));
    }

    private String getHtml(String label, String content) {
        if (content == null) {
            content = "";
        }
        return label + content;
    }

    private String removeHtmlTag(String info) {
        if (info == null)
            return "";
        return info.replaceAll("<.*?>", "").replaceAll("\\s", "");
    }
}

