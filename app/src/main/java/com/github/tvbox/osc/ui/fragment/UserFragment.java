package com.github.tvbox.osc.ui.fragment;

import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.UA;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * @author pj567
 * @date :2021/3/9
 * @description:
 */
public class UserFragment extends GridFragment {
    public UserFragment(MovieSort.SortData sortData) {
        super(sortData);
    }

    @Override
    protected void onFragmentResume() {
        super.onFragmentResume();

        if (Hawk.get(HawkConfig.HOME_REC, 0) == 2) {
            List<VodInfo> allVodRecord = RoomDataManger.getAllVodRecord(20);
            List<Movie.Video> vodList = new ArrayList<>();
            for (VodInfo vodInfo : allVodRecord) {
                Movie.Video vod = new Movie.Video();
                vod.id = vodInfo.id;
                vod.sourceKey = vodInfo.sourceKey;
                vod.name = vodInfo.name;
                vod.pic = vodInfo.pic;
                if (vodInfo.playNote != null && !vodInfo.playNote.isEmpty())
                    vod.note = "上次看到" + vodInfo.playNote;
                vodList.add(vod);
            }
            gridAdapter.setNewData(vodList);
            
            if (vodList.isEmpty()) {
                showEmpty();
            } else {
                showSuccess();
            }
        }
    }

    @Override
    protected void initData() {
        if (Hawk.get(HawkConfig.HOME_REC, 0) == 1) {
            super.initData();
        } else {
            showLoading();
            setDouBanData();
        }
    }

    private void setDouBanData() {
        try {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DATE);
            String today = String.format(Locale.getDefault(), "%d%d%d", year, month, day);
            String requestDay = Hawk.get("home_hot_day", "");
            if (requestDay.equals(today)) {
                String json = Hawk.get("home_hot", "");
                if (!json.isEmpty()) {
                    ArrayList<Movie.Video> hotMovies = loadHots(json);
                    if (!hotMovies.isEmpty()) {
                        gridAdapter.setNewData(hotMovies);
                        // 缓存数据加载完成，显示成功状态
                        showSuccess();
                    } else {
                        // 缓存数据解析失败或为空，显示空状态
                        gridAdapter.setNewData(new ArrayList<>());
                        showEmpty();
                    }
                    return;
                }
            }
            String doubanUrl = "https://movie.douban.com/j/new_search_subjects?sort=U&range=0,10&tags=&playable=1&start=0&year_range=" + year + "," + year;
            OkGo.<String>get(doubanUrl)
                    .headers("User-Agent", UA.randomOne())
                    .execute(new AbsCallback<>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            String netJson = response.body();
                            Hawk.put("home_hot_day", today);
                            Hawk.put("home_hot", netJson);
                            if (mActivity != null) {
                                mActivity.runOnUiThread(() -> {
                                    ArrayList<Movie.Video> hotMovies = loadHots(netJson);
                                    gridAdapter.setNewData(hotMovies);
                                    if (hotMovies.isEmpty()) {
                                        showEmpty();
                                    } else {
                                        showSuccess();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(Response<String> response) {
                            super.onError(response);
                            // 加载失败显示空状态
                            if (mActivity != null) {
                                mActivity.runOnUiThread(() -> {
                                    gridAdapter.setNewData(new ArrayList<>());
                                    showEmpty();
                                });
                            }
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            if (response.body() != null) {
                                return response.body().string();
                            }
                            return "";
                        }
                    });
        } catch (Throwable th) {
            th.printStackTrace();
            gridAdapter.setNewData(new ArrayList<>());
            showEmpty();
        }
    }

    private ArrayList<Movie.Video> loadHots(String json) {
        ArrayList<Movie.Video> result = new ArrayList<>();
        try {
            JsonObject infoJson = new Gson().fromJson(json, JsonObject.class);
            JsonArray array = infoJson.getAsJsonArray("data");
            int limit = Math.min(array.size(), 25);
            for (int i = 0; i < limit; i++) {
                JsonElement ele = array.get(i);
                JsonObject obj = ele.getAsJsonObject();
                Movie.Video vod = new Movie.Video();
                vod.name = obj.get("title").getAsString();
                vod.note = obj.get("rate").getAsString();
                if (!vod.note.isEmpty()) vod.note += " 分";
                vod.pic = obj.get("cover").getAsString()
                        + "@User-Agent=" + UA.randomOne()
                        + "@Referer=https://www.douban.com/";
                result.add(vod);
            }
        } catch (Throwable ignored) {
        }
        return result;
    }
}