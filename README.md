# accessibilityDemo
利用accessibility执行自动化操作,由于autojs 作者删除部分关键性方法，导致不能wsyw
迫于生活,才衍生出该APP

## 保活措施
1. `ForegroundService` 前台服务
2. `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` 电池优化管理列表

## 辅助服务状态
```
     val accessibilitySettingsOn = AutoInstallerUtil.isAccessibilitySettingsOn(this)

```
## 通知服务
[bark通知到iOS设备](https://github.com/Finb/Bark)


## 启动脚步
1. 开启该APP的辅助服务即可
2. 关闭电池优化


## 停止脚步
监听了音量变化，调制最小即可快速停止脚本


