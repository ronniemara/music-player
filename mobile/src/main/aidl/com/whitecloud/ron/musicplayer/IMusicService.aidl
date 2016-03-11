// IMusicService.aidl
package com.whitecloud.ron.musicplayer;

// Declare any non-default types here with import statements
import com.whitecloud.ron.musicplayer.artist.Singer;
import com.whitecloud.ron.musicplayer.track.Song;
import com.whitecloud.ron.musicplayer.IMusicServiceCallback;

interface IMusicService {
    oneway void getArtists(String query);
    oneway void getTopTracks(in Singer singer);
    oneway void registerCallback(IMusicServiceCallback callback);
    oneway void unregisterCallback(IMusicServiceCallback callback);
    oneway void getToken(int Position);
}
