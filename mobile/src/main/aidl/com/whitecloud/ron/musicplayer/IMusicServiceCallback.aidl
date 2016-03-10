// IMusicServiceCallback.aidl
package com.whitecloud.ron.musicplayer;

// Declare any non-default types here with import statements
import com.whitecloud.ron.musicplayer.artist.Singer;

interface IMusicServiceCallback {
    void onGetArtists(in List<Singer> singers);
}
