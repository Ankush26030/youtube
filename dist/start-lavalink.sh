#!/bin/bash

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Error: .env file not found!"
    echo "Please create a .env file with your Spotify credentials."
    echo "You can use the example.env as a template."
    exit 1
fi

# Load environment variables from .env file
export $(grep -v '^#' .env | xargs)

# Verify Spotify credentials are set
if [ -z "$SPOTIFY_CLIENT_ID" ] || [ -z "$SPOTIFY_CLIENT_SECRET" ]; then
    echo "Error: Spotify credentials are missing in .env file!"
    echo "Please make sure SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET are properly set."
    exit 1
fi

echo "Starting Lavalink with YouTubeify plugin..."
echo "Spotify credentials loaded from .env file."

# Start Lavalink with environment variables available
java -jar Lavalink.jar