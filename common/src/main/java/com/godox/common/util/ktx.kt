package com.godox.common.util

import android.app.Service
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue


/**
 * Author: ChenBabys
 * Date: 2025/4/23
 * Description:
 */


fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

// dp转px扩展函数
val Float.dp: Float get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this,
    Resources.getSystem().displayMetrics
)


/**
 * 震动
 */
fun Context.vibrate() {
    if (Build.VERSION.SDK_INT >= 31) {
        // android 12 及以上使用新的 VibratorManager，创建 EFFECT_TICK 轻微震动（需要线性震动马达硬件支持）
        val manager: VibratorManager =
            getSystemService(VibratorManager::class.java)
        manager.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    } else if (Build.VERSION.SDK_INT >= 29) {
        // android 10 及以上使用原 Vibrator，创建 EFFECT_TICK 轻微震动（需要线性震动马达硬件支持）
        val vib = getSystemService(Vibrator::class.java) as Vibrator
        vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    } else {
        // 10 以下的系统，没有系统 API 驱动线性震动马达，只能创建普通震动
        val vib = getSystemService(Service.VIBRATOR_SERVICE) as Vibrator //震动70毫秒
        vib.vibrate(70)
    }
}
