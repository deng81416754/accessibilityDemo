package com.dgyiheda.android.accessibility

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {

    companion object{
        private const val TAG = "ForegroundService"
        var serviceIsLive: Boolean = false
        private const val NOTIFICATION_ID = 1111
        //唯一的通知通道的ID
        private const val notificationChannelId = "notification_channel_id_01"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG,"OnCreate")
        startForegroundWithNotification()
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG,"onBind")
        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG,"onUnbind")
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG,"onStartCommand")
        //数据获取
        val data: String? = intent?.getStringExtra("Foreground") ?: ""
        Toast.makeText(this, data, Toast.LENGTH_SHORT).show()
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 开启前景服务并发送通知
     */
    private fun startForegroundWithNotification(){
        //8.0及以上注册通知渠道
        createNotificationChannel()
        val notification: Notification = createForegroundNotification()
        //将服务置于启动状态 ,NOTIFICATION_ID指的是创建的通知的ID
        startForeground(NOTIFICATION_ID, notification)
        //发送通知到状态栏
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification);
    }


    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel(){
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //Android8.0以上的系统，新建消息通道
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            //用户可见的通道名称
            val channelName: String = "Foreground Service Notification"
            //通道的重要程度
            val importance: Int = NotificationManager.IMPORTANCE_HIGH
            //构建通知渠道
            val notificationChannel: NotificationChannel = NotificationChannel(notificationChannelId,
                channelName, importance)
            notificationChannel.description = "Channel description"
            //LED灯
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            //震动
            notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)
            notificationChannel.enableVibration(true)
            //向系统注册通知渠道，注册后不能改变重要性以及其他通知行为
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    /**
     * 创建服务通知
     */
    private fun createForegroundNotification(): Notification {
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, notificationChannelId)
        //通知小图标
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
        //通知标题
        builder.setContentTitle(getString(R.string.app_name))
        //通知内容
        builder.setContentText("前台进程运行中…")
        //设置通知显示的时间
        builder.setWhen(System.currentTimeMillis())
        //设定启动的内容
        val  activityIntent: Intent = Intent(this, MainActivity::class.java)
        activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this,
            1,activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        //设置为进行中的通知
        builder.setOngoing(true)
        //创建通知并返回
        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        stopForeground(true)
        ForegroundService.serviceIsLive = false;
    }


}