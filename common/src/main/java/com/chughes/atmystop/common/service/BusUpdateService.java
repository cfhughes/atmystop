package com.chughes.atmystop.common.service;

import com.chughes.atmystop.common.model.BusUpdateData;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
public class BusUpdateService {

    public static final int EXPIRATION_MINUTES = 30;
    private RedisTemplate<String, Object> redisTemplate;
    private SerializationService<BusUpdateData> busUpdateDataSerializationService;

    public BusUpdateService(RedisTemplate<String, Object> redisTemplate, SerializationService<BusUpdateData> busUpdateDataSerializationService) {
        this.redisTemplate = redisTemplate;
        this.busUpdateDataSerializationService = busUpdateDataSerializationService;
    }

    public void saveBusUpdate(BusUpdateData busUpdateData){
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            Expiration expiration = Expiration.seconds(60 * EXPIRATION_MINUTES);
            byte[] id = BigInteger.valueOf(busUpdateData.getId()).toByteArray();
            connection.set(id,busUpdateDataSerializationService.serialize(busUpdateData, BusUpdateData.class),
                    expiration, RedisStringCommands.SetOption.UPSERT);
            return null;
        });
    }

    public BusUpdateData findById(int hashCode) {
        return redisTemplate.execute((RedisCallback<BusUpdateData>) connection -> {
            byte[] id = BigInteger.valueOf(hashCode).toByteArray();
            return busUpdateDataSerializationService.deserialize(connection.get(id), BusUpdateData.class);
        });
    }
}
