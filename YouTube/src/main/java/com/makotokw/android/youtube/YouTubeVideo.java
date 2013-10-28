package com.makotokw.android.youtube;

import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class YouTubeVideo {

    public static void setRequestQueue(RequestQueue requestQueue) {
        YouTubeVideo.sRequestQueue = requestQueue;
    }

    public interface Listener {
        void onResponse(YouTubeError error);
    }

    private static final String TAG = YouTubeVideo.class.getSimpleName();

    private static RequestQueue sRequestQueue;
    private static DefaultRetryPolicy sRetryPolicy;
    public static final int DEFAULT_NETWORK_TIMEOUT_MS = 30000;
    public static final int DEFAULT_NETWORK_MAX_RETRIES = 1;
    public static final float DEFAULT_NETWORK_BACKOFF_MULT = 1.0f;

    private String mVideoId;
    private String mTitle;
    private String mMediaDescription;
    private long mDuration;
    private String mThumbnailUrl;

    private JSONArray mStreamMap;
    private JSONObject mContentAttributes; // from watchUrl

    public static String createWatchUrl(String videoId) {
        return "http://www.youtube.com/watch?v=" + videoId;
    }

    public String getVideoId() {
        return mVideoId;
    }

    public void setVideoId(String videoId) {
        this.mVideoId = videoId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getMediaDescription() {
        return mMediaDescription;
    }

    public void setMediaDescription(String mediaDescription) {
        this.mMediaDescription = mediaDescription;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        this.mDuration = duration;
    }

    public String getWatchUrl() {
        return YouTubeVideo.createWatchUrl(mVideoId);
    }

    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.mThumbnailUrl = thumbnailUrl;
    }

    protected JSONArray getStreamMap() {
        return mStreamMap;
    }

    protected void setStreamMap(JSONArray streamMap) {
        mStreamMap = streamMap;
    }

    protected JSONObject getContentAttributes() {
        return mContentAttributes;
    }

    protected void setContentAttributes(JSONObject contentAttributes) {
        mContentAttributes = contentAttributes;
    }

    public String getMediaUrl(YouTubeVideoQuality quality) {
        String mediaUrl = null;
        try {
            if (mStreamMap != null) {
                int mediaCount = mStreamMap.length();
                if (mediaCount > 0) {
                    int index = 0;
                    if (quality == YouTubeVideoQuality.Medium) {
                        index = Math.min(mediaCount - 1, 1);
                    } else if (quality == YouTubeVideoQuality.Small) {
                        index = mediaCount - 1;
                    }
                    JSONObject stream = mStreamMap.getJSONObject(index);
                    if (stream != null) {
                        mediaUrl = stream.getString("url");
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mediaUrl;
    }

    public void retributeDataFromWatchPage(final Listener listener) {

        if (YouTubeVideo.sRequestQueue == null) {
            throw new IllegalStateException("YouTubeVideo requires RequestQueue. You should set it by using setRequestQueue");
        }

        if (YouTubeVideo.sRetryPolicy == null) {
            YouTubeVideo.sRetryPolicy = new DefaultRetryPolicy(
                    DEFAULT_NETWORK_TIMEOUT_MS,
                    DEFAULT_NETWORK_MAX_RETRIES,
                    DEFAULT_NETWORK_BACKOFF_MULT
            );
        }

        StringRequest request = new StringRequest(getWatchUrl(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        YouTubeError error = null;
                        try {
                            YouTubeWatchPageParser.getInstance().parsePageWithData(response, YouTubeVideo.this);
                        } catch (YouTubeError youTubeError) {
                            error = youTubeError;
                        }

                        if (listener != null) {
                            listener.onResponse(error);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.d(TAG, "onErrorResponse: " + volleyError.getLocalizedMessage());
                        if (listener != null) {
                            YouTubeError error = new YouTubeError(volleyError);
                            listener.onResponse(error);
                        }
                    }
                }
        );

        request.setRetryPolicy(YouTubeVideo.sRetryPolicy);
        YouTubeVideo.sRequestQueue.add(request);
        YouTubeVideo.sRequestQueue.start();
    }






    public static void log(String str) {
        if (str.length() > 4000) {
            Log.e(TAG, str.substring(0, 4000));
            log(str.substring(4000));
        } else {
            Log.e(TAG, str);
        }
    }
}
