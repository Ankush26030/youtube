package com.github.youtubeify.source.spotify;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

/**
 * Spotify-specific track info.
 */
public class SpotifyAudioTrackInfo {

    private final String title;
    private final String artist;
    private final long duration;
    private final String identifier;
    private final String uri;

    /**
     * @param title Track title
     * @param artist Track artist
     * @param duration Track duration in milliseconds
     * @param identifier Spotify track ID
     * @param uri Spotify track URI
     */
    public SpotifyAudioTrackInfo(String title, String artist, long duration, String identifier, String uri) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.identifier = identifier;
        this.uri = uri;
    }

    /**
     * @return Track title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return Track artist
     */
    public String getArtist() {
        return artist;
    }

    /**
     * @return Track duration in milliseconds
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @return Spotify track ID
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return Spotify track URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Convert to a standard AudioTrackInfo
     * 
     * @return Standard AudioTrackInfo
     */
    public AudioTrackInfo toAudioTrackInfo() {
        return new AudioTrackInfo(title, artist, duration, identifier, false, uri);
    }
}
