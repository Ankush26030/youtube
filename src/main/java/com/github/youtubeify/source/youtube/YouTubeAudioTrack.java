package com.github.youtubeify.source.youtube;

import com.github.youtubeify.util.HttpUtils;
import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;

/**
 * Audio track that handles processing YouTube tracks.
 */
public class YouTubeAudioTrack extends DelegatedAudioTrack {

    private static final Logger log = LoggerFactory.getLogger(YouTubeAudioTrack.class);

    private final YouTubeAudioSourceManager sourceManager;

    /**
     * @param trackInfo Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public YouTubeAudioTrack(YouTubeAudioTrackInfo trackInfo, YouTubeAudioSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    /**
     * @param trackInfo Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public YouTubeAudioTrack(AudioTrackInfo trackInfo, YouTubeAudioSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            log.debug("Starting to process track {}", getIdentifier());
            
            String videoId = getVideoId();
            processStandardTrack(executor, httpInterface, videoId);
        }
    }

    private String getVideoId() {
        if (trackInfo instanceof YouTubeAudioTrackInfo) {
            return ((YouTubeAudioTrackInfo) trackInfo).getVideoId();
        } else {
            // Extract from URL or identifier
            String identifier = trackInfo.identifier;
            
            if (identifier.length() == 11) {
                return identifier; // Assume it's a video ID
            }
            
            // Extract from URL
            String videoId = HttpUtils.extractVideoIdFromUrl(identifier);
            if (videoId != null) {
                return videoId;
            }
            
            throw new FriendlyException("Could not extract video ID from track", Severity.FAULT, null);
        }
    }

    private void processStandardTrack(LocalAudioTrackExecutor executor, HttpInterface httpInterface, String videoId) throws Exception {
        try {
            // Get stream URL from InnerTube API (this would be implemented in YouTubeAuthManager)
            Map<String, String> streamInfo = sourceManager.authManager.getStreamInfo(videoId);
            
            if (streamInfo == null || !streamInfo.containsKey("url")) {
                throw new FriendlyException("Could not find stream URL for video " + videoId, Severity.SUSPICIOUS, null);
            }
            
            String streamUrl = streamInfo.get("url");
            String contentType = streamInfo.getOrDefault("contentType", "audio/mp4");
            
            log.debug("Starting audio stream for video {} with URL: {}", videoId, streamUrl);
            
            try (InputStream stream = new URL(streamUrl).openStream()) {
                if (contentType.contains("audio/mp4") || contentType.contains("audio/mpeg")) {
                    // Process as MP3
                    processAsMP3(executor, stream);
                } else {
                    // Process with appropriate format handler
                    processWithAppropriateFdormat(executor, stream, contentType);
                }
            }
        } catch (Exception e) {
            log.error("Error processing YouTube track {}", videoId, e);
            throw new FriendlyException("Error processing YouTube track: " + e.getMessage(), Severity.FAULT, e);
        }
    }

    private void processAsMP3(LocalAudioTrackExecutor executor, InputStream stream) throws Exception {
        // Create an MP3 track from the stream and process it
        AudioTrack delegateTrack = new Mp3AudioTrack(trackInfo, stream);
        processDelegate(executor, delegateTrack);
    }

    private void processWithAppropriateFdormat(LocalAudioTrackExecutor executor, InputStream stream, String contentType) throws Exception {
        // This would handle other formats if needed
        // For simplicity, this example just uses MP3 processing for all
        processAsMP3(executor, stream);
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new YouTubeAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
