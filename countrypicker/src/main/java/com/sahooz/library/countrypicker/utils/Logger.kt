package com.sahooz.library.countrypicker.utils

import android.util.Log

object Logger {
    // 是否开启日志
    var isEnabled = false

    fun log(message: String) {
        if (isEnabled) {
            Log.d("countryPicker", message)
        }
    }
}
