package com.whitecloud.ron.musicplayer.track;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {

    String mPreviewUrl;
    String mName;
    String mAlbum;
    String mSmallImageUrl;
    String mLargeImageUrl;

    public Song(String previewUrl, String name, String album, String smallUrl, String largeUrl) {
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

    private Song(Parcel source) {
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

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}
