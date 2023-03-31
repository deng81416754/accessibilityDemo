package com.dgyiheda.android.accessibility

import android.app.Application
import com.zhouyou.http.EasyHttp
import com.zhouyou.http.cache.model.CacheMode

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        EasyHttp.init(this);//默认初始化
        EasyHttp.getInstance()

            //可以全局统一设置全局URL
            .setBaseUrl("https://api.day.app")//设置全局URL  url只能是域名 或者域名+端口号
            .setCertificates()
            .debug("Dennis Http", true)
            .setReadTimeOut(60 * 1000)
            .setWriteTimeOut(60 * 100)
            .setConnectTimeout(60 * 100)
            .setCacheMode(CacheMode.NO_CACHE)
    }
}