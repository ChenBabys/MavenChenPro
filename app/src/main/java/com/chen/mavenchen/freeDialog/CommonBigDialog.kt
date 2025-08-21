package com.chen.mavenchen.freeDialog

import android.content.Context
import android.content.DialogInterface
import android.graphics.Rect
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentManager
import com.chen.freedialog.BaseFreeDialogFragment
import com.chen.freedialog.config.SwipeDirection
import com.chen.mavenchen.R
import com.chen.mavenchen.databinding.DialogCommonBigInputValueBinding
import com.chen.mavenchen.databinding.DialogCommonInputValueBinding


/**
 * Author: ChenBabys
 * Date: 2025/3/6
 * Description: 4.0.0版本开始，公用的名称、值输入框.
 * fragmentManager必须是外部传入的，内部的是没有依赖于任何父容器的，用不了
 */
class CommonBigDialog(private val fragmentManager: FragmentManager) : BaseFreeDialogFragment<DialogCommonBigInputValueBinding>() {
    private var mOnActionListener: OnActionListener? = null
    private val config = Config()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): DialogCommonBigInputValueBinding {
        return DialogCommonBigInputValueBinding.inflate(inflater, container, false)
    }

    override fun initView(binding: DialogCommonBigInputValueBinding) {
        // 固定宽度
        dialogConfig.apply {
            fixWidth = ViewGroup.LayoutParams.MATCH_PARENT
            isTouchOutSideCancelable = true
            softInputAdjustNothing = true

            // 关于这个drag功能，是有效的，但这个MavenChenPro项目有问题，实际用到其他项目是可行的，不用理
            dragViewId = R.id.tv_cancel

            // 手势滑动关闭
            swipeToDismissEnabled = true
            swipeDirection = SwipeDirection.SWIPE_DIRECTION_DOWN
            swipeThreshold = 0.25f
            swipeVelocityThreshold = 800f
            swipeDismissAnimDuration = 300
        }


        //默认隐藏描述，除非输入描述
        binding.tvCancel.setOnClickListener {
            this.dismiss()
            mOnActionListener?.onCancel()
        }
        binding.tvConfirm.setOnClickListener {
            this.dismiss()
            mOnActionListener?.onConfirm(binding.etInput.text.toString().trim())
        }

        if (config.hintTitle.isNotBlank()) {
            binding.tvTitle.text = config.hintTitle
        }

        if (config.hintInput.isNotBlank()) {
            binding.etInput.hint = config.hintInput
        }

        if (config.describe.isNotBlank()) {
            binding.tvDescribe.text = config.describe
        }
        if (config.defaultInputString.isNotBlank()) {
            binding.etInput.setText(config.defaultInputString)
            binding.etInput.setSelection(binding.etInput.text.toString().length)
        }
        if (config.selectAllOnFocus) {
            binding.etInput.setSelectAllOnFocus(true)
        }
        if (config.maxInputLength >= 0) {
            val fArray = arrayOfNulls<InputFilter>(1)
            fArray[0] = InputFilter.LengthFilter(config.maxInputLength)
            binding.etInput.filters = fArray
        }
        if (config.cancelText.isNotBlank()) {
            binding.tvCancel.text = config.cancelText
        }
        if (config.cancelText.isNotBlank()) {
            binding.tvConfirm.text = config.cancelText
        }
        if (config.confirmTextColor >= 0) {
            binding.tvConfirm.setTextColor(config.confirmTextColor)
        }

        if (config.focusAndShowInput) {
            // binding.etInput.onFocusChangeListener = object : View.OnFocusChangeListener {
            //     override fun onFocusChange(v: View?, hasFocus: Boolean) {
            //         if (hasFocus) {
            //             val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //             imm.showSoftInput(binding.etInput, InputMethodManager.SHOW_IMPLICIT)
            //         } else {
            //             val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //             if (imm.isActive) {
            //                 imm.hideSoftInputFromWindow(binding.etInput.windowToken, 0)
            //             }
            //         }
            //     }
            // }


            //KeyboardUtils.showSoftInput(binding.etInput, InputMethodManager.SHOW_IMPLICIT)

            binding.etInput.isFocusable = true
            binding.etInput.requestFocus()
            //binding.etInput.requestFocusFromTouch()
            binding.etInput.postDelayed(
                Runnable {
                    //展示输入框
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(binding.etInput, 0)
                },
                100,
            )
        }

    }

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val dialog = FoldInputTouchDialog(requireContext(), theme)
//        dialog.setCancelable(dialogConfig.isTouchOutSideCancelable)
//        dialog.setCanceledOnTouchOutside(dialogConfig.isTouchOutSideCancelable)
//        // 关键设置：让 Dialog 整体上移，避免键盘遮挡
//        dialog.getWindow()?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
//        return dialog
//    }

    override fun onStart() {
        super.onStart()
        val dialog = getDialog()
        if (dialog != null) {
            val window = dialog.window
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            // 监听键盘高度变化
            adjustDialogPosition(window!!)
        }
    }

    private val targetOffsetPx = 50 // 目标上移距离（键盘顶部上方50px）
    private fun adjustDialogPosition(window: Window) {
        // 1. 获取键盘高度
        val visibleFrame = Rect()
        window.decorView.getWindowVisibleDisplayFrame(visibleFrame)
        val screenHeight = window.decorView.getRootView().height
        // 2. 计算目标位置
        val params = window.attributes

        val yValue = params.y - 200

        // 3. 应用位置调整（带平滑动画）
        window.decorView.animate()
            .translationY(yValue.toFloat())
            .setDuration(100)
            .withEndAction {
                params.y = yValue
                window.setAttributes(params)
            }
            .start()
    }


    fun setTitle(hintTitle: String): CommonBigDialog {
        config.hintTitle = hintTitle
        return this
    }

    /**
     * 填充输入框内的提示
     */
    fun setInputHint(hintInput: String): CommonBigDialog {
        config.hintInput = hintInput
        return this
    }

    /**
     * 设置底部的描述
     */
    fun setDescribe(describe: String): CommonBigDialog {
        config.describe = describe
        return this
    }

    fun setFocusAndShowInput(isFocus: Boolean): CommonBigDialog {
        config.focusAndShowInput = isFocus
        return this
    }

    fun setDefaultInputString(inputStr: String): CommonBigDialog {
        config.defaultInputString = inputStr
        return this
    }

    /**
     *  全部选中并且获取焦点(暂不需要)
     */
    fun setSelectAllOnFocus(isFocus: Boolean): CommonBigDialog {
        config.selectAllOnFocus = isFocus
        return this
    }

    fun setMaxInputLength(maxLength: Int): CommonBigDialog {
        config.maxInputLength = maxLength
        return this
    }

    fun setCancelText(cancelText: String): CommonBigDialog {
        config.cancelText = cancelText
        return this
    }

    fun setConfirmText(confirmText: String): CommonBigDialog {
        config.confirmText = confirmText
        return this
    }

    fun setConfirmTextColor(color: Int): CommonBigDialog {
        config.confirmTextColor = color
        return this
    }


    fun setActionListener(mOnActionListener: OnActionListener): CommonBigDialog {
        this.mOnActionListener = mOnActionListener
        return this
    }

    /**
     * 展示弹框
     */
    fun show(): CommonBigDialog {
        show(fragmentManager, this.javaClass.simpleName)
        return this
    }

    override fun dismiss() {
        super.dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive) {
            imm.hideSoftInputFromWindow(binding.etInput.windowToken, 0)
        }
        super.onDismiss(dialog)
    }

    interface OnActionListener {
        fun onCancel() {}//非必须实现
        fun onConfirm(inputName: String)
    }

    class Config {
        var hintTitle: String = ""
        var hintInput: String = ""
        var describe: String = ""
        var cancelText: String = ""
        var confirmText: String = ""
        var confirmTextColor: Int = -1
        var maxInputLength: Int = -1
        var selectAllOnFocus: Boolean = false
        var focusAndShowInput: Boolean = false
        var defaultInputString: String = ""
    }


}