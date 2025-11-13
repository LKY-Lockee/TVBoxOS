package com.github.tvbox.osc.bean;

import java.util.List;

public class SubtitleData {

    private Boolean isNew;

    private List<Subtitle> subtitleList;

    private Boolean isZip;

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public List<Subtitle> getSubtitleList() {
        return subtitleList;
    }

    public void setSubtitleList(List<Subtitle> subtitle) {
        this.subtitleList = subtitle;
    }

    public Boolean getIsZip() {
        return isZip;
    }

    public void setIsZip(Boolean zip) {
        isZip = zip;
    }

    @Override
    public String toString() {
        return "SubtitleData{" +
                "isNew='" + isNew + '\'' +
                '}';
    }
}
