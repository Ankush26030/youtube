package com.github.youtubeify.util;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for HTTP operations.
 */
public class HttpUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("(?:v=|/v/|youtu\\.be/)([a-zA-Z0-9_-]{11})");
    private static final Pattern JSON_PATTERN = Pattern.compile("var\\s+([a-zA-Z0-9_]+)\\s*=\\s*(\\{.*?\\});", Pattern.DOTALL);

    /**
     * URL encode a string.
     *
     * @param text Text to encode
     * @return URL encoded text
     */
    public static String encodeUrl(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to URL encode text", e);
            return text;
        }
    }

    /**
     * Extract video ID from a YouTube URL.
     *
     * @param url YouTube URL
     * @return Video ID or null if not found
     */
    public static String extractVideoIdFromUrl(String url) {
        Matcher matcher = VIDEO_ID_PATTERN.matcher(url);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * Extract cookies from HTTP response.
     *
     * @param response HTTP response
     * @return Map of cookies
     */
    public static Map<String, String> extractCookies(CloseableHttpResponse response) {
        Map<String, String> cookies = new HashMap<>();
        
        Header[] headers = response.getHeaders("Set-Cookie");
        for (Header header : headers) {
            String value = header.getValue();
            int separatorIndex = value.indexOf(';');
            
            if (separatorIndex > 0) {
                String cookiePair = value.substring(0, separatorIndex);
                String[] parts = cookiePair.split("=", 2);
                
                if (parts.length == 2) {
                    cookies.put(parts[0], parts[1]);
                }
            }
        }
        
        return cookies;
    }

    /**
     * Format cookies for HTTP request.
     *
     * @param cookies Map of cookies
     * @return Formatted cookie string
     */
    public static String formatCookies(Map<String, String> cookies) {
        StringBuilder builder = new StringBuilder();
        
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            
            builder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return builder.toString();
    }

    /**
     * Extract initial data from a YouTube page.
     *
     * @param html HTML content
     * @param variableName Name of the JavaScript variable containing the data
     * @return Extracted data as a Map
     */
    public static Map<String, Object> extractInitialData(String html, String variableName) {
        String pattern = "var " + variableName + " = (\\{.*?\\});";
        Pattern r = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher m = r.matcher(html);
        
        if (m.find()) {
            String json = m.group(1);
            try {
                // This is a crude approximation - in a real implementation,
                // you'd want to use a proper JSON parser
                Map<String, Object> result = new HashMap<>();
                extractJsonFields(json, result);
                return result;
            } catch (Exception e) {
                log.error("Failed to parse JSON data", e);
            }
        }
        
        return null;
    }

    /**
     * A very simplified JSON parser for extracting fields.
     * This is just for demonstration and would not work for complex JSON.
     * In a real implementation, use a proper JSON library.
     *
     * @param json JSON string
     * @param result Result map
     */
    private static void extractJsonFields(String json, Map<String, Object> result) {
        // This is a placeholder for a real JSON parser
        // In a real implementation, use a proper JSON library like org.json or Jackson
    }

    /**
     * Convert ISO 8601 duration to seconds.
     *
     * @param isoDuration ISO 8601 duration string (e.g., "PT1H2M3S")
     * @return Duration in seconds
     */
    public static long isoDurationToSeconds(String isoDuration) {
        Pattern pattern = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?");
        Matcher matcher = pattern.matcher(isoDuration);
        
        if (matcher.matches()) {
            long hours = 0;
            long minutes = 0;
            long seconds = 0;
            
            if (matcher.group(1) != null) {
                hours = Long.parseLong(matcher.group(1));
            }
            
            if (matcher.group(2) != null) {
                minutes = Long.parseLong(matcher.group(2));
            }
            
            if (matcher.group(3) != null) {
                seconds = Long.parseLong(matcher.group(3));
            }
            
            return hours * 3600 + minutes * 60 + seconds;
        }
        
        return 0;
    }

    /**
     * Convert time text (e.g., "1:02:03") to seconds.
     *
     * @param timeText Time text
     * @return Time in seconds
     */
    public static long timeTextToSeconds(String timeText) {
        String[] parts = timeText.split(":");
        
        if (parts.length == 3) {
            // Hours:Minutes:Seconds
            return Long.parseLong(parts[0]) * 3600 + Long.parseLong(parts[1]) * 60 + Long.parseLong(parts[2]);
        } else if (parts.length == 2) {
            // Minutes:Seconds
            return Long.parseLong(parts[0]) * 60 + Long.parseLong(parts[1]);
        } else if (parts.length == 1) {
            // Seconds only
            return Long.parseLong(parts[0]);
        }
        
        return 0;
    }
}
