package com.chughes.atmystop.common.model;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.GeoIndexed;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BusStopData implements Serializable {

    private String id;

    private String agency;

    private String title;

    Point location;

    Set<TripHeadSign> trips;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public Set<TripHeadSign> getTrips() {
        return trips;
    }

    public void setTrips(Set<TripHeadSign> trips) {
        this.trips = trips;
    }
}
