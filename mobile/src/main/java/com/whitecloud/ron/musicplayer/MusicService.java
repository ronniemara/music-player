package com.whitecloud.ron.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.whitecloud.ron.musicplayer.artist.Singer;
import com.whitecloud.ron.musicplayer.track.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;

public class MusicService extends Service {

    private final static String TAG = MusicService.class.getSimpleName();
    private Messenger mReqMessenger;

    private ArrayList<Singer>  mArtists = new ArrayList<>();
    private ArrayList<Song> mSongs = new ArrayList<>();

    private SpotifyService spotify;

    private Handler mRequestHandler;


    @Override
    public void onCreate() {
        super.onCreate();

        mRequestHandler = new RequestHandler();
        mReqMessenger = new Messenger(mRequestHandler);

        SpotifyApi api = new SpotifyApi();
        spotify = api.getService();
    }

    class RequestHandler extends Handler {

        SharedPreferences database;
        private final int MAX_FIXED_THREAD_POOL = 4;
        ExecutorService mExecutor;
        final int GET_ARTISTS = 1;
        final int GET_TRACKS = 2;


        public RequestHandler() {
            database = PreferenceManager.getDefaultSharedPreferences(MusicService.this);
            mExecutor = Executors.newFixedThreadPool(MAX_FIXED_THREAD_POOL);
        }

        @Override
        public void handleMessage(final Message msg) {
            final Messenger replyMessenger = msg.replyTo;

            switch (msg.what) {
                case GET_ARTISTS: {
                    final String query = ArtistsFragment.getQuery(msg);
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
                }

                break;
                case  GET_TRACKS: {
                    final Singer singer = TracksFragment.getSinger(msg);
                      mExecutor.execute(new Runnable() {
                          @Override
                          public void run() {
                             Message message = onGetTopTracks(singer.getmSpotifyId());
                              try {
                                  replyMessenger.send(message);
                              } catch (RemoteException r) {
                                  r.printStackTrace();
                              }
                          }
                      });
                    }
                }
        }
    }

   	Message onGetArtists( String query) throws RetrofitError {

        ArtistsPager artists = spotify.searchArtists(query);
        List<Artist> artistList = artists.artists.items;

        //clear previous artist list
        mArtists.clear();
        for (int i = 0; i < artistList.size(); i++){
            Artist artist = artistList.get(i);
            String imageUrl = artist.images.isEmpty() ? null : artist.images.get(0).url;
            mArtists.add(new Singer(artist.name, artist.id, imageUrl));
        }

        Message reply = Message.obtain();
        Bundle data = new Bundle();
        data.putParcelableArrayList("artists", mArtists);
        reply.setData(data);

        return reply;
    }


    static ArrayList<Singer> artists(Message msg) {
        return msg.getData().getParcelableArrayList("artists");
    }

    Message onGetTopTracks(String spotifyId)  throws RetrofitError {
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        Tracks tracks = spotify.getArtistTopTrack(spotifyId, "CA");
        List<Track> trackList = tracks.tracks;

        mSongs.clear();

        if(!trackList.isEmpty()) {
            for (int i = 0; i < trackList.size(); i++) {
                Track track = trackList.get(i);
                mSongs.add(new Song(track.preview_url, track.name, track.album.name, track.album.images.get(0).url, track.album.images.get(1).url));
            }
        }

        Message msg = Message.obtain();
        Bundle data = new Bundle();
        data.putParcelableArrayList("com.whitecloud.ron.musicplayer.songs", mSongs);
        msg.setData(data);

        return  msg;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mReqMessenger.getBinder();
    }
}
