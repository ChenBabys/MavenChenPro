package com.godox.common.costomseekbar;

import android.util.Log;
import android.view.View;


public class Logger {
    private static boolean debug = false;
    private static final String TAG = "ColorSeekBarLib";

    public static void i(String s) {
        if (debug) Log.i(TAG, s);
    }

    public static void i(float value) {
        if (debug) Log.i(TAG, String.valueOf(value));
    }

    public static void spec(int spec) {
        if (debug) {
            switch (spec) {
                case View.MeasureSpec.AT_MOST:
                    Log.i(TAG, "AT_MOST");
                    break;
                case View.MeasureSpec.EXACTLY:
                    Log.i(TAG, "EXACTLY");
                    break;
                case View.MeasureSpec.UNSPECIFIED:
                    Log.i(TAG, "UNSPECIFIED");
                    break;
                default:
                    Log.i(TAG, String.valueOf(spec));
                    break;
            }
        }
    }
}
