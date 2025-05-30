package com.godox.common.kdtablayout.widget.tab

import android.annotation.SuppressLint
import android.content.Context
import com.godox.common.kdtablayout.HsvEvaluator

/**
 * Created By：XuQK
 * Created Date：2/21/20 4:24 PM
 * Description：
 */
@SuppressLint("ViewConstructor")
open class KDColorMorphingTextTab(context: Context, text: String) :
    KDSizeMorphingTextTab(context, text) {

    private val hsvEvaluator = HsvEvaluator()

    init {
        this.text = text
    }

    override fun onScrolling(selectedFraction: Float, selectedInLeft: Boolean) {
        super.onScrolling(selectedFraction, selectedInLeft)
        textColor = hsvEvaluator.evaluate(selectedFraction, normalTextColor, selectedTextColor)
    }
}
