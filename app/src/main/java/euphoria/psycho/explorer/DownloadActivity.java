
package euphoria.psycho.explorer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import euphoria.psycho.share.FileShare;

public class DownloadActivity extends Activity implements DownloadNotifier {
    private TextView mTitle;
    private TextView mSubTitle;

    private final Handler mHandler = new Handler();

    @Override
    public void downloadStart(String uri, int total) {
        mHandler.post(() -> {
            mTitle.setText(uri);
            mSubTitle.setText(String.format("0/%d", total));
        });
    }

    @Override
    public void downloadFailed(String uri, String message) {
        mHandler.post(() -> mTitle.setText(message));
    }

    @Override
    public void downloadProgress(String uri, String fileName) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTitle.setText(fileName);
            }
        });
    }

    @Override
    public void downloadProgress(String uri, int currentSize, int total, long downloadBytes, long speed) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (speed == 0) {
                    mSubTitle.setText(String.format("%d/%d %s", currentSize, total, FileShare.formatFileSize(downloadBytes)));
                } else {
                    mSubTitle.setText(String.format("%d/%d %s %s/s", currentSize, total, FileShare.formatFileSize(downloadBytes), FileShare.formatFileSize(speed)));
                }
            }
        });

    }

    @Override
    public void downloadCompleted(String uri, String directory) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTitle.setText(directory);
                mSubTitle.setText("已完成");
            }
        });

    }

    @Override
    public void mergeVideoCompleted(String outPath) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DownloadActivity.this, outPath, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void mergeVideoFailed(String message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DownloadActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_activity);
        mTitle = findViewById(R.id.title);
        mSubTitle = findViewById(R.id.subtitle);
        new DownloadThread(getIntent().getData().toString(), this, this)
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
