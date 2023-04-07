# accessibilityDemo
利用accessibility执行自动化操作,由于autojs 作者删除部分关键性方法，导致不能wsyw
迫于生活,才衍生出该APP
解决您上班途中迟到问题，只需一部备用手机置于公司工位，设置一下上班打卡时间，接下来的事就交给我们吧。 此应用本意是方便自己，有不到之处还请谅解。 本应用仅限学习和内部使用，严禁商用和用作其他非法用途，如有违反，与本人无关！！！ 本应用的出发点是为了解决上班路途遥远，或者每天卡点上班族的燃眉之急，出发点自认为是友好的，但是，不可滥用！！！ 使用注意事项：

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

##  结果如下
打卡成功时推送至iOS设备
推送样式
![image](https://camo.githubusercontent.com/9c3264a2ab464ec9d483cc65797c045576b29618c7e195ecb0d387ded71dd9e0/687474703a2f2f7778342e73696e61696d672e636e2f6d773639302f303036306c6d37546c7931673062746a6867696d696a33306b753061363076312e6a7067)
打卡失败（失败原因有很多，比如，企业微信账号被自己另一个手机挤下去，再比如，企业微信没有搜索到考勤机设备，或者企业微信进错企业，没有打卡规则，或者企业微信打卡手机又2个以上，因为企业微信最多只能有两个常用打卡手机等等情况都会导致打卡失败，所以，在使用本软件之前，最好先自行测试一两天没确认没问题之后再使用，谢谢理解！）