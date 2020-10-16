package com.chughes.atmystop.common.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalTime;

@RedisHash("StopTimeData")
public class StopTimeData implements Serializable {

    @Id
    private String stopId;
    private LocalTime arrivalTime;
    @Indexed private String routeId;
    private String tripId;
    private String serviceId;
    private boolean lastStop;

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public boolean isLastStop() {
        return lastStop;
    }

    public void setLastStop(boolean lastStop) {
        this.lastStop = lastStop;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    @Override
    public String toString() {
        return "StopTimeData{" +
                "stopId='" + stopId + '\'' +
                '}';
    }
}
