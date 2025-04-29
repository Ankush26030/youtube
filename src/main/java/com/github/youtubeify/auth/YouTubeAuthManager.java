package com.github.youtubeify.auth;

import com.github.youtubeify.util.HttpUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Manager for YouTube authentication and API calls.
 * This uses a combination of approaches to reliably fetch YouTube content:
 * 1. YouTube Data API (if API key is available)
 * 2. Innertube API (YouTube's internal API)
 * 3. Web scraping as fallback
 */
public class YouTubeAuthManager {

    private static final Logger log = LoggerFactory.getLogger(YouTubeAuthManager.class);

    private static final String INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"; // Public client key
    private static final String INNERTUBE_CONTEXT = "{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20230120.00.00\"}}";
    private static final String INNERTUBE_BASE_URL = "https://www.youtube.com/youtubei/v1";
    private static final String DATA_API_BASE_URL = "https://www.googleapis.com/youtube/v3";
    
    private final String apiKey;
    private final CloseableHttpClient httpClient;
    private String innertubeContext;
    private Map<String, String> cookies;

    public YouTubeAuthManager() {
        this.apiKey = System.getenv("YOUTUBE_API_KEY");
        this.httpClient = HttpClients.createDefault();
        this.cookies = new HashMap<>();
        this.innertubeContext = INNERTUBE_CONTEXT;
        
        if (apiKey == null) {
            log.warn("YouTube API key not found in environment variables, falling back to alternative methods");
        }
        
        // Initialize session cookies
        initSession();
    }

    /**
     * Initialize a YouTube session to get cookies.
     */
    private void initSession() {
        try {
            HttpGet request = new HttpGet("https://www.youtube.com");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                // Store cookies for future requests
                cookies = HttpUtils.extractCookies(response);
                log.debug("Initialized YouTube session with {} cookies", cookies.size());
            }
        } catch (IOException e) {
            log.error("Failed to initialize YouTube session", e);
        }
    }

    /**
     * Refresh authentication by getting new session cookies.
     */
    public void refreshAuth() {
        log.info("Refreshing YouTube authentication session");
        initSession();
    }

    /**
     * Get video details using the best available method.
     *
     * @param videoId YouTube video ID
     * @return Video details as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    public JSONObject getVideoDetails(String videoId) throws IOException {
        if (apiKey != null) {
            // Try YouTube Data API first
            try {
                return getVideoDetailsFromDataApi(videoId);
            } catch (Exception e) {
                log.warn("Failed to get video details from Data API, falling back to InnerTube API", e);
            }
        }
        
        // Fall back to InnerTube API
        try {
            return getVideoDetailsFromInnertubeApi(videoId);
        } catch (Exception e) {
            log.warn("Failed to get video details from InnerTube API, falling back to web scraping", e);
            
            // Fall back to web scraping
            return getVideoDetailsFromWebPage(videoId);
        }
    }

    /**
     * Get video details from YouTube Data API.
     *
     * @param videoId YouTube video ID
     * @return Video details as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    private JSONObject getVideoDetailsFromDataApi(String videoId) throws IOException {
        String url = DATA_API_BASE_URL + "/videos?id=" + videoId + "&part=snippet,contentDetails,statistics&key=" + apiKey;
        
        HttpGet request = new HttpGet(url);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = new JSONObject(responseBody);
                JSONArray items = json.getJSONArray("items");
                
                if (items.length() > 0) {
                    JSONObject item = items.getJSONObject(0);
                    JSONObject snippet = item.getJSONObject("snippet");
                    JSONObject contentDetails = item.getJSONObject("contentDetails");
                    
                    // Convert to a standardized format
                    JSONObject result = new JSONObject();
                    result.put("videoId", videoId);
                    result.put("title", snippet.getString("title"));
                    result.put("author", snippet.getString("channelTitle"));
                    
                    // Convert ISO 8601 duration to seconds
                    String duration = contentDetails.getString("duration");
                    long seconds = HttpUtils.isoDurationToSeconds(duration);
                    result.put("lengthSeconds", seconds);
                    
                    return result;
                }
            }
            
            throw new IOException("Failed to get video details from Data API: " + responseBody);
        }
    }

    /**
     * Get video details from YouTube InnerTube API.
     *
     * @param videoId YouTube video ID
     * @return Video details as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    private JSONObject getVideoDetailsFromInnertubeApi(String videoId) throws IOException {
        String url = INNERTUBE_BASE_URL + "/player?key=" + INNERTUBE_API_KEY;
        
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", "application/json");
        
        // Add cookies if available
        if (!cookies.isEmpty()) {
            request.addHeader("Cookie", HttpUtils.formatCookies(cookies));
        }
        
        // Create payload
        JSONObject payload = new JSONObject();
        payload.put("videoId", videoId);
        payload.put("context", new JSONObject(innertubeContext));
        
        request.setEntity(new StringEntity(payload.toString()));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = new JSONObject(responseBody);
                
                // Extract basic video details
                JSONObject videoDetails = json.getJSONObject("videoDetails");
                
                // Convert to a standardized format
                JSONObject result = new JSONObject();
                result.put("videoId", videoDetails.getString("videoId"));
                result.put("title", videoDetails.getString("title"));
                result.put("author", videoDetails.getString("author"));
                result.put("lengthSeconds", Long.parseLong(videoDetails.getString("lengthSeconds")));
                
                return result;
            }
            
            throw new IOException("Failed to get video details from InnerTube API: " + responseBody);
        }
    }

    /**
     * Get video details by scraping the YouTube web page.
     *
     * @param videoId YouTube video ID
     * @return Video details as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    private JSONObject getVideoDetailsFromWebPage(String videoId) throws IOException {
        String url = "https://www.youtube.com/watch?v=" + videoId;
        
        HttpGet request = new HttpGet(url);
        
        // Add cookies if available
        if (!cookies.isEmpty()) {
            request.addHeader("Cookie", HttpUtils.formatCookies(cookies));
        }
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                // Look for ytInitialPlayerResponse in the page
                Map<String, Object> initialData = HttpUtils.extractInitialData(responseBody, "ytInitialPlayerResponse");
                
                if (initialData != null && initialData.containsKey("videoDetails")) {
                    Map<String, Object> videoDetails = (Map<String, Object>) initialData.get("videoDetails");
                    
                    // Convert to a standardized format
                    JSONObject result = new JSONObject();
                    result.put("videoId", videoDetails.get("videoId"));
                    result.put("title", videoDetails.get("title"));
                    result.put("author", videoDetails.get("author"));
                    result.put("lengthSeconds", Long.parseLong(videoDetails.get("lengthSeconds").toString()));
                    
                    return result;
                }
            }
            
            throw new IOException("Failed to get video details from web page");
        }
    }

    /**
     * Get playlist details using the best available method.
     *
     * @param playlistId YouTube playlist ID
     * @return Playlist details as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    public JSONObject getPlaylistDetails(String playlistId) throws IOException {
        if (apiKey != null) {
            // Try YouTube Data API first
            try {
                return getPlaylistDetailsFromDataApi(playlistId);
            } catch (Exception e) {
                log.warn("Failed to get playlist details from Data API, falling back to InnerTube API", e);
            }
        }
        
        // Fall back to InnerTube API
        return getPlaylistDetailsFromInnertubeApi(playlistId);
    }

    /**
     * Get playlist details from YouTube Data API.
     *
     * @param playlistId YouTube playlist ID
     * @return Playlist details as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    private JSONObject getPlaylistDetailsFromDataApi(String playlistId) throws IOException {
        // Get playlist info
        String playlistUrl = DATA_API_BASE_URL + "/playlists?id=" + playlistId + "&part=snippet&key=" + apiKey;
        HttpGet playlistRequest = new HttpGet(playlistUrl);
        
        JSONObject playlistInfo;
        try (CloseableHttpResponse response = httpClient.execute(playlistRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = new JSONObject(responseBody);
                JSONArray items = json.getJSONArray("items");
                
                if (items.length() > 0) {
                    playlistInfo = items.getJSONObject(0).getJSONObject("snippet");
                } else {
                    throw new IOException("Playlist not found");
                }
            } else {
                throw new IOException("Failed to get playlist details: " + responseBody);
            }
        }
        
        // Get playlist items
        String itemsUrl = DATA_API_BASE_URL + "/playlistItems?playlistId=" + playlistId 
                + "&part=snippet,contentDetails&maxResults=50&key=" + apiKey;
        HttpGet itemsRequest = new HttpGet(itemsUrl);
        
        JSONArray videos = new JSONArray();
        try (CloseableHttpResponse response = httpClient.execute(itemsRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = new JSONObject(responseBody);
                JSONArray items = json.getJSONArray("items");
                
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    JSONObject snippet = item.getJSONObject("snippet");
                    String videoId = item.getJSONObject("contentDetails").getString("videoId");
                    
                    JSONObject videoDetails = new JSONObject();
                    videoDetails.put("videoId", videoId);
                    videoDetails.put("title", snippet.getString("title"));
                    videoDetails.put("author", snippet.optString("videoOwnerChannelTitle", "Unknown"));
                    
                    // We don't have length information from playlist items, so use a placeholder
                    videoDetails.put("lengthSeconds", 0);
                    
                    videos.put(videoDetails);
                }
            } else {
                throw new IOException("Failed to get playlist items: " + responseBody);
            }
        }
        
        // Assemble result
        JSONObject result = new JSONObject();
        result.put("playlistId", playlistId);
        result.put("title", playlistInfo.getString("title"));
        result.put("videos", videos);
        
        return result;
    }

    /**
     * Get playlist details from YouTube InnerTube API.
     *
     * @param playlistId YouTube playlist ID
     * @return Playlist details as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    private JSONObject getPlaylistDetailsFromInnertubeApi(String playlistId) throws IOException {
        String url = INNERTUBE_BASE_URL + "/browse?key=" + INNERTUBE_API_KEY;
        
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", "application/json");
        
        // Add cookies if available
        if (!cookies.isEmpty()) {
            request.addHeader("Cookie", HttpUtils.formatCookies(cookies));
        }
        
        // Create payload
        JSONObject payload = new JSONObject();
        payload.put("browseId", "VL" + playlistId);
        payload.put("context", new JSONObject(innertubeContext));
        
        request.setEntity(new StringEntity(payload.toString()));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = new JSONObject(responseBody);
                
                // Extract basic playlist details
                String title = "YouTube Playlist";
                try {
                    if (json.has("header")) {
                        title = json.getJSONObject("header")
                                .getJSONObject("playlistHeaderRenderer")
                                .getJSONObject("title")
                                .getString("simpleText");
                    }
                } catch (Exception e) {
                    log.warn("Could not extract playlist title", e);
                }
                
                // Extract videos
                JSONArray videos = new JSONArray();
                try {
                    JSONArray contents = json.getJSONObject("contents")
                            .getJSONObject("twoColumnBrowseResultsRenderer")
                            .getJSONArray("tabs").getJSONObject(0)
                            .getJSONObject("tabRenderer")
                            .getJSONObject("content")
                            .getJSONObject("sectionListRenderer")
                            .getJSONArray("contents").getJSONObject(0)
                            .getJSONObject("itemSectionRenderer")
                            .getJSONArray("contents").getJSONObject(0)
                            .getJSONObject("playlistVideoListRenderer")
                            .getJSONArray("contents");
                    
                    for (int i = 0; i < contents.length(); i++) {
                        if (contents.getJSONObject(i).has("playlistVideoRenderer")) {
                            JSONObject renderer = contents.getJSONObject(i).getJSONObject("playlistVideoRenderer");
                            String videoId = renderer.getString("videoId");
                            
                            String videoTitle = "Unknown";
                            try {
                                videoTitle = renderer.getJSONObject("title").getString("simpleText");
                            } catch (Exception e) {
                                try {
                                    videoTitle = renderer.getJSONObject("title").getJSONArray("runs")
                                            .getJSONObject(0).getString("text");
                                } catch (Exception e2) {
                                    log.warn("Could not extract video title", e2);
                                }
                            }
                            
                            String videoAuthor = "Unknown";
                            try {
                                videoAuthor = renderer.getJSONObject("shortBylineText").getJSONArray("runs")
                                        .getJSONObject(0).getString("text");
                            } catch (Exception e) {
                                log.warn("Could not extract video author", e);
                            }
                            
                            long lengthSeconds = 0;
                            try {
                                String lengthText = renderer.getJSONObject("lengthText").getString("simpleText");
                                lengthSeconds = HttpUtils.timeTextToSeconds(lengthText);
                            } catch (Exception e) {
                                log.warn("Could not extract video length", e);
                            }
                            
                            JSONObject videoDetails = new JSONObject();
                            videoDetails.put("videoId", videoId);
                            videoDetails.put("title", videoTitle);
                            videoDetails.put("author", videoAuthor);
                            videoDetails.put("lengthSeconds", lengthSeconds);
                            
                            videos.put(videoDetails);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not extract playlist videos", e);
                }
                
                // Assemble result
                JSONObject result = new JSONObject();
                result.put("playlistId", playlistId);
                result.put("title", title);
                result.put("videos", videos);
                
                return result;
            }
            
            throw new IOException("Failed to get playlist details from InnerTube API: " + responseBody);
        }
    }

    /**
     * Search for videos using the best available method.
     *
     * @param query Search query
     * @return Search results as JSONArray
     * @throws IOException if an error occurs during the API call
     */
    public JSONArray searchVideos(String query) throws IOException {
        if (apiKey != null) {
            // Try YouTube Data API first
            try {
                return searchVideosFromDataApi(query);
            } catch (Exception e) {
                log.warn("Failed to search videos from Data API, falling back to InnerTube API", e);
            }
        }
        
        // Fall back to InnerTube API
        return searchVideosFromInnertubeApi(query);
    }

    /**
     * Search for videos using YouTube Data API.
     *
     * @param query Search query
     * @return Search results as JSONArray
     * @throws IOException if an error occurs during the API call
     */
    private JSONArray searchVideosFromDataApi(String query) throws IOException {
        String url = DATA_API_BASE_URL + "/search?q=" + HttpUtils.encodeUrl(query) 
                + "&part=snippet&type=video&maxResults=10&key=" + apiKey;
        
        HttpGet request = new HttpGet(url);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = new JSONObject(responseBody);
                JSONArray items = json.getJSONArray("items");
                JSONArray results = new JSONArray();
                
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    JSONObject snippet = item.getJSONObject("snippet");
                    String videoId = item.getJSONObject("id").getString("videoId");
                    
                    JSONObject videoDetails = new JSONObject();
                    videoDetails.put("videoId", videoId);
                    videoDetails.put("title", snippet.getString("title"));
                    videoDetails.put("author", snippet.getString("channelTitle"));
                    
                    // We don't have length information from search results, so use a placeholder
                    videoDetails.put("lengthSeconds", 0);
                    
                    results.put(videoDetails);
                }
                
                return results;
            }
            
            throw new IOException("Failed to search videos from Data API: " + responseBody);
        }
    }

    /**
     * Search for videos using YouTube InnerTube API.
     *
     * @param query Search query
     * @return Search results as JSONArray
     * @throws IOException if an error occurs during the API call
     */
    private JSONArray searchVideosFromInnertubeApi(String query) throws IOException {
        String url = INNERTUBE_BASE_URL + "/search?key=" + INNERTUBE_API_KEY;
        
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", "application/json");
        
        // Add cookies if available
        if (!cookies.isEmpty()) {
            request.addHeader("Cookie", HttpUtils.formatCookies(cookies));
        }
        
        // Create payload
        JSONObject payload = new JSONObject();
        payload.put("query", query);
        payload.put("context", new JSONObject(innertubeContext));
        
        request.setEntity(new StringEntity(payload.toString()));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = new JSONObject(responseBody);
                JSONArray results = new JSONArray();
                
                try {
                    JSONArray contents = json.getJSONObject("contents")
                            .getJSONObject("twoColumnSearchResultsRenderer")
                            .getJSONObject("primaryContents")
                            .getJSONObject("sectionListRenderer")
                            .getJSONArray("contents").getJSONObject(0)
                            .getJSONObject("itemSectionRenderer")
                            .getJSONArray("contents");
                    
                    for (int i = 0; i < contents.length(); i++) {
                        JSONObject content = contents.getJSONObject(i);
                        
                        if (content.has("videoRenderer")) {
                            JSONObject renderer = content.getJSONObject("videoRenderer");
                            String videoId = renderer.getString("videoId");
                            
                            String videoTitle = "Unknown";
                            try {
                                videoTitle = renderer.getJSONObject("title").getJSONArray("runs")
                                        .getJSONObject(0).getString("text");
                            } catch (Exception e) {
                                log.warn("Could not extract video title", e);
                            }
                            
                            String videoAuthor = "Unknown";
                            try {
                                videoAuthor = renderer.getJSONObject("ownerText").getJSONArray("runs")
                                        .getJSONObject(0).getString("text");
                            } catch (Exception e) {
                                log.warn("Could not extract video author", e);
                            }
                            
                            long lengthSeconds = 0;
                            try {
                                if (renderer.has("lengthText")) {
                                    String lengthText = renderer.getJSONObject("lengthText").getString("simpleText");
                                    lengthSeconds = HttpUtils.timeTextToSeconds(lengthText);
                                }
                            } catch (Exception e) {
                                log.warn("Could not extract video length", e);
                            }
                            
                            JSONObject videoDetails = new JSONObject();
                            videoDetails.put("videoId", videoId);
                            videoDetails.put("title", videoTitle);
                            videoDetails.put("author", videoAuthor);
                            videoDetails.put("lengthSeconds", lengthSeconds);
                            
                            results.put(videoDetails);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not extract search results", e);
                }
                
                return results;
            }
            
            throw new IOException("Failed to search videos from InnerTube API: " + responseBody);
        }
    }

    /**
     * Get stream information for a YouTube video.
     *
     * @param videoId YouTube video ID
     * @return Map with stream information (url, contentType)
     * @throws IOException if an error occurs during the API call
     */
    public Map<String, String> getStreamInfo(String videoId) throws IOException {
        // Use InnerTube API to get stream information
        String url = INNERTUBE_BASE_URL + "/player?key=" + INNERTUBE_API_KEY;
        
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", "application/json");
        
        // Add cookies if available
        if (!cookies.isEmpty()) {
            request.addHeader("Cookie", HttpUtils.formatCookies(cookies));
        }
        
        // Create payload
        JSONObject payload = new JSONObject();
        payload.put("videoId", videoId);
        payload.put("context", new JSONObject(innertubeContext));
        
        request.setEntity(new StringEntity(payload.toString()));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = new JSONObject(responseBody);
                
                // Find an audio-only stream
                if (json.has("streamingData")) {
                    JSONObject streamingData = json.getJSONObject("streamingData");
                    
                    // First, try to find an audio-only adaptive format
                    if (streamingData.has("adaptiveFormats")) {
                        JSONArray adaptiveFormats = streamingData.getJSONArray("adaptiveFormats");
                        
                        for (int i = 0; i < adaptiveFormats.length(); i++) {
                            JSONObject format = adaptiveFormats.getJSONObject(i);
                            
                            String mimeType = format.getString("mimeType");
                            if (mimeType.startsWith("audio/")) {
                                Map<String, String> result = new HashMap<>();
                                result.put("url", format.getString("url"));
                                result.put("contentType", mimeType);
                                result.put("bitrate", String.valueOf(format.getInt("bitrate")));
                                
                                return result;
                            }
                        }
                    }
                    
                    // If no audio-only format is found, use a regular format
                    if (streamingData.has("formats")) {
                        JSONArray formats = streamingData.getJSONArray("formats");
                        
                        if (formats.length() > 0) {
                            JSONObject format = formats.getJSONObject(0);
                            
                            Map<String, String> result = new HashMap<>();
                            result.put("url", format.getString("url"));
                            result.put("contentType", format.getString("mimeType"));
                            
                            return result;
                        }
                    }
                }
            }
            
            throw new IOException("Failed to get stream information for video " + videoId);
        }
    }
}
