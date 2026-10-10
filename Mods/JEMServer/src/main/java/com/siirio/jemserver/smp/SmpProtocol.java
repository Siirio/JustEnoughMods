package com.siirio.jemserver.smp;

public final class SmpProtocol {
    public static final String VERSION = "3";
    public static final int MAX_REQUEST_BYTES = 4096;
    public static final int MAX_VIEW_BYTES = 1048576;
    public static final int MAX_FILTER = 64;
    public static final int MAX_SEARCH = 160;
    public static final int MAX_TITLE = 80;
    public static final int MAX_DESCRIPTION = 1024;
    public static final int MAX_LANGUAGE = 24;
    public static final int MAX_MESSAGE = 256;
    public static final long RESYNC_MILLIS = 1000;
    public static final int MAX_PAGE = 10000;
    public static final int RECEIPTS = 64;
    public static final int ACTION_TICKS = 3;
    public static final int QUERY_TICKS = 3;
    public static final int REVIVE_TICKS = 3;

    private SmpProtocol() {}
}
