package com.milling.admin.models.menu;

import java.util.ArrayList;

public class InfinityItem {
    private String title;
    private ArrayList<InfinityItem> childs;
    private Integer index;
    private InfinityItem parent;
    public InfinityItem(String title, ArrayList<InfinityItem> childs) {
        this.title = title;
        this.childs = childs;
        this.parent = null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<InfinityItem> getChilds() {
        return childs;
    }

    public void setChilds(ArrayList<InfinityItem> childs) {
        this.childs = childs;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public InfinityItem getParent() {
        return parent;
    }

    public void setParent(InfinityItem parent) {
        this.parent = parent;
    }
}
