package com.swj.shiwujie.data.model;

import java.util.List;

public class FamilyVO {
    private List<BlindVO> blindVOList;
    private VolunteerVO creatorVolunteer;
    private String familyDescription;
    private Long familyId;
    private String familyName;
    private List<VolunteerVO> volunteerVOList;

    // Getters and Setters
    public List<BlindVO> getBlindVOList() {
        return blindVOList;
    }

    public void setBlindVOList(List<BlindVO> blindVOList) {
        this.blindVOList = blindVOList;
    }

    public VolunteerVO getCreatorVolunteer() {
        return creatorVolunteer;
    }

    public void setCreatorVolunteer(VolunteerVO creatorVolunteer) {
        this.creatorVolunteer = creatorVolunteer;
    }

    public String getFamilyDescription() {
        return familyDescription;
    }

    public void setFamilyDescription(String familyDescription) {
        this.familyDescription = familyDescription;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public List<VolunteerVO> getVolunteerVOList() {
        return volunteerVOList;
    }

    public void setVolunteerVOList(List<VolunteerVO> volunteerVOList) {
        this.volunteerVOList = volunteerVOList;
    }
} 