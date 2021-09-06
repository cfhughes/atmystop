package com.chughes.atmystop.appserver.model;

import java.time.LocalTime;

public class RealtimeTripInfo implements Comparable<RealtimeTripInfo> {

    private long secondsLate;

    private String tripId;

    private LocalTime scheduledTime;

    private String displayTime;

    private String service;

    private String route;

    private String color;

    private String textColor;

    private String headsign;

    public long getSecondsLate() {
        return secondsLate;
    }

    public void setSecondsLate(long secondsLate) {
        this.secondsLate = secondsLate;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getDisplayTime() {
        return displayTime;
    }

    public void setDisplayTime(String displayTime) {
        this.displayTime = displayTime;
    }

    @Override
    public int compareTo(RealtimeTripInfo o) {
        return scheduledTime.plusSeconds(getSecondsLate()).compareTo(o.getScheduledTime().plusSeconds(o.getSecondsLate()));
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getHeadsign() {
        return headsign;
    }

    public void setHeadsign(String headsign) {
        this.headsign = headsign;
    }

    @Override
    public String toString() {
        return "RealtimeTripInfo{" +
                "secondsLate=" + secondsLate +
                ", scheduledTime=" + scheduledTime +
                ", displayTime='" + displayTime + '\'' +
                ", route='" + route + '\'' +
                ", headsign='" + headsign + '\'' +
                '}';
    }
}
