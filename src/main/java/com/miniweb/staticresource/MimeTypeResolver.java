package com.miniweb.staticresource;

import java.util.Locale;
import java.util.Map;

/**
 * MIME 类型解析器。
 *
 * @author zicheng bin
 */
public class MimeTypeResolver {

    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
            Map.entry("html", "text/html; charset=UTF-8"),
            Map.entry("htm", "text/html; charset=UTF-8"),
            Map.entry("css", "text/css; charset=UTF-8"),
            Map.entry("js", "application/javascript; charset=UTF-8"),
            Map.entry("json", "application/json; charset=UTF-8"),
            Map.entry("txt", "text/plain; charset=UTF-8"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("pdf", "application/pdf"));

    /**
     * 根据文件名获取 MIME 类型。
     *
     * @param fileName 文件名
     * @return MIME 类型
     */
    public String resolve(String fileName) {
        int index = fileName.lastIndexOf('.');

        if (index < 0 || index == fileName.length() - 1) {
            return "application/octet-stream";
        }

        String extension = fileName.substring(index + 1)
                .toLowerCase(Locale.ROOT);

        return MIME_TYPES.getOrDefault(
                extension,
                "application/octet-stream");
    }
}