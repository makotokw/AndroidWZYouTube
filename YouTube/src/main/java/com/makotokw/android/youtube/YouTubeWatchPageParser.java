package com.makotokw.android.youtube;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

public class YouTubeWatchPageParser {

    private static final String TAG = YouTubeVideo.class.getSimpleName();

    public static final String CONTENT_KEY = "content";
    public static final String ERRORS_KEY = "errors";
    public static final String CONTENT_VIDEO_TITLE_KEY = "content/video/title";
    public static final String CONTENT_VIDEO_LENGTH_KEY = "content/video/length";
    public static final String CONTENT_VIDEO_THUMBNAIL_KEY = "content/video/thumbnail";
    public static final String CONTENT_VIDEO_STREAM_KEY = "content/video/stream";

    private static YouTubeWatchPageParser sInstance = null;
    private String mBeforeJsonStatement;
    private String mAfterJsonStatement;
    private Hashtable<String, String[]> mPathComponents;

    private YouTubeWatchPageParser() {

        mBeforeJsonStatement = "var bootstrap_data = \")]}'";
        mAfterJsonStatement = "\";";

        mPathComponents = new Hashtable<String, String[]>();
        mPathComponents.put(CONTENT_KEY, new String[] {"content"});
        mPathComponents.put(ERRORS_KEY, new String[] {"errors"});
        mPathComponents.put(CONTENT_VIDEO_TITLE_KEY, new String[] {"video", "title"});
        mPathComponents.put(CONTENT_VIDEO_LENGTH_KEY, new String[] {"video", "length_seconds"});
        mPathComponents.put(CONTENT_VIDEO_THUMBNAIL_KEY, new String[] {"video", "thumbnail_for_watch"});
        mPathComponents.put(CONTENT_VIDEO_STREAM_KEY, new String[] {"player_data", "fmt_stream_map"});
    }

    public static YouTubeWatchPageParser getInstance() {
        if (sInstance == null) {
            sInstance = new YouTubeWatchPageParser();
        }
        return sInstance;
    }

    public String getBeforeJsonStatement() {
        return mBeforeJsonStatement;
    }

    public void setBeforeJsonStatement(String beforeJsonStatement) {
        mBeforeJsonStatement = beforeJsonStatement;
    }

    public String getAfterJsonStatement() {
        return mAfterJsonStatement;
    }

    public void setAfterJsonStatement(String afterJsonStatement) {
        mAfterJsonStatement = afterJsonStatement;
    }

    public void putPathComponents(String key, String [] pathComponents) {
        mPathComponents.put(key, pathComponents);
    }

    public void putPathComponents(String key, JSONArray pathComponents) {
        if (pathComponents.length() > 0) {
            ArrayList<String> stringArrayList = new ArrayList<String>();
            for (int i = 0; i < pathComponents.length(); i++) {
                try {
                    stringArrayList.add(pathComponents.getString(i));
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            putPathComponents(key, stringArrayList.toArray(new String[0]));
        }
    }

    public void parsePageWithData(String html, YouTubeVideo copyToVideo) throws ParseError {

        if (html.length() == 0) {
            throw new ParseError(null, "page is empty");
        }

        String jsonString = extractBootstrapData(html);
        if (jsonString == null) {
            throw new ParseError(html, "The JSON data could not be found");
        }

        try {
            JSONObject jsonData = new JSONObject(jsonString);
            JSONObject contentAttributes = null;

            if (!jsonData.has("content")) {
                if (jsonData.has("errors")) {
                    String errorMessage = "The content data could not be found.";
                    JSONArray errors = jsonData.getJSONArray("errors");
                    if (errors != null && errors.length() > 0) {
                        errorMessage = errors.getString(0);
                    }
                    throw new ParseError(html, errorMessage);
                }
            } else {
                contentAttributes = jsonData.getJSONObject("content");

                if (StringUtils.isEmpty(copyToVideo.getTitle())) {
                    String title = (String)getForKey(contentAttributes, CONTENT_VIDEO_TITLE_KEY);
                    if (title != null) {
                        title = StringEscapeUtils.unescapeJava(title);
                        copyToVideo.setTitle(title);
                    }
                }
                if (copyToVideo.getDuration() <= 0) {
                    Number lengthSeconds = (Number)getForKey(contentAttributes, CONTENT_VIDEO_LENGTH_KEY);
                    if (lengthSeconds != null) {
                        Log.d(TAG, "length_seconds = " + lengthSeconds.longValue());
                        copyToVideo.setDuration(lengthSeconds.longValue());
                    }
                }

                if (StringUtils.isEmpty(copyToVideo.getThumbnailUrl())) {
                    String thumbnailForWatch = (String)getForKey(contentAttributes, CONTENT_VIDEO_THUMBNAIL_KEY);
                    if (thumbnailForWatch != null) {
                        Log.d(TAG, "thumbnail_for_watch = " + thumbnailForWatch);
                        copyToVideo.setThumbnailUrl(thumbnailForWatch);
                    }
                }

                copyToVideo.setStreamMap((JSONArray)getForKey(contentAttributes, CONTENT_VIDEO_STREAM_KEY));
                copyToVideo.setContentAttributes(contentAttributes);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            throw new ParseError(jsonString, "The JSON data could not be parsed", e);
        }
    }

    private Object getForKey(JSONObject jsonObject, String key) {
        try {
            JSONObject object = findParentObjectForKey(jsonObject, key);
            if (object != null) {
                String [] pathComponents = mPathComponents.get(key);
                String lastComponent = pathComponents[pathComponents.length - 1];
                return object.get(lastComponent);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    private JSONObject findParentObjectForKey(JSONObject jsonObject, String key) throws JSONException {
        JSONObject cursor = jsonObject;
        String [] pathComponents = mPathComponents.get(key);
        if (pathComponents != null && pathComponents.length > 0) {
            int numberOfComponents = pathComponents.length;
            if (numberOfComponents > 1) {
                for (int i = 0; i < numberOfComponents - 1; i++) {
                    cursor = jsonObject.getJSONObject(pathComponents[i]);
                    if (cursor == null) {
                        break;
                    }
                }
            }
        }
        return cursor;
    }

    private String extractBootstrapData(String html) {
        String jsonString = null;

        String start = null;
        String startFull = mBeforeJsonStatement;
        String stringShrunk = startFull.replace(" ", "");

        if (html.contains(startFull)) {
            start = startFull;
        } else if (html.contains(stringShrunk)) {
            start = stringShrunk;
        }

        if (start != null) {
            int startIndex = html.indexOf(start) + start.length();
            int endIndex = html.indexOf(mAfterJsonStatement, startIndex);
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
}
