package com.collarmc.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class Utils {

    private static final ObjectMapper JSON_MAPPER;
    private static final ObjectMapper MESSAGE_PACK_MAPPER;

    static {
        JSON_MAPPER = new ObjectMapper()
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        MESSAGE_PACK_MAPPER = new ObjectMapper(new MessagePackFactory())
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public static final SecureRandom SECURERANDOM;

    static {
        try {
            SECURERANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ObjectMapper jsonMapper() {
        return JSON_MAPPER;
    }

    public static ObjectMapper messagePackMapper() {
        return MESSAGE_PACK_MAPPER;
    }

    public static SecureRandom secureRandom() {
        return SECURERANDOM;
    }

    private Utils() {}
}
