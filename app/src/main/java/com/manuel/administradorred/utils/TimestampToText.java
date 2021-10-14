package com.manuel.administradorred.utils;

import android.app.Application;

import com.manuel.administradorred.R;

public class TimestampToText extends Application {
    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public String getTimeAgo(long time) {
        if (time < 1000000000000L) {
            time *= 1000;
        }
        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return getString(R.string.a_while_ago);
        }
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return getString(R.string.a_while_ago);
        } else if (diff < 2 * MINUTE_MILLIS) {
            return getString(R.string.a_minute_ago);
        } else if (diff < 50 * MINUTE_MILLIS) {
            return getString(R.string.ago) + " " + diff / MINUTE_MILLIS + " " + getString(R.string.minutes);
        } else if (diff < 90 * MINUTE_MILLIS) {
            return getString(R.string.an_hour_ago);
        } else if (diff < 24 * HOUR_MILLIS) {
            return getString(R.string.ago) + " " + diff / HOUR_MILLIS + " " + getString(R.string.hours);
        } else if (diff < 48 * HOUR_MILLIS) {
            return getString(R.string.yesterday);
        } else {
            return getString(R.string.ago) + " " + diff / DAY_MILLIS + " " + getString(R.string.days);
        }
    }
}