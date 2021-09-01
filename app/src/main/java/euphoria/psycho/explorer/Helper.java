package euphoria.psycho.explorer;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.webkit.DownloadListener;

import java.io.File;

import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.IntentShare;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.WebViewShare;
import euphoria.psycho.videos.VideosHelper;

public class Helper {
    public static File createCacheDirectory(Context context) {
        File cacheDirectory = new File(new File(context.getCacheDir(), "Explorer"), "Cache");
        if (!cacheDirectory.isDirectory()) {
            cacheDirectory.mkdirs();
        }
        return cacheDirectory;
    }

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

    public static void viewVideo(MainActivity mainActivity, String uri) {
        //String uri = URLEncoder.encode(value, "UTF-8");
        DialogShare.createAlertDialogBuilder(mainActivity, "询问", (dialog, which) -> {
            dialog.dismiss();
            if (PreferenceShare.getPreferences().getBoolean("chrome", false)) {
                IntentShare.launchChrome(mainActivity, uri);
            } else {
                VideosHelper.viewerChooser(mainActivity, uri);
            }
        }, (dialog, which) -> {
            mainActivity.getWebView().loadUrl(uri);
            dialog.dismiss();
        })
                .setMessage("是否使用浏览器打开视频链接")
                .show();
    }
}//