package euphoria.psycho.player;

import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.util.Formatter;
import java.util.List;

import euphoria.psycho.share.DateTimeShare;

import static euphoria.psycho.player.PlayerHelper.getNavigationBarHeight;
import static euphoria.psycho.player.PlayerHelper.getNavigationBarSize;
import static euphoria.psycho.player.PlayerHelper.hideSystemUI;
import static euphoria.psycho.player.PlayerHelper.rotateScreen;
import static euphoria.psycho.player.PlayerHelper.showSystemUI;

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
    private List<File> mFiles = null;
    private int mNavigationBarHeight;
    private int mNavigationBarWidth;
    private boolean mScrubbing;
    private VideoTouchHelper mVideoTouchHelper;

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
        mTextureVideoView.setVideoURI(getIntent().getData());
        mTextureVideoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mExoContentFrame.setAspectRatio(mp.getVideoWidth() * 1.0f / mp.getVideoHeight());
                mExoProgress.setDuration(mp.getDuration());
                mHandler.post(mProgressChecker);
                mTextureVideoView.start();
            }
        });
        seekToLastedState();
    }

    private void savePosition() {
//        String uri = getCurrentUri();
//        if (uri == null || mPlayer == null) return;
//        if (mPlayer.getDuration() - mPlayer.getCurrentPosition() > 60 * 1000)
//            mBookmarker.setBookmark(uri, (int) mPlayer.getCurrentPosition());
    }

    private void seekToLastedState() {
//        mStartWindow = mPlayer.getCurrentWindowIndex();
//        String uri = getCurrentUri();
//        if (uri == null) return;
//        long bookmark = mBookmarker.getBookmark(uri);
//        if (bookmark > 0) {
//            seekTo(mPlayer.getCurrentWindowIndex(), bookmark);
//        }
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setTitle(Utils.getFileName(uri));
//        }
//        updateNavigation();
    }

    private int setProgress() {
        int position = mTextureVideoView.getCurrentPosition();
        mExoDuration.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, mTextureVideoView.getDuration()));
        mExoPosition.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, position));
        mExoProgress.setPosition(position);
        return position;
    }

    private void setupView() {
        mRootView.setOnTouchListener((v, event) -> mVideoTouchHelper.onTouch(event));
        mExoProgress.addListener(this);
        mExoPlay.setOnClickListener(v -> {
            hideController();
        });
        mExoPause.setOnClickListener(v -> {
            hideController();
        });
        mExoPrev.setOnClickListener(v -> {
            hideController();
        });
        mExoNext.setOnClickListener(v -> {
            hideController();
        });
        //
        mExoRew.setOnClickListener(v -> {
            rotateScreen(this);
        });
        mExoDelete.setOnClickListener(v -> {
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
        mVideoTouchHelper = new VideoTouchHelper(this, this);
    }

    @Override
    public boolean onScroll(float distanceX, float distanceY) {
        int delta = (int) distanceX * -100;
        int positionMs = delta + mTextureVideoView.getCurrentPosition();
        if (positionMs > 0 && Math.abs(delta) > 1000)
            mTextureVideoView.seekTo(positionMs);
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
        mTextureVideoView.seekTo((int) position);
        mHandler.post(mProgressChecker);
        hideController();
    }

    @Override
    public void onSingleTapConfirmed() {
        show();
    }
}