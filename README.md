此项目已停止更新，请关注 https://github.com/grandiloquent/91porn-client

# 视频浏览器

一个可以解析和下载各大平台视频的Android手机软件。

<img src="images/3.gif" width="33.3%"><img src="images/1.gif" width="33.3%">

## 更新历史

* 1.1.5

    * 优化观看已下载视频的逻辑

* 1.1.4

    * 修正下载视频时的BUG

* 1.1.3

    * 优化下载 m3u8 格式视频的逻辑
    * 下载 91porn、57ck、xvideos 平台视频，使用标题保存文件，更便于辨识

## 支持的视频平台

* [x] 快手
* [x] 抖音
* [x] TikTok
* [x] [AcFun弹幕视频网](https://www.acfun.cn/)
* [x] [爱奇艺](https://m.iqiyi.com/)
* [x] [B站](https://www.bilibili.com/)
* [x] [芒果TV](https://mgtv.com/)
* [x] [腾讯视频](https://v.qq.com/)
* [x] [西瓜视频](https://m.ixigua.com/)
* [x] [中央卫视](https://tv.cctv.com/m/index.shtml)
* [x] [Twitter](https://m.twitter.com)
* [x] [YouTube](https://m.youtube.com)
* [x] [91porn](https://91porn.com/index.php): 批量下载视频
* [x] [XVideos](https://xvideos.com)
* [x] [PornHub](https://www.pornhub.com)
* [x] [PornOne](https://pornone.com/)
* [x] http://57ck.cc/
* [x] [搜索视频](http://47.106.105.122)

## 腾讯视频会员

下载安装**腾讯会议**手机软件，通过福利入口绑定邮箱可领取**7天腾讯视频会员**

使用浏览器登陆腾讯视频后，`F12`键 > 控制台 > 粘贴代码 `document.cookie` 回车执行 > 复制单引号中间的字符串 > 粘贴到该软件即可下载腾讯会员视频

## 使用方法

* 打开视频页面点击视频，在不影响体验的状态下，将自动解析播放
* 打开视频所在页面，点击顶部下载按钮进行解析
* 视频存放目录：*/storage/emulated/0/Download* 和 */storage/emulated/0/Android/data/euphoria.psycho.explorer/files/Download*

*由于Android新版本更严格的储存策略，可以通过数据线连续电脑访问/storage/emulated/0/Android/data/euphoria.psycho.explorer/files/Download目录下的视频文件*

## 程序下载

* [示例](https://github.com/grandiloquent/VideoBrowser/releases)
* [国内](https://lucidu.cn/article/jqdkgl)

## 引用

* https://github.com/HaarigerHarald/android-youtubeExtractor
* https://github.com/soarcn/BottomSheet
* https://github.com/google/volley
* https://github.com/Tencent/rapidjson
* https://github.com/yhirose/cpp-httplib
* https://github.com/openssl/openssl
* https://github.com/google/brotli

## How to download TikTok videos?

1. Copy the video shared link or page address from the tiktok application or website, some like:

        https://vm.tiktok.com/ZSJkHUCwK/
        https://www.tiktok.com/@travelscenerykj/video/6990367736601922822

2. Click the add link button in the upper left corner

3. Paste the video shared link or page address into the input,then click the ok button.

## 问题

* 如果某些页面的视频无法下载，请使用菜单刷新页面
* 如果某些视频提示无法解析，请再次尝试，或者通过[此链接反馈](http://lucidu.cn/feedback)。
