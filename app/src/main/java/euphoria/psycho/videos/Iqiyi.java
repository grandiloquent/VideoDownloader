package euphoria.psycho.videos;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.share.KeyShare;

public class Iqiyi extends BaseExtractor<String[]> {
    public static Pattern MATCH_IQIYI = Pattern.compile("\\.iqiyi\\.com/v_");

    public Iqiyi(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String[] fetchVideoUri(String uri) {
        return Native.fetchIqiyi(uri);
    }

    @Override
    protected void processVideo(String[] videoUris) {
        executeTask(videoUris);
    }

    private void executeTask(String[] videoUris) {
        DownloadManager manager = (DownloadManager) mMainActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        for (String uris : videoUris) {
            downloadFile(manager, uris, KeyShare.md5(uris) + ".f4v", "video/x-f4v");
        }
    }

    private void downloadFile(DownloadManager manager, String url, String filename, String mimetype) {
        final DownloadManager.Request request;
        Uri uri = Uri.parse(url);
        request = new DownloadManager.Request(uri);
        request.setMimeType(mimetype);
        // set downloaded file destination to /sdcard/Download.
        // or, should it be set to one of several Environment.DIRECTORY* dirs depending on mimetype?
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        // let this downloaded file be scanned by MediaScanner - so that it can
        // show up in Gallery app, for example.
        request.allowScanningByMediaScanner();
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
//        String cookies = CookieManager.getInstance().getCookie(url, privateBrowsing);
//        request.addRequestHeader("cookie", cookies);
//        request.addRequestHeader("User-Agent", userAgent);
//        request.addRequestHeader("Referer", referer);
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        manager.enqueue(request);
    }
}
