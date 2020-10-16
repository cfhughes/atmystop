package com.chughes.atmystop.abq_data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AbqDataPayload {

    @SerializedName("allroutes")
    @Expose
    private List<Vehicle> allRoutes;

    public List<Vehicle> getAllRoutes() {
        return allRoutes;
    }
}
