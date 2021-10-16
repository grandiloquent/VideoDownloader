package euphoria.psycho.downloader;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Process;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.player.VideoActivity;

public class JavaScriptInterface {
    private MainActivity mMainActivity;

    public JavaScriptInterface(MainActivity activity) {
        mMainActivity = activity;
    }
    @JavascriptInterface
    public void handleRequest(String uri) {
        ProgressDialog dialog = new ProgressDialog(mMainActivity);
        dialog.setMessage("正在下载中...");
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                String videoUri = null;
                if (uri.contains("91porn.com")) {
                    videoUri = Native.fetch91Porn(uri);
                } else if (uri.contains("xvideos.com")) {
                    videoUri = Native.fetchXVideos(uri);
                } else {
                    videoUri = Native.fetch91Porn(uri);
                }
                String finalVideoUri = videoUri;
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (finalVideoUri == null) {
                            Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent intent = new Intent(mMainActivity, VideoActivity.class);
                        intent.setData(Uri.parse(finalVideoUri));
                        mMainActivity.startActivity(intent);
                    }
                });
            }
        }).start();
    }

    @JavascriptInterface
    public void weather(boolean isCity) {
        ProgressDialog dialog = new ProgressDialog(mMainActivity);
        dialog.setMessage("加载中...");
        dialog.show();
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String value =
                    isCity ? Native.fetchWeather("湖南省", "长沙市", "长沙市")
                            : Native.fetchWeather("湖南省", "益阳市", "桃江县");
            mMainActivity.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, value));
            mMainActivity.runOnUiThread(dialog::dismiss);
        }).start();
    }
}

