package euphoria.psycho.explorer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.webkit.WebView;
import android.widget.Toast;

import euphoria.psycho.downloader.DownloadTaskDatabase;
import euphoria.psycho.downloader.DownloaderService;
import euphoria.psycho.downloader.DownloaderTask;
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
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                String uri = Native.fetchBilibili("https://www.bilibili.com/video/BV14U4y1w7fn");
//                Log.e("B5aOx2", String.format("run, %s", uri));
//            }
//        }).start();
        checkUpdate();
        /*insertDownloaderTaskForTesting("1.mp4",
                "https://apd-vlive.apdcdn.tc.qq.com/vipzj.video.tc.qq.com/s0030zieohi.mp4?vkey=D55F78607374927AB935085D5EA288B50442C8F4B68EE337F9D088CCC91AEF6149744EB9AE25A4580D487220DE32D09670661CD154556BCFCBBECCC8C825FB0C98F9401BA9AD98DB241105D9F38742CF8301197B2F6C575C764FF48EDF101048CC39829E04E0A46E0F0044353F27B167C6483F4923B46D04&level=0&fmt=hd&platform=10201");
        insertDownloaderTaskForTesting("2.mp4",
                "https://apd-vlive.apdcdn.tc.qq.com/vipzj.video.tc.qq.com/g00308ipqeo.mp4?vkey=0CD9E9506385202E2034E315D72CABE223A77CA90941CF2E6758AD72ACC7F1BB770D7098168A9159E8504F1F4282A05B05E9B5BA78004584945B41182CDC1E75A709B1397FA4730E7C0C23DA88FBD16B657C4227CB7821E70C929A8998D2CAF68D7960276A1488411BC68624B8939BDAD8A93C304741E5E8&level=0&fmt=hd&platform=10201");
        insertDownloaderTaskForTesting("3.mp4",
                "https://apd-vlive.apdcdn.tc.qq.com/vipzj.video.tc.qq.com/m0030oxlea2.mp4?vkey=B9C00130283DA0120284EF42E48F48E2D48CDBAD5523C5DB6F47FFC29F3B9A051C1C4E6F0D9BBF3E1D6F41447EDC7430F5B73CB8FA5F562439E14EA71E63005BB1D22BA1AF09429951023CF20B511FE7F04FBE990620EB54B7827CBD0C0E43C406FA468C2BAD7C9B92332BAE4FB09115CA12A4FED9D424C4&level=0&fmt=hd&platform=10201");

        Intent downloaderActivity = new Intent(this, DownloaderActivity.class);
        startActivity(downloaderActivity);*/
    }

    void insertDownloaderTaskForTesting(String fileName, String uri) {
        DownloaderTask task = new DownloaderTask();
        task.Directory = DownloaderService.createVideoDownloadDirectory(this).getAbsolutePath();
        task.FileName = fileName;
        task.Uri = uri;
        DownloadTaskDatabase.getInstance(this).insertDownloadTask(task);
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