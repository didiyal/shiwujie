package com.swj.shiwujie.volunteer.ui.community;

public class CommunityActivity {
    private int imageResId;
    private String status;
    private String date;
    private String title;
    private String type;

    public CommunityActivity(int imageResId, String status, String date, String title, String type) {
        this.imageResId = imageResId;
        this.status = status;
        this.date = date;
        this.title = title;
        this.type = type;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getStatus() {
        return status;
    }

    public String getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }
} 