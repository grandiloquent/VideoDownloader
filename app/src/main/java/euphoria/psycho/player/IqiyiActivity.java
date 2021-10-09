package euphoria.psycho.player;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnTimedMetaDataAvailableListener;
import android.media.MediaPlayer.OnTimedTextListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.TimedMetaData;
import android.media.TimedText;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.HashMap;

import euphoria.psycho.explorer.R;
import euphoria.psycho.share.DateTimeShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.WebViewShare;
import euphoria.psycho.videos.Iqiyi;

import static euphoria.psycho.player.PlayerHelper.getNavigationBarHeight;
import static euphoria.psycho.player.PlayerHelper.getNavigationBarSize;
import static euphoria.psycho.player.PlayerHelper.hideSystemUI;
import static euphoria.psycho.player.PlayerHelper.rotateScreen;
import static euphoria.psycho.player.PlayerHelper.showSystemUI;
import static euphoria.psycho.player.PlayerHelper.switchPlayState;
import static euphoria.psycho.videos.VideosHelper.USER_AGENT;

public class IqiyiActivity extends BaseVideoActivity implements
        GestureDetector.OnGestureListener,
        TimeBar.OnScrubListener,
        OnPreparedListener,
        OnCompletionListener,
        OnBufferingUpdateListener,
        OnSeekCompleteListener,
        OnVideoSizeChangedListener,
        OnTimedTextListener,
        OnTimedMetaDataAvailableListener,
        OnErrorListener,
        OnInfoListener,
        Iqiyi.Callback {
    public static final long DEFAULT_SHOW_TIMEOUT_MS = 5000L;
    public static final String EXTRA_PLAYLSIT = "extra.PLAYLSIT";
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
    private int mDuration;
    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 1000 - (pos % 1000));
        }
    };
    private final Runnable mHideAction = this::hide;

    private void downloadFile(DownloadManager manager, String url, String filename, String mimetype) {
        final DownloadManager.Request request;
        Uri uri = Uri.parse(url);
        request = new DownloadManager.Request(uri);
        request.setMimeType(mimetype);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        manager.enqueue(request);
    }

    private void downloadVideo() {
        if (mPlayList != null) {
            executeTask(mPlayList);
        } else {
            Uri videoUri = getIntent().getData();
            if (videoUri.toString().contains("m3u8")) {
                Intent intent = new Intent(IqiyiActivity.this, euphoria.psycho.tasks.VideoActivity.class);
                intent.setData(videoUri);
                IqiyiActivity.this.startActivity(intent);
            } else {
                WebViewShare.downloadFile(this, KeyShare.toHex(videoUri.toString().getBytes(StandardCharsets.UTF_8)), videoUri.toString(), USER_AGENT);
            }
        }
    }

    private void executeTask(String[] videoUris) {
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        for (String uris : videoUris) {
            downloadFile(manager, uris, KeyShare.md5(uris) + ".f4v", "video/x-f4v");
        }
    }

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
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
    }

    private boolean loadPlayList() {
        mPlayList = getIntent().getStringArrayExtra(EXTRA_PLAYLSIT);
        if (mPlayList != null) {
            playPlayList(mCurrentPlaybackIndex);
            return true;
        }
        return false;
    }

    private void playPlayList(int index) {
        if (mPlayList[index].contains("http://data.video.iqiyi.com")) {
            Iqiyi.getVideoAddress(mPlayList[index], this);
        }
        mPlayer.setVideoPath(mPlayList[index]);
    }

    private int setProgress() {
        int position = mPlayer.getCurrentPosition();
        mExoDuration.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, mDuration));
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
            this.downloadVideo();
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
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Logger.e(String.format("onError, %s %s", what, extra));
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Logger.e(String.format("onInfo, %s %s", what, extra));
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mHashMap.put(mPlayList[mCurrentPlaybackIndex], mp.getDuration());
        mDuration = mHashMap.values().stream().mapToInt(integer -> integer).sum();
        mExoProgress.setDuration(mDuration);
        mHandler.post(mProgressChecker);
        mPlayer.start();
        mExoPlay.setImageResource(R.drawable.exo_controls_pause);
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
        runOnUiThread(() -> mPlayer.setVideoPath(uri));
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

}