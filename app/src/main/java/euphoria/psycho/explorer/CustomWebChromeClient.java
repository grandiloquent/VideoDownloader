package euphoria.psycho.explorer;

import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;

// https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/android/webkit/WebViewClient.java
public class CustomWebChromeClient extends WebChromeClient {
    public CustomWebChromeClient() {
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        //Log.e("TAG/", "Debug: onConsoleMessage, \n" + consoleMessage.message());
        return super.onConsoleMessage(consoleMessage);
    }

}
