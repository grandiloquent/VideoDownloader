
package euphoria.psycho.explorer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;


import euphoria.psycho.share.FileShare;

public class DownloadActivity extends Activity implements DownloadNotifier {
    private String URI = "https://ccn.killcovid2021.com//m3u8/505212/505212.m3u8?st=WMDuO07zTRAYck8PUj-pZQ&e=1628554252";
    private TextView mTitle;
    private TextView mSubTitle;

    private final Handler mHandler = new Handler();

    @Override
    public void downloadStart(String uri) {
        mHandler.post(() -> mTitle.setText(URI));
    }

    @Override
    public void downloadFailed(String uri, String message) {
        mTitle.setText(message);
    }

    @Override
    public void downloadProgress(String uri, String fileName, long totalSize) {
        mTitle.setText(fileName);
        mSubTitle.setText(FileShare.formatFileSize(totalSize));
    }

    @Override
    public void downloadProgress(String uri, long totalSize, long downloadBytes, long speed) {
        if (totalSize == downloadBytes) {
            mSubTitle.setText("已完成");
        } else {
            mSubTitle.setText(String.format("%s/%s %s/s", FileShare.formatFileSize(downloadBytes), FileShare.formatFileSize(totalSize), FileShare.formatFileSize(speed)));
        }

    }

    @Override
    public void downloadCompleted(String uri, String directory) {
        mTitle.setText(directory);
        mSubTitle.setText("已完成");

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_activity);
        mTitle = findViewById(R.id.title);
        mSubTitle = findViewById(R.id.subtitle);
        new DownloadThread(URI, this, this)
                .start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    //
}
/*
https://github.com/aosp-mirror/platform_packages_providers_downloadprovider/blob/master/src/com/android/providers/downloads/DownloadThread.java
https://android.googlesource.com/platform/packages/apps/Gallery2/
*/
