package com.makotokw.android.youtube;

public class YouTubeError extends Exception {

    public YouTubeError() {
    }

    public YouTubeError(String exceptionMessage) {
        super(exceptionMessage);
    }

    public YouTubeError(String exceptionMessage, Throwable reason) {
        super(exceptionMessage, reason);
    }

    public YouTubeError(Throwable cause) {
        super(cause);
    }
}
