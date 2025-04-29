package com.github.youtubeify.source.youtube;

import com.github.youtubeify.auth.YouTubeAuthManager;
import com.github.youtubeify.util.HttpUtils;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;

/**
 * Audio source manager that implements YouTube video support.
 */
public class YouTubeAudioSourceManager implements AudioSourceManager, HttpConfigurable {

    private static final Logger log = LoggerFactory.getLogger(YouTubeAudioSourceManager.class);

    private static final String YOUTUBE_DOMAIN_PATTERN = "(?:youtube\\.com|youtu\\.be)";
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("^([a-zA-Z0-9_-]{11})$");
    private static final Pattern VIDEO_URL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?(?:" + YOUTUBE_DOMAIN_PATTERN + ")/(?:watch\\?v=|shorts/)([a-zA-Z0-9_-]{11})(?:&.*|\\?.*)?$"
    );
    private static final Pattern PLAYLIST_URL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?(?:" + YOUTUBE_DOMAIN_PATTERN + ")/playlist\\?list=([a-zA-Z0-9_-]+)(?:&.*|\\?.*)?$"
    );
    private static final Pattern SHORT_URL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?(?:youtu\\.be)/([a-zA-Z0-9_-]{11})(?:\\?.*)?$"
    );
    private static final Pattern SEARCH_PATTERN = Pattern.compile("^ytsearch:(.*)$");

    private final HttpInterfaceManager httpInterfaceManager;
    private final YouTubeAuthManager authManager;

    public YouTubeAudioSourceManager() {
        this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
        this.authManager = new YouTubeAuthManager();
        log.info("YouTube audio source manager initialized");
    }

    @Override
    public String getSourceName() {
        return "youtube";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        String identifier = reference.identifier;

        // Direct video ID
        Matcher videoIdMatcher = VIDEO_ID_PATTERN.matcher(identifier);
        if (videoIdMatcher.matches()) {
            return loadVideo(videoIdMatcher.group(1));
        }

        // Video URL
        Matcher videoUrlMatcher = VIDEO_URL_PATTERN.matcher(identifier);
        if (videoUrlMatcher.matches()) {
            return loadVideo(videoUrlMatcher.group(1));
        }

        // Short URL
        Matcher shortUrlMatcher = SHORT_URL_PATTERN.matcher(identifier);
        if (shortUrlMatcher.matches()) {
            return loadVideo(shortUrlMatcher.group(1));
        }

        // Playlist URL
        Matcher playlistMatcher = PLAYLIST_URL_PATTERN.matcher(identifier);
        if (playlistMatcher.matches()) {
            return loadPlaylist(playlistMatcher.group(1));
        }

        // Search query
        Matcher searchMatcher = SEARCH_PATTERN.matcher(identifier);
        if (searchMatcher.matches()) {
            return loadSearch(searchMatcher.group(1));
        }

        return null;
    }

    private AudioItem loadVideo(String videoId) {
        try {
            JSONObject videoDetails = authManager.getVideoDetails(videoId);
            
            if (videoDetails == null) {
                throw new FriendlyException("This video does not exist.", Severity.COMMON, null);
            }
            
            // Extract basic info from video details
            String title = videoDetails.getString("title");
            String uploader = videoDetails.getString("author");
            long duration = videoDetails.getLong("lengthSeconds") * 1000; // Convert to milliseconds
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
            
            YouTubeAudioTrackInfo trackInfo = new YouTubeAudioTrackInfo(
                    title,
                    uploader,
                    duration,
                    videoId,
                    false,
                    videoUrl
            );
            
            return new YouTubeAudioTrack(trackInfo, this);
        } catch (IOException e) {
            throw new FriendlyException("Error loading YouTube video", Severity.FAULT, e);
        }
    }

    private AudioItem loadPlaylist(String playlistId) {
        try {
            JSONObject playlistDetails = authManager.getPlaylistDetails(playlistId);
            
            if (playlistDetails == null) {
                throw new FriendlyException("This playlist does not exist.", Severity.COMMON, null);
            }
            
            String playlistName = playlistDetails.getString("title");
            JSONArray videos = playlistDetails.getJSONArray("videos");
            List<AudioTrack> tracks = new ArrayList<>();
            
            for (int i = 0; i < videos.length(); i++) {
                JSONObject video = videos.getJSONObject(i);
                String videoId = video.getString("videoId");
                String title = video.getString("title");
                String uploader = video.getString("author");
                long duration = video.getLong("lengthSeconds") * 1000; // Convert to milliseconds
                String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
                
                YouTubeAudioTrackInfo trackInfo = new YouTubeAudioTrackInfo(
                        title,
                        uploader,
                        duration,
                        videoId,
                        false,
                        videoUrl
                );
                
                tracks.add(new YouTubeAudioTrack(trackInfo, this));
            }
            
            return new BasicAudioPlaylist(playlistName, tracks, null, false);
        } catch (IOException e) {
            throw new FriendlyException("Error loading YouTube playlist", Severity.FAULT, e);
        }
    }

    private AudioItem loadSearch(String query) {
        try {
            JSONArray searchResults = authManager.searchVideos(query);
            
            if (searchResults == null || searchResults.length() == 0) {
                return AudioReference.NO_TRACK;
            }
            
            List<AudioTrack> tracks = new ArrayList<>();
            
            for (int i = 0; i < Math.min(searchResults.length(), 10); i++) {
                JSONObject video = searchResults.getJSONObject(i);
                String videoId = video.getString("videoId");
                String title = video.getString("title");
                String uploader = video.getString("author");
                long duration = video.getLong("lengthSeconds") * 1000; // Convert to milliseconds
                String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
                
                YouTubeAudioTrackInfo trackInfo = new YouTubeAudioTrackInfo(
                        title,
                        uploader,
                        duration,
                        videoId,
                        false,
                        videoUrl
                );
                
                tracks.add(new YouTubeAudioTrack(trackInfo, this));
            }
            
            return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
        } catch (IOException e) {
            throw new FriendlyException("Error searching YouTube", Severity.FAULT, e);
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return track instanceof YouTubeAudioTrack;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        YouTubeAudioTrack youtubeTrack = (YouTubeAudioTrack) track;
        YouTubeAudioTrackInfo trackInfo = (YouTubeAudioTrackInfo) youtubeTrack.getInfo();
        
        output.writeUTF(trackInfo.getVideoId());
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        String videoId = input.readUTF();
        
        if (trackInfo instanceof YouTubeAudioTrackInfo) {
            return new YouTubeAudioTrack((YouTubeAudioTrackInfo) trackInfo, this);
        } else {
            // Convert regular track info to YouTube track info
            YouTubeAudioTrackInfo youtubeTrackInfo = new YouTubeAudioTrackInfo(
                    trackInfo.title,
                    trackInfo.author,
                    trackInfo.length,
                    videoId,
                    trackInfo.isStream,
                    "https://www.youtube.com/watch?v=" + videoId
            );
            
            return new YouTubeAudioTrack(youtubeTrackInfo, this);
        }
    }

    @Override
    public void shutdown() {
        httpInterfaceManager.close();
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
    }

    /**
     * Get an HTTP interface for making requests.
     *
     * @return HTTP interface
     */
    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }
}
