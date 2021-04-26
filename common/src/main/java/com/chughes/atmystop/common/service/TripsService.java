package com.chughes.atmystop.common.service;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TripsService {

    private RedisTemplate<String, Object> redisTemplate;

    public TripsService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getHeadsignByTrip(String agencyId, String tripId) {
        return redisTemplate.execute((RedisCallback<String>) connection -> {
            byte[] bytes = connection.get(("tripInfo:" + agencyId + ":" + tripId).getBytes());
            if (bytes != null) {
                return new String(bytes);
            } else {
                return "";
            }
        });
    }


}
