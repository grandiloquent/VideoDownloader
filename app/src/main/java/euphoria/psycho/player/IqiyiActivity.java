package euphoria.psycho.player;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Process;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import euphoria.psycho.downloader.DownloadTaskDatabase;
import euphoria.psycho.downloader.DownloaderActivity;
import euphoria.psycho.downloader.DownloaderService;
import euphoria.psycho.downloader.DownloaderTask;
import euphoria.psycho.share.DateTimeShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.videos.Iqiyi;

public class IqiyiActivity extends PlayerActivity implements
        Iqiyi.Callback {
    public static final long DEFAULT_SHOW_TIMEOUT_MS = 5000L;
    public static final String EXTRA_PLAYLSIT = "extra.PLAYLSIT";
    public static final String EXTRA_TYPE = "extra.TYPE";
    private final HashMap<String, Integer> mHashMap = new HashMap<>();
    private int mDuration;

    private void downloadVideos() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("正在下载...");
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
                    downloaderTask.Uri = uri;
                    downloaderTask.Directory = dir.getAbsolutePath();
                    downloaderTask.FileName = String.format("%02d-%s.mp4", atomicInteger.incrementAndGet(), KeyShare.md5(uri));
                    downloadTaskDatabase.insertDownloadTask(downloaderTask);
                    ft.run();
                });
                try {
                    ft.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            runOnUiThread(() -> {
                dialog.dismiss();
                Intent activity = new Intent(this, DownloaderActivity.class);
                startActivity(activity);
            });
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlayList = getIntent().getStringArrayExtra(EXTRA_PLAYLSIT);
        if (mPlayList != null) {
            playPlayList(mCurrentPlaybackIndex);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void playPlayList(int index) {
        Iqiyi.getVideoAddress(mPlayList[index], this);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mProgress.setBufferedPosition((long) (mHashMap.values().stream().mapToInt(integer -> integer).sum() * (1f * percent / 100)));
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        super.onPrepared(mp);
        mHashMap.put(mPlayList[mCurrentPlaybackIndex], mp.getDuration());
        mDuration = mHashMap.values().stream().mapToInt(integer -> integer).sum();
        Log.e("B5aOx2", String.format("onPrepared, %s %s", mp.getDuration(), mDuration));
        mProgress.setDuration(mDuration);
        mExoDuration.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, mDuration));
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        int delta = (int) distanceX * -100;
        //if (Math.abs(delta) < 1000) return true;
        delta = (delta / 1000) * 1000;
        if (delta == 0) {
            if (distanceX > 0) {
                delta = -1000;
            } else {
                delta = 1000;
            }
        }
        int positionMs = delta + mPlayer.getCurrentPosition();
        if (positionMs > 0) {
            mPlayer.seekTo(positionMs);
        }
        return true;
    }

    @Override
    public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
        mPlayer.seekTo(15 * 1000 + 30 * 1000);
        mHandler.post(mProgressChecker);
        hideController();
        Log.e("B5aOx2", String.format("onScrubStop, %s %s",
                DateTimeShare.getStringForTime(mStringBuilder,mFormatter, mPlayer.getCurrentPosition()),
                DateTimeShare.getStringForTime(mStringBuilder,mFormatter,6 * 60 * 1000 + 15 * 1000 + 30 * 1000)
        ));
    }

    @Override
    public void onVideoUri(String uri) {
        runOnUiThread(() -> {
            if (uri != null) {
                mPlayer.setVideoPath(uri);
            } else {
                Toast.makeText(IqiyiActivity.this, "无法解析视频", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}
