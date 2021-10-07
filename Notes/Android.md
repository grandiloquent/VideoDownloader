# Android

## adb

### 通过Wifi调试

```
adb tcpip 5000
adb connect 192.168.0.109:5000
```

## Logcat

```
^(?!.*(wificond|SELinux|PackageUtil|QSAnimator)).*$
```

## 更多

- https://developer.android.com/studio/command-line/adb
