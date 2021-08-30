package euphoria.psycho.explorer;

import android.app.Activity;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

// https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/android/webkit/WebViewClient.java
public class CustomWebChromeClient extends WebChromeClient {
    private final Activity mActivity;

    public CustomWebChromeClient(Activity activity) {
        mActivity = activity;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        //Log.e("TAG/", "Debug: onConsoleMessage, \n" + consoleMessage.message());
        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        mActivity.setTitle(title);
    }

}
