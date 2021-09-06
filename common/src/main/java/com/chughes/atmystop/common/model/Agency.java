package com.chughes.atmystop.common.model;

import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.List;
import java.util.TimeZone;

@RedisHash("Agency")
public class Agency implements Serializable {

    private static final long serialVersionUID = 20210905L;

    private String id;

    private TimeZone timeZone;

    private List<String> serviceIds;

    private List<String> yesterdayServiceIds;

    public List<String> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(List<String> serviceIds) {
        this.serviceIds = serviceIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public List<String> getYesterdayServiceIds() {
        return yesterdayServiceIds;
    }

    public void setYesterdayServiceIds(List<String> yesterdayServiceIds) {
        this.yesterdayServiceIds = yesterdayServiceIds;
    }
}
