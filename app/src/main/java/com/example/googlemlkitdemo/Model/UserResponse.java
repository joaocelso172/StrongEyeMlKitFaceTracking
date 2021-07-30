package com.example.googlemlkitdemo.Model;

public class UserResponse {

    private Boolean canAccess;
    private String name;

    public UserResponse(Boolean canAccess, String name) {
        this.canAccess = canAccess;
        this.name = name;
    }

    public UserResponse(UserResponse ur){
        this.canAccess = ur.canAccess;
        this.name = ur.name;
    }

    public Boolean getCanAccess() {
        return canAccess;
    }

    public void setCanAccess(Boolean canAccess) {
        this.canAccess = canAccess;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
