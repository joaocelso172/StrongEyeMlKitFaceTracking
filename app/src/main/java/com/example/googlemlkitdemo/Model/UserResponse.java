package com.example.googlemlkitdemo.Model;

import com.google.gson.annotations.SerializedName;

public class UserResponse {

    private Boolean canAccess;
    private String name;
    @SerializedName("FaceId")
    private String faceID;
    @SerializedName("ExternalImageId")
    private String nationalInsuranceNumber; //CPF
    private boolean wasFound;

    public UserResponse(Boolean canAccess, String name) {
        this.canAccess = canAccess;
        this.name = name;
    }

    public UserResponse (Boolean wasFound, String nationalInsuranceNumber, String faceID){
        if (wasFound != null) {
            this.wasFound = wasFound;
        } else wasFound = true;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.faceID = faceID;
    }

    public UserResponse(UserResponse ur){
        this.canAccess = ur.canAccess;
        this.name = ur.name;
    }

    public String getFaceID() {
        return faceID;
    }

    public void setFaceID(String faceID) {
        this.faceID = faceID;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public void setNationalInsuranceNumber(String nationalInsuranceNumber) {
        this.nationalInsuranceNumber = nationalInsuranceNumber;
    }

    public boolean isWasFound() {
        return wasFound;
    }

    public void setWasFound(boolean wasFound) {
        this.wasFound = wasFound;
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
