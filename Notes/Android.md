# Android

## adb

### 通过Wifi调试

```
adb tcpip 5000
adb connect 192.168.0.109:5000
```

```
adb shell cmd package list packages
adb shell pm list packages -f

adb shell pm path com.android.chrome
adb pull /path/to/apk chrome.apk
```

## Logcat

```
^(?!.*(wificond|SELinux|PackageUtil|QSAnimator)).*$
```

## 更多

- https://developer.android.com/studio/command-line/adb

|名称|快捷键|
|---|---|
|Introduce Constant...|Ctrl+Alt+C|

```
^(?:[\t ]*(?:\r?\n|\r)){2,}
Log\.e\([^\n]+\);\n
^(?!.*(wificond|SELinux|crashpad|QSAnimator|DisplayFeatureHal|libglean_ffi|BatteryStatsService)).*$
```

- https://github.com/espressif/arduino-esp32
- https://github.com/skylot/jadx

- https://android.googlesource.com/platform/packages/providers/DownloadProvider/+/master/src/com/android/providers/downloads/DownloadThread.java
