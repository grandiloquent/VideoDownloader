package euphoria.psycho.explorer;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.DialogShare.Callback;
import euphoria.psycho.share.PackageShare;
import euphoria.psycho.share.PermissionShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.WebViewShare;
import euphoria.psycho.videos.AcFunShare;
import euphoria.psycho.videos.Bilibili;
import euphoria.psycho.videos.DouYin;
import euphoria.psycho.videos.Iqiyi;
import euphoria.psycho.videos.KuaiShou;
import euphoria.psycho.videos.Porn91;
import euphoria.psycho.videos.PornHub;
import euphoria.psycho.videos.PornOne;
import euphoria.psycho.videos.XVideosRedShare;
import euphoria.psycho.videos.XVideos;
import euphoria.psycho.videos.YouTube;

import static android.os.Build.VERSION.SDK_INT;

public class MainActivity extends Activity implements ClientInterface {
    public static final String LAST_ACCESSED = "lastAccessed";
    private static final int REQUEST_PERMISSION = 66;
    private WebView mWebView;
    private BookmarkDatabase mBookmarkDatabase;
    private String mVideoUrl;

    public BookmarkDatabase getBookmarkDatabase() {
        return mBookmarkDatabase;
    }

    public String getVideoUrl() {
        return mVideoUrl;
    }

    public WebView getWebView() {
        return mWebView;
    }

    private void checkChrome() {
        if (PreferenceShare.getPreferences().getBoolean("chrome", false) ||
                PackageShare.isAppInstalled(this, "com.android.chrome")) {
            PreferenceShare.getEditor().putBoolean("chrome", true).apply();
        }
    }

    private boolean checkPermissions() {
        List<String> needPermissions = new ArrayList<>();
        // we need the WRITE_EXTERNAL_STORAGE
        // permission to download video
        if (!PermissionShare.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE)) {
            needPermissions.add(permission.WRITE_EXTERNAL_STORAGE);
        }
        if (needPermissions.size() > 0) {
            if (SDK_INT >= VERSION_CODES.M) {
                requestPermissions(needPermissions.toArray(new String[0]), REQUEST_PERMISSION);
                return true;
            }
        }
        return false;
    }

    private void configureWebView() {
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, WebViewShare.getFileType(MainActivity.this, url));
            WebViewShare.downloadFile(MainActivity.this, fileName, url, userAgent);
        });
        WebViewShare.setWebView(mWebView, Helper.createCacheDirectory(this).getAbsolutePath());
        mWebView.setWebViewClient(new CustomWebViewClient(this));
        mWebView.setWebChromeClient(new CustomWebChromeClient(this));
        mWebView.setDownloadListener(Helper.getDownloadListener(this));
        WebViewShare.supportCookie(mWebView);
    }

    private void initialize() {
        // Logger.d(String.format("initialize: %s", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                byte[] buffer = new byte[128];
//                byte[] buf = "c8fe382e726ae919036f"
//                        .getBytes(StandardCharsets.UTF_8);
//                int result = NativeShare.get91Porn(buf,
//                        buffer,
//                        128
//                );
//               Logger.d(String.format("run: %d %s", result, new String(buffer, 0, result)));
//
//            }
//        }).start();
        setContentView(R.layout.activity_main);
        PreferenceShare.initialize(this);
        // check whether the chrome is installed
        // if is such we should like to use the chrome to play the video
        // for better UX
        checkChrome();
        mWebView = findViewById(R.id.web);
        // Bind all event handlers
        new ListenerDelegate(this);
        mBookmarkDatabase = new BookmarkDatabase(this);
        // Set the corresponding parameters of WebView
        configureWebView();
        loadStartPage();
//        Intent service = new Intent(this, DownloadService.class);
//        service.setData(Uri.parse("https://ccn.killcovid2021.com//m3u8/509537/509537.m3u8?st=fn0MNMJXISpLi0f-fGK-5g&e=1629364436"));
//        startService(service);

    }

    private void loadStartPage() {
        // mWebView.loadUrl("https://i.y.qq.com/n2/m/index.html");
        if (getIntent().getData() != null) {
            mWebView.loadUrl(getIntent().getData().toString());
        } else {
            mWebView.loadUrl(PreferenceShare.getPreferences()
                    .getString(LAST_ACCESSED, ListenerDelegate.HELP_URL));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
//            if (Environment.isExternalStorageManager()) {
//                initialize();
//            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if we have obtained all
        // the permissions required  to run the app
        if (checkPermissions()) return;
        initialize();
    }

    @Override
    protected void onPause() {
        // WebView can be null when the pause event occurs
        if (mWebView != null)
            PreferenceShare.putString(LAST_ACCESSED, mWebView.getUrl());
        super.onPause();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onBackPressed() {
        // When the user press the back button,
        // tries to return to the previously visited page
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            boolean result = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            // If the user does not provide a necessary permission,
            // exit the program
            if (!result) {
                Toast.makeText(this, "缺少必要权限，程序无法运行", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        initialize();
    }

    @Override
    public void onVideoUrl(String uri) {
        mVideoUrl = uri;
    }

    @Override
    public boolean shouldOverrideUrlLoading(String uri) {
        if (Porn91.handle(uri, this)) {
            return true;
        }
        if (YouTube.handle(uri, this)) {
            return true;
        }
        if (AcFunShare.parsingVideo(this, uri)) {
            return true;
        }
        if (XVideos.handle(uri, this)) {
            return true;
        }
        if (PornHub.handle(uri, this)) {
            return true;
        }
        if (PornOne.handle(uri, this)) {
            return true;
        }
        return false;
    }
}
