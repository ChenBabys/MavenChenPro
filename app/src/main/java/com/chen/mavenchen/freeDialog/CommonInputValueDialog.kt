package com.chen.mavenchen.freeDialog

import android.content.Context
import android.content.DialogInterface
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.KeyboardUtils
import com.chen.freedialog.BaseFreeDialogFragment
import com.chen.mavenchen.R
import com.chen.mavenchen.databinding.DialogCommonInputValueBinding


/**
 * Author: ChenBabys
 * Date: 2025/3/6
 * Description: 4.0.0版本开始，公用的名称、值输入框.
 * fragmentManager必须是外部传入的，内部的是没有依赖于任何父容器的，用不了
 */
class CommonInputValueDialog(private val fragmentManager: FragmentManager) : BaseFreeDialogFragment<DialogCommonInputValueBinding>() {
    private var mOnActionListener: OnActionListener? = null
    private val config = Config()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): DialogCommonInputValueBinding {
        return DialogCommonInputValueBinding.inflate(inflater, container, false)
    }

    override fun initView(binding: DialogCommonInputValueBinding) {
        // 固定宽度
        dialogConfig.fixWidth = 800
        dialogConfig.dragViewId = R.id.tv_title

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



            KeyboardUtils.showSoftInput(binding.etInput, InputMethodManager.SHOW_IMPLICIT)

//            binding.etInput.isFocusable = true
//            binding.etInput.requestFocus()
////            binding.etInput.requestFocusFromTouch()
//            binding.etInput.postDelayed(
//                Runnable {
//                    //展示输入框
//                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                    imm.showSoftInput(binding.root, 0)
//                },
//                100,
//            )
        }

    }

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val dialog = FoldInputTouchDialog(requireContext(), theme)
//        dialog.setCancelable(dialogConfig.isTouchOutSideCancelable)
//        dialog.setCanceledOnTouchOutside(dialogConfig.isTouchOutSideCancelable)
//        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
//        return dialog
//    }

    fun setTitle(hintTitle: String): CommonInputValueDialog {
        config.hintTitle = hintTitle
        return this
    }

    /**
     * 填充输入框内的提示
     */
    fun setInputHint(hintInput: String): CommonInputValueDialog {
        config.hintInput = hintInput
        return this
    }

    /**
     * 设置底部的描述
     */
    fun setDescribe(describe: String): CommonInputValueDialog {
        config.describe = describe
        return this
    }

    fun setFocusAndShowInput(isFocus: Boolean): CommonInputValueDialog {
        config.focusAndShowInput = isFocus
        return this
    }

    fun setDefaultInputString(inputStr: String): CommonInputValueDialog {
        config.defaultInputString = inputStr
        return this
    }

    /**
     *  全部选中并且获取焦点(暂不需要)
     */
    fun setSelectAllOnFocus(isFocus: Boolean): CommonInputValueDialog {
        config.selectAllOnFocus = isFocus
        return this
    }

    fun setMaxInputLength(maxLength: Int): CommonInputValueDialog {
        config.maxInputLength = maxLength
        return this
    }

    fun setCancelText(cancelText: String): CommonInputValueDialog {
        config.cancelText = cancelText
        return this
    }

    fun setConfirmText(confirmText: String): CommonInputValueDialog {
        config.confirmText = confirmText
        return this
    }

    fun setConfirmTextColor(color: Int): CommonInputValueDialog {
        config.confirmTextColor = color
        return this
    }


    fun setActionListener(mOnActionListener: OnActionListener): CommonInputValueDialog {
        this.mOnActionListener = mOnActionListener
        return this
    }

    /**
     * 展示弹框
     */
    fun show(): CommonInputValueDialog {
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