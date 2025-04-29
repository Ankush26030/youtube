package com.github.youtubeify;

import com.github.youtubeify.source.SpotifySourceManager;
import com.github.youtubeify.source.YouTubeSourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import dev.arbjerg.lavalink.api.AudioPlayerManagerConfiguration;
import dev.arbjerg.lavalink.api.IPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Main plugin class for YouTubeify.
 * This plugin enables long-term stable playback of YouTube and Spotify tracks in Lavalink
 * without requiring patches.
 */
@Component
public class YouTubeifyPlugin implements IPlugin, AudioPlayerManagerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(YouTubeifyPlugin.class);

    private final YouTubeSourceManager youtubeSourceManager;
    private final SpotifySourceManager spotifySourceManager;

    public YouTubeifyPlugin() {
        this.youtubeSourceManager = new YouTubeSourceManager();
        this.spotifySourceManager = new SpotifySourceManager();
        log.info("YouTubeify plugin initialized");
    }

    @Override
    public String getName() {
        return "YouTubeify";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void configurePlayerManager(AudioPlayerManager audioPlayerManager) {
        // Remove the default YouTube source manager if present
        try {
            audioPlayerManager.getSourceManagers().removeIf(sourceManager ->
                    sourceManager.getClass().getSimpleName().contains("Youtube"));
            log.info("Removed default YouTube source manager");
        } catch (Exception e) {
            log.warn("Could not remove default YouTube source manager", e);
        }

        // Register our source managers
        audioPlayerManager.registerSourceManager(youtubeSourceManager);
        audioPlayerManager.registerSourceManager(spotifySourceManager);
        log.info("Registered YouTubeify source managers");
    }

    @Override
    public List<AudioPlayerManagerConfiguration> getAudioPlayerManagerConfigurations() {
        return Collections.singletonList(this);
    }
}
