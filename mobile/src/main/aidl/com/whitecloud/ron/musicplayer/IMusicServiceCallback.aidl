// IMusicServiceCallback.aidl
package com.whitecloud.ron.musicplayer;

// Declare any non-default types here with import statements
import com.whitecloud.ron.musicplayer.artist.Singer;
import com.whitecloud.ron.musicplayer.track.Song;
import android.support.v4.media.session.MediaSessionCompat;

interface IMusicServiceCallback {
    void onGetArtists(in List<Singer> singers);
    void onGetTopTracks(in List<Song> songs);
    void onGetToken(in MediaSessionCompat.Token token);
}
