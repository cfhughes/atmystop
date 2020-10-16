package com.chughes.atmystop.common.model;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.GeoIndexed;

@RedisHash("BusStopData")
public class BusStopData {

    private String id;

    @GeoIndexed
    Point location;




}
