package com.github.youtubeify.source;

import com.github.youtubeify.auth.SpotifyAuthManager;
import com.github.youtubeify.source.spotify.SpotifyAudioSourceManager;
import com.github.youtubeify.source.spotify.SpotifyAudioTrack;
import com.github.youtubeify.source.spotify.SpotifyAudioTrackInfo;
import com.github.youtubeify.util.TrackUtils;
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
 * Source manager for Spotify tracks.
 */
public class SpotifySourceManager implements AudioSourceManager, HttpConfigurable {

    private static final Logger log = LoggerFactory.getLogger(SpotifySourceManager.class);

    private static final Pattern TRACK_PATTERN = Pattern.compile("^(?:https?://)?(?:open\\.)?spotify\\.com/track/([a-zA-Z0-9]+)(?:\\?.*)?$");
    private static final Pattern ALBUM_PATTERN = Pattern.compile("^(?:https?://)?(?:open\\.)?spotify\\.com/album/([a-zA-Z0-9]+)(?:\\?.*)?$");
    private static final Pattern PLAYLIST_PATTERN = Pattern.compile("^(?:https?://)?(?:open\\.)?spotify\\.com/playlist/([a-zA-Z0-9]+)(?:\\?.*)?$");
    private static final Pattern ARTIST_PATTERN = Pattern.compile("^(?:https?://)?(?:open\\.)?spotify\\.com/artist/([a-zA-Z0-9]+)(?:\\?.*)?$");

    private final SpotifyAudioSourceManager internalSourceManager;
    private final HttpInterfaceManager httpInterfaceManager;
    private final SpotifyAuthManager authManager;
    private final YouTubeSourceManager youtubeSourceManager;

    public SpotifySourceManager() {
        this.internalSourceManager = new SpotifyAudioSourceManager();
        this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
        this.authManager = new SpotifyAuthManager();
        this.youtubeSourceManager = new YouTubeSourceManager(); // For resolving Spotify tracks to YouTube
        
        log.info("Spotify source manager initialized");
    }

    @Override
    public String getSourceName() {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        String identifier = reference.identifier;

        Matcher trackMatcher = TRACK_PATTERN.matcher(identifier);
        if (trackMatcher.matches()) {
            return loadTrack(trackMatcher.group(1));
        }

        Matcher albumMatcher = ALBUM_PATTERN.matcher(identifier);
        if (albumMatcher.matches()) {
            return loadAlbum(albumMatcher.group(1));
        }

        Matcher playlistMatcher = PLAYLIST_PATTERN.matcher(identifier);
        if (playlistMatcher.matches()) {
            return loadPlaylist(playlistMatcher.group(1));
        }

        Matcher artistMatcher = ARTIST_PATTERN.matcher(identifier);
        if (artistMatcher.matches()) {
            return loadArtistTopTracks(artistMatcher.group(1));
        }

        return null;
    }

    private AudioItem loadTrack(String trackId) {
        try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
            JSONObject trackObj = authManager.getTrack(trackId);
            
            if (trackObj == null) {
                throw new FriendlyException("This Spotify track does not exist.", Severity.COMMON, null);
            }
            
            SpotifyAudioTrackInfo trackInfo = extractTrackInfo(trackObj);
            
            // Convert Spotify track to YouTube track for playback
            String searchQuery = trackInfo.getArtist() + " - " + trackInfo.getTitle();
            AudioItem youtubeTrack = resolveToYouTubeTrack(searchQuery);
            
            if (youtubeTrack instanceof AudioTrack) {
                AudioTrackInfo ytTrackInfo = ((AudioTrack) youtubeTrack).getInfo();
                
                // Create a new SpotifyAudioTrack with the YouTube track for actual playback
                return new SpotifyAudioTrack(
                        new AudioTrackInfo(
                                trackInfo.getTitle(),
                                trackInfo.getArtist(),
                                ytTrackInfo.length,
                                trackInfo.getIdentifier(),
                                false,
                                trackInfo.getUri()
                        ),
                        (AudioTrack) youtubeTrack,
                        this
                );
            } else {
                throw new FriendlyException(
                        "Could not find a YouTube match for Spotify track: " + trackInfo.getTitle(),
                        Severity.COMMON,
                        null
                );
            }
        } catch (IOException e) {
            throw new FriendlyException("Error loading Spotify track", Severity.FAULT, e);
        }
    }

    private AudioItem loadAlbum(String albumId) {
        try {
            JSONObject albumObj = authManager.getAlbum(albumId);
            
            if (albumObj == null) {
                throw new FriendlyException("This Spotify album does not exist.", Severity.COMMON, null);
            }
            
            String albumName = albumObj.getString("name");
            String albumArtist = TrackUtils.getMainArtistName(albumObj.getJSONArray("artists"));
            
            JSONArray tracks = albumObj.getJSONObject("tracks").getJSONArray("items");
            List<AudioTrack> trackList = new ArrayList<>();
            
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject trackObj = tracks.getJSONObject(i);
                SpotifyAudioTrackInfo trackInfo = extractTrackInfo(trackObj);
                
                // Resolve each track to YouTube
                String searchQuery = trackInfo.getArtist() + " - " + trackInfo.getTitle();
                AudioItem youtubeTrack = resolveToYouTubeTrack(searchQuery);
                
                if (youtubeTrack instanceof AudioTrack) {
                    AudioTrackInfo ytTrackInfo = ((AudioTrack) youtubeTrack).getInfo();
                    
                    trackList.add(new SpotifyAudioTrack(
                            new AudioTrackInfo(
                                    trackInfo.getTitle(),
                                    trackInfo.getArtist(),
                                    ytTrackInfo.length,
                                    trackInfo.getIdentifier(),
                                    false,
                                    trackInfo.getUri()
                            ),
                            (AudioTrack) youtubeTrack,
                            this
                    ));
                }
            }
            
            return new BasicAudioPlaylist(albumName + " - " + albumArtist, trackList, null, false);
        } catch (IOException e) {
            throw new FriendlyException("Error loading Spotify album", Severity.FAULT, e);
        }
    }

    private AudioItem loadPlaylist(String playlistId) {
        try {
            JSONObject playlistObj = authManager.getPlaylist(playlistId);
            
            if (playlistObj == null) {
                throw new FriendlyException("This Spotify playlist does not exist.", Severity.COMMON, null);
            }
            
            String playlistName = playlistObj.getString("name");
            JSONArray tracks = playlistObj.getJSONObject("tracks").getJSONArray("items");
            List<AudioTrack> trackList = new ArrayList<>();
            
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject trackObj = tracks.getJSONObject(i).getJSONObject("track");
                SpotifyAudioTrackInfo trackInfo = extractTrackInfo(trackObj);
                
                // Resolve each track to YouTube
                String searchQuery = trackInfo.getArtist() + " - " + trackInfo.getTitle();
                AudioItem youtubeTrack = resolveToYouTubeTrack(searchQuery);
                
                if (youtubeTrack instanceof AudioTrack) {
                    AudioTrackInfo ytTrackInfo = ((AudioTrack) youtubeTrack).getInfo();
                    
                    trackList.add(new SpotifyAudioTrack(
                            new AudioTrackInfo(
                                    trackInfo.getTitle(),
                                    trackInfo.getArtist(),
                                    ytTrackInfo.length,
                                    trackInfo.getIdentifier(),
                                    false,
                                    trackInfo.getUri()
                            ),
                            (AudioTrack) youtubeTrack,
                            this
                    ));
                }
            }
            
            return new BasicAudioPlaylist(playlistName, trackList, null, false);
        } catch (IOException e) {
            throw new FriendlyException("Error loading Spotify playlist", Severity.FAULT, e);
        }
    }

    private AudioItem loadArtistTopTracks(String artistId) {
        try {
            JSONObject artistObj = authManager.getArtist(artistId);
            JSONArray topTracks = authManager.getArtistTopTracks(artistId);
            
            if (artistObj == null) {
                throw new FriendlyException("This Spotify artist does not exist.", Severity.COMMON, null);
            }
            
            String artistName = artistObj.getString("name");
            List<AudioTrack> trackList = new ArrayList<>();
            
            for (int i = 0; i < topTracks.length(); i++) {
                JSONObject trackObj = topTracks.getJSONObject(i);
                SpotifyAudioTrackInfo trackInfo = extractTrackInfo(trackObj);
                
                // Resolve each track to YouTube
                String searchQuery = trackInfo.getArtist() + " - " + trackInfo.getTitle();
                AudioItem youtubeTrack = resolveToYouTubeTrack(searchQuery);
                
                if (youtubeTrack instanceof AudioTrack) {
                    AudioTrackInfo ytTrackInfo = ((AudioTrack) youtubeTrack).getInfo();
                    
                    trackList.add(new SpotifyAudioTrack(
                            new AudioTrackInfo(
                                    trackInfo.getTitle(),
                                    trackInfo.getArtist(),
                                    ytTrackInfo.length,
                                    trackInfo.getIdentifier(),
                                    false,
                                    trackInfo.getUri()
                            ),
                            (AudioTrack) youtubeTrack,
                            this
                    ));
                }
            }
            
            return new BasicAudioPlaylist(artistName + " - Top Tracks", trackList, null, false);
        } catch (IOException e) {
            throw new FriendlyException("Error loading Spotify artist's top tracks", Severity.FAULT, e);
        }
    }

    private SpotifyAudioTrackInfo extractTrackInfo(JSONObject trackObj) {
        String title = trackObj.getString("name");
        String artist = TrackUtils.getMainArtistName(trackObj.getJSONArray("artists"));
        String identifier = trackObj.getString("id");
        String uri = trackObj.getString("uri");
        
        // Duration is in milliseconds
        long duration = trackObj.getLong("duration_ms");
        
        return new SpotifyAudioTrackInfo(title, artist, duration, identifier, uri);
    }

    private AudioItem resolveToYouTubeTrack(String searchQuery) {
        try {
            // Create a search query for YouTube
            AudioReference youtubeReference = new AudioReference(
                    "ytsearch:" + searchQuery.replace(" ", "+"),
                    searchQuery
            );
            
            // Load from YouTube source manager
            AudioItem resolved = youtubeSourceManager.loadItem(null, youtubeReference);
            
            if (resolved instanceof AudioPlaylist) {
                AudioPlaylist playlist = (AudioPlaylist) resolved;
                if (!playlist.getTracks().isEmpty()) {
                    return playlist.getTracks().get(0); // Return the first match
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("Failed to resolve YouTube track for: {}", searchQuery, e);
            return null;
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return track instanceof SpotifyAudioTrack;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        // For now, we'll just store the track ID and artist/title for re-resolving later
        SpotifyAudioTrack spotifyTrack = (SpotifyAudioTrack) track;
        output.writeUTF(spotifyTrack.getInfo().identifier);
        output.writeUTF(spotifyTrack.getInfo().title);
        output.writeUTF(spotifyTrack.getInfo().author);
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        String spotifyId = input.readUTF();
        String title = input.readUTF();
        String artist = input.readUTF();
        
        // Re-resolve to YouTube
        String searchQuery = artist + " - " + title;
        AudioItem youtubeTrack = resolveToYouTubeTrack(searchQuery);
        
        if (youtubeTrack instanceof AudioTrack) {
            return new SpotifyAudioTrack(trackInfo, (AudioTrack) youtubeTrack, this);
        }
        
        throw new IOException("Could not re-resolve Spotify track: " + title);
    }

    @Override
    public void shutdown() {
        internalSourceManager.shutdown();
        httpInterfaceManager.close();
        youtubeSourceManager.shutdown();
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
        youtubeSourceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
        youtubeSourceManager.configureBuilder(configurator);
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
