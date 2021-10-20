package euphoria.psycho.explorer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Process;
import android.webkit.JavascriptInterface;

public class JavaScriptInterface {
    private Activity mMainActivity;

    public JavaScriptInterface(Activity activity) {
        mMainActivity = activity;
    }

    @JavascriptInterface
    public void handleRequest(String uri) {
        Intent starter = new Intent(mMainActivity, WebActivity.class);
        starter.putExtra("extra.URI", uri);
        mMainActivity.startActivity(starter);
//        ProgressDialog dialog = new ProgressDialog(mMainActivity);
//        dialog.setMessage("解析...");
//        dialog.show();
//        new Thread(() -> {
//            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
//            String videoUri;
//            if (uri.contains("91porn.com")) {
//                videoUri = Native.fetch91Porn(StringShare.substringAfter(uri, "91porn.com"));
//            } else if (uri.contains("xvideos.com")) {
//                videoUri = Native.fetchXVideos(uri);
//            } else {
//                videoUri = Native.fetch57Ck(uri);
//            }
//            String finalVideoUri = videoUri;
//            mMainActivity.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    dialog.dismiss();
//                    if (finalVideoUri == null || finalVideoUri.length() == 0) {
//                        Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
//                        return;
//                    }
//                    mMainActivity.setVideoList(new String[]{finalVideoUri});
//                    mMainActivity.getWebView().loadUrl("file:///android_asset/index.html");
//                }
//            });
//        }).start();
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

