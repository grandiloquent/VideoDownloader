package euphoria.psycho.videos;

import android.app.ProgressDialog;
import android.os.Process;
import android.widget.Toast;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DialogShare;

public abstract class BaseExtractor<T> {
    private final String mInputUri;
    protected final MainActivity mMainActivity;

    protected BaseExtractor(String inputUri, MainActivity mainActivity) {
        mInputUri = inputUri;
        mMainActivity = mainActivity;
    }

    public void parsingVideo() {
        ProgressDialog progressDialog = DialogShare.createProgressDialog(mMainActivity);
        progressDialog.show();
        String uri = processUri(mInputUri);
        performTask(uri, progressDialog);
    }

    protected abstract T fetchVideoUri(String uri);

    protected abstract String processUri(String inputUri);

    protected abstract void processVideo(T videoUri);

    private void performTask(String uri, ProgressDialog progressDialog) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            T videoUri = fetchVideoUri(uri);
            mMainActivity.runOnUiThread(() -> {
                if (videoUri != null) {
                    processVideo(videoUri);
                } else {
                    Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
            });
        }).start();
    }
}
//