package com.swj.shiwujie.data.model;

public class FamilyJoinReviewVO {
    private String applyTime;
    private Long blindId;
    private Long familyId;
    private Long reviewId;
    private String reviewStatus;
    private Long volunteerId;

    // Getters and Setters
    public String getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(String applyTime) {
        this.applyTime = applyTime;
    }

    public Long getBlindId() {
        return blindId;
    }

    public void setBlindId(Long blindId) {
        this.blindId = blindId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Long getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }
} 