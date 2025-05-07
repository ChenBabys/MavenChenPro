package cn.jzvd;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 视频播放器基类
 * 实现了视频播放器的基本功能，包括播放控制、全屏切换、手势控制等
 * 子类需要实现getLayoutId()方法来提供自定义布局
 */
public abstract class Jzvd extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {

    /** 日志标签 */
    public static final String TAG = "JZVD";
    /** 普通屏幕模式 */
    public static final int SCREEN_NORMAL = 0;
    /** 全屏模式 */
    public static final int SCREEN_FULLSCREEN = 1;
    /** 小窗口模式 */
    public static final int SCREEN_TINY = 2;
    /** 空闲状态 */
    public static final int STATE_IDLE = -1;
    /** 普通状态 */
    public static final int STATE_NORMAL = 0;
    /** 准备中状态 */
    public static final int STATE_PREPARING = 1;
    /** 准备中切换URL状态 */
    public static final int STATE_PREPARING_CHANGE_URL = 2;
    /** 准备中播放状态 */
    public static final int STATE_PREPARING_PLAYING = 3;
    /** 准备完成状态 */
    public static final int STATE_PREPARED = 4;
    /** 播放中状态 */
    public static final int STATE_PLAYING = 5;
    /** 暂停状态 */
    public static final int STATE_PAUSE = 6;
    /** 自动播放完成状态 */
    public static final int STATE_AUTO_COMPLETE = 7;
    /** 错误状态 */
    public static final int STATE_ERROR = 8;
    /** 视频显示类型：自适应 */
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER = 0;
    /** 视频显示类型：填充父容器 */
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT = 1;
    /** 视频显示类型：填充并裁剪 */
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP = 2;
    /** 视频显示类型：原始尺寸 */
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ORIGINAL = 3;
    /** 手势阈值 */
    public static final int THRESHOLD = 80;
    /** 当前播放器实例 */
    public static Jzvd CURRENT_JZVD;
    /** 容器列表 */
    public static LinkedList<ViewGroup> CONTAINER_LIST = new LinkedList<>();
    /** 是否显示工具栏 */
    public static boolean TOOL_BAR_EXIST = true;
    /** 全屏时的屏幕方向 */
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
    /** 普通模式时的屏幕方向 */
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    /** 是否保存播放进度 */
    public static boolean SAVE_PROGRESS = true;
    /** 是否显示WiFi提示对话框 */
    public static boolean WIFI_TIP_DIALOG_SHOWED = false;
    /** 视频显示类型 */
    public static int VIDEO_IMAGE_DISPLAY_TYPE = 0;
    /** 上次自动全屏时间 */
    public static long lastAutoFullscreenTime = 0;
    /** 播放暂停临时状态 */
    public static int ON_PLAY_PAUSE_TMP_STATE = 0;
    /** 缓冲状态备份 */
    public static int backUpBufferState = -1;
    /** 进度条滑动阻尼系数 */
    public static float PROGRESS_DRAG_RATE = 1f;
    /** 音频焦点变化监听器 */
    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    Log.d(TAG, "AUDIOFOCUS_LOSS [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    try {
                        Jzvd player = CURRENT_JZVD;
                        if (player != null && player.state == Jzvd.STATE_PLAYING) {
                            player.startButton.performClick();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };

    /** 当前状态 */
    public int state = -1;
    /** 当前屏幕模式 */
    public int screen = -1;
    /** 视频数据源 */
    public JZDataSource jzDataSource;
    /** 宽度比例 */
    public int widthRatio = 0;
    /** 高度比例 */
    public int heightRatio = 0;
    /** 媒体接口类 */
    public Class mediaInterfaceClass;
    /** 媒体接口实例 */
    public JZMediaInterface mediaInterface;
    /** 在列表中的位置 */
    public int positionInList = -1;
    /** 视频旋转角度 */
    public int videoRotation = 0;
    /** 手动跳转位置 */
    public int seekToManulPosition = -1;
    /** 提前跳转时间 */
    public long seekToInAdvance;

    /** 开始按钮 */
    public ImageView startButton;
    /** 进度条 */
    public SeekBar progressBar;
    /** 全屏按钮 */
    public ImageView fullscreenButton;
    /** 当前时间文本 */
    public TextView currentTimeTextView;
    /** 总时间文本 */
    public TextView totalTimeTextView;
    /** 视频容器 */
    public ViewGroup textureViewContainer;
    /** 顶部容器 */
    public ViewGroup topContainer;
    /** 底部容器 */
    public ViewGroup bottomContainer;
    /** 视频控制按钮 */
    public ImageView videoControl;
    /** 视频渲染视图 */
    public JZTextureView textureView;
    /** 是否预加载 */
    public boolean preloading = false;
    /** 返回全屏时间 */
    protected long gobakFullscreenTime = 0;
    /** 进入全屏时间 */
    protected long gotoFullscreenTime = 0;
    /** 进度更新定时器 */
    protected Timer UPDATE_PROGRESS_TIMER;
    /** 屏幕宽度 */
    protected int mScreenWidth;
    /** 屏幕高度 */
    protected int mScreenHeight;
    /** 音频管理器 */
    protected AudioManager mAudioManager;
    /** 进度更新任务 */
    protected ProgressTimerTask mProgressTimerTask;
    /** 是否正在触摸进度条 */
    protected boolean mTouchingProgressBar;
    /** 按下时的X坐标 */
    protected float mDownX;
    /** 按下时的Y坐标 */
    protected float mDownY;
    /** 是否正在调节音量 */
    protected boolean mChangeVolume;
    /** 是否正在调节进度 */
    protected boolean mChangePosition;
    /** 是否正在调节亮度 */
    protected boolean mChangeBrightness;
    /** 手势按下时的播放位置 */
    protected long mGestureDownPosition;
    /** 手势按下时的音量 */
    protected int mGestureDownVolume;
    /** 手势按下时的亮度 */
    protected float mGestureDownBrightness;
    /** 跳转时间位置 */
    protected long mSeekTimePosition;
    /** 上下文 */
    protected Context jzvdContext;
    /** 当前播放位置 */
    protected long mCurrentPosition;
    /** 布局参数 */
    protected ViewGroup.LayoutParams blockLayoutParams;
    /** 块索引 */
    protected int blockIndex;
    /** 块宽度 */
    protected int blockWidth;
    /** 块高度 */
    protected int blockHeight;

    /**
     * 构造函数
     * @param context 上下文
     */
    public Jzvd(Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数
     * @param context 上下文
     * @param attrs 属性集
     */
    public Jzvd(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 恢复播放
     * 增加准备状态逻辑
     */
    public static void goOnPlayOnResume() {
        if (CURRENT_JZVD != null) {
            Log.i("videostate","state:"+CURRENT_JZVD.state);
            if (CURRENT_JZVD.state == Jzvd.STATE_PAUSE) {
                if (ON_PLAY_PAUSE_TMP_STATE == STATE_PAUSE) {
                    CURRENT_JZVD.onStatePause();
                    CURRENT_JZVD.mediaInterface.pause();
                } else {
                    CURRENT_JZVD.onStatePlaying();
                    CURRENT_JZVD.mediaInterface.start();
                }
                ON_PLAY_PAUSE_TMP_STATE = 0;
            } else if (CURRENT_JZVD.state == Jzvd.STATE_PREPARING) {
                //准备状态暂停后的
                CURRENT_JZVD.startVideo();
            }
            if (CURRENT_JZVD.screen == Jzvd.SCREEN_FULLSCREEN) {
                JZUtils.hideStatusBar(CURRENT_JZVD.jzvdContext);
                JZUtils.hideSystemUI(CURRENT_JZVD.jzvdContext);
            }
        }
    }

    /**
     * 暂停播放
     * 增加准备状态逻辑
     */
    public static void goOnPlayOnPause() {
        if (CURRENT_JZVD != null) {
            if (CURRENT_JZVD.state == Jzvd.STATE_AUTO_COMPLETE ||
                    CURRENT_JZVD.state == Jzvd.STATE_NORMAL ||
                    CURRENT_JZVD.state == Jzvd.STATE_ERROR) {
                Jzvd.releaseAllVideos();
            } else if (CURRENT_JZVD.state == Jzvd.STATE_PREPARING) {
                //准备状态暂停的逻辑
                Jzvd.setCurrentJzvd(CURRENT_JZVD);
                CURRENT_JZVD.state = STATE_PREPARING;
            } else {
                ON_PLAY_PAUSE_TMP_STATE = CURRENT_JZVD.state;
                CURRENT_JZVD.onStatePause();
                CURRENT_JZVD.mediaInterface.pause();
            }
        }
    }

    /**
     * 直接启动全屏播放
     * @param context 上下文
     * @param _class 播放器类
     * @param url 视频URL
     * @param title 视频标题
     */
    public static void startFullscreenDirectly(Context context, Class _class, String url, String title) {
        startFullscreenDirectly(context, _class, new JZDataSource(url, title));
    }

    /**
     * 直接启动全屏播放
     * @param context 上下文
     * @param _class 播放器类
     * @param jzDataSource 视频数据源
     */
    public static void startFullscreenDirectly(Context context, Class _class, JZDataSource jzDataSource) {
        JZUtils.hideStatusBar(context);
        JZUtils.setRequestedOrientation(context, FULLSCREEN_ORIENTATION);
        JZUtils.hideSystemUI(context);

        ViewGroup vp = (ViewGroup) JZUtils.scanForActivity(context).getWindow().getDecorView();
        try {
            Constructor<Jzvd> constructor = _class.getConstructor(Context.class);
            final Jzvd jzvd = constructor.newInstance(context);
            LayoutParams lp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(jzvd, lp);
            jzvd.setUp(jzDataSource, JzvdStd.SCREEN_FULLSCREEN);
            jzvd.startVideo();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放所有视频
     */
    public static void releaseAllVideos() {
        Log.d(TAG, "releaseAllVideos");
        if (CURRENT_JZVD != null) {
            CURRENT_JZVD.reset();
            CURRENT_JZVD = null;
        }
        CONTAINER_LIST.clear();
    }

    /**
     * 处理返回键
     * @return 是否处理了返回键
     */
    public static boolean backPress() {
        Log.i(TAG, "backPress");
        if (CONTAINER_LIST.size() != 0 && CURRENT_JZVD != null) {//判断条件，因为当前所有goBack都是回到普通窗口
            CURRENT_JZVD.gotoNormalScreen();
            return true;
        } else if (CONTAINER_LIST.size() == 0 && CURRENT_JZVD != null && CURRENT_JZVD.screen != SCREEN_NORMAL) {//退出直接进入的全屏
            CURRENT_JZVD.clearFloatScreen();
            return true;
        }
        return false;
    }

    /**
     * 设置当前播放器
     * @param jzvd 播放器实例
     */
    public static void setCurrentJzvd(Jzvd jzvd) {
        Log.i("videostate","setCurrentJzvd"+((CURRENT_JZVD != null)));
        if (CURRENT_JZVD != null) CURRENT_JZVD.reset();
        CURRENT_JZVD = jzvd;
    }

    /**
     * 设置TextureView旋转角度
     * @param rotation 旋转角度
     */
    public static void setTextureViewRotation(int rotation) {
        if (CURRENT_JZVD != null) {
            CURRENT_JZVD.videoRotation = rotation;
            CURRENT_JZVD.textureView.setRotation(rotation);
        }
    }

    /**
     * 设置视频显示类型
     * @param type 显示类型
     */
    public static void setVideoImageDisplayType(int type) {
        VIDEO_IMAGE_DISPLAY_TYPE = type;
        if (CURRENT_JZVD != null) {
            CURRENT_JZVD.textureView.requestLayout();
        }
    }

    /**
     * 获取布局ID
     * @return 布局ID
     */
    public abstract int getLayoutId();

    /**
     * 初始化
     * @param context 上下文
     */
    public void init(Context context) {
        View.inflate(context, getLayoutId(), this);
        jzvdContext = context;
        startButton = findViewById(R.id.start);
        fullscreenButton = findViewById(R.id.fullscreen);
        progressBar = findViewById(R.id.bottom_seek_progress);
        currentTimeTextView = findViewById(R.id.current);
        totalTimeTextView = findViewById(R.id.total);
        bottomContainer = findViewById(R.id.layout_bottom);
        textureViewContainer = findViewById(R.id.surface_container);
        topContainer = findViewById(R.id.layout_top);
        videoControl = (ImageView)findViewById(R.id.iv_video_control);


        if (startButton == null) {
            startButton = new ImageView(context);
        }
        if (fullscreenButton == null) {
            fullscreenButton = new ImageView(context);
        }
        if (progressBar == null) {
            progressBar = new SeekBar(context);
        }
        if (currentTimeTextView == null) {
            currentTimeTextView = new TextView(context);
        }
        if (totalTimeTextView == null) {
            totalTimeTextView = new TextView(context);
        }
        if (bottomContainer == null) {
            bottomContainer = new LinearLayout(context);
        }
        if (textureViewContainer == null) {
            textureViewContainer = new FrameLayout(context);
        }
        if (topContainer == null) {
            topContainer = new RelativeLayout(context);
        }
        if(videoControl == null){
            videoControl = new ImageView(context);
        }

        startButton.setOnClickListener(this);
        fullscreenButton.setOnClickListener(this);
        progressBar.setOnSeekBarChangeListener(this);
        bottomContainer.setOnClickListener(this);
        textureViewContainer.setOnClickListener(this);
        textureViewContainer.setOnTouchListener(this);
        videoControl.setOnClickListener(this);

        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;

        state = STATE_IDLE;
    }

    /**
     * 设置视频源
     * @param url 视频URL
     * @param title 视频标题
     */
    public void setUp(String url, String title) {
        setUp(new JZDataSource(url, title), SCREEN_NORMAL);
    }

    /**
     * 设置视频源
     * @param url 视频URL
     * @param title 视频标题
     * @param screen 屏幕模式
     */
    public void setUp(String url, String title, int screen) {
        setUp(new JZDataSource(url, title), screen);
    }

    /**
     * 设置视频源
     * @param jzDataSource 视频数据源
     * @param screen 屏幕模式
     */
    public void setUp(JZDataSource jzDataSource, int screen) {
        setUp(jzDataSource, screen, JZMediaSystem.class);
    }

    /**
     * 设置视频源
     * @param url 视频URL
     * @param title 视频标题
     * @param screen 屏幕模式
     * @param mediaInterfaceClass 媒体接口类
     */
    public void setUp(String url, String title, int screen, Class mediaInterfaceClass) {
        setUp(new JZDataSource(url, title), screen, mediaInterfaceClass);
    }

    /**
     * 设置视频源
     * @param jzDataSource 视频数据源
     * @param screen 屏幕模式
     * @param mediaInterfaceClass 媒体接口类
     */
    public void setUp(JZDataSource jzDataSource, int screen, Class mediaInterfaceClass) {
        this.jzDataSource = jzDataSource;
        this.screen = screen;
        onStateNormal();
        this.mediaInterfaceClass = mediaInterfaceClass;
    }

    /**
     * 设置媒体接口
     * @param mediaInterfaceClass 媒体接口类
     */
    public void setMediaInterface(Class mediaInterfaceClass) {
        reset();
        this.mediaInterfaceClass = mediaInterfaceClass;
    }

    /**
     * 点击事件处理
     * @param v 被点击的视图
     */
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start|| i == R.id.iv_video_control) {
            clickStart();
        } else if (i == R.id.fullscreen) {
            clickFullscreen();
        }
    }

    /**
     * 点击全屏按钮
     */
    protected void clickFullscreen() {
        Log.i(TAG, "onClick fullscreen [" + this.hashCode() + "] ");
        if (state == STATE_AUTO_COMPLETE) return;
        if (screen == SCREEN_FULLSCREEN) {
            //quit fullscreen
            backPress();
        } else {
            Log.d(TAG, "toFullscreenActivity [" + this.hashCode() + "] ");
            gotoFullscreen();
        }
    }

    /**
     * 点击开始按钮
     */
    protected void clickStart() {
        Log.i(TAG, "onClick start [" + this.hashCode() + "] ");
        if (jzDataSource == null || jzDataSource.urlsMap.isEmpty() || jzDataSource.getCurrentUrl() == null) {
            Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
            return;
        }
        if (state == STATE_NORMAL) {
            if (!jzDataSource.getCurrentUrl().toString().startsWith("file") && !
                    jzDataSource.getCurrentUrl().toString().startsWith("/") &&
                    !JZUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {//这个可以放到std中
                showWifiDialog();
                return;
            }
            startVideo();
        } else if (state == STATE_PLAYING) {
            Log.d(TAG, "pauseVideo [" + this.hashCode() + "] ");
            mediaInterface.pause();
            onStatePause();
        } else if (state == STATE_PAUSE) {
            mediaInterface.start();
            onStatePlaying();
        } else if (state == STATE_AUTO_COMPLETE) {
            startVideo();
        }
    }

    /**
     * 触摸事件处理
     * @param v 被触摸的视图
     * @param event 触摸事件
     * @return 是否处理了触摸事件
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchActionDown(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touchActionMove(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    touchActionUp();
                    break;
            }
        }
        return false;
    }

    /**
     * 触摸抬起处理
     */
    protected void touchActionUp() {
        Log.i(TAG, "onTouch surfaceContainer actionUp [" + this.hashCode() + "] ");
        mTouchingProgressBar = false;
        dismissProgressDialog();
        dismissVolumeDialog();
        dismissBrightnessDialog();
        if (mChangePosition) {
            mediaInterface.seekTo(mSeekTimePosition);
            long duration = getDuration();
            int progress = (int) (mSeekTimePosition * 100 / (duration == 0 ? 1 : duration));
            progressBar.setProgress(progress);
        }
        if (mChangeVolume) {
            //change volume event
        }
        startProgressTimer();
    }

    /**
     * 触摸移动处理
     * @param x X坐标
     * @param y Y坐标
     */
    protected void touchActionMove(float x, float y) {
        Log.i(TAG, "onTouch surfaceContainer actionMove [" + this.hashCode() + "] ");
        float deltaX = x - mDownX;
        float deltaY = y - mDownY;
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(deltaY);
        if (screen == SCREEN_FULLSCREEN) {
            //拖动的是NavigationBar和状态栏
            if (mDownX > JZUtils.getScreenWidth(getContext()) || mDownY < JZUtils.getStatusBarHeight(getContext())) {
                return;
            }
            if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                    cancelProgressTimer();
                    if (absDeltaX >= THRESHOLD) {
                        // 全屏模式下的CURRENT_STATE_ERROR状态下,不响应进度拖动事件.
                        // 否则会因为mediaplayer的状态非法导致App Crash
                        if (state != STATE_ERROR) {
                            mChangePosition = true;
                            mGestureDownPosition = getCurrentPositionWhenPlaying();
                        }
                    } else {
                        //如果y轴滑动距离超过设置的处理范围，那么进行滑动事件处理
                        if (mDownX < mScreenHeight * 0.5f) {//左侧改变亮度
                            mChangeBrightness = true;
                            WindowManager.LayoutParams lp = JZUtils.getWindow(getContext()).getAttributes();
                            if (lp.screenBrightness < 0) {
                                try {
                                    mGestureDownBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                                    Log.i(TAG, "current system brightness: " + mGestureDownBrightness);
                                } catch (Settings.SettingNotFoundException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                mGestureDownBrightness = lp.screenBrightness * 255;
                                Log.i(TAG, "current activity brightness: " + mGestureDownBrightness);
                            }
                        } else {//右侧改变声音
                            mChangeVolume = true;
                            mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        }
                    }
                }
            }
        }
        if (mChangePosition) {
            long totalTimeDuration = getDuration();
            if (PROGRESS_DRAG_RATE <= 0) {
                Log.d(TAG, "error PROGRESS_DRAG_RATE value");
                PROGRESS_DRAG_RATE = 1f;
            }
            mSeekTimePosition = (int) (mGestureDownPosition + deltaX * totalTimeDuration / (mScreenWidth * PROGRESS_DRAG_RATE));
            if (mSeekTimePosition > totalTimeDuration)
                mSeekTimePosition = totalTimeDuration;
            String seekTime = JZUtils.stringForTime(mSeekTimePosition);
            String totalTime = JZUtils.stringForTime(totalTimeDuration);

            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
        }
        if (mChangeVolume) {
            deltaY = -deltaY;
            int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int deltaV = (int) (max * deltaY * 3 / mScreenHeight);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
            //dialog中显示百分比
            int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / mScreenHeight);
            showVolumeDialog(-deltaY, volumePercent);
        }

        if (mChangeBrightness) {
            deltaY = -deltaY;
            int deltaV = (int) (255 * deltaY * 3 / mScreenHeight);
            WindowManager.LayoutParams params = JZUtils.getWindow(getContext()).getAttributes();
            if (((mGestureDownBrightness + deltaV) / 255) >= 1) {//这和声音有区别，必须自己过滤一下负值
                params.screenBrightness = 1;
            } else if (((mGestureDownBrightness + deltaV) / 255) <= 0) {
                params.screenBrightness = 0.01f;
            } else {
                params.screenBrightness = (mGestureDownBrightness + deltaV) / 255;
            }
            JZUtils.getWindow(getContext()).setAttributes(params);
            //dialog中显示百分比
            int brightnessPercent = (int) (mGestureDownBrightness * 100 / 255 + deltaY * 3 * 100 / mScreenHeight);
            showBrightnessDialog(brightnessPercent);
//                        mDownY = y;
        }
    }

    /**
     * 触摸按下处理
     * @param x X坐标
     * @param y Y坐标
     */
    protected void touchActionDown(float x, float y) {
        Log.i(TAG, "onTouch surfaceContainer actionDown [" + this.hashCode() + "] ");
        mTouchingProgressBar = true;

        mDownX = x;
        mDownY = y;
        mChangeVolume = false;
        mChangePosition = false;
        mChangeBrightness = false;
    }

    /**
     * 普通状态处理
     */
    public void onStateNormal() {
        Log.i(TAG, "onStateNormal " + " [" + this.hashCode() + "] ");
        state = STATE_NORMAL;
        cancelProgressTimer();
        if (mediaInterface != null) mediaInterface.release();
    }

    /**
     * 准备中状态处理
     */
    public void onStatePreparing() {
        Log.i(TAG, "onStatePreparing " + " [" + this.hashCode() + "] ");
        state = STATE_PREPARING;
        resetProgressAndTime();
    }

    /**
     * 准备中播放状态处理
     */
    public void onStatePreparingPlaying() {
        Log.i(TAG, "onStatePreparingPlaying " + " [" + this.hashCode() + "] ");
        state = STATE_PREPARING_PLAYING;
    }

    /**
     * 准备中切换URL状态处理
     */
    public void onStatePreparingChangeUrl() {
        Log.i(TAG, "onStatePreparingChangeUrl " + " [" + this.hashCode() + "] ");
        state = STATE_PREPARING_CHANGE_URL;

        releaseAllVideos();
        startVideo();

//        mediaInterface.prepare();
    }

    /**
     * 切换URL
     * @param jzDataSource 新的视频数据源
     * @param seekToInAdvance 提前跳转时间
     */
    public void changeUrl(JZDataSource jzDataSource, long seekToInAdvance) {
        this.jzDataSource = jzDataSource;
        this.seekToInAdvance = seekToInAdvance;
        onStatePreparingChangeUrl();
    }

    /**
     * 准备完成处理
     */
    public void onPrepared() {
        Log.i(TAG, "onPrepared " + " [" + this.hashCode() + "] ");
        state = STATE_PREPARED;
        if (!preloading) {
            mediaInterface.start();//这里原来是非县城
            preloading = false;
        }
        if (jzDataSource.getCurrentUrl().toString().toLowerCase().contains("mp3") ||
                jzDataSource.getCurrentUrl().toString().toLowerCase().contains("wma") ||
                jzDataSource.getCurrentUrl().toString().toLowerCase().contains("aac") ||
                jzDataSource.getCurrentUrl().toString().toLowerCase().contains("m4a") ||
                jzDataSource.getCurrentUrl().toString().toLowerCase().contains("wav")) {
            onStatePlaying();
        }
    }

    /**
     * 开始预加载
     */
    public void startPreloading() {
        preloading = true;
        startVideo();
    }

    /**
     * 预加载完成后开始播放
     */
    public void startVideoAfterPreloading() {
        if (state == STATE_PREPARED) {
            mediaInterface.start();
        } else {
            preloading = false;
            startVideo();
        }
    }

    /**
     * 播放中状态处理
     */
    public void onStatePlaying() {
        Log.i(TAG, "onStatePlaying " + " [" + this.hashCode() + "] ");
        if (state == STATE_PREPARED) {//如果是准备完成视频后第一次播放，先判断是否需要跳转进度。
            Log.d(TAG, "onStatePlaying:STATE_PREPARED ");
            mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (seekToInAdvance != 0) {
                mediaInterface.seekTo(seekToInAdvance);
                seekToInAdvance = 0;
            } else {
                long position = JZUtils.getSavedProgress(getContext(), jzDataSource.getCurrentUrl());
                if (position != 0) {
                    mediaInterface.seekTo(position);//这里为什么区分开呢，第一次的播放和resume播放是不一样的。 这里怎么区分是一个问题。然后
                }
            }
        }
        state = STATE_PLAYING;
        startProgressTimer();
    }

    /**
     * 暂停状态处理
     */
    public void onStatePause() {
        Log.i(TAG, "onStatePause " + " [" + this.hashCode() + "] ");
        state = STATE_PAUSE;
        startProgressTimer();
    }

    /**
     * 错误状态处理
     */
    public void onStateError() {
        Log.i(TAG, "onStateError " + " [" + this.hashCode() + "] ");
        state = STATE_ERROR;
        cancelProgressTimer();
    }

    /**
     * 自动播放完成状态处理
     */
    public void onStateAutoComplete() {
        Log.i(TAG, "onStateAutoComplete " + " [" + this.hashCode() + "] ");
        state = STATE_AUTO_COMPLETE;
        cancelProgressTimer();
        progressBar.setProgress(100);
        currentTimeTextView.setText(totalTimeTextView.getText());
    }

    /**
     * 播放信息处理
     * @param what 信息类型
     * @param extra 信息码
     */
    public void onInfo(int what, int extra) {
        Log.d(TAG, "onInfo what - " + what + " extra - " + extra);
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
            if (state == Jzvd.STATE_PREPARED
                    || state == Jzvd.STATE_PREPARING_CHANGE_URL
                    || state == Jzvd.STATE_PREPARING_PLAYING) {
                onStatePlaying();//开始渲染图像，真正进入playing状态
            }
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            Log.d(TAG, "MEDIA_INFO_BUFFERING_START");
            backUpBufferState = state;
            setState(STATE_PREPARING_PLAYING);
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            Log.d(TAG, "MEDIA_INFO_BUFFERING_END");
            if (backUpBufferState != -1) {
                setState(backUpBufferState);
                backUpBufferState = -1;
            }
        }
    }

    /**
     * 播放错误处理
     * @param what 错误类型
     * @param extra 错误码
     */
    public void onError(int what, int extra) {
        Log.e(TAG, "onError " + what + " - " + extra + " [" + this.hashCode() + "] ");
        if (what != 38 && extra != -38 && what != -38 && extra != 38 && extra != -19) {
            onStateError();
            mediaInterface.release();
        }
    }

    /**
     * 播放完成处理
     */
    public void onCompletion() {
        Runtime.getRuntime().gc();
        Log.i(TAG, "onAutoCompletion " + " [" + this.hashCode() + "] ");
        cancelProgressTimer();
        dismissBrightnessDialog();
        dismissProgressDialog();
        dismissVolumeDialog();
        onStateAutoComplete();
        mediaInterface.release();
        JZUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        JZUtils.saveProgress(getContext(), jzDataSource.getCurrentUrl(), 0);

        if (screen == SCREEN_FULLSCREEN) {
            if (CONTAINER_LIST.size() == 0) {
                clearFloatScreen();//直接进入全屏
            } else {
                gotoNormalCompletion();
            }
        }
    }

    /**
     * 普通模式播放完成处理
     */
    public void gotoNormalCompletion() {
        gobakFullscreenTime = System.currentTimeMillis();//退出全屏
        ViewGroup vg = (ViewGroup) (JZUtils.scanForActivity(jzvdContext)).getWindow().getDecorView();
        vg.removeView(this);
        textureViewContainer.removeView(textureView);
        CONTAINER_LIST.getLast().removeViewAt(blockIndex);//remove block
        CONTAINER_LIST.getLast().addView(this, blockIndex, blockLayoutParams);
        CONTAINER_LIST.pop();
        setScreenNormal();
        JZUtils.showStatusBar(jzvdContext);
        JZUtils.setRequestedOrientation(jzvdContext, NORMAL_ORIENTATION);
        JZUtils.showSystemUI(jzvdContext);
    }

    /**
     * 重置播放器
     */
    public void reset() {
        Log.i(TAG, "reset " + " [" + this.hashCode() + "] ");
        if (state == STATE_PLAYING || state == STATE_PAUSE) {
            long position = getCurrentPositionWhenPlaying();
            JZUtils.saveProgress(getContext(), jzDataSource.getCurrentUrl(), position);
        }
        cancelProgressTimer();
        dismissBrightnessDialog();
        dismissProgressDialog();
        dismissVolumeDialog();
        onStateNormal();
        textureViewContainer.removeAllViews();

        AudioManager mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        JZUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mediaInterface != null) mediaInterface.release();
    }

    /**
     * 设置状态
     * @param state 状态
     */
    public void setState(int state) {
        switch (state) {
            case STATE_NORMAL:
                onStateNormal();
                break;
            case STATE_PREPARING:
                onStatePreparing();
                break;
            case STATE_PREPARING_PLAYING:
                onStatePreparingPlaying();
                break;
            case STATE_PREPARING_CHANGE_URL:
                onStatePreparingChangeUrl();
                break;
            case STATE_PLAYING:
                onStatePlaying();
                break;
            case STATE_PAUSE:
                onStatePause();
                break;
            case STATE_ERROR:
                onStateError();
                break;
            case STATE_AUTO_COMPLETE:
                onStateAutoComplete();
                break;
        }
    }

    /**
     * 设置屏幕模式
     * @param screen 屏幕模式
     */
    public void setScreen(int screen) {//特殊的个别的进入全屏的按钮在这里设置  只有setup的时候能用上
        switch (screen) {
            case SCREEN_NORMAL:
                setScreenNormal();
                break;
            case SCREEN_FULLSCREEN:
                setScreenFullscreen();
                break;
            case SCREEN_TINY:
                setScreenTiny();
                break;
        }
    }

    /**
     * 开始播放视频
     */
    public void startVideo() {
        Log.d(TAG, "startVideo [" + this.hashCode() + "] ");
        setCurrentJzvd(this);
        try {
            Constructor<JZMediaInterface> constructor = mediaInterfaceClass.getConstructor(Jzvd.class);
            this.mediaInterface = constructor.newInstance(this);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        addTextureView();
        JZUtils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        onStatePreparing();

    }

    /**
     * 测量视图尺寸
     * @param widthMeasureSpec 宽度测量规格
     * @param heightMeasureSpec 高度测量规格
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (screen == SCREEN_FULLSCREEN || screen == SCREEN_TINY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (widthRatio != 0 && heightRatio != 0) {
            int specWidth = MeasureSpec.getSize(widthMeasureSpec);
            int specHeight = (int) ((specWidth * (float) heightRatio) / widthRatio);
            setMeasuredDimension(specWidth, specHeight);

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, MeasureSpec.EXACTLY);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, MeasureSpec.EXACTLY);
            getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }

    /**
     * 添加TextureView
     */
    public void addTextureView() {
        Log.d(TAG, "addTextureView [" + this.hashCode() + "] ");
        if (textureView != null) textureViewContainer.removeView(textureView);
        textureView = new JZTextureView(getContext().getApplicationContext());
        textureView.setSurfaceTextureListener(mediaInterface);

        LayoutParams layoutParams =
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        textureViewContainer.addView(textureView, layoutParams);
    }

    /**
     * 清除浮动屏幕
     */
    public void clearFloatScreen() {
        JZUtils.showStatusBar(getContext());
        JZUtils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);
        JZUtils.showSystemUI(getContext());

        ViewGroup vg = (ViewGroup) (JZUtils.scanForActivity(getContext())).getWindow().getDecorView();
        vg.removeView(this);
        if (mediaInterface != null) mediaInterface.release();
        CURRENT_JZVD = null;
    }

    /**
     * 视频尺寸变化处理
     * @param width 宽度
     * @param height 高度
     */
    public void onVideoSizeChanged(int width, int height) {
        Log.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        if (textureView != null) {
            if (videoRotation != 0) {
                textureView.setRotation(videoRotation);
            }
            textureView.setVideoSize(width, height);
        }
    }

    /**
     * 开始进度更新定时器
     */
    public void startProgressTimer() {
        Log.i(TAG, "startProgressTimer: " + " [" + this.hashCode() + "] ");
        cancelProgressTimer();
        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300);
    }

    /**
     * 取消进度更新定时器
     */
    public void cancelProgressTimer() {
        if (UPDATE_PROGRESS_TIMER != null) {
            UPDATE_PROGRESS_TIMER.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    /**
     * 进度更新处理
     * @param progress 进度
     * @param position 位置
     * @param duration 时长
     */
    public void onProgress(int progress, long position, long duration) {
//        Log.d(TAG, "onProgress: progress=" + progress + " position=" + position + " duration=" + duration);
        mCurrentPosition = position;
        if (!mTouchingProgressBar) {
            if (seekToManulPosition != -1) {
                if (seekToManulPosition > progress) {
                    return;
                } else {
                    seekToManulPosition = -1;//这个关键帧有没有必要做
                }
            } else {
                progressBar.setProgress(progress);
            }
        }
        if (position != 0) currentTimeTextView.setText(JZUtils.stringForTime(position));
        totalTimeTextView.setText(JZUtils.stringForTime(duration));
    }

    /**
     * 设置缓冲进度
     * @param bufferProgress 缓冲进度
     */
    public void setBufferProgress(int bufferProgress) {
        progressBar.setSecondaryProgress(bufferProgress);
    }

    /**
     * 重置进度和时间
     */
    public void resetProgressAndTime() {
        mCurrentPosition = 0;
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(JZUtils.stringForTime(0));
        totalTimeTextView.setText(JZUtils.stringForTime(0));
    }

    /**
     * 获取播放时的当前位置
     * @return 当前位置
     */
    public long getCurrentPositionWhenPlaying() {
        long position = 0;
        if (state == STATE_PLAYING || state == STATE_PAUSE || state == STATE_PREPARING_PLAYING) {
            try {
                position = mediaInterface.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return position;
            }
        }
        return position;
    }

    /**
     * 获取视频时长
     * @return 视频时长
     */
    public long getDuration() {
        long duration = 0;
        try {
            duration = mediaInterface.getDuration();
        } catch (Exception e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    /**
     * 开始跟踪进度条
     * @param seekBar 进度条
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStartTrackingTouch [" + this.hashCode() + "] ");
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    /**
     * 停止跟踪进度条
     * @param seekBar 进度条
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStopTrackingTouch [" + this.hashCode() + "] ");
        startProgressTimer();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (state != STATE_PLAYING &&
                state != STATE_PAUSE) return;
        long time = seekBar.getProgress() * getDuration() / 100;
        seekToManulPosition = seekBar.getProgress();
        mediaInterface.seekTo(time);
        Log.i(TAG, "seekTo " + time + " [" + this.hashCode() + "] ");
    }

    /**
     * 进度条进度变化处理
     * @param seekBar 进度条
     * @param progress 进度
     * @param fromUser 是否来自用户操作
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            //设置这个progres对应的时间，给textview
            long duration = getDuration();
            currentTimeTextView.setText(JZUtils.stringForTime(progress * duration / 100));
        }
    }

    /**
     * 克隆播放器
     * @param vg 视图组
     */
    public void cloneAJzvd(ViewGroup vg) {
        try {
            Constructor<Jzvd> constructor = (Constructor<Jzvd>) Jzvd.this.getClass().getConstructor(Context.class);
            Jzvd jzvd = constructor.newInstance(getContext());
            jzvd.setId(getId());
            jzvd.setMinimumWidth(blockWidth);
            jzvd.setMinimumHeight(blockHeight);
            vg.addView(jzvd, blockIndex, blockLayoutParams);
            jzvd.setUp(jzDataSource.cloneMe(), SCREEN_NORMAL, mediaInterfaceClass);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * 进入全屏模式
     */
    public void gotoFullscreen() {
        gotoFullscreenTime = System.currentTimeMillis();
        ViewGroup vg = (ViewGroup) getParent();
        jzvdContext = vg.getContext();
        blockLayoutParams = getLayoutParams();
        blockIndex = vg.indexOfChild(this);
        blockWidth = getWidth();
        blockHeight = getHeight();

        vg.removeView(this);
        cloneAJzvd(vg);
        CONTAINER_LIST.add(vg);
        vg = (ViewGroup) (JZUtils.scanForActivity(jzvdContext)).getWindow().getDecorView();

        ViewGroup.LayoutParams fullLayout = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        vg.addView(this, fullLayout);

        setScreenFullscreen();
        JZUtils.hideStatusBar(jzvdContext);
        JZUtils.setRequestedOrientation(jzvdContext, FULLSCREEN_ORIENTATION);
        JZUtils.hideSystemUI(jzvdContext);//华为手机和有虚拟键的手机全屏时可隐藏虚拟键 issue:1326

    }

    /**
     * 进入普通模式
     */
    public void gotoNormalScreen() {//goback本质上是goto
        gobakFullscreenTime = System.currentTimeMillis();//退出全屏
        ViewGroup vg = (ViewGroup) (JZUtils.scanForActivity(jzvdContext)).getWindow().getDecorView();
        vg.removeView(this);
//        CONTAINER_LIST.getLast().removeAllViews();
        CONTAINER_LIST.getLast().removeViewAt(blockIndex);//remove block
        CONTAINER_LIST.getLast().addView(this, blockIndex, blockLayoutParams);
        CONTAINER_LIST.pop();

        setScreenNormal();//这块可以放到jzvd中
        JZUtils.showStatusBar(jzvdContext);
        JZUtils.setRequestedOrientation(jzvdContext, NORMAL_ORIENTATION);
        JZUtils.showSystemUI(jzvdContext);
    }

    /**
     * 设置普通屏幕模式
     */
    public void setScreenNormal() {//TODO 这块不对呀，还需要改进，设置flag之后要设置ui，不设置ui这么写没意义呀
        screen = SCREEN_NORMAL;
    }

    /**
     * 设置全屏模式
     */
    public void setScreenFullscreen() {
        screen = SCREEN_FULLSCREEN;
    }

    /**
     * 设置小窗口模式
     */
    public void setScreenTiny() {
        screen = SCREEN_TINY;
    }

    /**
     * 自动全屏
     * @param x X坐标
     */
    public void autoFullscreen(float x) {//TODO写道demo中
        if (CURRENT_JZVD != null
                && (state == STATE_PLAYING || state == STATE_PAUSE)
                && screen != SCREEN_FULLSCREEN
                && screen != SCREEN_TINY) {
            if (x > 0) {
                JZUtils.setRequestedOrientation(getContext(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                JZUtils.setRequestedOrientation(getContext(), ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
            gotoFullscreen();
        }
    }

    /**
     * 自动退出全屏
     */
    public void autoQuitFullscreen() {
        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000
//                && CURRENT_JZVD != null
                && state == STATE_PLAYING
                && screen == SCREEN_FULLSCREEN) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }

    /**
     * 跳转完成处理
     */
    public void onSeekComplete() {

    }

    /**
     * 显示WiFi提示对话框
     */
    public void showWifiDialog() {
    }

    /**
     * 显示进度对话框
     * @param deltaX X偏移
     * @param seekTime 跳转时间
     * @param seekTimePosition 跳转位置
     * @param totalTime 总时间
     * @param totalTimeDuration 总时长
     */
    public void showProgressDialog(float deltaX,
                                   String seekTime, long seekTimePosition,
                                   String totalTime, long totalTimeDuration) {
    }

    /**
     * 关闭进度对话框
     */
    public void dismissProgressDialog() {

    }

    /**
     * 显示音量对话框
     * @param deltaY Y偏移
     * @param volumePercent 音量百分比
     */
    public void showVolumeDialog(float deltaY, int volumePercent) {

    }

    /**
     * 关闭音量对话框
     */
    public void dismissVolumeDialog() {

    }

    /**
     * 显示亮度对话框
     * @param brightnessPercent 亮度百分比
     */
    public void showBrightnessDialog(int brightnessPercent) {

    }

    /**
     * 关闭亮度对话框
     */
    public void dismissBrightnessDialog() {

    }

    /**
     * 获取应用上下文
     * @return 应用上下文
     */
    public Context getApplicationContext() {
        Context context = getContext();
        if (context != null) {
            Context applicationContext = context.getApplicationContext();
            if (applicationContext != null) {
                return applicationContext;
            }
        }
        return context;
    }

    /**
     * 自动全屏监听器
     */
    public static class JZAutoFullscreenListener implements SensorEventListener {
        /**
         * 传感器数据变化处理
         * @param event 传感器事件
         */
        @Override
        public void onSensorChanged(SensorEvent event) {//可以得到传感器实时测量出来的变化值
            final float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            float z = event.values[SensorManager.DATA_Z];
            //过滤掉用力过猛会有一个反向的大数值
            if (x < -12 || x > 12) {
                if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000) {
                    if (Jzvd.CURRENT_JZVD != null) Jzvd.CURRENT_JZVD.autoFullscreen(x);
                    lastAutoFullscreenTime = System.currentTimeMillis();
                }
            }
        }

        /**
         * 传感器精度变化处理
         * @param sensor 传感器
         * @param accuracy 精度
         */
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    /**
     * 进度更新任务
     */
    public class ProgressTimerTask extends TimerTask {
        /**
         * 运行任务
         */
        @Override
        public void run() {
            if (state == STATE_PLAYING || state == STATE_PAUSE || state == STATE_PREPARING_PLAYING) {
//                Log.v(TAG, "onProgressUpdate " + "[" + this.hashCode() + "] ");
                post(() -> {
                    long position = getCurrentPositionWhenPlaying();
                    long duration = getDuration();
                    int progress = (int) (position * 100 / (duration == 0 ? 1 : duration));
                    onProgress(progress, position, duration);
                });
            }
        }
    }

}
