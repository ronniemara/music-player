package com.whitecloud.ron.musicplayer.track;

import android.os.Parcel;
import android.os.Parcelable;

import com.whitecloud.ron.musicplayer.artist.Artist;

/**
 * Created by ron on 19/01/16.
 */
public class Track implements Parcelable {

    String mPreviewUrl;
    String mName;
    String mAlbum;
    String mSmallImageUrl;
    String mLargeImageUrl;

    public Track(String previewUrl, String name, String album, String smallUrl, String largeUrl) {
        mPreviewUrl =previewUrl;
        mName = name;
        mAlbum =album;
        mSmallImageUrl = smallUrl;
        mLargeImageUrl = largeUrl;
    }

    public String getmPreviewUrl() {
        return mPreviewUrl;
    }

    public void setmPreviewUrl(String mPreviewUrl) {
        this.mPreviewUrl = mPreviewUrl;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmAlbum() {
        return mAlbum;
    }

    public void setmAlbum(String mAlbum) {
        this.mAlbum = mAlbum;
    }

    public String getmSmallImageUrl() {
        return mSmallImageUrl;
    }

    public void setmSmallImageUrl(String mSmallImageUrl) {
        this.mSmallImageUrl = mSmallImageUrl;
    }

    public String getmLargeImageUrl() {
        return mLargeImageUrl;
    }

    public void setmLargeImageUrl(String mLargeImageUrl) {
        this.mLargeImageUrl = mLargeImageUrl;
    }

    private Track(Parcel source) {
        mPreviewUrl = source.readString();
        mName = source.readString();
        mAlbum = source.readString();
        mSmallImageUrl = source.readString();
        mLargeImageUrl = source.readString();

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPreviewUrl);
        dest.writeString(mName);
        dest.writeString(mAlbum);
        dest.writeString(mSmallImageUrl);
        dest.writeString(mLargeImageUrl);
    }

    public final Creator<Track> CREATOR = new Creator<Track>() {
        @Override
        public Track createFromParcel(Parcel source) {
            return new Track(source);
        }

        @Override
        public Track[] newArray(int size) {
            return new Track[size];
        }
    };
}
