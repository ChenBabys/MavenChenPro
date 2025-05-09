package com.godox.common.util

import android.app.Activity
import java.util.Stack

/**
 * Author: ChenBabys
 * Date: 2024/12/16
 * Description: 活动管理器，暂时不启用了，用AndroidCodeUtil库里面的ActivityUtils替代挺好
 */
object ActivityManager {
    // 用于存放 Activity 实例的栈
    private val activityStack: Stack<Activity> = Stack()

    /** 添加 Activity 到栈中 */
    fun addActivity(activity: Activity) {
        activityStack.push(activity)
    }

    /** 移除指定的 Activity 出栈 */
    fun removeActivity(activity: Activity) {
        activityStack.remove(activity)
    }

    /** 获取当前栈顶的 Activity */
    fun currentActivity(): Activity? {
        return if (activityStack.isNotEmpty()) activityStack.peek() else null
    }

    /** 关闭指定的 Activity */
    fun finishActivity(activity: Activity) {
        if (!activity.isFinishing) {
            activity.finish()
            removeActivity(activity)
        }
    }

    /** 关闭所有 Activity */
    fun finishAllActivities() {
        for (activity in activityStack) {
            if (!activity.isFinishing) {
                activity.finish()
            }
        }
        activityStack.clear()
    }

    /** 关闭除指定 Activity 之外的所有 Activity */
    fun finishAllActivitiesExcept(activityToKeep: Activity) {
        for (activity in activityStack) {
            if (activity != activityToKeep && !activity.isFinishing) {
                activity.finish()
            }
        }
        activityStack.clear()
        addActivity(activityToKeep)
    }
}
