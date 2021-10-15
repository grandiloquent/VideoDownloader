package euphoria.psycho.player;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnTimedMetaDataAvailableListener;
import android.media.MediaPlayer.OnTimedTextListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.TimedMetaData;
import android.media.TimedText;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import euphoria.psycho.downloader.DownloadTaskDatabase;
import euphoria.psycho.downloader.DownloaderActivity;
import euphoria.psycho.downloader.DownloaderService;
import euphoria.psycho.downloader.DownloaderTask;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.explorer.R;
import euphoria.psycho.explorer.SettingsFragment;
import euphoria.psycho.share.DateTimeShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.videos.Iqiyi;

import static euphoria.psycho.player.PlayerHelper.getNavigationBarHeight;
import static euphoria.psycho.player.PlayerHelper.getNavigationBarSize;
import static euphoria.psycho.player.PlayerHelper.hideSystemUI;
import static euphoria.psycho.player.PlayerHelper.rotateScreen;
import static euphoria.psycho.player.PlayerHelper.showSystemUI;
import static euphoria.psycho.player.PlayerHelper.switchPlayState;

public class TencentActivity extends BaseVideoActivity implements
        GestureDetector.OnGestureListener,
        TimeBar.OnScrubListener,
        OnPreparedListener,
        OnCompletionListener,
        OnBufferingUpdateListener,
        OnSeekCompleteListener,
        OnVideoSizeChangedListener,
        OnTimedTextListener,
        OnTimedMetaDataAvailableListener,
        OnInfoListener,
        Iqiyi.Callback {
    public static final long DEFAULT_SHOW_TIMEOUT_MS = 5000L;
    public static final String EXTRA_PLAYLSIT = "extra.PLAYLSIT";
    public static final String EXTRA_VIDEO_ID = "extra.VIDEO_ID";
    public static final String EXTRA_VIDEO_FORMAT = "extra.FORMAT";

    private final Handler mHandler = new Handler();
    private final HashMap<String, Integer> mHashMap = new HashMap<>();
    private final StringBuilder mStringBuilder = new StringBuilder();
    private final Formatter mFormatter = new Formatter(mStringBuilder);
    private int mNavigationBarHeight;
    private int mNavigationBarWidth;
    private boolean mScrubbing;
    private GestureDetector mVideoTouchHelper;
    private int mCurrentPlaybackIndex;
    private String[] mPlayList;
    private int mVideoFormat;
    private String mVideoId;
    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 1000 - (pos % 1000));
        }
    };
    private final Runnable mHideAction = this::hide;
    private int mCurrentPosition;


    private void hide() {
        if (mController.getVisibility() == View.VISIBLE) {
            mController.setVisibility(View.GONE);
            mHandler.removeCallbacks(mHideAction);
            hideSystemUI(this, false);
            mHandler.removeCallbacks(mProgressChecker);
        }
    }

    private void hideController() {
        mHandler.postDelayed(mHideAction, DEFAULT_SHOW_TIMEOUT_MS);
    }

    private void initializePlayer() {
        mExoDelete.setVisibility(View.GONE);
        hideController();
        if (!loadPlayList())
            return;
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this::playbackError);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
    }

    //
    private boolean playbackError(MediaPlayer mp, int what, int extra) {
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
        new AlertDialog.Builder(this)
                .setTitle("错误")
                .setMessage(String.format("%s %s", whatString, extraString))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
        return true;
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


    private void playPlayList(int index) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Referer", "https://v.qq.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Mobile Safari/537.36");
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("正在加载...");
        dialog.show();
        new Thread(() -> {
            String uri = mPlayList[index];
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String key = getAuthorizationKey(uri);
            runOnUiThread(() -> {
                dialog.dismiss();
                Log.e("B5aOx2", String.format("playPlayList, %s", key));
                mPlayer.setVideoURI(Uri.parse(key), headers);
            });
        }).start();
    }

    private int setProgress() {
        int position = mPlayer.getCurrentPosition();
        mExoDuration.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, mPlayer.getDuration()));
        mExoPosition.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, position));
        mExoProgress.setPosition(position);
        return position;
    }

    private void setupView() {
        mRootView.setOnTouchListener((v, event) -> {
            mVideoTouchHelper.onTouchEvent(event);
            return true;
        });
        mExoProgress.addListener(this);
        mExoPlay.setOnClickListener(v -> {
            switchPlayState(mPlayer, mExoPlay);
        });
        mExoPrev.setOnClickListener(v -> {
            mCurrentPlaybackIndex = previous(mCurrentPlaybackIndex);
        });
        mExoNext.setOnClickListener(v -> {
            mCurrentPlaybackIndex = next(mCurrentPlaybackIndex);
        });
        mExoRew.setOnClickListener(v -> {
            rotateScreen(this);
        });
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        }
        mFileDownload.setOnClickListener(v -> {
            this.downloadVideos();
        });
    }

    private void show() {
        if (mController.getVisibility() == View.VISIBLE) return;
        mController.setVisibility(View.VISIBLE);
        showSystemUI(this, true);
        if (mIsHasBar) {
            PlayerHelper.adjustController(this, mController, mNavigationBarHeight, mNavigationBarWidth);
        }
        mHandler.post(mProgressChecker);
        hideController();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.stopPlayback();
    }

    @Override
    protected void onPause() {
        mPlayer.suspend();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayer.resume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    void initialize() {
        super.initialize();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Point point = getNavigationBarSize(this);
        mNavigationBarHeight = getNavigationBarHeight(this);
        mNavigationBarWidth = point.x;
        setupView();
        mVideoTouchHelper = new GestureDetector(this, this);
        mVideoTouchHelper.setContextClickListener(e -> false);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mExoProgress.setBufferedPosition((long) (mPlayer.getDuration() * (1f * percent / 100)));
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mExoPlay.setImageResource(R.drawable.exo_controls_play);
        mCurrentPlaybackIndex = next(mCurrentPlaybackIndex);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        String whatString = Integer.toString(what);
        switch (what) {
            case MediaPlayer.MEDIA_INFO_UNKNOWN: {
                whatString = "MEDIA_INFO_UNKNOWN";
            }
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING: {
                whatString = "MEDIA_INFO_VIDEO_TRACK_LAGGING";
            }
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START: {
                whatString = "MEDIA_INFO_VIDEO_RENDERING_START";
            }
            case MediaPlayer.MEDIA_INFO_BUFFERING_START: {
                whatString = "MEDIA_INFO_BUFFERING_START";
            }
            case MediaPlayer.MEDIA_INFO_BUFFERING_END: {
                whatString = "MEDIA_INFO_BUFFERING_END";
            }
            case 703: {
                whatString = "MEDIA_INFO_NETWORK_BANDWIDTH";
            }
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING: {
                whatString = "MEDIA_INFO_BAD_INTERLEAVING";
            }
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE: {
                whatString = "MEDIA_INFO_NOT_SEEKABLE";
            }
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE: {
                whatString = "MEDIA_INFO_METADATA_UPDATE";
            }
            case MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE: {
                whatString = "MEDIA_INFO_UNSUPPORTED_SUBTITLE";
            }
            case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT: {
                whatString = "MEDIA_INFO_SUBTITLE_TIMED_OUT";
            }
        }
        Log.e("B5aOx2", String.format("onInfo, %s \n extra = %d", whatString, extra));
        if (extra > 0) {
            mCurrentPosition = mPlayer.getCurrentPosition();
            playPlayList(mCurrentPlaybackIndex);
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mExoProgress.setDuration(mp.getDuration());
        mHandler.post(mProgressChecker);
        mPlayer.start();
        mExoPlay.setImageResource(R.drawable.exo_controls_pause);
        if (mCurrentPosition != 0) {
            mPlayer.seekTo(mCurrentPosition);
            mCurrentPosition = 0;
        }
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
    public void onScrubMove(TimeBar timeBar, long position) {
        mExoPosition.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, position));
    }

    @Override
    public void onScrubStart(TimeBar timeBar, long position) {
        mHandler.removeCallbacks(mHideAction);
        mHandler.removeCallbacks(mProgressChecker);
        mScrubbing = true;
    }

    @Override
    public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
        mScrubbing = false;
        mPlayer.seekTo((int) position);
        mHandler.post(mProgressChecker);
        hideController();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        show();
        return true;
    }

    @Override
    public void onTimedMetaDataAvailable(MediaPlayer mp, TimedMetaData data) {
    }

    @Override
    public void onTimedText(MediaPlayer mp, TimedText text) {
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
    }

    @Override
    public void onVideoUri(String uri) {
        runOnUiThread(() -> {
            if (uri != null) {
                mPlayer.setVideoPath(uri);
            } else {
                Toast.makeText(TencentActivity.this, "无法解析视频", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    int next(int currentPlaybackIndex) {
        int nextPlaybackIndex = mPlayList.length > currentPlaybackIndex + 1 ? currentPlaybackIndex + 1 : 0;
        playPlayList(nextPlaybackIndex);
        return nextPlaybackIndex;
    }

    int previous(int currentPlaybackIndex) {
        int nextPlaybackIndex = currentPlaybackIndex - 1 > -1 ? currentPlaybackIndex - 1 : 0;
        playPlayList(nextPlaybackIndex);
        return nextPlaybackIndex;
    }

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
                    downloaderTask.Uri = getAuthorizationKey(uri);
                    downloaderTask.Directory = dir.getAbsolutePath();
                    downloaderTask.FileName = String.format("%02d-%s.mp4", atomicInteger.incrementAndGet(), KeyShare.md5(uri));
                    downloadTaskDatabase.insertDownloadTask(downloaderTask);
                    ft.run();
                });
                try {
                    ft.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
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
}
