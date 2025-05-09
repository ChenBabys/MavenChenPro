package com.godox.common.util

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.lang.Exception
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 队列式数据发送工具类
 * 改自SendQueueUtils，专门用于nfc队列处理数据的，
 * 目前改动的有，
 * 1.改为手动采样发送数据去入网,取消了自动模式，原本是间隔320毫秒就发送的，现在取消了的自动模式，全部改为手动模式触发再拿（当拿不到数据时才会主动一次），增加第一次会触发一条
 * 2.单次发送数据可选为多条数据发送
 * 3.增加了间隔采样逻辑，适用与进度条发送命令使用，非进度条切勿开启间隔采样
 * 复制一份出来了，备用的
 */
class DeviceQueueUtils<T> : Handler.Callback {
    // 默认延时时间320ms
    private var DEF_DELAY_TIME: Long =320L

    // 默认采样间隔为不间隔（0）
    private var samplingTime: Long = 0L

    // 添加数据的采样记录时间
    private var addDataSamplingTime = 0L
    private val hanSendDelayedMessage = 0
    private val hanQueueListenerDelayedMessage = 1
    private val mHandler = Handler(Looper.getMainLooper(), this)
    private var multiDataMode = false
    private var multiCount: Int = 5//多数据模式时填充，单次拿几条数据，默认是五条

    //队列数据列表（用并发集合类列表对象）, 需要频繁移除第一个元素，思考使用：使用 Deque（如 ArrayDeque）
    private var sendDataList = CopyOnWriteArrayList<T>()

    //单数据模式时选sendDataNow，多数据模式时选sendDataListNow
    private var sendDataNow: T? = null
    private var sendDataListNow: MutableList<T>? = null

    //是否是第一次加载本队列，第一次加载会自动发一次，之后的都要手动，如果出现手动却无数据，则由isHandNoData处理自动发一次
    private var isFirstLoad = true

    //是否正在处理中
    private var isStart = false

    //是否是自动处理模式，默认不是，因为这个队列涉及的初衷就是给nfc用的
    private var isAutoMode = false

    //手动触发拿数据时拿不到了，就标记为true
    private var isHandNoData = false

    //第一个参数表示，是否是多数据模式，如果是单数据模式时，只会返回一条数据到list中回去，多数据模式时会多条
    private var sendQueueListener: ((multiDataMode: Boolean, MutableList<T>) -> Unit)? = null

    /**
     * 开启数据发送队列
     */
    fun start(sendQueueListener: (multiDataMode: Boolean, MutableList<T>) -> Unit) {
        this.sendQueueListener = sendQueueListener
    }

    /**
     * 设置单次采样时间
     * 适用于进度条快速队列，普通队列千万别开启间隔采样，会丢失数据
     */
    fun setSamplingTime(samplingTime: Long) {
        this.samplingTime = samplingTime
    }

    /**
     * 设置单次处理数据的间隔时间
     */
    fun setIntervalSendTime(delayTimeMill: Long) {
        this.DEF_DELAY_TIME = delayTimeMill
    }

    /**
     * 设置多数据发送模式，不设置则默认是单数据
     * multiDataMode:true 多数据模式,false单数据模式
     * multiCount:多数据模式时传每次拿多少条数据
     */
    fun setMultiDataMode(multiDataMode: Boolean, multiCount: Int? = null): DeviceQueueUtils<T> {
        this.multiDataMode = multiDataMode
        if (multiCount != null) {
            this.multiCount = multiCount
        }
        return this
    }

    /**
     * 设置自动模式，默认是非自动的，默认是除了首次之外都是handSendDataNext去手动拿下次的数据处理。手动模式合适哪些处理回去后隔一段时间再来拿的逻辑
     * 自动模式适合完毕就拿下一次
     */
    fun setAutoMode(isAutoMode: Boolean): DeviceQueueUtils<T> {
        this.isAutoMode = isAutoMode
        return this
    }

    /**
     * 添加要发送的数据，纯纯手动模式(仅手动模式时的逻辑)，只有手动拿不到数据才会触发再自动一次，而之后继续由手动触发
     * 手动模式时，和addDataAndFirstLoadAutoSend方法尽量二选一（但其实用也无所谓，只要你有两个数据入口去填充，随便你）
     * addFirstIndex:添加到第一个去,下一个就执行,只使用于手动模式，不适合自动模式，
     * ！！！自动模式请使用addDataAndFirstLoadAutoSend
     * 如果需要连续的命令，那么首次构建时请用addDataAndFirstLoadAutoSend，否则无效
     *  frameDelayNewTimeMs:当前帧延时时间的自定义，默认是使用通用时间,即不传
     * 参数循序不要乱变换
     */
    fun addDataSamplingNotFirstLoad(data: T, addFirstIndex: Boolean = false, frameDelayNewTimeMs: Long? = null) {
        // frameDelayNewTimeMs不空的话就不拦截了的
        if (!checkSamplingIsOk() && frameDelayNewTimeMs == null) {
            return
        }
        addDataNotFirstLoad(data, addFirstIndex, frameDelayNewTimeMs)
    }

    /**
     * 添加要发送的数据，纯纯手动模式(仅手动模式时的逻辑)，只有手动拿不到数据才会触发再自动一次，而之后继续由手动触发
     * 手动模式时，和addDataAndFirstLoadAutoSend方法尽量二选一（但其实用也无所谓，只要你有两个数据入口去填充，随便你）
     * addFirstIndex:添加到第一个去,下一个就执行,只使用于手动模式，不适合自动模式，
     * ！！！自动模式请使用addDataAndFirstLoadAutoSend
     * 如果需要连续的命令，那么首次构建时请用addDataAndFirstLoadAutoSend，否则无效
     *  frameDelayNewTimeMs:当前帧延时时间的自定义，默认是使用通用时间,即不传
     * 参数循序不要乱变换
     */
    fun addDataNotFirstLoad(data: T, addFirstIndex: Boolean = false, frameDelayNewTimeMs: Long? = null) {
        if (addFirstIndex) {
            sendDataList.add(0, data)
        } else {
            sendDataList.add(data)
        }
        //避免无数据后走不下去，有一次触发了拿不到数据后，就自动要一次数据给过去
        if (isHandNoData && sendDataList.size > 0) {
            handSendDataNext(frameDelayNewTimeMs)
            isHandNoData = false//重置
        }
        //即使走这个方法，走过了一次，再也不允许触发所谓的首次自动，即它有可能在别处再调用addDataAndFirstLoadAutoSend去加数据
        isFirstLoad = false
    }

    /**
     * 添加数据并且在第一次加载本队列时会自动发一条数据出去，之后全是手动(仅手动模式时的逻辑)，直到手动拿不到数据才会触发再自动一次，而之后继续由手动触发
     * 和addData方法尽量二选一，（但其实用也无所谓，只要你有两个数据入口去填充，随便你））
     * addFirstIndex:是否插入到第一个。这样下一位就会立刻执行它了
     * frameDelayNewTimeMs:当前帧延时时间的自定义，默认是使用通用时间,即不传
     * 参数循序不要乱变换
     * 采样的方式添加
     */
    fun addDataSamplingAndFirstLoadAutoSend(data: T, addFirstIndex: Boolean = false, frameDelayNewTimeMs: Long? = null) {
        // frameDelayNewTimeMs不空的话就不拦截了的
        if (!checkSamplingIsOk() && frameDelayNewTimeMs == null) {
            return
        }
        addDataAndFirstLoadAutoSend(data, addFirstIndex, frameDelayNewTimeMs)
    }

    /**
     * 添加数据并且在第一次加载本队列时会自动发一条数据出去，之后全是手动(仅手动模式时的逻辑)，直到手动拿不到数据才会触发再自动一次，而之后继续由手动触发
     * 和addData方法尽量二选一，（但其实用也无所谓，只要你有两个数据入口去填充，随便你））
     * addFirstIndex:是否插入到第一个。这样下一位就会立刻执行它了
     * frameDelayNewTimeMs:当前帧延时时间的自定义，默认是使用通用时间,即不传
     * 参数循序不要乱变换
     */
    fun addDataAndFirstLoadAutoSend(data: T, addFirstIndex: Boolean = false, frameDelayNewTimeMs: Long? = null) {
        if (addFirstIndex) {
            sendDataList.add(0, data)
        } else {
            sendDataList.add(data)
        }
        //避免无数据后走不下去，有一次触发了拿不到数据后，就自动要一次数据给过去
        if (isHandNoData && sendDataList.size > 0) {
            handSendDataNext(frameDelayNewTimeMs)
            isHandNoData = false//重置
        }
        //第一次加载本队列时走一次自动触发
        if (isFirstLoad) {
            handSendDataNext(frameDelayNewTimeMs)
            isFirstLoad = false//之后都设为不是第一次触发
        }
    }

    /**
     * 检查采样是否可行，可行就执行采样
     * 默认不符合条件则不采样
     */
    private fun checkSamplingIsOk(): Boolean {
        var isSamplingOk = false
        val currentTimeMs = System.currentTimeMillis()
        // 若是0则代表不间隔采样，则可以直接采样
        when {
            samplingTime == 0L -> {
                isSamplingOk = true
            }

            currentTimeMs - addDataSamplingTime >= samplingTime -> {
                isSamplingOk = true
                addDataSamplingTime = currentTimeMs
            }
        }
        return isSamplingOk
    }


    /**
     * 手动发送下一个数据过去入网，一定要在你上次拿的数据处理完了，再去拿新的数据。！！！！
     * frameDelayNewTimeMs:默认都是需要延时的，特殊处理则自定义延时时间
     */
    fun handSendDataNext(frameDelayNewTimeMs: Long? = null) {
        if (!isStart) {
            isStart = true
            sendDataNext(frameDelayNewTimeMs)
        } else Log.d("测试用的", "isStart = true，走不下去")
    }

    /**
     * 删除某个项
     * 返回值时删除是否成功的布尔值
     */
    fun deleteDataItem(item: T?): Boolean {
        return sendDataList.remove(item)
    }

    /**
     * 获取当前的列表出去做改动，毕竟直接进来删除不现实，有泛型，强转不合适
     */
    fun getQueueList(): MutableList<T> {
        return sendDataList
    }//返回的数据要以MutableList的方式反，不要用CopyOnWriteArrayList，否则又是复制一份回去的

    /**
     * 默认都是获取默认延时的
     */
    private fun sendDataNext(frameDelayNewTimeMs: Long? = null) {
        mHandler.removeMessages(hanSendDelayedMessage)
        //默认都延时一点，避免并发发生,除非需要不延时时，则特殊处理自定义时间
        mHandler.sendEmptyMessageDelayed(hanSendDelayedMessage, frameDelayNewTimeMs ?: DEF_DELAY_TIME)
    }

    private fun stopSendData() {
        mHandler.removeMessages(hanSendDelayedMessage)
        isStart = false
    }

    /**
     * 情况队列中未处理的数据
     */
    fun clearQueueData() {
        sendDataList.clear()
    }

    /**
     * 结束发送队列并回收资源
     */
    fun destroy() {
        sendQueueListener = null
        sendDataList.clear()
        stopSendData()
        mHandler.removeMessages(hanQueueListenerDelayedMessage)
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            hanSendDelayedMessage -> {
                try {
                    if (isStart) {
                        if (sendDataList.isNotEmpty()) {
                            when (multiDataMode) {
                                false -> {
                                    // 尽量别使用removeFirst，升级gradle后会有问题,改为removeAt
                                    sendDataNow = sendDataList[0]
                                    sendDataList.removeAt(0)
                                    mHandler.sendEmptyMessage(hanQueueListenerDelayedMessage)
                                }

                                true -> {
                                    if (sendDataListNow == null) {
                                        sendDataListNow = mutableListOf()
                                    } else {
                                        sendDataListNow!!.clear()
                                    }
                                    for (index in 0 until multiCount) {
                                        if (sendDataList.isEmpty()) {
                                            break
                                        }//如果已经空了，则直接退出循环

                                        // 尽量别使用removeFirst，升级gradle后会有问题,2025/1/22改为removeAt
                                        val takeFirst = sendDataList[0]
                                        sendDataListNow!!.add(takeFirst)
                                        sendDataList.removeAt(0)
                                    }
                                    mHandler.sendEmptyMessage(hanQueueListenerDelayedMessage)
                                }
                            }
                        } else {
                            stopSendData()
                            isHandNoData = true
                            Log.d("测试用的", "没数据了，去stop了")
                        }
                    }
                } catch (e: Exception) {
                    isStart = false//异常也算处理完了，复原
                    e.printStackTrace()
                    Log.d("测试用的", "发生异常了，${e.message}")
                }
            }

            hanQueueListenerDelayedMessage -> {
                when (multiDataMode) {
                    false -> {
                        if (sendDataNow != null && sendQueueListener != null) {
                            val singleItemList = mutableListOf<T>()
                            singleItemList.add(sendDataNow!!)
                            sendQueueListener!!(false, singleItemList)//单数据时列表给一条数据回去
                        }
                    }

                    true -> {
                        if (sendDataListNow != null && sendQueueListener != null) {
                            sendQueueListener!!(true, sendDataListNow!!)//多数据时把列表全部拿回去
                        }
                    }
                }
                isStart = false//处理完了就复原
                //如果是自动模式则去自动处理下一条
                if (isAutoMode) {
                    handSendDataNext()
                }
            }
        }
        return false
    }
}
