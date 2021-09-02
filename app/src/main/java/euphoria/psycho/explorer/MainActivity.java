package euphoria.psycho.explorer;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.share.PackageShare;
import euphoria.psycho.share.PermissionShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.WebViewShare;
import euphoria.psycho.tasks.VideoService;
import euphoria.psycho.videos.AcFunShare;
import euphoria.psycho.videos.Porn91;
import euphoria.psycho.videos.PornHub;
import euphoria.psycho.videos.PornOne;
import euphoria.psycho.videos.YouTube;

public class MainActivity extends Activity implements ClientInterface {
    public static final String KEY_LAST_ACCESSED = "lastAccessed";
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
            requestPermissions(needPermissions.toArray(new String[0]), REQUEST_PERMISSION);
            return true;
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
        mWebView.setWebChromeClient(new CustomWebChromeClient());
        mWebView.setDownloadListener(Helper.getDownloadListener(this));
        WebViewShare.supportCookie(mWebView);
    }

    private void initialize() {
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
        Intent service = new Intent(this, VideoService.class);
        service.setAction(VideoService.CHECK_UNFINISHED_VIDEO_TASKS);
        startService(service);
//        Intent intent = new Intent(this, VideoActivity.class);
//        intent.setData(Uri.parse("file:///storage/emulated/0/Android/data/euphoria.psycho.explorer/files/Download/c271c470ab12ae4c79385304302d0d1c.mp4"));
//        startActivity(intent); 

    }

    private void loadStartPage() {
        if (getIntent().getData() != null) {
            mWebView.loadUrl(getIntent().getData().toString());
        } else {
            mWebView.loadUrl(PreferenceShare.getPreferences()
                    .getString(KEY_LAST_ACCESSED, ListenerDelegate.HELP_URL));
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
            PreferenceShare.putString(KEY_LAST_ACCESSED, mWebView.getUrl());
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
                Toast.makeText(this, R.string.need_permissions, Toast.LENGTH_LONG).show();
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
        if (PornHub.handle(uri, this)) {
            return true;
        }
        if (PornOne.handle(uri, this)) {
            return true;
        }
        return false;
    }
}