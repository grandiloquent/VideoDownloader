package euphoria.psycho.player;

import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.util.Formatter;

import euphoria.psycho.explorer.R;
import euphoria.psycho.share.DateTimeShare;

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
        VideoTouchHelper.Listener,
        TimeBar.OnScrubListener {
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
    private VideoTouchHelper mVideoTouchHelper;
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
            throw new IllegalStateException();
        }
        mCurrentPlaybackIndex = lookupCurrentVideo(videoPath, mFiles);
        mPlayer.setVideoPath(mFiles[mCurrentPlaybackIndex].getAbsolutePath());
        mPlayer.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mExoContentFrame.setAspectRatio(mp.getVideoWidth() * 1.0f / mp.getVideoHeight());
                mExoProgress.setDuration(mp.getDuration());
                mHandler.post(mProgressChecker);
                mPlayer.start();
                seekToLastedState();
                mExoPlay.setImageResource(R.drawable.exo_controls_pause);
            }
        });

    }

    private void savePosition() {
        String uri = mFiles[mCurrentPlaybackIndex].getAbsolutePath();
        if (mPlayer == null) return;
        if (mPlayer.getDuration() - mPlayer.getCurrentPosition() > 60 * 1000)
            mBookmarker.setBookmark(uri, (int) mPlayer.getCurrentPosition());
    }

    private void seekToLastedState() {
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
        mRootView.setOnTouchListener((v, event) -> mVideoTouchHelper.onTouch(event));
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
    protected void onStart() {
        super.onStart();
        initializePlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayer.resume();
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
        mVideoTouchHelper = new VideoTouchHelper(this, this);
    }

    @Override
    public boolean onScroll(float distanceX, float distanceY) {
        int delta = (int) distanceX * -100;
        int positionMs = delta + mPlayer.getCurrentPosition();
        if (positionMs > 0 && Math.abs(delta) > 1000)
            mPlayer.seekTo(positionMs);
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
    public void onSingleTapConfirmed() {
        show();
    }
}