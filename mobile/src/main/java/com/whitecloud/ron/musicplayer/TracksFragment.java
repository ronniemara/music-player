package com.whitecloud.ron.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.whitecloud.ron.musicplayer.artist.Singer;
import com.whitecloud.ron.musicplayer.track.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class TracksFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    RecyclerView mTracksRecyclerView;
    MyTrackRecyclerViewAdapter mViewAdapter;

    private List<Song> mSongs;
    private Singer mArtist;

    private Messenger mReqMessengerRef;
    private Messenger mReplyMessenger;
    private ReplyHandler mReplyHandler;
    private boolean isBound;

    final int GET_ARTISTS = 1;
    final int GET_TRACKS = 2;
    final int GET_TRACKS_OK = 3;

    private final String TAG = TracksFragment.class.getSimpleName();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TracksFragment() {
    }

    class ReplyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case GET_TRACKS_OK : {
                    ArrayList<Song> songs = MusicService.getSongs(msg);

                    mSongs.clear();

                    for(int i=0; i< songs.size(); i++) {
                        Song song = songs.get(i);
                        if(!songs.isEmpty()) {
                            mSongs.add(new Song(song.getmPreviewUrl(), song.getmName(),
                                    song.getmAlbum(), song.getmSmallImageUrl(), song.getmLargeImageUrl()));
                            mViewAdapter.notifyDataSetChanged();
                        }
                    }
                    Log.i(TAG, Integer.toString(mSongs.size()));
                }
            }
        }
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static TracksFragment newInstance(int columnCount) {
        TracksFragment fragment = new TracksFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        Bundle extras = getActivity().getIntent().getExtras();

        if(extras != null && extras.containsKey("com.whitecloud.ron.Singer")) {
            mArtist = (Singer) extras.get("com.whitecloud.ron.Singer");
        }

        mSongs = new ArrayList<>();
        mReplyHandler = new ReplyHandler();
        mReplyMessenger = new Messenger(mReplyHandler);

    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), MusicService.class);
        getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "inside TracksFragment");
            mReqMessengerRef = new Messenger(service);
            if(mReqMessengerRef != null) {
                Message message = Message.obtain();
                message.replyTo = mReplyMessenger;
                message.what = GET_TRACKS;
                Bundle data = new Bundle();
                data.putParcelable("com.whitecloud.ron.mArtist", mArtist);
                message.setData(data);

                try {
                    mReqMessengerRef.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mReqMessengerRef = null;
            isBound = false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            mTracksRecyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                mTracksRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                mTracksRecyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            mViewAdapter = new MyTrackRecyclerViewAdapter(mSongs, mListener);

            mTracksRecyclerView.setAdapter(mViewAdapter);
        }

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(mServiceConnection);
    }

    public void showDialog() {
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
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(Song item);
    }

    public static Singer getSinger(Message message) {
        return message.getData().getParcelable("com.whitecloud.ron.mArtist");
    }
}
