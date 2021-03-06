package com.chughes.atmystop.common.service;

import com.chughes.atmystop.common.model.BusStopData;
import org.springframework.dao.DataAccessException;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BusStopsService {

    private static final byte[] REDIS_KEY_STOPS = "STOPS".getBytes();

    private RedisTemplate<String, Object> redisTemplate;

    public BusStopsService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addAllStops(List<BusStopData> stops){
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.del(REDIS_KEY_STOPS);
            stops.forEach(busStopData -> {
                connection.geoCommands().geoAdd(REDIS_KEY_STOPS, busStopData.getLocation(), SerializationUtils.serialize(busStopData));
            });
            return null;
        });
    }

    public List<BusStopData> nearestStops(Point point){
        return redisTemplate.execute((RedisCallback<List<BusStopData>>) connection -> {
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .sortAscending().limit(100);
            return connection.geoRadius(REDIS_KEY_STOPS,new Circle(point, new Distance(1000, Metrics.MILES)),args)
                    .getContent().stream().map((geoLocationGeoResult -> {
                        return (BusStopData) SerializationUtils.deserialize(geoLocationGeoResult.getContent().getName());
                    })).collect(Collectors.toList());
        });
    }

}
