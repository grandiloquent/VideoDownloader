package euphoria.psycho.player;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.Nullable;
import euphoria.psycho.downloader.DownloadTaskDatabase;
import euphoria.psycho.downloader.DownloaderActivity;
import euphoria.psycho.downloader.DownloaderService;
import euphoria.psycho.downloader.DownloaderTask;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.explorer.R;
import euphoria.psycho.explorer.SettingsFragment;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.videos.Iqiyi;

public class TencentActivity extends PlayerActivity implements
        Iqiyi.Callback {
    public static final String EXTRA_PLAYLSIT = "extra.PLAYLSIT";
    public static final String EXTRA_VIDEO_FORMAT = "extra.FORMAT";
    public static final String EXTRA_VIDEO_ID = "extra.VIDEO_ID";

    private int mVideoFormat;
    private String mVideoId;

    private void downloadVideos() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("下载...");
        dialog.show();
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            AtomicInteger atomicInteger = new AtomicInteger();
            Arrays.stream(mPlayList).forEach(p -> {
                final FutureTask<Object> ft = new FutureTask<Object>(() -> {
                }, new Object());
                DownloadTaskDatabase downloadTaskDatabase = DownloadTaskDatabase.getInstance(this);
                File dir = DownloaderService.createVideoDownloadDirectory(this);
                Iqiyi.getVideoAddress(p, uri -> {
                    DownloaderTask downloaderTask = new DownloaderTask();
                    downloaderTask.Uri = getAuthorizationKey(uri);
                    downloaderTask.Directory = dir.getAbsolutePath();
                    downloaderTask.FileName = String.format("%02d-%s.mp4", atomicInteger.incrementAndGet(), KeyShare.md5(uri));
                    downloadTaskDatabase.insertDownloadTask(downloaderTask);
                    ft.run();
                });
                try {
                    ft.get();
                } catch (Exception ignored) {
                }
            });
            runOnUiThread(() -> {
                dialog.dismiss();
                Intent activity = new Intent(this, DownloaderActivity.class);
                startActivity(activity);
            });
        }).start();
    }

    private String getAuthorizationKey(String uri) {
        String url = StringShare.substringBeforeLast(uri, "?");
        String fileName = StringShare.substringAfterLast(url, "/");
        String cookie = PreferenceShare.getPreferences().getString(SettingsFragment.KEY_TENCENT, null);
        String key = Native.fetchTencentKey(
                fileName,
                mVideoId,
                mVideoFormat,
                cookie
        );
        return url + "?vkey=" + key;
    }
    private void initializePlayer() {
        if (!loadPlayList())
            return;

    }

    private boolean loadPlayList() {
        mPlayList = getIntent().getStringArrayExtra(EXTRA_PLAYLSIT);
        if (mPlayList.length == 1) {
            mExoNext.setVisibility(View.GONE);
            mExoPrev.setVisibility(View.GONE);
        }
        mVideoFormat = getIntent().getIntExtra(EXTRA_VIDEO_FORMAT, 0);
        mVideoId = getIntent().getStringExtra(EXTRA_VIDEO_ID);
        if (mPlayList != null) {
            playPlayList(mCurrentPlaybackIndex);
            return true;
        }
        return false;
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFileDownload.setOnClickListener(v -> {
            this.downloadVideos();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
    }

    @Override
    protected void playPlayList(int index) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Referer", "https://v.qq.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Mobile Safari/537.36");
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("加载...");
        dialog.show();
        new Thread(() -> {
            String uri = mPlayList[index];
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String key = getAuthorizationKey(uri);
            runOnUiThread(() -> {
                dialog.dismiss();
                mPlayer.setVideoURI(Uri.parse(key), headers);
            });
        }).start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        String whatString = "MEDIA_ERROR_UNKNOWN";
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN: {
                whatString = "MEDIA_ERROR_UNKNOWN";
            }
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED: {
                whatString = "MEDIA_ERROR_SERVER_DIED";
            }
        }
        String extraString = "MEDIA_ERROR_IO";
        switch (extra) {
            case MediaPlayer.MEDIA_ERROR_IO: {
                extraString = "MEDIA_ERROR_IO";
            }
            case MediaPlayer.MEDIA_ERROR_MALFORMED: {
                extraString = "MEDIA_ERROR_MALFORMED";
            }
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED: {
                extraString = "MEDIA_ERROR_UNSUPPORTED";
            }
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT: {
                extraString = "MEDIA_ERROR_TIMED_OUT";
            }
        }
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            mCurrentPosition = mPlayer.getCurrentPosition();
            playPlayList(mCurrentPlaybackIndex);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("´íÎó")
                    .setMessage(String.format("%s %s", whatString, extraString))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                    .show();
        }
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //        String whatString = Integer.toString(what);
//        switch (what) {
//            case MediaPlayer.MEDIA_INFO_UNKNOWN: {
//                whatString = "MEDIA_INFO_UNKNOWN";
//            }
//            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING: {
//                whatString = "MEDIA_INFO_VIDEO_TRACK_LAGGING";
//            }
//            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START: {
//                whatString = "MEDIA_INFO_VIDEO_RENDERING_START";
//            }
//            case MediaPlayer.MEDIA_INFO_BUFFERING_START: {
//                whatString = "MEDIA_INFO_BUFFERING_START";
//            }
//            case MediaPlayer.MEDIA_INFO_BUFFERING_END: {
//                whatString = "MEDIA_INFO_BUFFERING_END";
//            }
//            case 703: {
//                whatString = "MEDIA_INFO_NETWORK_BANDWIDTH";
//            }
//            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING: {
//                whatString = "MEDIA_INFO_BAD_INTERLEAVING";
//            }
//            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE: {
//                whatString = "MEDIA_INFO_NOT_SEEKABLE";
//            }
//            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE: {
//                whatString = "MEDIA_INFO_METADATA_UPDATE";
//            }
//            case MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE: {
//                whatString = "MEDIA_INFO_UNSUPPORTED_SUBTITLE";
//            }
//            case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT: {
//                whatString = "MEDIA_INFO_SUBTITLE_TIMED_OUT";
//            }
//        }
        if (extra > 0) {
            mCurrentPosition = mPlayer.getCurrentPosition();
            playPlayList(mCurrentPlaybackIndex);
        }
        return false;
    }

    // If the player has loaded enough data
    // we immediately start playing the video
    // and if the user temporarily switches to another program
    // we will cache the playback position
    // and when the player is opened again
    // we will try to jump to the previous playback position
    @Override
    public void onPrepared(MediaPlayer mp) {
        super.onPrepared(mp);
        mProgress.setDuration(mp.getDuration());
    }
    @Override
    public void onVideoUri(String uri) {
        runOnUiThread(() -> {
            if (uri != null) {
                mPlayer.setVideoPath(uri);
            } else {
                Toast.makeText(TencentActivity.this, R.string.unable_to_extract_video, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}
