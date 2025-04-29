package com.github.youtubeify.source.spotify;

import com.github.youtubeify.auth.SpotifyAuthManager;
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

/**
 * Bare-bones implementation of Spotify source manager.
 * This is primarily used as a helper for the main source manager.
 */
public class SpotifyAudioSourceManager implements AudioSourceManager, HttpConfigurable {

    private static final Logger log = LoggerFactory.getLogger(SpotifyAudioSourceManager.class);

    private final HttpInterfaceManager httpInterfaceManager;
    private final SpotifyAuthManager authManager;

    public SpotifyAudioSourceManager() {
        this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
        this.authManager = new SpotifyAuthManager();
        log.info("Spotify audio source manager initialized");
    }

    @Override
    public String getSourceName() {
        return "spotify-internal";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        // This implementation is intentionally minimal since the main source manager handles loading
        return null;
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return track instanceof SpotifyAudioTrack;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        // Basic information only, the main source manager handles the complex encoding
        output.writeUTF(track.getInfo().identifier);
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        // Since Spotify tracks are proxied through YouTube, this is a placeholder
        throw new UnsupportedOperationException("Direct decoding of Spotify tracks is not supported");
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
