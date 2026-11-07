
package com.miniweb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java 基础类型转换器。
 *
 * @author zicheng bin
 */
public class TypeConverter {

    private static final Logger log = LoggerFactory.getLogger(TypeConverter.class);

    /**
     * 将字符串转换为指定 Java 类型。
     */
    public Object convert(String value, Class<?> targetType) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Cannot convert null to " + targetType.getName());
        }

        if (targetType == String.class) {
            return value;
        }

        try {
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.valueOf(value);
            }

            if (targetType == long.class || targetType == Long.class) {
                return Long.valueOf(value);
            }

            if (targetType == double.class || targetType == Double.class) {
                return Double.valueOf(value);
            }

            if (targetType == float.class || targetType == Float.class) {
                return Float.valueOf(value);
            }

            if (targetType == boolean.class || targetType == Boolean.class) {
                if (!"true".equalsIgnoreCase(value)
                        && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException(
                            "Invalid boolean value: " + value);
                }

                return Boolean.valueOf(value);
            }

            if (targetType == short.class || targetType == Short.class) {
                return Short.valueOf(value);
            }

            if (targetType == byte.class || targetType == Byte.class) {
                return Byte.valueOf(value);
            }

            if (targetType == char.class || targetType == Character.class) {
                if (value.length() != 1) {
                    throw new IllegalArgumentException(
                            "Invalid char value: " + value);
                }

                return value.charAt(0);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Cannot convert '" + value + "' to "
                            + targetType.getSimpleName(),
                    e);
        }

        throw new IllegalArgumentException(
                "Unsupported parameter type: " + targetType.getName());
    }
}
