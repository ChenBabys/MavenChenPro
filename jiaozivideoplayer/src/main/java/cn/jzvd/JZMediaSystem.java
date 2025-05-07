package cn.jzvd;

import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 系统媒体播放器实现类
 * 使用Android系统MediaPlayer实现视频播放功能
 * 实现了JZMediaInterface接口，处理视频播放、暂停、跳转等操作
 * 实现了MediaPlayer的各种回调接口，处理播放状态变化
 */
public class JZMediaSystem extends JZMediaInterface implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnVideoSizeChangedListener {

    /** 系统MediaPlayer实例 */
    public MediaPlayer mediaPlayer;

    /**
     * 构造函数
     * @param jzvd 播放器实例
     */
    public JZMediaSystem(Jzvd jzvd) {
        super(jzvd);
    }

    /**
     * 准备播放器
     * 创建新的MediaPlayer实例并设置各种监听器
     */
    @Override
    public void prepare() {
        release();
        mMediaHandlerThread = new HandlerThread("JZVD");
        mMediaHandlerThread.start();
        mMediaHandler = new Handler(mMediaHandlerThread.getLooper());//主线程还是非主线程，就在这里
        handler = new Handler();

        mMediaHandler.post(() -> {
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setLooping(jzvd.jzDataSource.looping);
                mediaPlayer.setOnPreparedListener(JZMediaSystem.this);
                mediaPlayer.setOnCompletionListener(JZMediaSystem.this);
                mediaPlayer.setOnBufferingUpdateListener(JZMediaSystem.this);
                mediaPlayer.setScreenOnWhilePlaying(true);
                mediaPlayer.setOnSeekCompleteListener(JZMediaSystem.this);
                mediaPlayer.setOnErrorListener(JZMediaSystem.this);
                mediaPlayer.setOnInfoListener(JZMediaSystem.this);
                mediaPlayer.setOnVideoSizeChangedListener(JZMediaSystem.this);
                Class<MediaPlayer> clazz = MediaPlayer.class;
                //如果不用反射，没有url和header参数的setDataSource函数
                Method method = clazz.getDeclaredMethod("setDataSource", String.class, Map.class);
                method.invoke(mediaPlayer, jzvd.jzDataSource.getCurrentUrl().toString(), jzvd.jzDataSource.headerMap);
                mediaPlayer.prepareAsync();
                mediaPlayer.setSurface(new Surface(SAVED_SURFACE));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 开始播放视频
     */
    @Override
    public void start() {
        mMediaHandler.post(() -> mediaPlayer.start());
    }

    /**
     * 暂停播放视频
     */
    @Override
    public void pause() {
        mMediaHandler.post(() -> mediaPlayer.pause());
    }

    /**
     * 检查是否正在播放
     * @return 如果正在播放返回true，否则返回false
     */
    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    /**
     * 跳转到指定时间点
     * @param time 目标时间点（毫秒）
     */
    @Override
    public void seekTo(long time) {
        mMediaHandler.post(() -> {
            try {
                mediaPlayer.seekTo((int) time);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 释放播放器资源
     * 释放MediaPlayer实例并退出处理线程
     */
    @Override
    public void release() {//not perfect change you later
        if (mMediaHandler != null && mMediaHandlerThread != null && mediaPlayer != null) {//不知道有没有妖孽
            HandlerThread tmpHandlerThread = mMediaHandlerThread;
            MediaPlayer tmpMediaPlayer = mediaPlayer;
            JZMediaInterface.SAVED_SURFACE = null;

            mMediaHandler.post(() -> {
                tmpMediaPlayer.setSurface(null);
                tmpMediaPlayer.release();
                tmpHandlerThread.quit();
            });
            mediaPlayer = null;
        }
    }

    /**
     * 获取当前播放位置
     * @return 当前播放位置（毫秒）
     */
    @Override
    public long getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    /**
     * 获取视频总时长
     * @return 视频总时长（毫秒）
     */
    @Override
    public long getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        } else {
            return 0;
        }
    }

    /**
     * 设置音量
     * @param leftVolume 左声道音量
     * @param rightVolume 右声道音量
     */
    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        if (mMediaHandler == null) return;
        mMediaHandler.post(() -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(leftVolume, rightVolume);
        });
    }

    /**
     * 设置播放速度
     * @param speed 播放速度
     */
    @Override
    public void setSpeed(float speed) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PlaybackParams pp = mediaPlayer.getPlaybackParams();
            pp.setSpeed(speed);
            mediaPlayer.setPlaybackParams(pp);
        }
    }

    /**
     * 播放器准备完成回调
     * @param mediaPlayer MediaPlayer实例
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        handler.post(() -> jzvd.onPrepared());//如果是mp3音频，走这里
    }

    /**
     * 播放完成回调
     * @param mediaPlayer MediaPlayer实例
     */
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        handler.post(() -> jzvd.onCompletion());
    }

    /**
     * 缓冲进度更新回调
     * @param mediaPlayer MediaPlayer实例
     * @param percent 缓冲进度百分比
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, final int percent) {
        handler.post(() -> jzvd.setBufferProgress(percent));
    }

    /**
     * 跳转完成回调
     * @param mediaPlayer MediaPlayer实例
     */
    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        handler.post(() -> jzvd.onSeekComplete());
    }

    /**
     * 播放错误回调
     * @param mediaPlayer MediaPlayer实例
     * @param what 错误类型
     * @param extra 错误码
     * @return 是否处理了错误
     */
    @Override
    public boolean onError(MediaPlayer mediaPlayer, final int what, final int extra) {
        handler.post(() -> jzvd.onError(what, extra));
        return true;
    }

    /**
     * 播放信息回调
     * @param mediaPlayer MediaPlayer实例
     * @param what 信息类型
     * @param extra 信息码
     * @return 是否处理了信息
     */
    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, final int what, final int extra) {
        handler.post(() -> jzvd.onInfo(what, extra));
        return false;
    }

    /**
     * 视频尺寸变化回调
     * @param mediaPlayer MediaPlayer实例
     * @param width 视频宽度
     * @param height 视频高度
     */
    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        handler.post(() -> jzvd.onVideoSizeChanged(width, height));
    }

    /**
     * 设置视频渲染Surface
     * @param surface 视频渲染Surface
     */
    @Override
    public void setSurface(Surface surface) {
        mediaPlayer.setSurface(surface);
    }

    /**
     * SurfaceTexture可用回调
     * @param surface SurfaceTexture实例
     * @param width 宽度
     * @param height 高度
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (SAVED_SURFACE == null) {
            SAVED_SURFACE = surface;
            prepare();
        } else {
            jzvd.textureView.setSurfaceTexture(SAVED_SURFACE);
        }
    }

    /**
     * SurfaceTexture尺寸变化回调
     * @param surface SurfaceTexture实例
     * @param width 宽度
     * @param height 高度
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    /**
     * SurfaceTexture销毁回调
     * @param surface SurfaceTexture实例
     * @return 是否处理了销毁事件
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    /**
     * SurfaceTexture更新回调
     * @param surface SurfaceTexture实例
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
