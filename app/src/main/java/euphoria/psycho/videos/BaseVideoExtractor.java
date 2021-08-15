package euphoria.psycho.videos;

import android.os.Process;
import android.app.ProgressDialog;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

public abstract class BaseVideoExtractor<T> {
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

    protected abstract T fetchVideoUri(String uri);

    protected String getString(String uri, String[][] headers) {
        try {
            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", BaseVideoExtractor.USER_AGENT);
            if (headers != null) {
                for (int i = 0; i < headers.length; i++) {
                    urlConnection.setRequestProperty(headers[i][0], headers[i][1]);
                }
            }
            int code = urlConnection.getResponseCode();
            if (code < 400 && code >= 200) {
                return NetShare.readString(urlConnection);
            } else {
                return null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

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
