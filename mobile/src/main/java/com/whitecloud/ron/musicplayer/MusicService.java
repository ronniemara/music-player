package com.whitecloud.ron.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;


import android.support.v4.app.NotificationCompat;

import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.MediaController;
import android.widget.Toast;

import com.whitecloud.ron.musicplayer.artist.Singer;
import com.whitecloud.ron.musicplayer.model.MusicProvider;
import com.whitecloud.ron.musicplayer.track.Song;
import com.whitecloud.ron.musicplayer.ui.ArtistsFragment;

import com.whitecloud.ron.musicplayer.ui.TracksFragment;
import com.whitecloud.ron.musicplayer.utils.LogHelper;
import com.whitecloud.ron.musicplayer.utils.MediaIDHelper;
import com.whitecloud.ron.musicplayer.utils.QueueHelper;
import com.whitecloud.ron.musicplayer.utils.WearHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;

import retrofit.RetrofitError;


public class MusicService extends Service implements
        MusicProvider.Callback {

    private final static String TAG = MusicService.class.getSimpleName();
    private Messenger mReqMessenger;
    private List<Singer>    mArtists = new ArrayList<>();
    private ArrayList<Song> mSongs   = new ArrayList<>();

    private Handler                            mRequestHandler;
    private final DelayedStopHandler           mDelayedStopHandler = new DelayedStopHandler(this);
    private MediaSessionCompat                 mMediaSession;
    private List<MediaDescriptionCompat>       queueItemList;
    private MediaMetadataCompat                mMetaData;
    private List<MediaSessionCompat.QueueItem> mPlayingQueue;

    // Action to thumbs up a media item
    private static final String CUSTOM_ACTION_THUMBS_UP = "com.whitecloud.ron.musicplayer.THUMBS_UP";
    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final  String ACTION_CMD = "com.whitecloud.ron.musicplayer.ACTION_CMD";
    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final  String CMD_NAME   = "CMD_NAME";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final  String CMD_PAUSE  = "CMD_PAUSE";
    // Delay stopSelf by using a handler.
    private static final int    STOP_DELAY = 30000;

    private int mState = PlaybackStateCompat.STATE_NONE;

    public static final int GET_ARTISTS   = 0;
    public static final int GET_TRACKS    = 1;
    public static final int GET_TRACKS_OK = 2;
    public static final int GET_TOKEN     = 3;

//    final int ERROR_RESPONSE =3;

    private LocalPlayback mPlayback;
    private MusicProvider mMusicProvider;
    private int mCurrentIndexOnQueue = 0;
    private NotificationManagerCompat mMediaNotificationManager;
    private boolean                   mServiceStarted;
    private SpotifyService            spotify;


    public LocalPlayback getmPlayback() {
        return mPlayback;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        //initialize fields
        mRequestHandler = new RequestHandler();
        mReqMessenger = new Messenger(mRequestHandler);

        //initialize session
        mMediaSession = new MediaSessionCompat(this, "Music Service");
        mMediaSession.setCallback(mCallback);
        mMediaSession.setQueue(mPlayingQueue);


        //where will get the music
        mMusicProvider = new MusicProvider();

//        Context context = getApplicationContext();
//        Intent intent = new Intent(context, NowPlayingActivity.class);
//        PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
//                intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        mMediaSession.setSessionActivity(pi);



        //spotify api
        spotify = new SpotifyApi().getService();

        //the playing queue
        mPlayingQueue = new ArrayList<>();
        mPlayback = new LocalPlayback(this, mMusicProvider);
    }


    //Called by the system every time a client explicitly starts the service by calling startService(Intent),
    // providing the arguments it supplied and a unique integer token representing the start request.
    // Do not call this method directly.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSession, intent);

        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        return START_STICKY;
    }

    private MediaSessionCompat.Callback mCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            LogHelper.d(TAG, "play");

            if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
                mPlayingQueue = QueueHelper.getRandomQueue(mMusicProvider);
                mMediaSession.setQueue(mPlayingQueue);
                mMediaSession.setQueueTitle(getString(R.string.random_queue_title));
                // start playing from the beginning of the queue
                mCurrentIndexOnQueue = 0;
            }

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
        }

        @Override
        public void onSkipToQueueItem(long id) {
            super.onSkipToQueueItem(id);
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
        }

        @Override
        public void onRewind() {
            super.onRewind();
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            super.onSetRating(rating);
        }
    };

    /**
     * Handle a request to play music
     */
    private void handlePlayRequest() {
        LogHelper.d(TAG, "handlePlayRequest: mState=" + mPlayback.getState());

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (!mServiceStarted) {
            LogHelper.v(TAG, "Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(new Intent(getApplicationContext(), MusicService.class));
            mServiceStarted = true;
        }

        if (!mMediaSession.isActive()) {
            mMediaSession.setActive(true);
        }

        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            updateMetadata();
            mPlayback.play(mPlayingQueue.get(mCurrentIndexOnQueue));
        }
    }

    private void updateMetadata() {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            LogHelper.e(TAG, "Can't retrieve current metadata.");
            updatePlaybackState(getResources().getString(R.string.error_no_metadata));
            return;
        }
        MediaSessionCompat.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
        String musicId = MediaIDHelper.extractMusicIDFromMediaID(
                queueItem.getDescription().getMediaId());
        MediaMetadataCompat track = mMusicProvider.getMusic(musicId);
        if (track == null) {
            throw new IllegalArgumentException("Invalid musicId " + musicId);
        }
        final String trackId = track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        if (!TextUtils.equals(musicId, trackId)) {
            IllegalStateException e = new IllegalStateException("track ID should match musicId.");
            LogHelper.e(TAG, "track ID should match musicId.",
                    " musicId=", musicId, " trackId=", trackId,
                    " mediaId from queueItem=", queueItem.getDescription().getMediaId(),
                    " title from queueItem=", queueItem.getDescription().getTitle(),
                    " mediaId from track=", track.getDescription().getMediaId(),
                    " title from track=", track.getDescription().getTitle(),
                    //  " source.hashcode from track=", track.getString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE).hashCode(),
                    e);
            throw e;
        }
        LogHelper.d(TAG, "Updating metadata for MusicID= " + musicId);
        mMediaSession.setMetadata(track);

        // Set the proper album artwork on the media session, so it can be shown in the
        // locked screen and in other places.
        if (track.getDescription().getIconBitmap() == null &&
                track.getDescription().getIconUri() != null) {
            String albumUri = track.getDescription().getIconUri().toString();
            AlbumArtCache.getInstance().fetch(albumUri, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    MediaSessionCompat.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
                    MediaMetadataCompat          track     = mMusicProvider.getMusic(trackId);
                    track = new MediaMetadataCompat.Builder(track)

                            // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                            // example, on the lockscreen background when the media session is active.
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)

                            // set small version of the album art in the DISPLAY_ICON. This is used on
                            // the MediaDescription and thus it should be small to be serialized if
                            // necessary..
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)

                            .build();

                    mMusicProvider.updateMusic(trackId, track);

                    // If we are still playing the same music
                    String currentPlayingId = MediaIDHelper.extractMusicIDFromMediaID(
                            queueItem.getDescription().getMediaId());
                    if (trackId.equals(currentPlayingId)) {
                        mMediaSession.setMetadata(track);
                    }
                }
            });
        }
    }


    public static ArrayList<Song> getSongs(Message msg) {
        return msg.getData().getParcelableArrayList("com.whitecloud.ron.musicplayer.songs");
    }

    public static MediaSessionCompat.Token getToken(Message msg) {
        return msg.getData().getParcelable("com.whitecloud.ron.Token");
    }

    @Override
    public void onMusicCatalogReady(boolean success, Messenger replyMessenger, List<Song> songs) {
        Message msg = Message.obtain();
        msg.what = GET_TRACKS_OK;
        Bundle data = new Bundle();
        data.putParcelableArrayList("com.whitecloud.ron.musicplayer.songs", (ArrayList) songs);
        msg.setData(data);

        try {
            replyMessenger.send(msg);
        } catch (RemoteException r) {
            r.printStackTrace();
        }
    }

    class RequestHandler extends Handler {

        SharedPreferences database;
        private final int MAX_FIXED_THREAD_POOL = 4;
        ExecutorService mExecutor;

        public RequestHandler() {
            database = PreferenceManager.getDefaultSharedPreferences(MusicService.this);
            mExecutor = Executors.newFixedThreadPool(MAX_FIXED_THREAD_POOL);
        }

        @Override
        public void handleMessage(final Message msg) {
            final Messenger replyMessenger = msg.replyTo;
                if(replyMessenger == null) {
                    return ;
                }


            switch (msg.what) {
                case MusicService.GET_ARTISTS: {
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
                case MusicService.GET_TRACKS: {
                    final Singer singer = TracksFragment.getSinger(msg);
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                onGetTopTracks(singer.getmSpotifyId(), replyMessenger);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }

                case MusicService.GET_TOKEN: {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Message message = onGetToken();
                            try {
                                replyMessenger.send(message);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }
    }

    private Message onGetToken() {
        MediaSessionCompat.Token token = mMediaSession.getSessionToken();

        Message message = Message.obtain();
        message.what = GET_TOKEN;
        Bundle data = new Bundle();
        data.putParcelable("com.whitecloud.ron.Token", token);
        message.setData(data);

        return message;
    }

    Message onGetArtists(String query) throws RetrofitError {

        mArtists = mMusicProvider.searchArtists(query);

        Message reply = Message.obtain();
        Bundle  data  = new Bundle();
        if (!mArtists.isEmpty()) {
            data.putParcelableArrayList("artists", (ArrayList) mArtists);
        }
        reply.setData(data);

        return reply;
    }


    public static ArrayList<Singer> artists(Message msg) {
        return msg.getData().getParcelableArrayList("artists");
    }

    void onGetTopTracks(String spotifyId, Messenger replyHandler) throws RetrofitError, IOException {
        mMusicProvider.retrieveMediaAsync(this, spotifyId, replyHandler);
        updatePlaybackState(null);
    }


    private void updatePlaybackState(String error) {
        LogHelper.d(TAG, "updatePlaybackState, playback state=" + mPlayback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null && mPlayback.isConnected()) {
            position = mPlayback.getCurrentStreamPosition();
        }

        PlaybackStateCompat.Builder stateBuilder;
        long                        actions = getAvailableActions();
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PAUSE);

        setCustomAction(stateBuilder);
        int state = mPlayback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mMediaSession.setPlaybackState(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            NotificationCompat.Builder builder = MediaStyleHelper.from(getApplicationContext(), mMediaSession);

            builder
                    .setSmallIcon(R.drawable.notification_icon)
                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));

            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_media_pause, getString(R.string.pause),
                    MediaStyleHelper.getActionIntent(getApplicationContext(),
                            KeyEvent.KEYCODE_MEDIA_PAUSE)
            ));

            builder.setStyle(new android.support.v7.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0)
                    .setMediaSession(mMediaSession.getSessionToken()));

        }

    }

    private void setCustomAction(PlaybackStateCompat.Builder stateBuilder) {
        MediaMetadataCompat currentMusic = getCurrentPlayingMusic();
        if (currentMusic != null) {
            // Set appropriate "Favorite" icon on Custom action:
            String musicId      = currentMusic.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            int    favoriteIcon = R.drawable.ic_star_off;
            if (mMusicProvider.isFavorite(musicId)) {
                favoriteIcon = R.drawable.ic_star_on;
            }
            LogHelper.d(TAG, "updatePlaybackState, setting Favorite custom action of music ",
                    musicId, " current favorite=", mMusicProvider.isFavorite(musicId));
            Bundle customActionExtras = new Bundle();
            WearHelper.setShowCustomActionOnWear(customActionExtras, true);
            stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                    CUSTOM_ACTION_THUMBS_UP, getString(R.string.favorite), favoriteIcon)
                    .setExtras(customActionExtras)
                    .build());
        }
    }

    private MediaMetadataCompat getCurrentPlayingMusic() {
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            if (item != null) {
                LogHelper.d(TAG, "getCurrentPlayingMusic for musicId=",
                        item.getDescription().getMediaId());
                return mMusicProvider.getMusic(
                        MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId()));
            }
        }
        return null;
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
            return actions;
        }
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        if (mCurrentIndexOnQueue > 0) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mReqMessenger.getBinder();
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MusicService> mWeakReference;

        private DelayedStopHandler(MusicService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MusicService service = mWeakReference.get();
            if (service != null && service.mPlayback != null) {
                if (service.mPlayback.isPlaying()) {
                    LogHelper.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                LogHelper.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
                service.mServiceStarted = false;
            }
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    MediaSessionCompat.Callback mSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPlay() {
            super.onPlay();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
        }

        @Override
        public void onSkipToQueueItem(long id) {
            super.onSkipToQueueItem(id);
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
        }

        @Override
        public void onRewind() {
            super.onRewind();
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            super.onSetRating(rating);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
        }
    };


   Playback.Callback mPlaybackCallback = new Playback.Callback() {
       @Override
       public void onCompletion() {

       }

       @Override
       public void onPlaybackStatusChanged(int state) {

       }

       @Override
       public void onError(String error) {

       }

       @Override
       public void onMetadataChanged(String mediaId) {

       }
   };





}
