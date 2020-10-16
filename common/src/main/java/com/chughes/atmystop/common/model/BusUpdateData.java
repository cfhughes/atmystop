package com.chughes.atmystop.common.model;

import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.LocalTime;

@RedisHash("BusUpdateData")
public class BusUpdateData implements Serializable {

    private int id;

    private LocalTime updateTime;

    private long secondsLate;

    private String nextStop;

    public BusUpdateData(int id, LocalTime updateTime, long secondsLate, String nextStop) {
        this.id = id;
        this.updateTime = updateTime;
        this.secondsLate = secondsLate;
        this.nextStop = nextStop;
    }

    public int getId() {
        return id;
    }

    public LocalTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalTime updateTime) {
        this.updateTime = updateTime;
    }

    public long getSecondsLate() {
        return secondsLate;
    }

    public void setSecondsLate(long secondsLate) {
        this.secondsLate = secondsLate;
    }

    public String getNextStop() {
        return nextStop;
    }

    public void setNextStop(String nextStop) {
        this.nextStop = nextStop;
    }
}
