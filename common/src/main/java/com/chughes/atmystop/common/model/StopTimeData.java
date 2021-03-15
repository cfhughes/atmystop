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
    @Indexed private String routeShortName;
    private String tripId;
    private String serviceId;
    private int stopSequence;
    private boolean lastStop;
    private String color;
    private String textColor;

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public void setRouteShortName(String routeShortName) {
        this.routeShortName = routeShortName;
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

    public int getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(int stopSequence) {
        this.stopSequence = stopSequence;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getColor() {
        return color;
    }

    public String getTextColor() {
        return textColor;
    }
}
