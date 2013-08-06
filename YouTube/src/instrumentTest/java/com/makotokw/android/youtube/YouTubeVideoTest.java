package com.makotokw.android.youtube;

import android.test.AndroidTestCase;
import android.util.Log;

import com.android.volley.toolbox.Volley;

import java.util.concurrent.CountDownLatch;

public class YouTubeVideoTest extends AndroidTestCase {

    private static final String TAG = YouTubeVideo.class.getSimpleName();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        YouTubeVideo.setRequestQueue(Volley.newRequestQueue(getContext()));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testWatchPage() throws InterruptedException {

        final CountDownLatch startSignal = new CountDownLatch(1);

        final YouTubeVideo video = new YouTubeVideo();
//        video.setVideoId("lmv1dTnhLH4");
        video.setVideoId("ycpC2ciP2OQ");

        assertNotNull("getWatchUrl", video.getWatchUrl());

        video.retributeDataFromWatchPage(new YouTubeVideo.Listener() {
            @Override
            public void onResponse(YouTubeError error) {
                if (error != null) {
                    fail(error.getMessage());
                } else {
                    Log.d("Test", video.getMediaUrl(YouTubeVideoQuality.Large));
                    assertNotNull("getMediaUrl", video.getMediaUrl(YouTubeVideoQuality.Large));
                }
                startSignal.countDown();
            }
        });

        startSignal.await();
    }

}
