package com.whitecloud.ron.musicplayer.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.whitecloud.ron.musicplayer.IMusicService;
import com.whitecloud.ron.musicplayer.IMusicServiceCallback;
import com.whitecloud.ron.musicplayer.MusicService;
import com.whitecloud.ron.musicplayer.R;
import com.whitecloud.ron.musicplayer.artist.Singer;
import com.whitecloud.ron.musicplayer.track.Song;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PlayerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayerFragment extends DialogFragment {

    private static final String SONG = "song";
    private View      mView;
    private TextView  name_textView;
    private TextView  album_textview;
    private ImageView imageView;

    private MediaControllerCompat                   mMediaControllerSession;
    private MediaControllerCompat.TransportControls mTransportControls;
    private MediaController                         mMediaControllerWidget;
    private MediaSessionCompat.Token                mToken;
    private Handler                                 mReplyHandler;
    private IMusicService                           mIMusicService;
    private int mCurrentIndexOnQueue = 0;

    private Song                          song;
    private OnFragmentInteractionListener mListener;
    private final String TAG = MusicService.class.getSimpleName();

    public PlayerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param song song to play
     * @return A new instance of fragment PlayerFragment.
     */

    public static PlayerFragment newInstance(Song song) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle         args     = new Bundle();
        args.putParcelable(SONG, song);

        fragment.setArguments(args);
        return fragment;
    }

    private IMusicServiceCallback.Stub mCallback = new IMusicServiceCallback.Stub() {
        @Override
        public void onGetArtists(List<Singer> singers) throws RemoteException {

        }

        @Override
        public void onGetTopTracks(List<Song> songs) throws RemoteException {

        }

        @Override
        public void onGetToken(MediaSessionCompat.Token token) throws RemoteException {
            mToken = token;
            mReplyHandler.sendMessage(mReplyHandler.obtainMessage(MusicService.GET_TOKEN));
        }


    };
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIMusicService = IMusicService.Stub.asInterface(service);

            try {
                mIMusicService.registerCallback(mCallback);
                mIMusicService.getToken(mCurrentIndexOnQueue);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIMusicService = null;
        }
    };

    class ReplyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MusicService.GET_TOKEN: {;
                    try {
                        mMediaControllerSession = new MediaControllerCompat(PlayerFragment.this.getActivity(), mToken);
                        mTransportControls = mMediaControllerSession.getTransportControls();
                        mMediaControllerSession.registerCallback(new ControllerSessionCallback());
                        //Log.i(TAG, mMediaControllerSession.getSessionToken().toString());
                        mMediaControllerWidget = new MediaController(getActivity()){
                            @Override
                            public boolean onTouchEvent(MotionEvent event) {
                                switch (event.getAction()) {
                                    case MotionEvent.ACTION_DOWN : {
                                        if(!isShowing()) {
                                            show();
                                            return true;
                                        }

                                    }
                                }
                                return super.onTouchEvent(event);
                            }
                        };
                        mMediaControllerWidget.setAnchorView(mView);
                        mMediaControllerWidget.setMediaPlayer(mediaPlayerControl);
                        mMediaControllerWidget.setEnabled(true);
                        mMediaControllerWidget.show();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void onTokenReceived(MediaSessionCompat.Token mToken) {

    }

    @Override
    public void onStart() {
        super.onStart();

        //bind to Music Service
        Intent intent = new Intent(getActivity(), MusicService.class);
        getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            song = getArguments().getParcelable(SONG);

        }

        Bundle extras = getActivity().getIntent().getExtras();
        song = extras.getParcelable("com.whitecloud.ron.Song");
        mCurrentIndexOnQueue = extras.getInt("position");

        mReplyHandler = new ReplyHandler();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_song_player, container, false);

        String name  = song.getmName();
        String album = song.getmAlbum();

        name_textView = (TextView) mView.findViewById(R.id.Player_song_name_textview);
        name_textView.setText(name);

        album_textview = (TextView) mView.findViewById(R.id.Player_song_album_textview);
        album_textview.setText(album);

        imageView = (ImageView) mView.findViewById(R.id.Player_image_view);
        Picasso.with(getActivity()).load(song.getmLargeImageUrl())
                .centerCrop().fit().into(imageView);


        return mView;
    }

    class ControllerSessionCallback extends MediaControllerCompat.Callback {

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            super.onQueueChanged(queue);
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            super.onQueueTitleChanged(title);
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            super.onExtrasChanged(extras);
        }

        @Override
        public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
            super.onAudioInfoChanged(info);
        }

        @Override
        public void binderDied() {
            super.binderDied();
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private MediaController.MediaPlayerControl mediaPlayerControl = new MediaController.MediaPlayerControl() {
        @Override
        public void start() {
            mTransportControls.play();
        }

        @Override
        public void pause() {
            mTransportControls.pause();
        }

        @Override
        public int getDuration() {
            //return mMediaControllerSession.getMetadata();
            return 0;
        }

        @Override
        public int getCurrentPosition() {
            return (int) mMediaControllerSession.getPlaybackState().getPosition();
        }

        @Override
        public void seekTo(int pos) {
            mTransportControls.seekTo(pos);
        }

        @Override
        public boolean isPlaying() {
            return mMediaControllerSession.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;
        }

        @Override
        public int getBufferPercentage() {
            return (int) mMediaControllerSession.getPlaybackState().getBufferedPosition();
        }

        @Override
        public boolean canPause() {
           return ((mMediaControllerSession.getPlaybackState().getActions() & PlaybackStateCompat.ACTION_PAUSE) != 0);
        }

        @Override
        public boolean canSeekBackward() {
            return ((mMediaControllerSession.getPlaybackState().getActions() & PlaybackStateCompat.ACTION_SEEK_TO) != 0);
        }

        @Override
        public boolean canSeekForward() {
            return ((mMediaControllerSession.getPlaybackState().getActions() & PlaybackStateCompat.ACTION_SEEK_TO) != 0);
        }

        @Override
        public int getAudioSessionId() {
            return 0;
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(mServiceConnection);
    }
}
