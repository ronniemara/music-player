package com.whitecloud.ron.musicplayer.artist;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by ron on 19/01/16.
 */
public class Artist implements Parcelable {
    private String mName;
    private String mSpotifyId;
    private String mImageUrl;

    public Artist(String name, String spotifyId, String imageUrl) {
        mName = name;
        mSpotifyId = spotifyId;
        mImageUrl =  imageUrl;
    }

    private Artist(Parcel source) {
        mName = source.readString();
        mSpotifyId = source.readString();
        mImageUrl = source.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mSpotifyId);
        dest.writeString(mImageUrl);
    }


    public final Creator<Artist> CREATOR = new Creator<Artist>() {
        @Override
        public Artist createFromParcel(Parcel source) {
            return new Artist(source);
        }

        @Override
        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmSpotifyId() {
        return mSpotifyId;
    }

    public void setmSpotifyId(String mSpotifyId) {
        this.mSpotifyId = mSpotifyId;
    }

    public String getmImageUrl() {
        return mImageUrl;
    }

    public void setmImageUrl(String mImageUrl) {
        this.mImageUrl = mImageUrl;
    }
}
