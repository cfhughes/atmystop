package com.chughes.atmystop.appserver.model;

import java.time.LocalTime;

public class RealtimeTripInfo implements Comparable<RealtimeTripInfo> {

    private long secondsLate;

    private String tripId;

    private LocalTime scheduledTime;

    private String displayTime;

    private String service;

    private String route;

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
        return scheduledTime.compareTo(o.getScheduledTime());
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
}
