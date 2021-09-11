package euphoria.psycho.player;

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
import android.os.Handler;
import android.view.GestureDetector;
import android.view.GestureDetector.OnContextClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.util.Formatter;

import euphoria.psycho.explorer.R;
import euphoria.psycho.share.DateTimeShare;
import euphoria.psycho.share.Logger;

import static euphoria.psycho.player.PlayerHelper.getNavigationBarHeight;
import static euphoria.psycho.player.PlayerHelper.getNavigationBarSize;
import static euphoria.psycho.player.PlayerHelper.getVideos;
import static euphoria.psycho.player.PlayerHelper.hideSystemUI;
import static euphoria.psycho.player.PlayerHelper.lookupCurrentVideo;
import static euphoria.psycho.player.PlayerHelper.openDeleteVideoDialog;
import static euphoria.psycho.player.PlayerHelper.playNextVideo;
import static euphoria.psycho.player.PlayerHelper.playPreviousVideo;
import static euphoria.psycho.player.PlayerHelper.rotateScreen;
import static euphoria.psycho.player.PlayerHelper.showSystemUI;
import static euphoria.psycho.player.PlayerHelper.switchPlayState;

// https://github.com/google/ExoPlayer
public class VideoActivity extends BaseVideoActivity implements
        GestureDetector.OnGestureListener,
        TimeBar.OnScrubListener,
        OnPreparedListener, OnCompletionListener, OnBufferingUpdateListener, OnSeekCompleteListener, OnVideoSizeChangedListener, OnTimedTextListener, OnTimedMetaDataAvailableListener, OnErrorListener, OnInfoListener {
    public static final long DEFAULT_SHOW_TIMEOUT_MS = 5000L;
    private final Handler mHandler = new Handler();
    private final StringBuilder mStringBuilder = new StringBuilder();
    private final Formatter mFormatter = new Formatter(mStringBuilder);
    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 1000 - (pos % 1000));
        }
    };
    private final Runnable mHideAction = this::hide;
    private Bookmarker mBookmarker;
    private File[] mFiles = null;
    private int mNavigationBarHeight;
    private int mNavigationBarWidth;
    private boolean mScrubbing;
    private GestureDetector mVideoTouchHelper;
    private int mCurrentPlaybackIndex;

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
        hideController();
        if (getIntent().getData() == null) {
            return;
        }
        String videoPath = getIntent().getData().getPath();
        mFiles = getVideos(videoPath);
        if (mFiles == null) {
            mPlayer.setVideoURI(getIntent().getData());
        }
        else {
            mCurrentPlaybackIndex = lookupCurrentVideo(videoPath, mFiles);
            mPlayer.setVideoPath(mFiles[mCurrentPlaybackIndex].getAbsolutePath());
        }
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnInfoListener(this);
    }

    private void savePosition() {
        String uri = mFiles[mCurrentPlaybackIndex].getAbsolutePath();
        if (mPlayer == null) return;
        if (mPlayer.getDuration() - mPlayer.getCurrentPosition() > 60 * 1000)
            mBookmarker.setBookmark(uri, (int) mPlayer.getCurrentPosition());
    }

    private void seekToLastedState() {
        if(mFiles==null)return;
        String uri = mFiles[mCurrentPlaybackIndex].getAbsolutePath();
        long bookmark = mBookmarker.getBookmark(uri);
        if (bookmark > 0) {
            mPlayer.seekTo((int) bookmark);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(Utils.getFileName(uri));
        }
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
            return  true;
        });
        mExoProgress.addListener(this);
        mExoPlay.setOnClickListener(v -> {
            switchPlayState(mPlayer, mExoPlay);
        });
        mExoPrev.setOnClickListener(v -> {
            savePosition();
            mCurrentPlaybackIndex = playPreviousVideo(mCurrentPlaybackIndex, mPlayer, mFiles);
        });
        mExoNext.setOnClickListener(v -> {
            savePosition();
            mCurrentPlaybackIndex = playNextVideo(mCurrentPlaybackIndex, mPlayer, mFiles);
        });
        //
        mExoRew.setOnClickListener(v -> {
            rotateScreen(this);
        });
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        }
        mExoDelete.setOnClickListener(v -> {
            openDeleteVideoDialog(this, unused -> {
                if (mFiles.length > 1) {
                    int nextPlaybackIndex = mCurrentPlaybackIndex + 1;
                    if (mCurrentPlaybackIndex >= mFiles.length) {
                        nextPlaybackIndex = 0;
                    }
                    String nextVideoPath = mFiles[nextPlaybackIndex].getAbsolutePath();
                    if (!mFiles[mCurrentPlaybackIndex].delete()) {
                        throw new IllegalStateException();
                    }
                    mFiles = getVideos(nextVideoPath);
                    mCurrentPlaybackIndex = lookupCurrentVideo(nextVideoPath, mFiles);
                    mPlayer.setVideoPath(nextVideoPath);
                } else {
                    if (!mFiles[mCurrentPlaybackIndex].delete()) {
                        throw new IllegalStateException();
                    }
                }
                return null;
            });
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
        savePosition();
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
        mBookmarker = new Bookmarker(this);
        setupView();
        mVideoTouchHelper = new GestureDetector(this, this);
        mVideoTouchHelper.setContextClickListener(new OnContextClickListener() {
            @Override
            public boolean onContextClick(MotionEvent e) {
                return false;
            }
        });
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mExoPlay.setImageResource(R.drawable.exo_controls_play);
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
        seekToLastedState();
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
}