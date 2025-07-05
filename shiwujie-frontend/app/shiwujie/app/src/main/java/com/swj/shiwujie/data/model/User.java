package com.swj.shiwujie.data.model;

public class User {
    private String phone;
    private String token;
    private int userType;

    public User(String phone, String token, int userType) {
        this.phone = phone;
        this.token = token;
        this.userType = userType;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }
} 