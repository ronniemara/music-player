package com.whitecloud.ron.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.whitecloud.ron.musicplayer.artist.Artist;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artists;
import kaaes.spotify.webapi.android.models.ArtistsPager;

public class MusicService extends Service implements onTaskCompleted{

    private IBinder mIBInder = new LocalBInder();
    private List<Artist>  mArtists = new ArrayList<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mIBInder;
    }

    @Override
    public void sendResult(List<Artist> result) {
        mArtists = result;
    }

    public class LocalBInder extends Binder {

        public MusicService getService() {
            return MusicService.this;
        }
    }

    public List<Artist> getArtists(String query) {
        FetchArtistsTask fetchArtistsTask =  new FetchArtistsTask(this);
        fetchArtistsTask.execute(query);
        return mArtists;
    }

    private class FetchArtistsTask extends AsyncTask<String, Void, List<Artist>> {

        onTaskCompleted mOnTaskCompleted = null;

        public FetchArtistsTask(onTaskCompleted listener) {
            mOnTaskCompleted = listener;
        }

        @Override
        protected List<Artist> doInBackground(String... params) {
            SpotifyApi api = new SpotifyApi();
            SpotifyService service = api.getService();
            ArtistsPager artistsPager = service.searchArtists(params[0]);
            List<kaaes.spotify.webapi.android.models.Artist> artistsList =  artistsPager.artists.items;

            List<Artist> result = new ArrayList<>();

            for (int i=0;  i < artistsList.size(); i++) {
                kaaes.spotify.webapi.android.models.Artist artist = artistsList.get(i);
                boolean isEmpty = artist.images.isEmpty();
                String url;
                if(isEmpty) {
                    url = null;
                }
                else {
                    url = artist.images.get(0).url;
                }
               result.add(new Artist(artist.name, artist.id, url));
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<Artist> artists) {
           mOnTaskCompleted.sendResult(artists);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled(List<Artist> artists) {
            super.onCancelled(artists);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }


    }



}
