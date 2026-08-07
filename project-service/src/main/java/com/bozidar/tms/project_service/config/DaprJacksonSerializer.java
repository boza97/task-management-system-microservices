package com.bozidar.tms.project_service.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;

import java.io.IOException;

public class DaprJacksonSerializer implements DaprObjectSerializer {

    private final ObjectMapper mapper;

    public DaprJacksonSerializer() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public byte[] serialize(Object o) throws IOException {
        if (o == null) {
            return null;
        }
        if (o instanceof byte[] bytes) {
            return bytes;
        }
        return mapper.writeValueAsBytes(o);
    }

    @Override
    public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
        if (data == null || data.length == 0) {
            return null;
        }
        JavaType javaType = mapper.getTypeFactory().constructType(type.getType());
        return mapper.readValue(data, javaType);
    }

    @Override
    public String getContentType() {
        return "application/json";
    }
}
