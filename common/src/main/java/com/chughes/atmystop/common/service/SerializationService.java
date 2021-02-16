package com.chughes.atmystop.common.service;

import com.chughes.atmystop.common.model.StopTimeData;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.IdStrategy;
import io.protostuff.runtime.RuntimeSchema;
import org.springframework.stereotype.Service;

@Service
public class SerializationService<T> {

    static final DefaultIdStrategy STRATEGY = new DefaultIdStrategy(IdStrategy.DEFAULT_FLAGS
            | IdStrategy.MORPH_COLLECTION_INTERFACES
            | IdStrategy.MORPH_MAP_INTERFACES
            | IdStrategy.MORPH_NON_FINAL_POJOS);

    LinkedBuffer buffer = LinkedBuffer.allocate();

    public byte[] serialize(T obj, Class<T> c){
        Schema<T> schema = RuntimeSchema.getSchema(c,STRATEGY);

        try {
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            buffer.clear();
        }

    }

    public T deserialize(byte[] bytes, Class<T> c) {
        if (bytes == null) {
            return null;
        }
        Schema<T> schema = RuntimeSchema.getSchema(c,STRATEGY);

        T result = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, result, schema);

        return result;
    }

}
