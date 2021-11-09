package euphoria.psycho.explorer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.IOException;

import euphoria.psycho.downloader.DownloadTaskDatabase;
import euphoria.psycho.downloader.DownloaderService;
import euphoria.psycho.downloader.DownloaderTask;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.tasks.HLSDownloadTask;
import euphoria.psycho.videos.Ck52;
import euphoria.psycho.videos.Porn91;
import euphoria.psycho.videos.PornHub;
import euphoria.psycho.videos.PornOne;
import euphoria.psycho.videos.XiGua;
import euphoria.psycho.videos.YouTube;

import static euphoria.psycho.explorer.Helper.KEY_LAST_ACCESSED;
import static euphoria.psycho.explorer.Helper.checkPermissions;
import static euphoria.psycho.explorer.Helper.configureWebView;
import static euphoria.psycho.explorer.Helper.loadStartPage;

public class MainActivity extends Activity implements ClientInterface {

    private WebView mWebView;
    private BookmarkDatabase mBookmarkDatabase;
    private String mVideoUrl;
    private String[] mVideoList;

    public BookmarkDatabase getBookmarkDatabase() {
        return mBookmarkDatabase;
    }

    public String getVideoUrl() {
        return mVideoUrl;
    }

    public WebView getWebView() {
        return mWebView;
    }

    private void checkUpdate() {
        Activity context = this;
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String versionName = UpdateUtils.getApplicationVersionName(context);
            if (versionName == null) {
                return;
            }
            String serverVersionName = Native.fetchApplicationVersion();
            if (serverVersionName == null) {
                return;
            }
            if (serverVersionName.compareTo(versionName) > 0) {
                context.runOnUiThread(() -> {
                    UpdateUtils.launchDialog(this, (dialog, which) -> UpdateUtils.launchDownloadActivity(context));
                });
            }

        }).start();
    }

    private void initialize() {
        setContentView(R.layout.activity_main);
        PreferenceShare.initialize(this);
        mWebView = findViewById(R.id.web);
        JavaScriptInterface javaScriptInterface = new JavaScriptInterface(this);
        mWebView.addJavascriptInterface(javaScriptInterface, "JInterface");
        // Bind all event handlers
        new ListenerDelegate(this);
        mBookmarkDatabase = new BookmarkDatabase(this);
        // Set the corresponding parameters of WebView
        configureWebView(this, mWebView);
        loadStartPage(this, mWebView);
        //checkUnfinishedVideoTasks(this);
        checkUpdate();
        // tryPlayVideo(this);
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("正在下载中...");
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    HLSDownloadTask task = new HLSDownloadTask(MainActivity.this)
                            .build("https://hls3-l3.xvideos-cdn.com/a6ca65727b70c76a41500cf2c62b1f65cfe55e19-1636436235/videos/hls/09/15/9c/09159c730f5097165cc4651eabef8958/hls-250p.m3u8");
                    Log.e("B5aOx2", String.format("run, %s", task.toString()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if we have obtained all
        // the permissions required  to run the app
        if (checkPermissions(this)) return;
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
    public String[] getVideoList() {
        return mVideoList;
    }

    public void setVideoList(String[] videoList) {
        mVideoList = videoList;
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
        if (PornHub.handle(uri, this)) {
            return true;
        }
        if (PornOne.handle(uri, this)) {
            return true;
        }
        if (XiGua.handle(uri, this)) {
            return true;
        }
//        if (mWebView.getUrl().contains("xvideos.com")) {
//            Pattern pattern = Pattern.compile("xvideos\\.com/video\\d+");
//            if (pattern.matcher(uri).find()) {
//                new Thread(() -> {
//                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
//                    XVideos.fetchVideos(uri);
//                }).start();
//            }
//        }
        return Ck52.handle(uri, this);
    }

    void insertDownloaderTaskForTesting(String fileName, String uri) {
        DownloaderTask task = new DownloaderTask();
        task.Directory = DownloaderService.createVideoDownloadDirectory(this).getAbsolutePath();
        task.FileName = fileName;
        task.Uri = uri;
        DownloadTaskDatabase.getInstance(this).insertDownloadTask(task);
    }
}