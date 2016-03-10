// IMusicService.aidl
package com.whitecloud.ron.musicplayer;

// Declare any non-default types here with import statements
import com.whitecloud.ron.musicplayer.artist.Singer;
import com.whitecloud.ron.musicplayer.IMusicServiceCallback;

interface IMusicService {
    oneway void getArtists(String query);
    oneway void registerCallback(IMusicServiceCallback callback);
    oneway void unregisterCallback(IMusicServiceCallback callback);
}
