package com.chen.mavenchen.freeDialog

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chen.mavenchen.R

class TestFreeDialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_test_free_dialog)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()

    }

    /**
     * 测试弹起键盘的上推
     */
    private fun initView() {
        val openDialog = findViewById<TextView>(R.id.open_dialog)
        val openDialog2 = findViewById<TextView>(R.id.open_dialog2)
        openDialog.setOnClickListener {
            CommonInputValueDialog(supportFragmentManager)
                .setTitle("哈哈哈哈")
                .setFocusAndShowInput(true)
                .setDescribe("测试一下")
                .setInputHint("输入文本测试")
                .show()
        }
        openDialog2.setOnClickListener {
            CommonBigDialog(supportFragmentManager)
                .setTitle("哈哈哈哈2")
                .setDescribe("测试一下2")
                .setInputHint("输入文本测试2")
                .show()
        }
    }
}
