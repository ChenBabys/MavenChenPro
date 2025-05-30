package cn.jzvd;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

import java.util.Formatter;
import java.util.Locale;

/**
 * 视频播放器工具类
 * 提供各种辅助功能，包括时间格式化、网络状态检查、屏幕方向控制等
 */
public class JZUtils {
    /** 日志标签 */
    public static final String TAG = "JZVD";
    /** 系统UI状态 */
    public static int SYSTEM_UI = 0;

    /**
     * 将毫秒时间转换为时分秒格式
     * @param timeMs 毫秒时间
     * @return 格式化后的时间字符串
     */
    public static String stringForTime(long timeMs) {
        if (timeMs <= 0 || timeMs >= 24 * 60 * 60 * 1000) {
            return "00:00";
        }
        long totalSeconds = timeMs / 1000;
        int seconds = (int) (totalSeconds % 60);
        int minutes = (int) ((totalSeconds / 60) % 60);
        int hours = (int) (totalSeconds / 3600);
        StringBuilder stringBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    /**
     * 检查是否连接到WiFi网络
     * 需要调用者持有ACCESS_NETWORK_STATE权限
     * @param context 上下文
     * @return 如果连接到WiFi返回true，否则返回false
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * 从Context对象中获取Activity
     * @param context 上下文
     * @return Activity对象，如果不是Activity则返回null
     */
    public static Activity scanForActivity(Context context) {
        if (context == null) return null;

        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return scanForActivity(((ContextWrapper) context).getBaseContext());
        }

        return null;
    }

    /**
     * 设置屏幕方向
     * @param context 上下文
     * @param orientation 屏幕方向
     */
    public static void setRequestedOrientation(Context context, int orientation) {
        if (JZUtils.scanForActivity(context) != null) {
            JZUtils.scanForActivity(context).setRequestedOrientation(
                    orientation);
        } else {
            JZUtils.scanForActivity(context).setRequestedOrientation(
                    orientation);
        }
    }

    /**
     * 获取Window对象
     * @param context 上下文
     * @return Window对象
     */
    public static Window getWindow(Context context) {
        if (JZUtils.scanForActivity(context) != null) {
            return JZUtils.scanForActivity(context).getWindow();
        } else {
            return JZUtils.scanForActivity(context).getWindow();
        }
    }

    /**
     * 将dp值转换为px值
     * @param context 上下文
     * @param dpValue dp值
     * @return px值
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 保存播放进度
     * @param context 上下文
     * @param url 视频URL
     * @param progress 播放进度
     */
    public static void saveProgress(Context context, Object url, long progress) {
        if (!Jzvd.SAVE_PROGRESS) return;
        Log.i(TAG, "saveProgress: " + progress);
        if (progress < 5000) {
            progress = 0;
        }
        SharedPreferences spn = context.getSharedPreferences("JZVD_PROGRESS",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spn.edit();
        editor.putLong("newVersion:" + url.toString(), progress).apply();
    }

    /**
     * 获取保存的播放进度
     * @param context 上下文
     * @param url 视频URL
     * @return 播放进度
     */
    public static long getSavedProgress(Context context, Object url) {
        if (!Jzvd.SAVE_PROGRESS) return 0;
        SharedPreferences spn = context.getSharedPreferences("JZVD_PROGRESS",
                Context.MODE_PRIVATE);
        return spn.getLong("newVersion:" + url.toString(), 0);
    }

    /**
     * 清除保存的播放进度
     * 如果url为null，则清除所有进度
     * @param context 上下文
     * @param url 视频URL，如果为null则清除所有进度
     */
    public static void clearSavedProgress(Context context, Object url) {
        if (url == null) {
            SharedPreferences spn = context.getSharedPreferences("JZVD_PROGRESS",
                    Context.MODE_PRIVATE);
            spn.edit().clear().apply();
        } else {
            SharedPreferences spn = context.getSharedPreferences("JZVD_PROGRESS",
                    Context.MODE_PRIVATE);
            spn.edit().putLong("newVersion:" + url.toString(), 0).apply();
        }
    }

    /**
     * 显示状态栏
     * @param context 上下文
     */
    @SuppressLint("RestrictedApi")
    public static void showStatusBar(Context context) {
        if (Jzvd.TOOL_BAR_EXIST) {
            JZUtils.getWindow(context).clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * 隐藏状态栏
     * 如果是沉浸式的，全屏前就没有状态栏
     * @param context 上下文
     */
    @SuppressLint("RestrictedApi")
    public static void hideStatusBar(Context context) {
        if (Jzvd.TOOL_BAR_EXIST) {
            JZUtils.getWindow(context).setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * 隐藏系统UI
     * @param context 上下文
     */
    @SuppressLint("NewApi")
    public static void hideSystemUI(Context context) {
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        ;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        SYSTEM_UI = JZUtils.getWindow(context).getDecorView().getSystemUiVisibility();
        JZUtils.getWindow(context).getDecorView().setSystemUiVisibility(uiOptions);

    }

    /**
     * 显示系统UI
     * @param context 上下文
     */
    @SuppressLint("NewApi")
    public static void showSystemUI(Context context) {
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        JZUtils.getWindow(context).getDecorView().setSystemUiVisibility(SYSTEM_UI);
    }

    /**
     * 获取状态栏高度
     * @param context 上下文
     * @return 状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }

    /**
     * 获取导航栏高度
     * @param context 上下文
     * @return 导航栏高度
     */
    public static int getNavigationBarHeight(Context context) {
        boolean var1 = ViewConfiguration.get(context).hasPermanentMenuKey();
        int var2;
        return (var2 = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android")) > 0 && !var1 ? context.getResources().getDimensionPixelSize(var2) : 0;
    }

    /**
     * 获取屏幕宽度
     * @param context 上下文
     * @return 屏幕宽度
     */
    public static int getScreenWidth(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    /**
     * 获取屏幕高度
     * @param context 上下文
     * @return 屏幕高度
     */
    public static int getScreenHeight(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }

}
