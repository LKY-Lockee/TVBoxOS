package com.github.tvbox.osc.player;

import android.content.Context;
import android.util.Pair;

import com.github.tvbox.osc.util.AudioTrackMemory;
import com.github.tvbox.osc.util.LOG;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Tracks;

import xyz.doikki.videoplayer.exo.ExoMediaPlayer;

import java.util.HashMap;
import java.util.Map;

public class ExoPlayer extends ExoMediaPlayer {

    private static AudioTrackMemory memory;

    public ExoPlayer(Context context) {
        super(context);
        memory = AudioTrackMemory.getInstance(context);
    }
    // 3. 获取所有轨道信息
    public TrackInfo getTrackInfo() {
        TrackInfo data = new TrackInfo();
        if (mInternalPlayer == null) return data;
        Tracks currentTracks = mInternalPlayer.getCurrentTracks();
        
        int audioGroupIndex = 0;
        int subtitleGroupIndex = 0;
        
        for (Tracks.Group trackGroup : currentTracks.getGroups()) {
            int trackType = trackGroup.getType();
            if (trackType != C.TRACK_TYPE_AUDIO && trackType != C.TRACK_TYPE_TEXT) continue;
            
            for (int i = 0; i < trackGroup.length; i++) {
                if (!trackGroup.isTrackSupported(i)) continue;
                
                Format fmt = trackGroup.getTrackFormat(i);
                TrackInfoBean bean = new TrackInfoBean();
                bean.language = getLanguage(fmt);
                bean.name = getName(fmt);
                bean.index = i;
                bean.selected = trackGroup.isTrackSelected(i);
                
                if (trackType == C.TRACK_TYPE_AUDIO) {
                    bean.groupIndex = audioGroupIndex;
                    data.addAudio(bean);
                } else {
                    bean.groupIndex = subtitleGroupIndex;
                    data.addSubtitle(bean);
                }
            }
            
            if (trackType == C.TRACK_TYPE_AUDIO) {
                audioGroupIndex++;
            } else {
                subtitleGroupIndex++;
            }
        }
        return data;
    }

    /**
     * 设置当前播放的音轨
     * @param groupIndex 音轨组的索引
     * @param trackIndex 音轨在组内的索引
     */
    public void setTrack(int groupIndex, int trackIndex, String playKey) {
        try {
            if (mInternalPlayer == null) {
                LOG.i("echo-setTrack: Player is null");
                return;
            }
            
            Tracks currentTracks = mInternalPlayer.getCurrentTracks();
            int audioGroupCount = 0;
            Tracks.Group targetGroup = null;
            
            // Find the target audio track group
            for (Tracks.Group trackGroup : currentTracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_AUDIO) {
                    if (audioGroupCount == groupIndex) {
                        targetGroup = trackGroup;
                        break;
                    }
                    audioGroupCount++;
                }
            }
            
            if (targetGroup == null || trackIndex >= targetGroup.length) {
                LOG.i("echo-setTrack: Invalid track index - group:" + groupIndex + ", track:" + trackIndex);
                return;
            }
            
            // In Media3, we need to use preferred audio language or manual track selection
            // For now, using the TrackSelectionParameters to prefer specific tracks
            Format targetFormat = targetGroup.getTrackFormat(trackIndex);
            
            // Set parameters to prefer this specific audio track
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setPreferredAudioLanguage(targetFormat.language != null ? targetFormat.language : "")
            );

            // 缓存到 map：下次同一路径播放时使用
            if (!playKey.isEmpty()) {
                memory.save(playKey, groupIndex, trackIndex);
            }
        } catch (Exception e) {
            LOG.i("echo-setTrack error: " + e.getMessage());
        }
    }

    //加载上一次选中的音轨
    public void loadDefaultTrack(String playKey) {
        Pair<Integer, Integer> pair = memory.exoLoad(playKey);
        if (pair == null) return;
        
        int groupIndex = pair.first;
        int trackIndex = pair.second;
        
        setTrack(groupIndex, trackIndex, "");
    }



    private static final Map<String, String> LANG_MAP = new HashMap<>();
    static {
        LANG_MAP.put("zh", "中文");
        LANG_MAP.put("zh-cn", "中文");
        LANG_MAP.put("en", "英语");
        LANG_MAP.put("en-us", "英语");
    }

    private String getLanguage(Format fmt){
        String lang = fmt.language;
        if (lang == null || lang.isEmpty() || "und".equalsIgnoreCase(lang)) {
            return "未知";
        }
        String name = LANG_MAP.get(lang.toLowerCase());
        return name != null ? name : lang;
    }

    private String getName(Format fmt){
        String channelLabel;
        if (fmt.channelCount <= 0) {
            channelLabel = "";
        } else if (fmt.channelCount == 1) {
            channelLabel = "单声道";
        } else if (fmt.channelCount == 2) {
            channelLabel = "立体声";
        } else {
            channelLabel = fmt.channelCount + " 声道";
        }
        String codec = "";
        if (fmt.sampleMimeType != null) {
            String mime = fmt.sampleMimeType.substring(fmt.sampleMimeType.indexOf('/') + 1);
            codec = mime.toUpperCase();
        }
        return String.join(", ", channelLabel, codec);
    }
}