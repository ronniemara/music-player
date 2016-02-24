package com.whitecloud.ron.musicplayer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.whitecloud.ron.musicplayer.artist.Singer;
import com.whitecloud.ron.musicplayer.ui.ArtistsFragment;


import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ArtistsFragment.OnListFragmentInteractionListener} and makes a call to the
 * specified {@link ArtistsFragment.OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyArtistsRecyclerViewAdapter extends RecyclerView.Adapter<MyArtistsRecyclerViewAdapter.ViewHolder> {

    private final List<Singer> mValues;
    private final ArtistsFragment.OnListFragmentInteractionListener mListener;

    public MyArtistsRecyclerViewAdapter(List<Singer> items, ArtistsFragment.OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_artists_one_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).getmName());

        Picasso.with(holder.mContentView.getContext())
                .load(mValues.get(position).getmImageUrl())
                .placeholder(R.drawable.placeholderscaled)
                .error(R.drawable.errorscaled)
                .centerCrop()
                .fit()
                .into(holder.mContentView);


        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final ImageView mContentView;
        public Singer mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.artist_name);
            mContentView = (ImageView) view.findViewById(R.id.artist_image);

        }

        @Override
        public String toString() {
            return super.toString() + " '" + mIdView.getText() + "'";
        }
    }
}
