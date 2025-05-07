package cn.jzvd;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;

/**
 * 视频播放器接口类
 * 定义了视频播放器的基本操作接口，包括播放、暂停、跳转等功能
 * 实现了TextureView.SurfaceTextureListener接口，用于处理视频渲染
 */
public abstract class JZMediaInterface implements TextureView.SurfaceTextureListener {

    /** 保存的SurfaceTexture，用于视频渲染 */
    public static SurfaceTexture SAVED_SURFACE;
    /** 媒体处理线程 */
    public HandlerThread mMediaHandlerThread;
    /** 媒体处理Handler */
    public Handler mMediaHandler;
    /** 主线程Handler */
    public Handler handler;
    /** 播放器实例 */
    public Jzvd jzvd;

    /**
     * 构造函数
     * @param jzvd 播放器实例
     */
    public JZMediaInterface(Jzvd jzvd) {
        this.jzvd = jzvd;
    }

    /**
     * 开始播放视频
     */
    public abstract void start();

    /**
     * 准备播放视频
     */
    public abstract void prepare();

    /**
     * 暂停播放视频
     */
    public abstract void pause();

    /**
     * 检查是否正在播放
     * @return 如果正在播放返回true，否则返回false
     */
    public abstract boolean isPlaying();

    /**
     * 跳转到指定时间点
     * @param time 目标时间点（毫秒）
     */
    public abstract void seekTo(long time);

    /**
     * 释放播放器资源
     */
    public abstract void release();

    /**
     * 获取当前播放位置
     * @return 当前播放位置（毫秒）
     */
    public abstract long getCurrentPosition();

    /**
     * 获取视频总时长
     * @return 视频总时长（毫秒）
     */
    public abstract long getDuration();

    /**
     * 设置音量
     * @param leftVolume 左声道音量
     * @param rightVolume 右声道音量
     */
    public abstract void setVolume(float leftVolume, float rightVolume);

    /**
     * 设置播放速度
     * @param speed 播放速度
     */
    public abstract void setSpeed(float speed);

    /**
     * 设置视频渲染Surface
     * @param surface 视频渲染Surface
     */
    public abstract void setSurface(Surface surface);
}
