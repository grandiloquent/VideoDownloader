package euphoria.psycho.explorer;

import android.graphics.Bitmap;
import android.os.Message;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.Logger;

public class CustomWebViewClient extends WebViewClient {


    private final ClientInterface mClientInterface;
    private final WebResourceResponse mEmptyResponse = new WebResourceResponse(
            "text/plain",
            "UTF-8",
            new ByteArrayInputStream("".getBytes())
    );
    private String mJavaScript;

    public CustomWebViewClient(ClientInterface clientInterface) {
        mClientInterface = clientInterface;
        try {
            mJavaScript = FileShare.readText(clientInterface.getContext().getAssets().open("youtube.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    //
    @Override
    public void onPageFinished(WebView view, String url) {
        view.evaluateJavascript(mJavaScript, null);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        if (mClientInterface.shouldOverrideUrlLoading(url)) {
            return true;
        } else if ((url.startsWith("https://") || url.startsWith("http://"))) {
            view.loadUrl(url);
        }
        return true;

    }


    @Override
    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (url.contains("://fans.91p20.space/")
                || url.contains("://ssl.google-analytics.com/")) {
            return mEmptyResponse;
        }
        return super.shouldInterceptRequest(view, url);
    }


    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return super.shouldInterceptRequest(view, request);
    }

}
