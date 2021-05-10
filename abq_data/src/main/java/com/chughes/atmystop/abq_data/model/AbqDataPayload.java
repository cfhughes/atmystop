package com.chughes.atmystop.abq_data.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AbqDataPayload {

    @JsonProperty("allroutes")
    private List<Vehicle> allRoutes;

    public List<Vehicle> getAllRoutes() {
        return allRoutes;
    }
}
