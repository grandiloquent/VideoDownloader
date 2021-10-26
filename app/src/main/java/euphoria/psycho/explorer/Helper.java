package euphoria.psycho.explorer;

import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.PlayerActivity;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.PackageShare;
import euphoria.psycho.share.PermissionShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.WebViewShare;
import euphoria.psycho.tasks.VideoService;

public class Helper {
    static final String KEY_LAST_ACCESSED = "lastAccessed";
    static final int REQUEST_PERMISSION = 66;

    public static DownloadListener getDownloadListener(final Context context) {
        return (url, userAgent, contentDisposition, mimetype, contentLength) -> Share.setClipboardText(context, url);
    }

    public static void openDownloadDialog(Context context, String videoId, String videoUrl) {
        new Builder(context)
                .setTitle(R.string.ask)
                .setMessage("是否下载视频？")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    WebViewShare.downloadFile(context, videoId + ".mp4", videoUrl, NetShare.DEFAULT_USER_AGENT);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .show();
    }

    static void checkChrome(Context context) {
        new Thread(() -> {
            if (PreferenceShare.getPreferences().getBoolean("chrome", false) ||
                    PackageShare.isAppInstalled(context, "com.android.chrome")) {
                PreferenceShare.getEditor().putBoolean("chrome", true).apply();
            }
        }).start();
    }

    static boolean checkPermissions(Activity activity) {
        List<String> needPermissions = new ArrayList<>();
        if (VERSION.SDK_INT <= 28 && !PermissionShare.checkSelfPermission(activity, permission.WRITE_EXTERNAL_STORAGE)) {
            needPermissions.add(permission.WRITE_EXTERNAL_STORAGE);
        }
        if (needPermissions.size() > 0) {
            activity.requestPermissions(needPermissions.toArray(new String[0]), REQUEST_PERMISSION);
            return true;
        }
        return false;
    }

    static void checkUnfinishedVideoTasks(Context context) {
        Intent service = new Intent(context, VideoService.class);
        service.setAction(VideoService.CHECK_UNFINISHED_VIDEO_TASKS);
        context.startService(service);
    }

    static void configureWebView(MainActivity context, WebView webView) {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, WebViewShare.getFileType(context, url));
            WebViewShare.downloadFile(context, fileName, url, userAgent);
        });
        WebViewShare.setWebView(webView, Helper.createCacheDirectory(context).getAbsolutePath());
        webView.setWebViewClient(new CustomWebViewClient(context));
        webView.setWebChromeClient(new CustomWebChromeClient(context));
        webView.setDownloadListener(Helper.getDownloadListener(context));
        WebViewShare.supportCookie(webView);
    }

    static File createCacheDirectory(Context context) {
        File cacheDirectory = new File(new File(context.getCacheDir(), "Explorer"), "Cache");
        if (!cacheDirectory.isDirectory()) {
            if (!cacheDirectory.mkdirs()) {
                throw new IllegalStateException();
            }
        }
        return cacheDirectory;
    }

    static void loadStartPage(Activity activity, WebView webView) {
        if (activity.getIntent().getData() != null) {
            webView.loadUrl(activity.getIntent().getData().toString());
        } else {
            webView.loadUrl(PreferenceShare.getPreferences()
                    .getString(KEY_LAST_ACCESSED, ListenerDelegate.HELP_URL));
        }
    }
//
    static void tryPlayVideo(Context context) {
        Intent intent = new Intent(context, PlayerActivity.class);
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            try {
                intent.setData(Uri.fromFile(Files.list(Paths.get("/storage/emulated/0/Android/data/euphoria.psycho.explorer/files/Download"))
                        .filter(path -> path.getFileName().toAbsolutePath().toString().endsWith(".mp4"))
                        .sorted((o1, o2) -> {
                            long result = 0;
                            try {
                                result = Files.getLastModifiedTime(o2).toMillis() - Files.getLastModifiedTime(o1).toMillis();
                            } catch (Exception ignored) {
                            }
                            if (result < 0) {
                                return -1;
                            } else if (result > 0) {
                                return 1;
                            } else {
                                return 0;
                            }
                        })
                        .findFirst().get().toFile()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        context.startActivity(intent);
    }
}//