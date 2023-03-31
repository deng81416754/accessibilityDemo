package com.dgyiheda.android.accessibility

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.permission.PermissionUtils
import com.lzf.easyfloat.utils.DisplayUtils


class MainActivity : AppCompatActivity() {

    var stringBuffer = StringBuffer()

    var tvStatus = lazy {
        findViewById<TextView>(R.id.tvStatus)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tvSetAccessibility).setOnClickListener {
            AutoInstallerUtil.toAccessibilityService(this)
        }
        findViewById<TextView>(R.id.tvFlushed).setOnClickListener {
            Log.d("12", "34")
            initView()
        }

        findViewById<TextView>(R.id.tvBattery).setOnClickListener {
            val i = Intent()
            //跳转到设置里面的电池优化管理列表
            i.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            startActivity(i)
        }
        initView()
        VolumeChangeHelper(this).registerVolumeChangeListener(object :
            VolumeChangeHelper.VolumeChangeListener {
            override fun onVolumeDownToMin() {
                var d = "a"
                d.toInt()
            }

            override fun onVolumeUp() {

            }

        })

    }

    override fun onResume() {
        super.onResume()
        PermissionUtils.checkPermission(this)
    }

    private fun initView() {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val currentActivity = activityManager.getRunningTasks(1)[0].topActivity!!.className

        stringBuffer.setLength(0)

        val accessibilitySettingsOn = AutoInstallerUtil.isAccessibilitySettingsOn(this)

        stringBuffer.append("辅助辅助状态：${accessibilitySettingsOn}\r\n")
        stringBuffer.append("悬浮窗状态：${accessibilitySettingsOn}")
        stringBuffer.append("悬浮窗状态：${accessibilitySettingsOn}")

        tvStatus.value.text = stringBuffer.toString()
        if (accessibilitySettingsOn) {
            if (!EasyFloat.isShow("testFloat")) {
                EasyFloat.with(this)
                    // 设置浮窗xml布局文件/自定义View，并可设置详细信息
                    .setLayout(R.layout.float_test) { }
                    // 设置浮窗显示类型，默认只在当前Activity显示，可选一直显示、仅前台显示
                    .setShowPattern(ShowPattern.ALL_TIME)
                    // 设置吸附方式，共15种模式，详情参考SidePattern
                    .setSidePattern(SidePattern.RESULT_HORIZONTAL)
                    // 设置浮窗的标签，用于区分多个浮窗
                    .setTag("testFloat")
                    // 设置浮窗是否可拖拽
                    .setDragEnable(true)
                    // 浮窗是否包含EditText，默认不包含
                    .hasEditText(false)
                    // 设置浮窗固定坐标，ps：设置固定坐标，Gravity属性和offset属性将无效
                    .setLocation(100, 200)
                    // 设置浮窗的对齐方式和坐标偏移量
                    .setGravity(Gravity.END or Gravity.CENTER_VERTICAL, 0, 200)
                    // 设置当布局大小变化后，整体view的位置对齐方式
                    .setLayoutChangedGravity(Gravity.END)
                    // 设置宽高是否充满父布局，直接在xml设置match_parent属性无效
                    .setMatchParent(widthMatch = false, heightMatch = false)
                    // 设置浮窗的出入动画，可自定义，实现相应接口即可（策略模式），无需动画直接设置为null
                    .setAnimator(DefaultAnimator())
                    // 设置系统浮窗的不需要显示的页面
                    .setFilter(MainActivity::class.java, MainActivity::class.java)
                    // 设置系统浮窗的有效显示高度（不包含虚拟导航栏的高度），基本用不到，除非有虚拟导航栏适配问题
                    .setDisplayHeight { context -> DisplayUtils.rejectedNavHeight(context) }
                    // 浮窗的一些状态回调，如：创建结果、显示、隐藏、销毁、touchEvent、拖拽过程、拖拽结束。
                    // ps：通过Kotlin DSL实现的回调，可以按需复写方法，用到哪个写哪个
                    .registerCallback {
                        createResult { isCreated, msg, view -> }
                        show { }
                        hide { }
                        dismiss { }
                        touchEvent { view, motionEvent -> }
                        drag { view, motionEvent -> }
                        dragEnd { }
                    }
                    // 创建浮窗（这是关键哦😂）
                    .show()
            }
        }


    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {

                return false
            }
            KeyEvent.KEYCODE_VOLUME_DOWN ->                 //音量键down
                return false
            else -> {}
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        VolumeChangeHelper(this).unregisterReceiver()
    }
}