/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.whitecloud.ron.musicplayer.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Messenger;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.whitecloud.ron.musicplayer.artist.Singer;
import com.whitecloud.ron.musicplayer.track.Song;
import com.whitecloud.ron.musicplayer.utils.LogHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;

/**
 * Utility class to get a list of MusicTrack's based on a server-side JSON
 * configuration.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);

    private SpotifyService spotify;




//    private static final String CATALOG_URL =
//        "http://storage.googleapis.com/automotive-media/music.json";
//
//    public static final String CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__";
//
//    private static final String JSON_MUSIC = "music";
//    private static final String JSON_TITLE = "title";
//    private static final String JSON_ALBUM = "album";
//    private static final String JSON_ARTIST = "artist";
//    private static final String JSON_GENRE = "genre";
//    private static final String JSON_SOURCE = "source";
//    private static final String JSON_IMAGE = "image";
//    private static final String JSON_TRACK_NUMBER = "trackNumber";
//    private static final String JSON_TOTAL_TRACK_COUNT = "totalTrackCount";
//    private static final String JSON_DURATION = "duration";

    // Categorized caches for music track data:
    private ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByGenre;
    private final ConcurrentMap<String, MutableMediaMetadata> mMusicListById;
    private ConcurrentMap<String, String> mMusicSourceList;

    private final Set<String> mFavoriteTracks;
    private final List<Singer> mArtists;
    private List<Song> mSongs;
    private static String mTrackSource;


    public static String getTrackSource(String mediaId) {
        return mTrackSource;
    }

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success, Messenger replyHandler, List<Song> songs);
    }

    public MusicProvider() {
        mMusicListByGenre = new ConcurrentHashMap<>();
        mMusicListById = new ConcurrentHashMap<>();
        mMusicSourceList = new ConcurrentHashMap<>();
        mFavoriteTracks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        SpotifyApi api = new SpotifyApi();
        spotify = api.getService();
        mArtists = new ArrayList<>();
        mSongs = new ArrayList<>();
    }

    /**
     * Get an iterator over the list of genres
     *
     * @return genres
     */
    public Iterable<String> getGenres() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mMusicListByGenre.keySet();
    }

    /**
     * Get music tracks of the given genre
     *
     */
    public Iterable<MediaMetadataCompat> getMusicsByGenre(String genre) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByGenre.containsKey(genre)) {
            return Collections.emptyList();
        }
        return mMusicListByGenre.get(genre);
    }

    /**
     * Very basic implementation of a search that filter music tracks with title containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicBySongTitle(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_TITLE, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with album containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicByAlbum(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ALBUM, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with artist containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicByArtist(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ARTIST, query);
    }

    Iterable<MediaMetadataCompat> searchMusic(String metadataField, String query) {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        ArrayList<MediaMetadataCompat> result = new ArrayList<>();
        query = query.toLowerCase(Locale.US);
        for (MutableMediaMetadata track : mMusicListById.values()) {
            if (track.metadata.getString(metadataField).toLowerCase(Locale.US)
                .contains(query)) {
                result.add(track.metadata);
            }
        }
        return result;
    }

    public List<Singer> searchArtists(String query) {
        ArtistsPager artists = spotify.searchArtists(query);
        List<Artist> artistList = artists.artists.items;

        //clear previous artist list
        mArtists.clear();
        for (int i = 0; i < artistList.size(); i++){
            Artist artist = artistList.get(i);
            String imageUrl = artist.images.isEmpty() ? null : artist.images.get(0).url;
            mArtists.add(new Singer(artist.name, artist.id, imageUrl));
        }
        return mArtists;
    }


    /**
     * Return the MediaMetadata for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    public MediaMetadataCompat getMusic(String musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId).metadata : null;
    }

    public synchronized void updateMusic(String musicId, MediaMetadataCompat metadata) {
        MutableMediaMetadata track = mMusicListById.get(musicId);
        if (track == null) {
            return;
        }

        String oldGenre = track.metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
        String newGenre = metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);

        track.metadata = metadata;

        // if genre has changed, we need to rebuild the list by genre
        if (!oldGenre.equals(newGenre)) {
            buildListsByGenre();
        }
    }

    public void setFavorite(String musicId, boolean favorite) {
        if (favorite) {
            mFavoriteTracks.add(musicId);
        } else {
            mFavoriteTracks.remove(musicId);
        }
    }

    public boolean isFavorite(String musicId) {
        return mFavoriteTracks.contains(musicId);
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    public void retrieveMediaAsync(final Callback callback, String spotifyId, final Messenger replyHandler) {
        LogHelper.d(TAG, "retrieveMediaAsync called");
        if (mCurrentState == State.INITIALIZED) {
            // Nothing to do, execute callback immediately
            callback.onMusicCatalogReady(true, replyHandler, mSongs);
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<String, Void, State>() {
            @Override
            protected State doInBackground(String... params) {
                retrieveMedia(params[0]);
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED, replyHandler, mSongs);
                }
            }
        }.execute(spotifyId);
    }

    private synchronized void buildListsByGenre() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByGenre = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            String genre = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
            List<MediaMetadataCompat> list = newMusicListByGenre.get(genre);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByGenre.put(genre, list);
            }
            list.add(m.metadata);
        }
        mMusicListByGenre = newMusicListByGenre;
    }

    private synchronized void getArtists(String query) {


        ArtistsPager artists = null;
        try {
            artists = spotify.searchArtists(query);
        } catch(RetrofitError e) {
            e.printStackTrace();
        }
        List<Artist> artistList = artists.artists.items;

        if(!artistList.isEmpty()) {
            for (int i = 0; i < artistList.size(); i++) {
                Artist artist   = artistList.get(i);
                String imageUrl = artist.images.isEmpty() ? null : artist.images.get(0).url;
                mArtists.add(new Singer(artist.name, artist.id, imageUrl));
            }
        }
    }

    private synchronized void retrieveMedia(String spotifyId) {

        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();
        Tracks tracks = spotify.getArtistTopTrack(spotifyId, "CA");
        List<Track> trackList = tracks.tracks;
//
//        mSongs.clear();
//
//        if(!trackList.isEmpty()) {
//            int count = 0;
//            for (int i = 0; i < trackList.size(); i++) {
//                Track track = trackList.get(i);
//                mSongs.add(new Song(track.preview_url, track.name, track.album.name, track.album.images.get(0).url, track.album.images.get(1).url));
//                URL url = null;
//                try {
//                    url = new URL(track.album.images.get(0).url);
//                }catch (MalformedURLException e) {
//                    e.printStackTrace();
//                }
//                Bitmap image = BitmapFactory.decodeStream(url.openStream());
//
//                MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
//                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artists.get(0).name)
//                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.name)
//                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id)
//                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, image);
//                MediaMetadataCompat metadata = metadataBuilder.build();
//
//                queueItemList = new ArrayList<>();
//                queueItemList.add(metadata.getDescription());
//                mPlayingQueue.add(new  MediaSessionCompat.QueueItem(metadata.getDescription(), count++));
//            }
//        }




//                int slashPos = CATALOG_URL.lastIndexOf('/');
//                String path = CATALOG_URL.substring(0, slashPos + 1);
//                JSONObject jsonObj = fetchJSONFromUrl(CATALOG_URL);
//                if (jsonObj == null) {
//                    return;
//                }
//                JSONArray tracks = jsonObj.getJSONArray(JSON_MUSIC);
                if (trackList != null) {
                    for (int j = 0; j < trackList.size(); j++) {
                        Track track = trackList.get(j);
                        MediaMetadataCompat item = new MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,  track.id)
//                                 .putString(CUSTOM_METADATA_TRACK_SOURCE, track.preview_url)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album.name)
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artists.get(0).name)
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration_ms)
                                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, track.type)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, track.album.images.get(0).url)
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.name)
                                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track.track_number)
                                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, trackList.size())
                                .build();
                        String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                        mMusicListById.put(musicId, new MutableMediaMetadata(musicId, item));
                        mMusicSourceList.put(musicId, track.preview_url);
                        mSongs.add(new Song(track.preview_url, track.name, track.album.name,
                                track.album.images.get(0).url, track.album.images.get(1).url));
                    }
                    buildListsByGenre();
                }
                mCurrentState = State.INITIALIZED;
            }
        } catch (RetrofitError e) {
            LogHelper.e(TAG, e, "Could not retrieve music list");
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                // Something bad happened, so we reset state to NON_INITIALIZED to allow
                // retries (eg if the network connection is temporary unavailable)
                mCurrentState = State.NON_INITIALIZED;
            }
        }
    }

//    private MediaMetadataCompat buildFromJSON(JSONObject json, String basePath) throws JSONException {
//        String title = json.getString(JSON_TITLE);
//        String album = json.getString(JSON_ALBUM);
//        String artist = json.getString(JSON_ARTIST);
//        String genre = json.getString(JSON_GENRE);
//        String source = json.getString(JSON_SOURCE);
//        String iconUrl = json.getString(JSON_IMAGE);
//        int trackNumber = json.getInt(JSON_TRACK_NUMBER);
//        int totalTrackCount = json.getInt(JSON_TOTAL_TRACK_COUNT);
//        int duration = json.getInt(JSON_DURATION) * 1000; // ms
//
//        LogHelper.d(TAG, "Found music track: ", json);
//
//        // Media is stored relative to JSON file
//        if (!source.startsWith("http")) {
//            source = basePath + source;
//        }
//        if (!iconUrl.startsWith("http")) {
//            iconUrl = basePath + iconUrl;
//        }
//        // Since we don't have a unique ID in the server, we fake one using the hashcode of
//        // the music source. In a real world app, this could come from the server.
//        String id = String.valueOf(source.hashCode());
//
//        // Adding the music source to the MediaMetadata (and consequently using it in the
//        // mediaSession.setMetadata) is not a good idea for a real world music app, because
//        // the session metadata can be accessed by notification listeners. This is done in this
//        // sample for convenience only.
//        return new MediaMetadataCompat.Builder()
//                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
////                .putString(CUSTOM_METADATA_TRACK_SOURCE, source)
//                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
//                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
//                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
//                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
//                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
//                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
//                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
//                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, totalTrackCount)
//                .build();
//    }

    /**
     * Download a JSON file from a server, parse the content and return the JSON
     * object.
     *
     * @return result JSONObject containing the parsed representation.
     */
//    private JSONObject fetchJSONFromUrl(String urlString) {
//        BufferedReader reader = null;
//        try {
//            URLConnection urlConnection = new URL(urlString).openConnection();
//            reader = new BufferedReader(new InputStreamReader(
//                    urlConnection.getInputStream(), "iso-8859-1"));
//            StringBuilder sb = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                sb.append(line);
//            }
//            return new JSONObject(sb.toString());
//        } catch (Exception e) {
//            LogHelper.e(TAG, "Failed to parse the json for media list", e);
//            return null;
//        } finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    // ignore
//                }
//            }
//        }
//    }


}
