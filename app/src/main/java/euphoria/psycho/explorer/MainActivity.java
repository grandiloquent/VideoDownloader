package euphoria.psycho.explorer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.videos.AcFun;
import euphoria.psycho.videos.Ck52;
import euphoria.psycho.videos.Porn91;
import euphoria.psycho.videos.PornHub;
import euphoria.psycho.videos.PornOne;
import euphoria.psycho.videos.YouTube;

import static euphoria.psycho.explorer.Helper.KEY_LAST_ACCESSED;
import static euphoria.psycho.explorer.Helper.checkChrome;
import static euphoria.psycho.explorer.Helper.checkPermissions;
import static euphoria.psycho.explorer.Helper.checkUnfinishedVideoTasks;
import static euphoria.psycho.explorer.Helper.configureWebView;
import static euphoria.psycho.explorer.Helper.loadStartPage;

public class MainActivity extends Activity implements ClientInterface {

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

    private void checkUpdate() {
        Activity context = this;
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String versionName = UpdateUtils.getApplicationVersionName(context);
            if (versionName == null) {
                return;
            }
            String serverVersionName = UpdateUtils.getServerVersionName();
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
        // check whether the chrome is installed
        // if is such we should like to use the chrome to play the video
        // for better UX
        checkChrome(this);
        mWebView = findViewById(R.id.web);
        // Bind all event handlers
        new ListenerDelegate(this);
        mBookmarkDatabase = new BookmarkDatabase(this);
        // Set the corresponding parameters of WebView
        configureWebView(this, mWebView);
        loadStartPage(this, mWebView);
        checkUnfinishedVideoTasks(this);
        //tryPlayVideo(this);
        //VideosHelper.invokeVideoPlayer(this, Uri.parse("https://ccn.killcovid2021.com//m3u8/521540/521540.m3u8?st=aM08zWUNiuUDfd4-rs_UUg&e=1631385002"));
//        new Iqiyi("https://www.iqiyi.com/v_19rrok4nt0.html", this)
//                .parsingVideo();
        //Log.e("B5aOx2", String.format("initialize, %s", getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)));
        new Thread(new Runnable() {
            @Override
            public void run() {
                String uri = Native.fetchQQ("https://m.v.qq.com/x/m/play?cid=znda81ms78okdwd&vid=&ptag=v_qq_com%23v.index.adaptor%233");
                Log.e("B5aOx2", String.format("run, %s", uri));
            }
        }).start();
        checkUpdate();
//        DownloaderTask downloaderTask = new DownloaderTask();
//        downloaderTask.Directory = DownloaderService.createVideoDownloadDirectory(this).getAbsolutePath();
//        downloaderTask.FileName = "1.mp4";
//        downloaderTask.Uri = "https://apd-vlive.apdcdn.tc.qq.com/vipzj.video.tc.qq.com/s0030zieohi.mp4?vkey=95EDF0145862FF228082EEFE1F8AA7152395DFD9C4DD6B4AAAB643C5FFED6A94164EC3B4DD75589AE60CC27DD01B73CB3852D09697701F0BD58A058096441E03063221A03F8B0A76F7B62FD432751A36E9F2D5DE82C21F765FF75B32CC586725D5674958FC3B11BC6A535F7AAB8F504B840C3251EB607113&level=0&fmt=hd&platform=10201";
//        DownloadTaskDatabase.getInstance(this)
//                .insertDownloadTask(downloaderTask);
//        Intent activity = new Intent(this, DownloaderActivity.class);
//        startActivity(activity);
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
        if (AcFun.handle(uri, this)) {
            return true;
        }
        if (PornHub.handle(uri, this)) {
            return true;
        }
        if (PornOne.handle(uri, this)) {
            return true;
        }
        return Ck52.handle(uri, this);
    }
}