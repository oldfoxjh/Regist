package kr.co.enord.dji.model;

import com.google.gson.annotations.SerializedName;

public class AltResponse {
    // variable name should be same as in the json response from php
//    @SerializedName("success")
//    boolean success;
    @SerializedName("altitude")
    int altitude;

    public int getAltitude() {
        return altitude;
    }
//    boolean getSuccess() {
//        return success;
//    }
}