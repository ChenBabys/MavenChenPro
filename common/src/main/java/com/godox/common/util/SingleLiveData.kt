package com.godox.common.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 滤掉默认MutableLiveData的粘性事件【即在fragment中二次注册这个activity中的viewModel初始化的LiveData是，会默认发送上次最后一条数据】的问题
 * 即首次注册不会发送上次的粘性事件效果和【QLiveData】一致了
 */
class SingleLiveData<T> : MutableLiveData<T>() {
    private val pending = AtomicBoolean(false)

    /**
     * 默认采用非粘性
     */
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner) { t ->
            // pending 比较后重置
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        }
    }

    /**
     * 和原生的MutableLiveData默认observe默认粘性效果一致，
     * 需要实现这种情况，可以直接用原生的MutableLiveData然后observe监听也可，随便
     */
    fun observeSticky(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner) { t ->
            pending.set(false) // 顺带重置pending
            observer.onChanged(t)
        }
    }

    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }

    override fun postValue(value: T) {
        pending.set(true)
        super.postValue(value)
    }

}
