package euphoria.psycho.explorer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.WebViewShare;

public class Helper {


    //                Request request = new Request(Uri.parse(url));
//
//                request.allowScanningByMediaScanner();
//                request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//
//                request.setDestinationInExternalFilesDir(MainActivity.this,
//                        Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url))));
//
//                DownloadManager downloadManager = (DownloadManager) MainActivity.this.getSystemService(DOWNLOAD_SERVICE);
//                downloadManager.enqueue(request);
    public static DownloadListener getDownloadListener(final Context context) {
        return (url, userAgent, contentDisposition, mimetype, contentLength) -> Share.setClipboardText(context, url);
    }

    public static void videoChooser(Context context, String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        context.startActivity(Intent.createChooser(intent, "打开视频链接"));
    }

    public static void viewVideo(MainActivity mainActivity, String value) {
        try {
            String uri = "https://hxz315.com/?v=" + URLEncoder.encode(value, "UTF-8");
            DialogShare.createAlertDialogBuilder(mainActivity, "询问", (dialog, which) -> {
                dialog.dismiss();
                Helper.videoChooser(mainActivity, uri);
            }, (dialog, which) -> {
                mainActivity.getWebView().loadUrl(uri);
                dialog.dismiss();
            })
                    .setMessage("是否使用浏览器打开视频链接")
                    .show();
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    public static File createCacheDirectory(Context context) {
        File cacheDirectory = new File(new File(context.getCacheDir(), "Explorer"), "Cache");
        Logger.d(String.format("createCacheDirectory: 览器储存目录 = %s", cacheDirectory.getAbsolutePath()));
        if (!cacheDirectory.isDirectory()) {
            boolean result = cacheDirectory.mkdirs();
            if (!result) {
                Logger.d(String.format("createCacheDirectory: 创建目录 %s 失败", cacheDirectory.getAbsolutePath()));
            }
        }
        return cacheDirectory;
    }

    public static void openDownloadDialog(Context context,String videoId, String videoUrl) {
        new AlertDialog.Builder(context)
                .setTitle("询问")
                .setMessage("是否下载视频？")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    WebViewShare.downloadFile(context, videoId + ".mp4", videoUrl, NetShare.DEFAULT_USER_AGENT);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
