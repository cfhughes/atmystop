package com.chughes.atmystop.appserver;

import com.chughes.atmystop.common.model.StopTimeData;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StopTimeService {

    private RedisTemplate<String, Object> redisTemplate;

    public StopTimeService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Collection<StopTimeData> getAllByStopIdAndAgency(String stopId, String agencyId, List<String> serviceIds) {
        return redisTemplate.execute((RedisCallback<Collection<StopTimeData>>) connection -> {
            byte[][] keyServiceIds = serviceIds.stream().map(String::getBytes).toArray(byte[][]::new);
            HashSet<byte[]> tripIds = new HashSet<>();
            for (byte[] keyServiceId : keyServiceIds) {
                tripIds.addAll(connection.sInter(keyServiceId,stopId.getBytes()));
            }
            return tripIds.stream().map(tripId -> {
                //System.out.println(tripId);
                StopTimeData stopTimeData = (StopTimeData) SerializationUtils.deserialize(connection.get((agencyId + ":" + new String(tripId) + ":" + stopId).getBytes()));
                return stopTimeData;
            }).filter((Objects::nonNull)).collect(Collectors.toList());
        });
    }
}
