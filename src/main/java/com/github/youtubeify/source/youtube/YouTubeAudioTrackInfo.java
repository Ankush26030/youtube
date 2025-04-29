package com.github.youtubeify.source.youtube;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

/**
 * YouTube-specific track info that includes the video ID.
 */
public class YouTubeAudioTrackInfo extends AudioTrackInfo {

    private final String videoId;

    /**
     * @param title Track title
     * @param author Track author
     * @param length Track length in milliseconds
     * @param videoId YouTube video ID
     * @param isStream Whether this track is a livestream
     * @param uri Track URI
     */
    public YouTubeAudioTrackInfo(String title, String author, long length, String videoId, boolean isStream, String uri) {
        super(title, author, length, videoId, isStream, uri);
        this.videoId = videoId;
    }

    /**
     * @return YouTube video ID
     */
    public String getVideoId() {
        return videoId;
    }
}
