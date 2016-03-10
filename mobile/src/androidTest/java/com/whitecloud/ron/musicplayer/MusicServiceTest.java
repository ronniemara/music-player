package com.whitecloud.ron.musicplayer;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.Toast;

import com.whitecloud.ron.musicplayer.artist.Singer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;



import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class MusicServiceTest {
    IBinder binder = null;
    Messenger mRequest;
    ReplyHandler mReplyHandler = new ReplyHandler();

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();


    @Before
    public void setUp() throws Exception {

    }

    @Test(expected = RuntimeException.class)
    public void testMessageWithNoReplyHandler() throws TimeoutException, RemoteException {
        // Create the service Intent.

        // Create the service Intent.
        Intent serviceIntent =
                new Intent(InstrumentationRegistry.getTargetContext(),
                        MusicService.class);

        // Bind the service and grab a reference to the binder.
        binder = mServiceRule.bindService(serviceIntent);
        mRequest = new Messenger(binder);

        Message message = Message.obtain();
        message.what = MusicService.GET_ARTISTS;
        message.replyTo = new Messenger(mReplyHandler);

        mRequest.send(message);
    }


    class ReplyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MusicService.GET_ARTISTS : {
                    List<Singer> singers = MusicService.artists(msg);
                }
            }
        }
    }

    @Test
    public void testOnCreate() throws Exception {

    }

    @Test
    public void testOnStartCommand() throws Exception {

    }

    @Test
    public void testGetSongs() throws Exception {

    }

    @Test
    public void testGetToken() throws Exception {

    }

    @Test
    public void testOnMusicCatalogReady() throws Exception {

    }

    @Test
    public void testOnGetArtists() throws Exception {

    }

    @Test
    public void testArtists() throws Exception {

    }

    @Test
    public void testOnGetTopTracks() throws Exception {

    }

    @Test
    public void testOnBind() throws Exception {

    }

    @Test
    public void testOnDestroy() throws Exception {

    }
}