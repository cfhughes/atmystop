package com.chughes.atmystop.common.service;

import com.chughes.atmystop.common.model.StopTimeData;
import com.chughes.atmystop.common.service.SerializationService;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StopTimeService {

    private RedisTemplate<String, Object> redisTemplate;
    private SerializationService<StopTimeData> stopTimeDataSerializationService;

    public StopTimeService(RedisTemplate<String, Object> redisTemplate, SerializationService<StopTimeData> stopTimeDataSerializationService) {
        this.redisTemplate = redisTemplate;
        this.stopTimeDataSerializationService = stopTimeDataSerializationService;
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
                return stopTimeDataSerializationService.deserialize(connection.get((agencyId + ":" + new String(tripId) + ":" + stopId).getBytes()), StopTimeData.class);
            }).filter((Objects::nonNull)).collect(Collectors.toList());
        });
    }

    public StopTimeData getByStopIdAgencyTripId(String stopId, String agencyId, String tripId) {
        return redisTemplate.execute((RedisCallback<StopTimeData>) connection ->
                stopTimeDataSerializationService.deserialize(connection.get((agencyId + ":" + tripId + ":" + stopId).getBytes()), StopTimeData.class));
    }
}
