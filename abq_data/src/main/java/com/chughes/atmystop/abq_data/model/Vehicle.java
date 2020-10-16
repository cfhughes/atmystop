package com.chughes.atmystop.abq_data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.time.LocalTime;

public class Vehicle {

    @SerializedName("vehicle_id")
    @Expose
    private String vehicleId;
    @SerializedName("msg_time")
    @Expose
    private LocalTime msgTime;
    @SerializedName("latitude")
    @Expose
    private Double latitude;
    @SerializedName("longitude")
    @Expose
    private Double longitude;
    @SerializedName("heading")
    @Expose
    private Integer heading;
    @SerializedName("speed_mph")
    @Expose
    private Integer speedMph;
    @SerializedName("route_short_name")
    @Expose
    private String routeShortName;
    @SerializedName("trip_id")
    @Expose
    private String tripId;
    @SerializedName("next_stop_id")
    @Expose
    private String nextStopId;
    @SerializedName("next_stop_name")
    @Expose
    private String nextStopName;
    @SerializedName("next_stop_sched_time")
    @Expose
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
