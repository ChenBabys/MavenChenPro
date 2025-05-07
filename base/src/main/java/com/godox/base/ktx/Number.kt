package com.godox.base.ktx

import com.blankj.utilcode.util.ConvertUtils

fun Number.dp2px(): Int = ConvertUtils.dp2px(toFloat())

fun Number.sp2px(): Int = ConvertUtils.sp2px(toFloat())

fun Number.px2dp(): Int = ConvertUtils.px2dp(toFloat())

fun Number.px2sp(): Int = ConvertUtils.px2sp(toFloat())
