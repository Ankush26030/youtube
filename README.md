# YouTubeify - Lavalink Plugin for YouTube and Spotify

YouTubeify is a Lavalink plugin that enables long-term stable playback of YouTube and Spotify tracks without requiring patches to Lavalink core.

## Features

- YouTube audio streaming with multiple fallback mechanisms for reliability
- Spotify track resolution and playback via YouTube
- Playlist support for both platforms
- Smart search functionality
- Comprehensive error handling and logging
- Long-term stability designed to survive YouTube API changes

## Installation

1. Download the latest `YouTubeify.jar` from the [releases page](https://github.com/yourusername/youtubeify/releases)
2. Place the JAR file in your Lavalink server's `plugins` directory
3. Configure your `application.yml` (see examples below)
4. Set up the necessary environment variables
5. Restart your Lavalink server

## Configuration

### Lavalink application.yml Examples

#### Basic Configuration Example

```yaml
server:
  port: 2333
  address: 0.0.0.0

lavalink:
  server:
    password: "youshallnotpass"
    sources:
      youtube: true  # Set to false if you want to use only YouTubeify's implementation
      bandcamp: true
      soundcloud: true
      twitch: true
      vimeo: true
      http: true
      local: false
    bufferDurationMs: 400
    youtubePlaylistLoadLimit: 6
    playerUpdateInterval: 5
    youtubeSearchEnabled: true
    soundcloudSearchEnabled: true
    gc-warnings: true
  
  plugins:
    # Option 1: If you have the JAR file locally in the plugins directory
    # - file: "/path/to/plugins/YouTubeify-1.0.0.jar"
    
    # Option 2: If you're referencing the JAR directly (recommended)
    - path: "plugins/YouTubeify-1.0.0.jar"
      snapshot: false
    
    # Option 3: For dependency reference (when published to a repository)
    # - dependency: "com.github.youtubeify:YouTubeify:1.0.0"
    #   repository: "https://jitpack.io"
    #   snapshot: false
```

> **Important**: Notice the difference between using `path:` versus `dependency:`. Use `path:` when you have a local JAR file, and `dependency:` when the plugin is available in a repository.

#### Advanced Configuration Example

```yaml
server:
  port: 2333
  address: 0.0.0.0

spring:
  main:
    banner-mode: log

lavalink:
  server:
    password: "youshallnotpass"
    sources:
      youtube: false  # Disable default YouTube source manager as YouTubeify will handle it
      bandcamp: true
      soundcloud: true
      twitch: true
      vimeo: true
      http: true
      local: false
    bufferDurationMs: 400
    frameBufferDurationMs: 5000
    youtubePlaylistLoadLimit: 6
    playerUpdateInterval: 5
    youtubeSearchEnabled: true
    soundcloudSearchEnabled: true
    gc-warnings: true
    ratelimit:
      ipBlocks: []
      excludedIps: ["127.0.0.1"]
      strategy: "LoadBalance"
      searchTriggersFail: true
      retryLimit: 3
    youtubeConfig:
      email: ""
      password: ""
  
  plugins:
    - path: "plugins/YouTubeify-1.0.0.jar"
      snapshot: false

metrics:
  prometheus:
    enabled: false
    endpoint: /metrics

sentry:
  dsn: ""
  environment: ""
  tags:
    some_key: some_value
    
logging:
  file:
    max-history: 30
    max-size: 1GB
  path: ./logs/
  level:
    root: INFO
    lavalink: INFO
```

### Environment Variables

The plugin requires the following environment variables:

- `SPOTIFY_CLIENT_ID`: Your Spotify application client ID
- `SPOTIFY_CLIENT_SECRET`: Your Spotify application client secret
- `YOUTUBE_API_KEY` (optional): YouTube Data API key for better performance

> **IMPORTANT**: These environment variables are critical for the plugin to work. The Spring ApplicationContext failures shown in the Lavalink console are often caused by missing environment variables. Make sure these are properly set before starting Lavalink.

You can set these environment variables in different ways:

#### 1. System Environment Variables

For Linux/macOS:
```bash
export SPOTIFY_CLIENT_ID=your_client_id
export SPOTIFY_CLIENT_SECRET=your_client_secret
export YOUTUBE_API_KEY=your_api_key
```

For Windows:
```batch
set SPOTIFY_CLIENT_ID=your_client_id
set SPOTIFY_CLIENT_SECRET=your_client_secret
set YOUTUBE_API_KEY=your_api_key
```

#### 2. Docker Environment Variables

If using Docker, add them to your docker-compose.yml:

```yaml
version: '3'
services:
  lavalink:
    image: fredboat/lavalink:latest
    environment:
      - SPOTIFY_CLIENT_ID=your_client_id
      - SPOTIFY_CLIENT_SECRET=your_client_secret
      - YOUTUBE_API_KEY=your_api_key
    volumes:
      - ./application.yml:/opt/Lavalink/application.yml
      - ./plugins:/opt/Lavalink/plugins
    ports:
      - "2333:2333"
```

### Creating Spotify API Credentials

1. Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard/)
2. Create a new application
3. Copy the Client ID and Client Secret

### Creating YouTube API Credentials (optional but recommended)

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project
3. Enable the YouTube Data API v3
4. Create API credentials and copy the API key

## Usage

Once installed, the plugin will automatically register YouTube and Spotify source managers with Lavalink. You can use the following URL formats to load tracks:

### YouTube

- `https://www.youtube.com/watch?v=VIDEO_ID` - Load a YouTube video
- `https://youtu.be/VIDEO_ID` - Load a YouTube video (short URL)
- `https://www.youtube.com/playlist?list=PLAYLIST_ID` - Load a YouTube playlist
- `ytsearch:query` - Search YouTube for tracks matching the query

### Spotify

- `https://open.spotify.com/track/TRACK_ID` - Load a Spotify track
- `https://open.spotify.com/album/ALBUM_ID` - Load a Spotify album
- `https://open.spotify.com/playlist/PLAYLIST_ID` - Load a Spotify playlist
- `https://open.spotify.com/artist/ARTIST_ID` - Load an artist's top tracks

## Discord Bot Integration Examples

### JDA (Java Discord API) with Lavalink

```java
// Initialize Lavalink node manager
DefaultLavaplayerManager lavaplayerManager = new DefaultLavaplayerManager();
LavalinkLoadBalancer loadBalancer = new LavalinkLoadBalancer();
LavalinkSocket socket = new LavalinkSocket(
    URI.create("ws://localhost:2333"), // Lavalink server address
    "youshallnotpass", // From Lavalink application.yml
    lavaplayerManager, 
    loadBalancer
);

// Connect to Lavalink node
socket.connect();

// Play music (Spotify URLs work automatically thanks to YouTubeify)
audioManager.loadItem("https://open.spotify.com/track/your-track-id", new AudioLoadResultHandler() {
    @Override
    public void trackLoaded(AudioTrack track) {
        player.playTrack(track);
    }
    // Other handler methods...
});
```

### Discord.js with Lavalink (erela.js)

```javascript
const { Client } = require('discord.js');
const { Manager } = require('erela.js');

const client = new Client();

// Set up Lavalink connection
const manager = new Manager({
    nodes: [
        {
            host: "localhost",
            port: 2333,
            password: "youshallnotpass",
        },
    ],
    send: (id, payload) => {
        const guild = client.guilds.cache.get(id);
        if (guild) guild.shard.send(payload);
    },
});

// Play Spotify playlists easily
client.on("message", async (message) => {
    if (message.content.startsWith("!play")) {
        const spotifyUrl = message.content.slice("!play".length).trim();
        const res = await manager.search(spotifyUrl, message.author);
        
        // YouTubeify handles the Spotify-to-YouTube conversion automatically
        const player = manager.create({
            guild: message.guild.id,
            voiceChannel: message.member.voice.channel.id,
            textChannel: message.channel.id,
        });
        
        player.connect();
        player.queue.add(res.tracks[0]);
        player.play();
    }
});
```

### Python (Discord.py with Wavelink)

```python
import discord
import wavelink
from discord.ext import commands

class MusicBot(commands.Bot):
    def __init__(self):
        super().__init__(command_prefix="!")
        self.add_cog(Music(self))
    
    async def on_ready(self):
        # Connect to Lavalink server with YouTubeify plugin
        await wavelink.NodePool.create_node(
            bot=self,
            host='127.0.0.1',
            port=2333,
            password='youshallnotpass'
        )
        print(f"Bot is ready as {self.user}")

class Music(commands.Cog):
    def __init__(self, bot):
        self.bot = bot
    
    @commands.command()
    async def play(self, ctx, *, query):
        # Connect to voice channel
        if not ctx.voice_client:
            vc = await ctx.author.voice.channel.connect(cls=wavelink.Player)
        else:
            vc = ctx.voice_client
        
        # Play Spotify URLs directly - YouTubeify handles the rest
        tracks = await wavelink.NodePool.get_node().get_tracks(query=query)
        
        if not tracks:
            return await ctx.send("No tracks found")
        
        await vc.play(tracks[0])
        await ctx.send(f"Now playing: {tracks[0].title}")

bot = MusicBot()
bot.run("your-bot-token")
```

## How It Works

The plugin does the following:

1. For YouTube tracks, it uses multiple methods to retrieve video information and streams:
   - YouTube Data API (if API key is provided)
   - InnerTube API (YouTube's internal API)
   - Web scraping as a last resort

2. For Spotify tracks, it:
   - Retrieves track metadata from the Spotify Web API
   - Creates an optimized search query
   - Finds the best matching YouTube video
   - Streams the audio using the YouTube source manager

## Troubleshooting

### Step-by-Step Fix for the "Constructor threw exception" Error

If you see this error in your Lavalink console:

```
Error starting ApplicationContext. 
Exception: Error creating bean with name 'pluginManager'
Failed to instantiate [lavalink.server.bootstrap.PluginManager$$SpringCGLIB$$0]
Constructor threw exception
```

Follow these EXACT steps to fix it:

#### 1. Verify Your Spotify API Credentials

First, make sure you have valid Spotify API credentials:
1. Get your credentials from the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard/)
2. Create a new file called `.env` in the same directory as your Lavalink.jar:
   ```
   SPOTIFY_CLIENT_ID=your_actual_spotify_client_id
   SPOTIFY_CLIENT_SECRET=your_actual_spotify_client_secret
   ```

#### 2. Use the Correct Plugin Path Format

Make sure your `application.yml` file references the plugin correctly:

```yaml
lavalink:
  server:
    password: "youshallnotpass"
    # other server settings...
  plugins:
    - dependency: "./plugins/YouTubeify-1.0.0.jar" # Relative path
    # OR
    - path: "/absolute/path/to/plugins/YouTubeify-1.0.0.jar" # Absolute path
```

#### 3. Create a Start Script

Create a script to properly load the environment variables before starting Lavalink:

For Linux/macOS (save as `start-lavalink.sh`):
```bash
#!/bin/bash
# Load environment variables from .env file
export $(grep -v '^#' .env | xargs)

# Start Lavalink with environment variables available
java -jar Lavalink.jar
```
Make it executable: `chmod +x start-lavalink.sh`

For Windows (save as `start-lavalink.bat`):
```batch
@echo off
:: Load environment variables from .env file
for /f "tokens=*" %%a in (.env) do set %%a

:: Start Lavalink with environment variables available
java -jar Lavalink.jar
```

#### 4. Use Docker with Environment Variables (Recommended Method)

Using Docker is the most reliable way to ensure environment variables are properly passed to Lavalink:

```yaml
# docker-compose.yml
version: '3'
services:
  lavalink:
    image: fredboat/lavalink:latest
    environment:
      - SPOTIFY_CLIENT_ID=${SPOTIFY_CLIENT_ID}
      - SPOTIFY_CLIENT_SECRET=${SPOTIFY_CLIENT_SECRET}
    volumes:
      - ./application.yml:/opt/Lavalink/application.yml
      - ./plugins:/opt/Lavalink/plugins
    ports:
      - "2333:2333"
```

Run with:
```bash
docker-compose up
```

### Common Issues

1. **Plugin Not Loading**
   - Ensure the JAR file is in the correct plugins directory
   - Check Lavalink logs for any error messages
   - Verify your `application.yml` format is correct
   - Make sure you're using the correct plugin loading syntax in `application.yml`:
     ```yaml
     lavalink:
       plugins:
         - path: "plugins/YouTubeify-1.0.0.jar"  # Use path for local JAR
           # NOT - dependency: "YouTubeify.jar"  # This only works for published plugins
     ```

2. **Constructor Threw Exception Error**
   - If you see this error in Lavalink logs:
     ```
     Error starting ApplicationContext.
     Exception: Error creating bean with name 'pluginManager'
     Failed to instantiate [lavalink.server.bootstrap.PluginManager$$SpringCGLIB$$0]
     Constructor threw exception
     ```
   - This usually means one of these issues:
     1. The JAR file path is incorrect in your `application.yml`
     2. Environment variables for Spotify are missing (SPOTIFY_CLIENT_ID, SPOTIFY_CLIENT_SECRET)
     3. The plugin JAR is incompatible with your Lavalink version
     4. Spring bean dependencies are incorrect or missing
   - Solutions (try these in order):
     - Solution 1: Make sure you're using local file reference with absolute path in `application.yml`:
       ```yaml
       lavalink:
         plugins:
           - file: "/absolute/path/to/YouTubeify-1.0.0.jar"
       ```
     - Solution 2: Set your environment variables directly in the environment, not in application.yml:
       ```bash
       # Linux/macOS
       export SPOTIFY_CLIENT_ID=your_client_id
       export SPOTIFY_CLIENT_SECRET=your_client_secret
       
       # Windows
       set SPOTIFY_CLIENT_ID=your_client_id
       set SPOTIFY_CLIENT_SECRET=your_client_secret
       ```
     - Solution 3: Make sure you're using the correct Lavalink version (v4.0.0+)
     - Solution 4: If you're using Docker, modify the start command to include environment variables:
       ```bash
       docker run -p 2333:2333 \
         -e SPOTIFY_CLIENT_ID=your_client_id \
         -e SPOTIFY_CLIENT_SECRET=your_client_secret \
         -v ./application.yml:/opt/Lavalink/application.yml \
         -v ./plugins:/opt/Lavalink/plugins \
         fredboat/lavalink:latest
       ```

3. **Spotify Authentication Failed**
   - Check that your Spotify client ID and secret are correct
   - Ensure you've set the environment variables properly
   - Look for any API rate limiting issues in the logs

4. **YouTube Playback Issues**
   - If you're experiencing YouTube errors, try adding a YouTube API key
   - Check for regional restrictions on videos
   - Look for any YouTube rate limiting in the logs

### Logging

For detailed logging, set the Lavalink log level in your `application.yml`:

```yaml
logging:
  level:
    root: INFO
    lavalink: DEBUG
    com.github.youtubeify: DEBUG  # YouTubeify specific logging
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
