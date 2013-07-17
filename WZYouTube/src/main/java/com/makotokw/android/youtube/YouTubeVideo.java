package com.makotokw.android.youtube;

import android.text.Html;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

//import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

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
    private JSONObject mContentAttributes; // from watchUrl

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
        return "http://www.youtube.com/watch?v=" + mVideoId;
    }

    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.mThumbnailUrl = thumbnailUrl;
    }

    public String getMediaUrl(YouTubeVideoQuality quality) {
        String mediaUrl = null;
        if (mContentAttributes.has("video")) {
            JSONObject video = null;
            try {
                video = mContentAttributes.getJSONObject("video");
                if (video.has("fmt_stream_map")) {
                    JSONArray videos = video.getJSONArray("fmt_stream_map");
                    int mediaCount = videos.length();
                    if (mediaCount > 0) {
                        int index = 0;
                        if (quality == YouTubeVideoQuality.Medium) {
                            index = Math.min(mediaCount - 1, 1);
                        } else if (quality == YouTubeVideoQuality.Small) {
                            index = mediaCount - 1;
                        }
                        JSONObject stream = videos.getJSONObject(index);
                        if (stream != null) {
                            mediaUrl = stream.getString("url");
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
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

//                        Log.d(TAG, "onResponse: " + response);

                        YouTubeError error = null;
                        try {
                            processWatchUrlPage(response);
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

    private void processWatchUrlPage(String html) throws ParseError {

        if (html.length() == 0) {
            throw new ParseError(null, "page is empty");
        }

//        Log.d(TAG, html);

        String jsonString = extractBootstrapData(html);
        if (jsonString == null) {
            jsonString = extractPiggybackData(html);
        }

        if (jsonString == null) {
            throw new ParseError(html, "The JSON data could not be found");
        }

        try {
            JSONObject jsonData = new JSONObject(jsonString);

            if (!jsonData.has("content")) {
                mContentAttributes = null;

                if (jsonData.has("errors")) {
                    String errorMessage = "The content data could not be found.";
                    JSONArray errors = jsonData.getJSONArray("errors");
                    if (errors != null && errors.length() > 0) {
                        errorMessage = errors.getString(0);
                    }
                    throw new ParseError(html, errorMessage);
                }

            } else {
                mContentAttributes = jsonData.getJSONObject("content");

                JSONObject video = mContentAttributes.getJSONObject("video");
                if (video != null) {
                    if (mTitle == null || mTitle.length() == 0) {
                        mTitle = video.getString("title");
                        Log.d(TAG, "title = " + mTitle);

                        mTitle = StringEscapeUtils.unescapeJava(mTitle);
                        Log.d(TAG, "title = " + mTitle);

                    }
                    if (mDuration <= 0) {
                        mDuration = video.getLong("length_seconds");
                        Log.d(TAG, "mDuration = " + mDuration);
                    }
                    if (mThumbnailUrl == null || mThumbnailUrl.length() == 0) {
                        mThumbnailUrl = video.getString("thumbnail_for_watch");
                        Log.d(TAG, "mThumbnailUrl = " + mThumbnailUrl);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            throw new ParseError(jsonString, "The JSON data could not be parsed", e);
        }
    }

    private String extractBootstrapData(String html) {
        String jsonString = null;

        String start = null;
        String startFull = "var bootstrap_data = \")]}'";
        String stringShrunk = startFull.replace(" ", "");

        if (html.contains(startFull)) {
            start = startFull;
        } else if (html.contains(stringShrunk)) {
            start = stringShrunk;
        }

        if (start != null) {
            int startIndex = html.indexOf(start) + start.length();
            int endIndex = html.indexOf("\";", startIndex);
            jsonString = html.substring(startIndex, endIndex);
            jsonString = unescapeString(jsonString);
        }
        return jsonString;
    }

    private String extractPiggybackData(String html) {
        String jsonString = null;

        String start = null;
        String startFull = "ls.setItem('PIGGYBACK_DATA', \")]}'";
        String stringShrunk = startFull.replace(" ", "");

        if (html.contains(startFull)) {
            start = startFull;
        } else if (html.contains(stringShrunk)) {
            start = stringShrunk;
        }

        if (start != null) {
            int startIndex = html.indexOf(start) + start.length();
            int endIndex = html.indexOf("\");", startIndex);
            jsonString = html.substring(startIndex, endIndex);
            jsonString = unescapeString(jsonString);
        }
        return jsonString;
    }

    private String unescapeString(String string) {
        String result = string.toString();
//        result = result.replace("\\\"", "\"");
        result = result.replace("\\\\\"", "'");
//        result = result.replace("\\\\\\/", "\\/");
//
        result = StringEscapeUtils.unescapeEcmaScript(result);

        return result;
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
