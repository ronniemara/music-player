package com.whitecloud.ron.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.whitecloud.ron.musicplayer.artist.Artist;
import com.whitecloud.ron.musicplayer.track.Track;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;

import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Tracks;

public class MusicService extends Service {

    private IBinder mIBInder = new LocalBInder();
    private final static String TAG = MusicService.class.getSimpleName();
    private Messenger mReqMessenger;

    private ArrayList<Artist>  mArtists = new ArrayList<>();
    private List<Track> mTracks = new ArrayList<>();

       // Handler messages
    private static final int H_GET_ARTISTS = 1;
    private static final int H_GET_TOP_TRACKS = 2;

    @Override
    public void onCreate() {
        super.onCreate();

        mRequestHandler = new RequestHandler();
        mReqMessenger = new Messenger(mRequestHandler);
    }

    class RequestHandler extends Handler {

        SharedPreferences database;

        ExecutorService mExecutor;

        private final int MAX_FIXED_THREAD_POOL = 4;


        public RequestHandler() {
            database = PreferenceManager.getDefaultSharedPreferences(MusicService.this);
            mExecutor = Executors.newFixedThreadPool(MAX_FIXED_THREAD_POOL);
        }


        final int GET_ARTISTS = 1;
        final int GET_TRACKS = 2;

        @Override
        public void handleMessage(final Message msg) {

            final Messenger replyMessenger = msg.replyTo;
            final String query = ArtistsFragment.getQuery(msg);
            switch (msg.what) {
                case GET_ARTISTS: {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, Integer.toString(msg.what));


                            Message artists = onGetArtists(query);

                            try {
                                replyMessenger.send(artists);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }

                        }
                    });


                    break;
                }
                case H_GET_TOP_TRACKS: {
                    onGetTopTracks(msg.getData().get("com.whitecloud.ron.musicplayer.spotifyId").toString());
                    break;
                }
        
        }
    }
}

   	Message onGetArtists( String query) {
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        ArtistsPager artists = spotify.searchArtists(query);
        List<kaaes.spotify.webapi.android.models.Artist> artistList = artists.artists.items;

        //clear previous artist list
        mArtists.clear();
        for (int i = 0; i < artistList.size(); i++){
            kaaes.spotify.webapi.android.models.Artist artist = artistList.get(i);
            String imageUrl = artist.images.isEmpty() ? null : artist.images.get(0).url;
            mArtists.add(new Artist(artist.name, artist.id, imageUrl));
        }

        Message reply = Message.obtain();
        Bundle data = new Bundle();
        data.putParcelableArrayList("artists", mArtists);
        reply.setData(data);

        return reply;
    }

    void getTopTracks(String spotifyId) {

    }

    static ArrayList<Artist> artists(Message msg) {
        return msg.getData().getParcelableArrayList("artists");
    }

    Message onGetTopTracks(String spotifyId) {
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        Tracks tracks = spotify.getTracks(spotifyId);
        List<kaaes.spotify.webapi.android.models.Track> trackList = tracks.tracks;

        for (int i = 0; i < trackList.size(); i++){
            kaaes.spotify.webapi.android.models.Track track = trackList.get(i);
            mTracks.add(new Track(track.preview_url, track.name, track.album.name, track.album.images.get(0).url, track.album.images.get(1).url));
        }

        Message msg = Message.obtain();
        Bundle data = new Bundle();
        data.putString("com.whitecloud.ron.musicplayer.spotifyId", spotifyId);
        msg.setData(data);


        return  msg;

    }

    private Handler mRequestHandler;

    public List<Artist> getmArtists() {
        return mArtists;
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mReqMessenger.getBinder();
    }


    public class LocalBInder extends Binder {

        public MusicService getService() {
            return MusicService.this;
        }
    }


}
