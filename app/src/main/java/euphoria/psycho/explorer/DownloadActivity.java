
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
    public void downloadStart(String uri) {
        mHandler.post(() -> mTitle.setText(uri));
    }

    @Override
    public void downloadFailed(String uri, String message) {
        mHandler.post(() -> mTitle.setText(message));
    }

    @Override
    public void downloadProgress(String uri, String fileName, long totalSize) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTitle.setText(fileName);
                if (totalSize > 0)
                    mSubTitle.setText(FileShare.formatFileSize(totalSize));
            }
        });
    }

    @Override
    public void downloadProgress(String uri, long totalSize, long downloadBytes, long speed) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (totalSize == downloadBytes) {
                    mSubTitle.setText("已完成");
                } else {
                    mSubTitle.setText(String.format("%s/%s %s/s", FileShare.formatFileSize(downloadBytes), FileShare.formatFileSize(totalSize), FileShare.formatFileSize(speed)));
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
