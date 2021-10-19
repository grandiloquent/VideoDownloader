package euphoria.psycho.explorer;

import android.graphics.Bitmap;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import euphoria.psycho.share.FileShare;

public class CustomWebViewClient extends WebViewClient {
    private final String[] mBlocks = new String[]{
            "://a.realsrv.com/",
            "://fans.91p20.space/",
            "://rpc-php.trafficfactory.biz/",
            "://ssl.google-analytics.com/",
            "://syndication.realsrv.com/",
            "://www.gstatic.com/",
            "/ads/"
    };
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
    public void onPageFinished(WebView view, String url) {
        view.evaluateJavascript(mJavaScript, null);
        if (url.equals("file:///android_asset/index.html")) {
            if (mClientInterface.getVideoList() == null) {
                return;
            }
            JSONObject obj = new JSONObject();
            try {
                JSONArray jsonArray = new JSONArray();
                for (String s : mClientInterface.getVideoList()) {
                    jsonArray.put(s);
                }
                obj.put("videos", jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            view.evaluateJavascript("start('" + obj.toString() + "')", null);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    @Override
    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (Arrays.stream(mBlocks).anyMatch(url::contains)) {
            return mEmptyResponse;
        }
        return super.shouldInterceptRequest(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return super.shouldInterceptRequest(view, request);
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
}