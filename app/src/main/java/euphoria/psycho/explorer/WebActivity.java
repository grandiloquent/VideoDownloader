package euphoria.psycho.explorer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import androidx.annotation.Nullable;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.share.WebViewShare;
import euphoria.psycho.tasks.HLSDownloadActivity;

import static euphoria.psycho.videos.VideosHelper.USER_AGENT;

public class WebActivity extends Activity {
    private WebView mWebView;
//    static final SimpleDateFormat FORMATTER = new SimpleDateFormat("E, dd MMM yyyy kk:mm:ss", Locale.US);
//    Map<String, String> mHeaders = new HashMap<String, String>() {{
//        put("Connection", "close");
//        put("Content-Type", "text/plain");
//        put("Access-Control-Allow-Origin", "47.106.105.122"/* your domain here */);
//        put("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
//        put("Access-Control-Max-Age", "600");
//        put("Access-Control-Allow-Credentials", "true");
//        put("Access-Control-Allow-Headers", "accept, authorization, Content-Type");
//        put("Via", "1.1 vegur");
//    }};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        // Set up the WebView control
        mWebView = findViewById(R.id.web);
        mWebView.clearCache(true);
        JavaInterface javaInterface = new JavaInterface();
        mWebView.addJavascriptInterface(javaInterface, "JInterface");
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String videoUri = getIntent().getStringExtra("extra.URI");
                if (videoUri == null) {
                    return;
                }
                Log.e("B5aOx2", String.format("onPageFinished, %s", videoUri));
                javaInterface.parse(videoUri);
            }

            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//                if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
//                    Date date = new Date();
//                    final String dateString = FORMATTER.format(date);
//                    mHeaders.put("Date", dateString + " GMT");
//                    return new WebResourceResponse("text/plain", "UTF-8", 200, "OK", mHeaders, null);
//                }
                // request.getRequestHeaders().put("Access-Control-Allow-Origin", "*");
                return null;
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private WebChromeClient.CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;
            private int mOriginalSystemUiVisibility;

            @Override
            public Bitmap getDefaultVideoPoster() {
                return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.e("B5aOx2", String.format("onConsoleMessage, %s", consoleMessage.message()));
                return super.onConsoleMessage(consoleMessage);
            }

            public void onHideCustomView() {
                ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
                this.mCustomView = null;
                getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
                setRequestedOrientation(this.mOriginalOrientation);
                this.mCustomViewCallback.onCustomViewHidden();
                this.mCustomViewCallback = null;
            }

            @Override
            public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
                if (this.mCustomView != null) {
                    onHideCustomView();
                    return;
                }
                this.mCustomView = paramView;
                this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                this.mOriginalOrientation = getRequestedOrientation();
                this.mCustomViewCallback = paramCustomViewCallback;
                ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
                getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            }
        });
        mWebView.loadUrl("http://47.106.105.122/video.html");
    }

    private class JavaInterface {
        @JavascriptInterface
        public void download(String videoUri, String title) {
            if (videoUri.contains("m3u8")) {
                Intent intent = new Intent(WebActivity.this, HLSDownloadActivity.class);
                intent.setData(Uri.parse(videoUri));
                if (title != null) {
                    intent.putExtra(HLSDownloadActivity.EXTRA_FILE_NAME, title);
                }
                WebActivity.this.startActivity(intent);
            } else {
                WebViewShare.downloadFile(WebActivity.this, (title == null ? KeyShare.toHex(videoUri.getBytes(StandardCharsets.UTF_8)) : title) + ".mp4", videoUri, USER_AGENT);
            }
        }

        @JavascriptInterface
        public void parse(String uri) {
            new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                String[] videoUris;
                if (uri.contains("91porn.com")) {
                    videoUris = Native.fetch91Porn(StringShare.substringAfter(uri, "91porn.com"), PreferenceShare.getPreferences()
                            .getBoolean("in_china", false));
                } else if (uri.contains("xvideos.com")) {
                    videoUris = Native.fetchXVideos(uri);
                } else {
                    videoUris = Native.fetch57Ck(uri);
                }
                String[] finalVideoUris = videoUris;
                runOnUiThread(() -> {
                    if (finalVideoUris == null || finalVideoUris.length < 2) {
                        Toast.makeText(WebActivity.this, "无法解析视频", Toast.LENGTH_LONG).show();
                        return;
                    }
                    JSONObject obj = new JSONObject();
                    try {
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(finalVideoUris[1]);
                        obj.put("title", finalVideoUris[0]);
                        obj.put("videos", jsonArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mWebView.evaluateJavascript("start('" + obj.toString() + "')", null);
                });
            }).start();
        }
    }
}
