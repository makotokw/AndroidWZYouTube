package com.makotokw.android.youtube;

public class ParseError extends YouTubeError {
    private String mNetworkResponse;

    public ParseError(String networkResponse, String exceptionMessage) {
        super(exceptionMessage);
        mNetworkResponse = networkResponse;
    }

    public ParseError(String networkResponse, String exceptionMessage, Throwable reason) {
        super(exceptionMessage, reason);
        mNetworkResponse = networkResponse;
    }

    public String getNetworkResponse() {
        return mNetworkResponse;
    }
}
