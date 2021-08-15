package euphoria.psycho.videos;

import android.os.Process;
import android.app.ProgressDialog;
import android.widget.Toast;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

public abstract class BaseVideoExtractor {
    public static String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";
    private final String mInputUri;
    protected final MainActivity mMainActivity;

    protected BaseVideoExtractor(String inputUri, MainActivity mainActivity) {
        mInputUri = inputUri;
        mMainActivity = mainActivity;
    }

    public void parsingVideo() {
        ProgressDialog progressDialog = DialogShare.createProgressDialog(mMainActivity);
        progressDialog.show();
        String uri = processUri(mInputUri);
        performTask(uri, progressDialog);
    }

    private void performTask(String uri, ProgressDialog progressDialog) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String videoUri = fetchVideoUri(uri);
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

    protected abstract String fetchVideoUri(String uri);

    protected abstract void processVideo(String videoUri);

    protected abstract String processUri(String inputUri);
}
