package euphoria.psycho.explorer;

import android.content.Context;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;

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
        return new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Share.setClipboardText(context, url);
            }
        };
    }

    public static void setWebView(WebView webView, String appCachePath) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(appCachePath);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

    }
}
