package com.dgyiheda.android.accessibility

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Created by yuyife on 2018/7/28 22:17.
 */
object AutoInstallerUtil {
    private val TAG = AutoInstallerUtil::class.java.simpleName
    fun execute(mContext: Context, applicationId: String, filePath: String): String? {
        if (TextUtils.isEmpty(filePath)) {
            return "文件不存在！"
        }
        if (!filePath.endsWith(".apk")) {
            return "文件格式不对！"
        }
        // 存储空间权限
        if (permissionDenied(mContext)) {
            return "未取得权限！"
        }

        // 允许安装应用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val b = mContext.packageManager.canRequestPackageInstalls()
            if (!b) {
                return "未取得权限！"
            }
        }
        val file = File(filePath)
        if (!file.exists()) {
            return "文件路径不存在！"
        }
        if (!isAccessibilitySettingsOn(mContext)) {
            toAccessibilityService(mContext)
            return "未开启应用自动安装服务！"
        }

        //Log.e("installApk",applicationId);
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val contentUri =
                FileProvider.getUriForFile(mContext, "$applicationId.fileProvider", file)
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }
        mContext.startActivity(intent)
        return null
    }

    //权限是否被拒绝
    private fun permissionDenied(mContext: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            for (str in permissions) {
                if (mContext.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    return true
                }
            }
        }
        return false
    }

    //去无障碍设置服务
    fun toAccessibilityService(mContext: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
    }

    //无障碍服务 是否开启
    fun isAccessibilitySettingsOn(mContext: Context): Boolean {
        var accessibilityEnabled = 0
        val service = mContext.packageName + "/" + AccessibilityService::class.java.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                mContext.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
            Log.e(TAG, "accessibilityEnabled = $accessibilityEnabled")
        } catch (e: SettingNotFoundException) {
            Log.e(
                TAG, "Error finding setting, default accessibility to not found: "
                        + e.message
            )
        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            Log.e(TAG, "***ACCESSIBILITY IS ENABLED*** ")
            val settingValue = Settings.Secure.getString(
                mContext.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()

                    //Log.e(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        Log.e(
                            TAG,
                            "We've found the correct setting - accessibility is switched on!"
                        )
                        return true
                    }
                }
            }
        } else {
            Log.e(TAG, "***ACCESSIBILITY IS DISABLED***")
        }
        return false
    }
}