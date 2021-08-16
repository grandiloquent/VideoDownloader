package euphoria.psycho.explorer;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.DialogShare.Callback;
import euphoria.psycho.share.Logger;
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
        if (!PermissionShare.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE)) {
            needPermissions.add(permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!PermissionShare.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE)) {
            needPermissions.add(permission.READ_EXTERNAL_STORAGE);
        }
        if (needPermissions.size() > 0) {
            if (SDK_INT >= VERSION_CODES.M) {
                requestPermissions(needPermissions.toArray(new String[0]), REQUEST_PERMISSION);
                return true;
            }
        }
        return false;
    }

    private void initialize() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                byte[] buffer = new byte[128];
//                byte[] buf="https://v.douyin.com/eoQggd5/"
//                        .getBytes(StandardCharsets.UTF_8);
//                int length= buf.length;
//                int result = NativeShare.getDouYin(buf
//                       ,
//                        length,
//                        buffer
//                );
//                Logger.d(String.format("run: %d %s", result, new String(buffer, 0, result)));
//
//            }
//        }).start();
        setContentView(R.layout.activity_main);
        PreferenceShare.initialize(this);
        findViewById(R.id.add_link).setOnClickListener(v -> {
            DialogShare.createEditDialog(this, "", new Callback() {
                @Override
                public void run(String string) {
                    if (DouYin.handle(string, MainActivity.this)) {
                        return;
                    }
                    if (KuaiShou.handle(string, MainActivity.this))
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
        checkChrome();
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
            String url = mWebView.getUrl();
            if (Porn91.handle(url, this)) {
                return;
            }
            if (YouTube.handle(url, this)) {
                return;
            }
            if (Iqiyi.MATCH_IQIYI.matcher(url).find()) {
                new Iqiyi(url, MainActivity.this).parsingVideo();
                return;
            }
            if (AcFunShare.parsingVideo(this, null)) return;
            if (XVideos.handle(url, this)) {
                return;
            }
            if (Bilibili.handle(url, this)) {
                return;
            }
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
        if (checkPermissions()) return;
//        if (SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
//            try {
//                Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
//                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
//                startActivityForResult(intent, 1);
//            } catch (Exception ex) {
//                Intent intent = new Intent();
//                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                startActivityForResult(intent, 1);
//            }
//            return;
//        }
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
        if (Porn91.handle(uri, this)) {
            return true;
        }
        if (YouTube.handle(uri, this)) {
            return true;
        }
        //if (IqiyiShare.parsingVideo(this, uri)) return true;
        if (AcFunShare.parsingVideo(this, uri)) return true;
        if (XVideos.handle(uri, this)) {
            return true;
        }
        return false;
    }
    //

}
