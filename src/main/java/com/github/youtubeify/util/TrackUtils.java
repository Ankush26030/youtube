package com.github.youtubeify.util;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility methods for track operations.
 */
public class TrackUtils {

    /**
     * Get main artist name from the artists array.
     *
     * @param artists JSON array of artists
     * @return Main artist name
     */
    public static String getMainArtistName(JSONArray artists) {
        if (artists.length() == 0) {
            return "Unknown Artist";
        }
        
        return artists.getJSONObject(0).getString("name");
    }

    /**
     * Join all artist names into a single string.
     *
     * @param artists JSON array of artists
     * @return Combined artist names
     */
    public static String joinArtistNames(JSONArray artists) {
        if (artists.length() == 0) {
            return "Unknown Artist";
        }
        
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < artists.length(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            
            result.append(artists.getJSONObject(i).getString("name"));
        }
        
        return result.toString();
    }

    /**
     * Create a search query string for a track.
     *
     * @param title Track title
     * @param artists Artist names
     * @return Search query
     */
    public static String createSearchQuery(String title, String artists) {
        // Clean up title and artists to improve search results
        title = cleanupTitle(title);
        
        return artists + " - " + title;
    }

    /**
     * Clean up title by removing common suffixes like "(Official Video)" etc.
     *
     * @param title Original title
     * @return Cleaned title
     */
    public static String cleanupTitle(String title) {
        // Remove text in parentheses that typically indicates a version but isn't part of the song title
        title = title.replaceAll("\\([^)]*version\\)", "")
                .replaceAll("\\([^)]*video\\)", "")
                .replaceAll("\\([^)]*audio\\)", "")
                .replaceAll("\\([^)]*official\\)", "")
                .replaceAll("\\([^)]*lyrics\\)", "")
                .replaceAll("\\[.*?\\]", "")  // Also remove text in square brackets
                .replaceAll("(?i)official video", "")
                .replaceAll("(?i)official audio", "")
                .replaceAll("(?i)official music video", "")
                .replaceAll("(?i)lyric video", "")
                .replaceAll("(?i)with lyrics", "");
        
        // Trim and remove multiple spaces
        title = title.trim().replaceAll("\\s+", " ");
        
        return title;
    }

    /**
     * Compare two titles for similarity.
     *
     * @param title1 First title
     * @param title2 Second title
     * @return Similarity score (0-1)
     */
    public static double compareTitles(String title1, String title2) {
        title1 = cleanupTitle(title1.toLowerCase());
        title2 = cleanupTitle(title2.toLowerCase());
        
        // Simple Levenshtein distance divided by max length
        int distance = levenshteinDistance(title1, title2);
        int maxLength = Math.max(title1.length(), title2.length());
        
        if (maxLength == 0) {
            return 1.0; // Both are empty strings
        }
        
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Calculate Levenshtein distance between two strings.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Levenshtein distance
     */
    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        // Create two work vectors of integer distances
        int[] v0 = new int[len2 + 1];
        int[] v1 = new int[len2 + 1];
        
        // Initialize v0 (the previous row of distances)
        // This row is A[0][i]: edit distance for an empty s1
        // The distance is just the number of characters to delete from s2
        for (int i = 0; i <= len2; i++) {
            v0[i] = i;
        }
        
        for (int i = 0; i < len1; i++) {
            // Calculate v1 (current row distances) from the previous row v0
            
            // First element of v1 is A[i+1][0]
            // Edit distance is delete (i+1) chars from s1 to match empty s2
            v1[0] = i + 1;
            
            // Use formula to fill in the rest of the row
            for (int j = 0; j < len2; j++) {
                int cost = (s1.charAt(i) == s2.charAt(j)) ? 0 : 1;
                v1[j + 1] = Math.min(v1[j] + 1, Math.min(v0[j + 1] + 1, v0[j] + cost));
            }
            
            // Copy v1 (current row) to v0 (previous row) for next iteration
            System.arraycopy(v1, 0, v0, 0, len2 + 1);
        }
        
        return v1[len2];
    }
}
