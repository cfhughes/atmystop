package com.chughes.atmystop.abq_data.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.time.LocalTime;

public class Vehicle {

    @JsonProperty("vehicle_id")
    private String vehicleId;

    @JsonProperty("msg_time")
    private LocalTime msgTime;

    private Double latitude;

    private Double longitude;

    private Integer heading;

    @JsonProperty("speed_mph")
    private Integer speedMph;

    @JsonProperty("route_short_name")
    private String routeShortName;

    @JsonProperty("trip_id")
    private String tripId;

    @JsonProperty("next_stop_id")
    private String nextStopId;

    @JsonProperty("next_stop_name")
    private String nextStopName;

    @JsonProperty("next_stop_sched_time")
    private LocalTime nextStopSchedTime;

    public String getVehicleId() {
        return vehicleId;
    }

    public LocalTime getMsgTime() {
        return msgTime;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getHeading() {
        return heading;
    }

    public Integer getSpeedMph() {
        return speedMph;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public String getTripId() {
        return tripId;
    }

    public String getNextStopId() {
        return nextStopId;
    }

    public String getNextStopName() {
        return nextStopName;
    }

    public LocalTime getNextStopSchedTime() {
        return nextStopSchedTime;
    }

}
