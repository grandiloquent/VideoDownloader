package euphoria.psycho.explorer;

import android.Manifest.permission;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
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
import euphoria.psycho.videos.DouYinShare;
import euphoria.psycho.videos.IqiyiShare;
import euphoria.psycho.videos.KuaiShouShare;
import euphoria.psycho.videos.Porn91Share;
import euphoria.psycho.videos.XVideosRedShare;
import euphoria.psycho.videos.XVideosShare;

public class MainActivity extends Activity implements ClientInterface {
    public static final String LAST_ACCESSED = "lastAccessed";
    private static final int REQUEST_PERMISSION = 66;
    private WebView mWebView;
    private BookmarkDatabase mBookmarkDatabase;
    private String mVideoUrl;

    public BookmarkDatabase getBookmarkDatabase() {
        return mBookmarkDatabase;
    }

    public WebView getWebView() {
        return mWebView;
    }

    private boolean checkPermissions() {
        List<String> needPermissions = new ArrayList<>();
        if (!PermissionShare.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE)) {
            needPermissions.add(permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!PermissionShare.checkSelfPermission(this, permission.INTERNET)) {
            needPermissions.add(permission.INTERNET);
        }
        if (needPermissions.size() > 0) {
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                requestPermissions(needPermissions.toArray(new String[0]), REQUEST_PERMISSION);
                return true;
            }
        }
        return false;
    }


    private void initialize() {
        setContentView(R.layout.activity_main);
        PreferenceShare.initialize(this);
        findViewById(R.id.add_link).setOnClickListener(v -> {
            DialogShare.createEditDialog(this, "", new Callback() {
                @Override
                public void run(String string) {
                    if (DouYinShare.parsingVideo(string, MainActivity.this))
                        return;
                    if (KuaiShouShare.parsingVideo(string, MainActivity.this))
                        return;
                    mWebView.loadUrl(string);
                }
            });
        });
        mWebView = findViewById(R.id.web);
        new ListenerDelegate(this);
        setDownloadVideo();
        mBookmarkDatabase = new BookmarkDatabase(this);
        setWebView();
        loadStartPage();
        if (PreferenceShare.getPreferences().getBoolean("chrome", false) ||
                PackageShare.isAppInstalled(this, "com.android.chrome")) {
            PreferenceShare.getEditor().putBoolean("chrome", true).apply();
        }
        //PackageShare.listAllInstalledPackages(this);
    }

    private void loadStartPage() {
        if (getIntent().getData() != null) {
            mWebView.loadUrl(getIntent().getData().toString());
        } else {
            mWebView.loadUrl(PreferenceShare.getPreferences()
                    .getString(LAST_ACCESSED, ListenerDelegate.HELP_URL));
        }
    }


    private void setDownloadVideo() {
        findViewById(R.id.file_download).setOnClickListener(v -> {
            if (XVideosRedShare.parsingXVideos(this, null)) return;
            if (Porn91Share.parsing91Porn(this, null)) return;
            if (mWebView.getUrl().contains("youtube.com/watch")) {
                Share.startYouTubeActivity(this, mWebView.getUrl());
                return;
            }
            if (IqiyiShare.parsingVideo(this, null)) return;
            if (AcFunShare.parsingVideo(this, null)) return;
            if (XVideosShare.parsingVideo(this, null)) return;
            if (mVideoUrl != null) {
                try {
                    mWebView.loadUrl("https://hxz315.com?v=" + URLEncoder.encode(mVideoUrl, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setWebView() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkPermissions()) return;
        initialize();
    }

    @Override
    protected void onPause() {
        if (mWebView != null)
            PreferenceShare.putString(LAST_ACCESSED, mWebView.getUrl());
        super.onPause();
    }

    @Override
    public void onBackPressed() {
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
        if (XVideosRedShare.parsingXVideos(this, uri)) return true;
        if (Porn91Share.parsing91Porn(this, uri)) return true;
        if (uri.contains("youtube.com/watch")) {
            Share.startYouTubeActivity(this, uri);
            return true;
        }
        if (IqiyiShare.parsingVideo(this, uri)) return true;
        if (AcFunShare.parsingVideo(this, uri)) return true;
        if (XVideosShare.parsingVideo(this, uri)) return true;
        return false;
    }
    //

}
