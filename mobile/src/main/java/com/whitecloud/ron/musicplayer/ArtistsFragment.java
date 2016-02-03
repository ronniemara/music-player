package com.whitecloud.ron.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.whitecloud.ron.musicplayer.artist.Singer;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class ArtistsFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private List<Singer> mArtists;

    private final String TAG = ArtistsFragment.class.getSimpleName();


    private boolean isBound = false;
    private SearchView mSearchView;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;


    private Messenger mReqMessengerRef;
    private Messenger mReplyMessenger;
    private ReplyHandler mReplyHandler;


    final int GET_ARTISTS = 1;
    final int GET_TRACKS = 2;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArtistsFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static ArtistsFragment newInstance(int columnCount) {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    public ServiceConnection mSrvcCxn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.i(TAG, "onServiceConnected");
            mReqMessengerRef = new Messenger(service);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            mReqMessengerRef = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        mArtists = new ArrayList<>();
        mReplyHandler = new ReplyHandler();
        mReplyMessenger = new Messenger(mReplyHandler);
        Intent intent = new Intent(getActivity(), MusicService.class);
        getActivity().bindService(intent, mSrvcCxn, Context.BIND_AUTO_CREATE);

        setHasOptionsMenu(true);


    }

    class ReplyHandler extends android.os.Handler {
        @Override
        public void handleMessage(Message msg) {

            ArrayList<Singer> artists = MusicService.artists(msg);

            mArtists.clear();

            for(int i=0; i< artists.size(); i++) {
                Singer artist = artists.get(i);
                if(!artists.isEmpty()) {
                    mArtists.add(new Singer(artist.getmName(), artist.getmSpotifyId(), artist.getmImageUrl()));
                    mAdapter.notifyDataSetChanged();
                }
            }
            Log.i(TAG, Integer.toString(mArtists.size()));
        }
    }



    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {

        getActivity().getMenuInflater().inflate(R.menu.artist_search, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);

        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchView.clearFocus();
                boolean isConnected = isConnected();

                if (isConnected) {
                  Message message = Message.obtain();
                    message.what = GET_ARTISTS;
                    message.replyTo = mReplyMessenger;
                    Bundle bundle = new Bundle();
                    bundle.putString("query", query );
                    message.setData(bundle);

                    try {
                        mReqMessengerRef.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(getActivity(), "There is no internet connection." +
                                    " Please try again when you have access to the internet.",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);

    }

    static String  getQuery(Message reqMessage) {
        return reqMessage.getData().getString("query");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artists_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            mRecyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                mRecyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            mAdapter = new MyArtistsRecyclerViewAdapter(mArtists, mListener);
            mRecyclerView.setAdapter(mAdapter);
        }

        return view;
    }

    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
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

        void onListFragmentInteraction(Singer item);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(mSrvcCxn);
    }
}
