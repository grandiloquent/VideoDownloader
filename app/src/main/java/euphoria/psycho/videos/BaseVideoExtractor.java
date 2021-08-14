package euphoria.psycho.videos;

import android.os.Process;
import android.app.ProgressDialog;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DialogShare;

public abstract class BaseVideoExtractor {
    public static String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";
    private final String mInputUri;
    private final MainActivity mMainActivity;

    protected BaseVideoExtractor(String inputUri, MainActivity mainActivity) {
        mInputUri = inputUri;
        mMainActivity = mainActivity;
    }

    public boolean parsingVideo() {
        if (!this.checkUri(mInputUri)) {
            return false;
        }
        ProgressDialog progressDialog = DialogShare.createProgressDialog(mMainActivity);
        String uri = processUri(mInputUri);
        performTask(uri, progressDialog);
        return true;

    }

    private void performTask(String uri, ProgressDialog progressDialog) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            mMainActivity.runOnUiThread(() -> {
                String videoUri = fetchVideoUri(uri);
                if (videoUri != null) {
                    mMainActivity.getWebView().loadUrl(videoUri);
                    Helper.openDownloadDialog(mMainActivity, uri, videoUri);
                }
                progressDialog.dismiss();
            });
        }).start();
    }

    protected abstract String fetchVideoUri(String uri);

    protected abstract boolean checkUri(String inputUri);

    protected abstract String processUri(String inputUri);
}
