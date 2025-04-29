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
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Manages authentication and API calls to Spotify Web API.
 */
public class SpotifyAuthManager {

    private static final Logger log = LoggerFactory.getLogger(SpotifyAuthManager.class);

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String API_BASE_URL = "https://api.spotify.com/v1";
    
    private String accessToken;
    private long tokenExpiry;
    
    private final String clientId;
    private final String clientSecret;
    private final CloseableHttpClient httpClient;

    public SpotifyAuthManager() {
        this.clientId = System.getenv("SPOTIFY_CLIENT_ID");
        this.clientSecret = System.getenv("SPOTIFY_CLIENT_SECRET");
        this.httpClient = HttpClients.createDefault();
        
        if (clientId == null || clientSecret == null) {
            log.warn("Spotify client credentials not found in environment variables");
        } else {
            authenticate();
        }
    }

    /**
     * Authenticate with Spotify API.
     */
    private void authenticate() {
        if (clientId == null || clientSecret == null) {
            log.error("Cannot authenticate with Spotify: missing client credentials");
            return;
        }

        try {
            HttpPost request = new HttpPost(TOKEN_URL);
            
            // Create Basic Auth header
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            request.addHeader("Authorization", "Basic " + encodedAuth);
            
            // Set form parameters
            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.setEntity(new StringEntity("grant_type=client_credentials"));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    JSONObject json = new JSONObject(responseBody);
                    
                    accessToken = json.getString("access_token");
                    int expiresIn = json.getInt("expires_in");
                    
                    // Set expiry time (with a small buffer)
                    tokenExpiry = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiresIn - 60);
                    
                    log.info("Spotify authentication successful, token expires in {} seconds", expiresIn);
                } else {
                    log.error("Spotify authentication failed: {}", responseBody);
                }
            }
        } catch (IOException e) {
            log.error("Error authenticating with Spotify", e);
        }
    }

    /**
     * Check if the current token is valid, refresh if needed.
     */
    private void ensureValidToken() {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiry) {
            log.info("Refreshing Spotify access token");
            authenticate();
        }
    }

    /**
     * Get track information from Spotify API.
     *
     * @param trackId Spotify track ID
     * @return Track information as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    public JSONObject getTrack(String trackId) throws IOException {
        ensureValidToken();
        
        if (accessToken == null) {
            log.error("Cannot get track: no valid Spotify access token");
            return null;
        }
        
        HttpGet request = new HttpGet(API_BASE_URL + "/tracks/" + trackId);
        request.addHeader("Authorization", "Bearer " + accessToken);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                return new JSONObject(responseBody);
            } else {
                log.error("Failed to get Spotify track {}: {}", trackId, responseBody);
                return null;
            }
        }
    }

    /**
     * Get album information from Spotify API.
     *
     * @param albumId Spotify album ID
     * @return Album information as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    public JSONObject getAlbum(String albumId) throws IOException {
        ensureValidToken();
        
        if (accessToken == null) {
            log.error("Cannot get album: no valid Spotify access token");
            return null;
        }
        
        HttpGet request = new HttpGet(API_BASE_URL + "/albums/" + albumId);
        request.addHeader("Authorization", "Bearer " + accessToken);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                return new JSONObject(responseBody);
            } else {
                log.error("Failed to get Spotify album {}: {}", albumId, responseBody);
                return null;
            }
        }
    }

    /**
     * Get playlist information from Spotify API.
     *
     * @param playlistId Spotify playlist ID
     * @return Playlist information as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    public JSONObject getPlaylist(String playlistId) throws IOException {
        ensureValidToken();
        
        if (accessToken == null) {
            log.error("Cannot get playlist: no valid Spotify access token");
            return null;
        }
        
        HttpGet request = new HttpGet(API_BASE_URL + "/playlists/" + playlistId);
        request.addHeader("Authorization", "Bearer " + accessToken);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                return new JSONObject(responseBody);
            } else {
                log.error("Failed to get Spotify playlist {}: {}", playlistId, responseBody);
                return null;
            }
        }
    }

    /**
     * Get artist information from Spotify API.
     *
     * @param artistId Spotify artist ID
     * @return Artist information as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    public JSONObject getArtist(String artistId) throws IOException {
        ensureValidToken();
        
        if (accessToken == null) {
            log.error("Cannot get artist: no valid Spotify access token");
            return null;
        }
        
        HttpGet request = new HttpGet(API_BASE_URL + "/artists/" + artistId);
        request.addHeader("Authorization", "Bearer " + accessToken);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                return new JSONObject(responseBody);
            } else {
                log.error("Failed to get Spotify artist {}: {}", artistId, responseBody);
                return null;
            }
        }
    }

    /**
     * Get artist's top tracks from Spotify API.
     *
     * @param artistId Spotify artist ID
     * @return Top tracks as JSONArray
     * @throws IOException if an error occurs during the API call
     */
    public JSONArray getArtistTopTracks(String artistId) throws IOException {
        ensureValidToken();
        
        if (accessToken == null) {
            log.error("Cannot get artist top tracks: no valid Spotify access token");
            return new JSONArray();
        }
        
        HttpGet request = new HttpGet(API_BASE_URL + "/artists/" + artistId + "/top-tracks?market=US");
        request.addHeader("Authorization", "Bearer " + accessToken);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject json = new JSONObject(responseBody);
                return json.getJSONArray("tracks");
            } else {
                log.error("Failed to get Spotify artist top tracks {}: {}", artistId, responseBody);
                return new JSONArray();
            }
        }
    }

    /**
     * Search for tracks on Spotify.
     *
     * @param query Search query
     * @return Search results as JSONObject
     * @throws IOException if an error occurs during the API call
     */
    public JSONObject searchTracks(String query) throws IOException {
        ensureValidToken();
        
        if (accessToken == null) {
            log.error("Cannot search tracks: no valid Spotify access token");
            return null;
        }
        
        String encodedQuery = HttpUtils.encodeUrl(query);
        HttpGet request = new HttpGet(API_BASE_URL + "/search?q=" + encodedQuery + "&type=track&limit=10");
        request.addHeader("Authorization", "Bearer " + accessToken);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                return new JSONObject(responseBody);
            } else {
                log.error("Failed to search Spotify tracks: {}", responseBody);
                return null;
            }
        }
    }
}
