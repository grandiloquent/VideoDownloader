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
        checkUpdate();
//        new Thread(() -> {
//            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
//            Native.fetchTencent("https://m.v.qq.com/x/m/play?cid=mzc002008hw5asj&vid=n003544n7zd", PreferenceShare.getPreferences().getString(SettingsFragment.KEY_TENCENT, null));
//        }).start();
//        PreferenceShare.getPreferences().edit().putString(
//                SettingsFragment.KEY_TENCENT,
//                "'tvfe_boss_uuid=491677ac313abfdb; video_guid=af96989d4497c4ba; pgv_pvid=9865587890; ts_uid=2412710660; bucket_id=9231002; login_remember=wx; video_omgid=; video_bucketid=4; vversion_name=8.2.95; video_platform=2; pgv_info=ssid=s6869248740; main_login=wx; vuserid=1428406875; openid=oXw7q0NaEfg_OxPKO1_QUWZxTSbc; appid=wxa75efa648b60994b; refresh_token=50_lxKKNEgXg4W8nYse305fyF9CfF8aigRSMp9WgAC2YrLTw7An9r3QTAD_j6RpbchkCGOvncjk5dlgQqu1admbFQpfZQd6Fz52k_MTjduDKyM; last_refresh_vuserid=1428406875; last_refresh_time=1634150632918; access_token=50_lxKKNEgXg4W8nYse305fyMoNiNM8DtAD0M9e75KlRfq0ctMWlCetIi-P5HNrHpb-9iM7ikmQb4Q6FePGnDKOtA; wx_nick=%E5%92%B8%E8%8F%9C; wx_head=http://thirdwx.qlogo.cn/mmopen/vi_32/PiajxSqBRaEJExiarEsa4tDDDxISFtZ4UuNziacY1x4qUaGUL8GhiatnbwIETRcD0LQ7iaibh6Quj4iazoHRww9XscDQQ/132; vusession=usZ7QNlPK6kvI3eyqkf7Pg..; ptag=|顶部导航区:主导航_LOGO; ad_play_index=56; ts_last=v.qq.com/; qv_als=uUEvA6WURJLI9UacA11634169231GJigLw=="
//        ).apply();
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