package com.dgyiheda.android.accessibility

import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.os.PowerManager
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ProcessUtils
import com.blankj.utilcode.util.TimeUtils
import com.zhouyou.http.EasyHttp
import com.zhouyou.http.callback.SimpleCallBack
import com.zhouyou.http.exception.ApiException
import java.util.*

/**
 * 辅助点击服务
 * Created by yuyife on 2018/7/25 23:15.
 */
class AccessibilityService : android.accessibilityservice.AccessibilityService() {
    private val packName = "com.tencent.wework"
    private val epMain = "com.tencent.wework.launch.WwMainActivity"
    private val workPage = "com.tencent.wework.enterprise.attendance.controller.AttendanceActivity2"
    private var currentActivity: String? = null
    private var currentPackageName: String? = null
    private val startAttendanceMinute = 50
    private val endAttendanceMinute = 15

    // 中午下班工作的结束分钟
    private val middayEndAttendanceMinute = 30

    // 下午下班工作的结束分钟
    private val afternoonEndAttendanceMinute = 59
    private val barkToken = "2Nm7jjJA5SUVhNC8FenV4Z"

    internal enum class Status {
        Up, Down
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == null) {
            Log.w(TAG, "onAccessibilityEvent packageName -> null")
            return
        }
        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            if (getCurrentActivity(event) != null) {
                currentActivity = getCurrentActivity(event)
            }
            val initPackageName = initPackageName(event)
            if (initPackageName != null) {
                currentPackageName = initPackageName
            }
            val calendar = Calendar.getInstance()
            // 获取当前日期是星期几，1代表周日，2代表周一，以此类推
            val weekday = calendar[Calendar.DAY_OF_WEEK]
            val hours = calendar[Calendar.HOUR_OF_DAY]
            val minute = calendar[Calendar.MINUTE]


            // 判断是否是周一至周五
            if (weekday >= Calendar.MONDAY && weekday <= Calendar.FRIDAY) {
                Log.d(TAG, "今天是工作日！")
                if ((hours == 7 || hours == 13) && minute >= startAttendanceMinute) {
                    Log.d(TAG, "进入上班卡，检测")
                    startCheck(Status.Up, event)
                } else if (hours == 12 && minute >= 15 && minute <= middayEndAttendanceMinute) {
                    Log.d(TAG, "进入中午下班卡，检测")
                    startCheck(Status.Down, event)
                } else if (hours == 17 && minute >= 45 && minute <= afternoonEndAttendanceMinute) {
                    Log.d(TAG, "进入下班卡，检测")
                    startCheck(Status.Down, event)
                } else {
                    Log.d(TAG, "不在工作时间范围内")
                }
            } else if (weekday == Calendar.SATURDAY) {
                Log.d(TAG, "今天是周六！")
                if (hours == 7 && minute >= startAttendanceMinute) {
                    Log.d(TAG, "进入上班卡，检测")
                    startCheck(Status.Up, event)
                } else if (hours == 12 && minute <= endAttendanceMinute) {
                    Log.d(TAG, "进入下班卡，检测")
                    startCheck(Status.Down, event)
                } else {
                    Log.d(TAG, "不在工作时间范围内")
                }
            }

//            Log.d(TAG, "eventType" + eventType);
//            iterateNodesAndHandleInstaller(event.getSource());
        }
    }

    var closeApp = false

    /**
     * @Description up down
     */
    @SuppressLint("InvalidWakeLockTag")
    private fun startCheck(status: Status, event: AccessibilityEvent) {
        Log.d(TAG, "当前应用=" + currentPackageName + (packName == initPackageName(event)))
        val pm = this.getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_DIM_WAKE_LOCK,
            "com.dgyiheda.android.accessibility"
        )
        wakeLock.acquire(5 * 60 * 1000L /*10 minutes*/) //亮屏
        if (closeApp) {
            val close = findNode(event.source, "结束运行")
            close?.let { tap(it) }
            val forciblyStop = findNode(event.source, "要强行停止吗")
            val confirm = findNode(event.source, "确定")
            if (forciblyStop != null && confirm != null) {
                tapXY(630, 1400, null)
            }
            return
        }
        if (packName == currentPackageName) {
            val workNode = findNode(event.source, "工作台")
            val dkMenu = findNode(event.source, "打卡")
            val auxiliary = findNode(event.source, "客户联系与管理")
            val upWork = findNode(event.source, "上班打卡")
            val downWork = findNode(event.source, "下班打卡")
            Log.d(TAG, "当前页面=$currentActivity")
            if (isEPWeChatMainPage(currentActivity)) {
                Log.d(TAG, "定位到主页")
                //定位在主页，没有找到打卡，但是找到工作台
                if (dkMenu == null && workNode != null) {
                    tap(workNode)
                    Log.d(TAG, "主页   ACTION_CLICK")
                }
                if (dkMenu != null && auxiliary != null) {
                    tap(dkMenu)
                    Log.d(TAG, "打卡   ACTION_CLICK")
                } else workNode?.let { tap(it) }
            } else if (isWorkMainPage(currentActivity)) {
                Log.d(TAG, "定位到工作页")
                val canTClockIn = findNode(event.source, "无法打卡")
                val disConnect = findNode(event.source, "不在打卡范围内")
                val disConnect2 = findNode(event.source, "未连接蓝牙考勤机")
                val search = findNode(event.source, "正在搜寻蓝牙考勤机信号")
                val success = findNode(event.source, "打卡成功")
                val normal = findNode(event.source, "正常")
                ProcessUtils.killBackgroundProcesses(packName)
                if (canTClockIn == null && disConnect == null && disConnect2 == null && search == null) {
                    if (status == Status.Up) {
                        if (upWork != null) {
                            Log.d(TAG, "定位到设备,可以上班")
                            tapXY(330, 880, status)
                        }
                        if (downWork != null) {
                            Log.d(TAG, "已经上过班了")
                            AppUtils.launchAppDetailsSettings(packName)
                            closeApp = true
                        }
                    }
                    if (status == Status.Down) {
                        if (downWork != null) {
                            Log.d(TAG, "定位到设备，可以下班")
                            tapXY(330, 880, status)
                        }
                        if (upWork != null) {
                            Log.d(TAG, "已经下过班了")
                            AppUtils.launchAppDetailsSettings(packName)
                            closeApp = true
                        }
                    }
                    if (normal != null) {
                        Log.d(TAG, "检测到打卡了")
                    }
                } else {
                    Log.d(TAG, "未找到设备")
                }
            } else {
                //判断是否进了别的页面
                Log.d(TAG, "非法页面$currentActivity")
                back()
            }
        } else {
            AppUtils.launchApp(packName)
        }
    }

    private fun isEPWeChatMainPage(currentActivity: String?): Boolean {
        return !TextUtils.isEmpty(currentActivity) && epMain == currentActivity
    }

    private fun isWorkMainPage(currentActivity: String?): Boolean {
        return !TextUtils.isEmpty(currentActivity) && workPage == currentActivity
    }

    private fun getCurrentActivity(event: AccessibilityEvent): String? {
        //获取包名
        val packages = event.packageName.toString()
        //获取类名
        var name = event.className.toString()
        //过滤
        if (name.contains("com.")) {
            if (name.contains(packages)) {
                name = name.replace(packages, "")
                name = name.replace("..", "")
                if (name[0] == '.') name = name.substring(1)
            }
            Log.d(TAG, "包名$packages.$name")
            return "$packages.$name"
        }
        return null
    }

    private fun findNode(
        rootNode: AccessibilityNodeInfo?,
        nodeText: String
    ): AccessibilityNodeInfo? {
        if (rootNode == null) {
            return null
        }
        if (rootNode.text != null && rootNode.text.toString().contains(nodeText)) {
            return rootNode
        }
        if (rootNode.childCount > 0) {
            for (i in 0 until rootNode.childCount) {
                val childRoot = rootNode.getChild(i) ?: continue
                if (childRoot.text != null && childRoot.text.toString().contains(nodeText)) {
                    return childRoot
                } else {
                    if (childRoot.childCount > 0) {
                        val nextChildNode = findNode(childRoot, nodeText)
                        if (nextChildNode != null) {
                            return nextChildNode
                        }
                    }
                }
            }
            return null
        }
        return null
    }

    private fun initPackageName(event: AccessibilityEvent?): String? {
        if (event != null) {
            val nodeInfo = event.source //当前界面的可访问节点信息
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) { //界面变化事件
                if (TextUtils.isEmpty(event.packageName) || event.className == null) {
                    return null
                }
                val componentName =
                    ComponentName(event.packageName.toString(), event.className.toString())
                val activityInfo = tryGetActivity(componentName)
                val isActivity = activityInfo != null
                if (isActivity && nodeInfo != null && !TextUtils.isEmpty(nodeInfo.packageName)) {
                    return nodeInfo.packageName as String
                }
            }
        }
        return null
    }

    private fun tryGetActivity(componentName: ComponentName): ActivityInfo? {
        return try {
            packageManager.getActivityInfo(componentName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    override fun onInterrupt() {
        //被拦截
    }

    private fun tap(nodeInfo: AccessibilityNodeInfo) {
        SystemClock.sleep(1000)
        val rect = Rect()
        nodeInfo.getBoundsInScreen(rect)
        Log.d(
            TAG,
            "forceClick: " + rect.left + " " + rect.top + " " + rect.right + " " + rect.bottom
        )
        val x = (rect.left + rect.right) / 2
        val y = (rect.top + rect.bottom) / 2
        Log.d(TAG, "解析后控件的坐标x=" + x + "y=" + y)
        tapXY(x, y, null)
    }

    private fun tapXY(x: Int, y: Int, status: Status?) {
        Log.i(TAG, "tap++")
        val builder = GestureDescription.Builder()
        val p = Path()
        p.moveTo(x.toFloat(), y.toFloat())
        builder.addStroke(StrokeDescription(p, 1300L, 150L))
        val gesture = builder.build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                Log.i(TAG, "onCompleted...")
                //
                if (x == 630 && y == 1400) {
                    closeApp = false
                } else if (x == 330 && y == 880) {
                    notification(status)
                    back()
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                Log.e(TAG, "onCancelled...")
            }
        }, null)
    }

    private fun notification(status: Status?) {
        val nowString = TimeUtils.getNowString()
        val s = if (status == Status.Up) "上班" else "下班"
        EasyHttp.get("/2Nm7jjJA5SUVhNC8FenV4Z/$s/$nowString/辛苦了~打卡成功?sound=minuet")
            .execute(object : SimpleCallBack<String?>() {
                override fun onError(e: ApiException) {}
                override fun onSuccess(t: String?) {
                }
            })
    }

    private fun back() {
        tapXY(520, 1546, null)
    }

    companion object {
        private val TAG = AccessibilityService::class.java.simpleName
    }
}