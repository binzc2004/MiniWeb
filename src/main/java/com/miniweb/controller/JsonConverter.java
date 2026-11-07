
package com.miniweb.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.miniweb.MiniWeb;
/**
 * JSON 转换器。
 *
 * @author zicheng bin
 */
public class JsonConverter {

    private static final Logger log = MiniWeb.getLogger(JsonConverter.class);

    private final ObjectMapper objectMapper;

    public JsonConverter() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 将 JSON 字符串转换为指定 Java 类型。
     */
    public <T> T fromJson(String json, Class<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Failed to deserialize JSON to "
                            + targetType.getName(),
                    e);
        }
    }

    /**
     * 将 Java 对象转换为 JSON 字符串。
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Failed to serialize object to JSON",
                    e);
        }
    }
}
