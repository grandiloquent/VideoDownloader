package euphoria.psycho.explorer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CustomWebViewClient extends WebViewClient {


    private final ClientInterface mClientInterface;

    public CustomWebViewClient(ClientInterface clientInterface) {
        mClientInterface = clientInterface;
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        super.onLoadResource(view, url);
        if (url.contains("xvideos.red/")) {
            view.evaluateJavascript("[...document.querySelectorAll('.x-overlay')].forEach(x=>x.remove())", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {

                }
            });
        }
        if (!url.contains("www.hxz315.com") && (url.contains(".m3u8") || url.contains(".mp4"))) {

            mClientInterface.onVideoUrl(url);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);

    }

    @Override
    public void onPageFinished(WebView view, String url) {

    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        if ((url.startsWith("https://") || url.startsWith("http://"))) {
            view.loadUrl(url);
        }
        return true;

    }


    @Override
    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return super.shouldInterceptRequest(view, url);
    }


    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

        return super.shouldInterceptRequest(view, request);
    }
}
