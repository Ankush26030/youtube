package com.github.youtubeify.source.spotify;

import com.github.youtubeify.source.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio track that delegates its functionality to another track (YouTube).
 * This is essentially a wrapper around a YouTube track with Spotify metadata.
 */
public class SpotifyAudioTrack extends DelegatedAudioTrack {

    private static final Logger log = LoggerFactory.getLogger(SpotifyAudioTrack.class);

    private final AudioTrack delegate;
    private final SpotifySourceManager sourceManager;

    /**
     * @param trackInfo Track info
     * @param delegate The YouTube track that will handle the actual playback
     * @param sourceManager Source manager which was used to find this track
     */
    public SpotifyAudioTrack(AudioTrackInfo trackInfo, AudioTrack delegate, SpotifySourceManager sourceManager) {
        super(trackInfo);
        this.delegate = delegate;
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        log.debug("Processing Spotify track {} via YouTube delegate", getIdentifier());
        processDelegate(executor, delegate);
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new SpotifyAudioTrack(trackInfo, delegate.makeClone(), sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }

    /**
     * @return The YouTube track that handles the actual playback
     */
    public AudioTrack getDelegate() {
        return delegate;
    }
}
