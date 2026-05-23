package com.teach.javafx.bean;

public class Menu {
    private Integer id;
    private String name;
    private String title;
    private String url;
    private Integer pid;
    private Integer sort;
    private String userTypeIds;
    private String icon;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getUserTypeIds() {
        return userTypeIds;
    }

    public void setUserTypeIds(String userTypeIds) {
        this.userTypeIds = userTypeIds;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
