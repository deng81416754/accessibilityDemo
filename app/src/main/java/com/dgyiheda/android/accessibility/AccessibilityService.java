package com.dgyiheda.android.accessibility;

import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.IntentUtils;
import com.blankj.utilcode.util.JsonUtils;
import com.blankj.utilcode.util.ProcessUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import java.util.Calendar;

/**
 * 辅助点击服务
 * Created by yuyife on 2018/7/25 23:15.
 */
public class AccessibilityService extends android.accessibilityservice.AccessibilityService {

    private static final String TAG = AccessibilityService.class.getSimpleName();

    private boolean firstVerify = false;


    private String packName = "com.tencent.wework";
    private String epMain = "com.tencent.wework.launch.WwMainActivity";

    private String workPage = "com.tencent.wework.enterprise.attendance.controller.AttendanceActivity2";
    private String currentActivity;
    private String currentPackageName;


    private int startAttendanceMinute = 50;

    private int endAttendanceMinute = 15;
    // 中午下班工作的结束分钟
    private int middayEndAttendanceMinute = 30;
    // 下午下班工作的结束分钟
    private int afternoonEndAttendanceMinute = 59;

    private String barkToken = "2Nm7jjJA5SUVhNC8FenV4Z";

    enum Status {
        Up, Down
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event == null || event.getPackageName() == null) {
            Log.w(TAG, "onAccessibilityEvent packageName -> null");
            return;
        }

        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {

            if (getCurrentActivity(event) != null) {
                currentActivity = getCurrentActivity(event);
            }
            String initPackageName = initPackageName(event);
            if (initPackageName != null) {
                currentPackageName = initPackageName;
            }

            Calendar calendar = Calendar.getInstance();
            // 获取当前日期是星期几，1代表周日，2代表周一，以此类推
            int weekday = calendar.get(Calendar.DAY_OF_WEEK);

            int hours = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // 判断是否是周一至周五
            if (weekday >= Calendar.MONDAY && weekday <= Calendar.FRIDAY) {
                Log.d(TAG, "今天是工作日！");
                if ((hours == 7 || hours == 13) && minute >= startAttendanceMinute) {
                    Log.d(TAG, "进入上班卡，检测");
                    startCheck(Status.Up, event);

                } else if (hours == 12 && minute >= 15 && minute <= middayEndAttendanceMinute) {
                    Log.d(TAG, "进入中午下班卡，检测");
                    startCheck(Status.Down, event);

                } else if (hours == 17 && minute >= 45 && minute <= afternoonEndAttendanceMinute) {
                    Log.d(TAG, "进入下班卡，检测");
                    startCheck(Status.Down, event);
                } else {
                    Log.d(TAG, "不在工作时间范围内");
                }

            } else if (weekday == Calendar.SATURDAY) {
                Log.d(TAG, "今天是周六！");
                if (hours == 7 && minute >= startAttendanceMinute) {
                    Log.d(TAG, "进入上班卡，检测");
                    startCheck(Status.Up, event);

                } else if (hours == 12 && minute <= endAttendanceMinute) {
                    Log.d(TAG, "进入下班卡，检测");
                    startCheck(Status.Down, event);
                }

            }

//            Log.d(TAG, "eventType" + eventType);
//            iterateNodesAndHandleInstaller(event.getSource());

        }


    }

    boolean closeApp = false;

    /**
     * @Description up down
     **/
    private void startCheck(Status status, AccessibilityEvent event) {
        Log.d(TAG, "当前应用=" + currentPackageName + packName.equals(initPackageName(event)));
        if (closeApp) {
            AccessibilityNodeInfo close = findNode(event.getSource(), "结束运行");
            if (close != null) {
                tap(close);
            }
            AccessibilityNodeInfo forciblyStop = findNode(event.getSource(), "要强行停止吗");
            AccessibilityNodeInfo confirm = findNode(event.getSource(), "确定");
            if (forciblyStop != null && confirm != null) {
                tapXY(630, 1400, null);

            }
            return;
        }


        if (packName.equals(currentPackageName)) {
            AccessibilityNodeInfo workNode = findNode(event.getSource(), "工作台");
            AccessibilityNodeInfo dkMenu = findNode(event.getSource(), "打卡");
            AccessibilityNodeInfo auxiliary = findNode(event.getSource(), "客户联系与管理");
            AccessibilityNodeInfo upWork = findNode(event.getSource(), "上班打卡");
            AccessibilityNodeInfo downWork = findNode(event.getSource(), "下班打卡");

            Log.d(TAG, "当前页面=" + currentActivity);
            if (isEPWeChatMainPage(currentActivity)) {
                Log.d(TAG, "定位到主页");
                //定位在主页，没有找到打卡，但是找到工作台
                if (dkMenu == null && workNode != null) {
                    tap(workNode);
                    Log.d(TAG, "主页   ACTION_CLICK");
                }

                if (dkMenu != null && auxiliary != null) {
                    tap(dkMenu);
                    Log.d(TAG, "打卡   ACTION_CLICK");
                } else if (workNode != null) {
                    tap(workNode);
                }

            } else if (isWorkMainPage(currentActivity)) {
                Log.d(TAG, "定位到工作页");

                AccessibilityNodeInfo canTClockIn = findNode(event.getSource(), "无法打卡");
                AccessibilityNodeInfo disConnect = findNode(event.getSource(), "不在打卡范围内");
                AccessibilityNodeInfo disConnect2 = findNode(event.getSource(), "未连接蓝牙考勤机");
                AccessibilityNodeInfo search = findNode(event.getSource(), "正在搜寻蓝牙考勤机信号");
                AccessibilityNodeInfo success = findNode(event.getSource(), "打卡成功");

                ProcessUtils.killBackgroundProcesses(packName);

                if (canTClockIn == null && disConnect == null && disConnect2 == null && search == null) {

                    if (status == Status.Up) {
                        if (upWork != null) {
                            Log.d(TAG, "定位到设备,可以上班");
                            tapXY(330, 880, status);

                        }
                        if (downWork != null) {
                            Log.d(TAG, "已经上过班了");
                            AppUtils.launchAppDetailsSettings(packName);
                            closeApp = true;
                        }
                    }

                    if (status == Status.Down) {
                        if (downWork != null) {
                            Log.d(TAG, "定位到设备，可以下班");
                            tapXY(330, 880, status);
                        }
                        if (upWork != null) {
                            Log.d(TAG, "已经下过班了");
                            AppUtils.launchAppDetailsSettings(packName);
                            closeApp = true;
                        }

                    }
                    if (success!=null){
                        back();
                    }

                } else {
                    Log.d(TAG, "未找到设备");
                }


            } else {
                //判断是否进了别的页面
                Log.d(TAG, "非法页面" + currentActivity);
                back();
            }
        } else {
            AppUtils.launchApp(packName);

        }
    }

    private boolean isEPWeChatMainPage(String currentActivity) {
        return !TextUtils.isEmpty(currentActivity) && epMain.equals(currentActivity);
    }

    private boolean isWorkMainPage(String currentActivity) {
        return !TextUtils.isEmpty(currentActivity) && workPage.equals(currentActivity);
    }


    private String getCurrentActivity(AccessibilityEvent event) {
        //获取包名
        String packages = event.getPackageName().toString();
        //获取类名
        String name = event.getClassName().toString();
        //过滤
        if (name.contains("com.")) {
            if (name.contains(packages)) {
                name = name.replace(packages, "");
                name = name.replace("..", "");
                if (name.charAt(0) == '.') name = name.substring(1);
            }
            Log.d(TAG, "包名" + packages + "." + name);
            return packages + "." + name;
        }

        return null;
    }


    private AccessibilityNodeInfo findNode(AccessibilityNodeInfo rootNode, String nodeText) {
        if (rootNode == null) {
            return null;
        }
        if (rootNode.getText() != null && rootNode.getText().toString().contains(nodeText)) {
            return rootNode;
        }
        if (rootNode.getChildCount() > 0) {
            for (int i = 0; i < rootNode.getChildCount(); i++) {
                AccessibilityNodeInfo childRoot = rootNode.getChild(i);
                if (childRoot == null) {
                    continue;
                }
                if (childRoot.getText() != null && childRoot.getText().toString().contains(nodeText)) {
                    return childRoot;
                } else {
                    if (childRoot.getChildCount() > 0) {
                        AccessibilityNodeInfo nextChildNode = findNode(childRoot, nodeText);
                        if (nextChildNode != null) {
                            return nextChildNode;
                        }
                    }
                }
            }
            return null;
        }
        return null;
    }

    private String initPackageName(AccessibilityEvent event) {
        if (event != null) {
            final AccessibilityNodeInfo nodeInfo = event.getSource();//当前界面的可访问节点信息
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {//界面变化事件
                if (TextUtils.isEmpty(event.getPackageName()) || event.getClassName() == null) {
                    return null;
                }
                ComponentName componentName = new ComponentName(event.getPackageName().toString(), event.getClassName().toString());
                ActivityInfo activityInfo = tryGetActivity(componentName);
                boolean isActivity = activityInfo != null;
                if (isActivity && nodeInfo != null && !TextUtils.isEmpty(nodeInfo.getPackageName())) {
                    return (String) nodeInfo.getPackageName();
                }
            }
        }
        return null;
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void onInterrupt() {
        //被拦截
    }


    private void tap(AccessibilityNodeInfo nodeInfo) {
        SystemClock.sleep(1000);
        Rect rect = new Rect();
        nodeInfo.getBoundsInScreen(rect);
        Log.d(TAG, "forceClick: " + rect.left + " " + rect.top + " " + rect.right + " " + rect.bottom);
        int x = (rect.left + rect.right) / 2;
        int y = (rect.top + rect.bottom) / 2;
        Log.d(TAG, "解析后控件的坐标x=" + x + "y=" + y);

        tapXY(x, y, null);

    }

    private void tapXY(int x, int y, Status status) {
        Log.i(TAG, "tap++");

        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path p = new Path();
        p.moveTo(x, y);
        builder.addStroke(new GestureDescription.StrokeDescription(p, 1300L, 150L));
        GestureDescription gesture = builder.build();
        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.i(TAG, "onCompleted...");
                //
                if (x == 630 && y == 1400) {
                    closeApp = false;
                } else if (x == 330 && y == 800) {
                    notification(status);

                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.e(TAG, "onCancelled...");
            }
        }, null);

    }

    private void notification(Status status) {

        String nowString = TimeUtils.getNowString();
        String s = status == Status.Up ? "上班" : "下班";
        EasyHttp.get("/2Nm7jjJA5SUVhNC8FenV4Z/" + s + "/" + nowString + "/辛苦了~打卡成功?sound=minuet").execute(new SimpleCallBack<String>() {
            @Override
            public void onError(ApiException e) {

            }

            @Override
            public void onSuccess(String s) {

            }
        });
    }


    private void back() {
        tapXY(520, 1546, null);
    }


}
