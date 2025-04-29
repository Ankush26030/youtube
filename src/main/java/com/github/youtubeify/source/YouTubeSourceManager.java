package com.github.youtubeify.source;

import com.github.youtubeify.auth.YouTubeAuthManager;
import com.github.youtubeify.source.youtube.YouTubeAudioSourceManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Source manager for YouTube tracks.
 */
public class YouTubeSourceManager implements AudioSourceManager, HttpConfigurable {

    private static final Logger log = LoggerFactory.getLogger(YouTubeSourceManager.class);

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

    private final YouTubeAudioSourceManager internalSourceManager;
    private final HttpInterfaceManager httpInterfaceManager;
    private final YouTubeAuthManager authManager;

    public YouTubeSourceManager() {
        this.internalSourceManager = new YouTubeAudioSourceManager();
        this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
        this.authManager = new YouTubeAuthManager();
        
        log.info("YouTube source manager initialized");
    }

    @Override
    public String getSourceName() {
        return "youtube";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            return internalSourceManager.loadItem(manager, reference);
        } catch (FriendlyException e) {
            log.error("Error loading YouTube item: {}", e.getMessage());
            
            // If the error is related to rate limiting or IP blocking, we can try to refresh
            // the authentication or use a different approach
            if (e.getMessage().contains("429") || e.getMessage().contains("403")) {
                log.info("Attempting to refresh YouTube authentication");
                authManager.refreshAuth();
                
                // Try one more time
                try {
                    return internalSourceManager.loadItem(manager, reference);
                } catch (FriendlyException retryException) {
                    log.error("Failed to load YouTube item after authentication refresh: {}", retryException.getMessage());
                    throw retryException;
                }
            }
            
            throw e;
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return internalSourceManager.isTrackEncodable(track);
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        internalSourceManager.encodeTrack(track, output);
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return internalSourceManager.decodeTrack(trackInfo, input);
    }

    @Override
    public void shutdown() {
        internalSourceManager.shutdown();
        httpInterfaceManager.close();
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
        internalSourceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
        internalSourceManager.configureBuilder(configurator);
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
