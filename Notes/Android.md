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
