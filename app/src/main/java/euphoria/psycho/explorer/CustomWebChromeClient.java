package euphoria.psycho.explorer;

import android.Manifest;
import android.app.Activity;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

// https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/android/webkit/WebViewClient.java
public class CustomWebChromeClient extends WebChromeClient {
    private Activity mActivity;

    public CustomWebChromeClient(Activity activity) {
        mActivity = activity;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        //Log.e("TAG/", "Debug: onConsoleMessage, \n" + consoleMessage.message());
        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        for (String permission : request.getResources()) {
            switch (permission) {
                case "android.webkit.resource.AUDIO_CAPTURE": {
                    break;
                }
                case Manifest.permission.RECORD_AUDIO: {
                    break;
                }
                case Manifest.permission.CAMERA: {
                    break;
                }
            }
        }
        request.grant(request.getResources());
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        mActivity.setTitle(title);
    }

    @Override
    public void onRequestFocus(WebView view) {
        super.onRequestFocus(view);
    }
}
