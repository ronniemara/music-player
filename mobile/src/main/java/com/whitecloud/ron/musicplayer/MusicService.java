package com.whitecloud.ron.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by ron on 18/01/16.
 */
public class MusicService extends Service {

    private IBinder mIBInder = new LocalBInder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mIBInder;
    }

    public class LocalBInder extends Binder {

        public MusicService getService() {
            return MusicService.this;
        }
    }

}
