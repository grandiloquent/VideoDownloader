package euphoria.psycho.player;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import com.google.android.exoplayer2.C;

import java.io.File;
import java.util.Formatter;
import java.util.List;

import euphoria.psycho.share.DateTimeShare;
import euphoria.psycho.share.Logger;

import static com.google.android.exoplayer2.C.TIME_END_OF_SOURCE;
import static euphoria.psycho.player.PlayerHelper.getNavigationBarHeight;
import static euphoria.psycho.player.PlayerHelper.getNavigationBarSize;
import static euphoria.psycho.player.PlayerHelper.hideSystemUI;
import static euphoria.psycho.player.PlayerHelper.rotateScreen;
import static euphoria.psycho.player.PlayerHelper.showSystemUI;

// https://github.com/google/ExoPlayer
public class VideoActivity extends BaseVideoActivity implements
        View.OnLayoutChangeListener,
        VideoTouchHelper.Listener,
        TimeBar.OnScrubListener {
    public static final long DEFAULT_SHOW_TIMEOUT_MS = 5000L;
    private final Handler mHandler = new Handler();
    private final Runnable mUpdateProgressAction = this::updateProgress;
    private final Runnable mHideAction = this::hide;
    private Bookmarker mBookmarker;
    private List<File> mFiles = null;
    private int mNavigationBarHeight;
    private int mNavigationBarWidth;
    private boolean mScrubbing;
    private VideoTouchHelper mVideoTouchHelper;

    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {
        //Logger.e(String.format("onVideoSizeChanged, %sx%s %s %s", width, height, pixelWidthHeightRatio, ratio));
        // mExoContentFrame.setAspectRatio(ratio);
    }

    private static void applyTextureViewRotation(TextureView textureView,
                                                 int textureViewRotation) {
        float textureViewWidth = textureView.getWidth();
        float textureViewHeight = textureView.getHeight();
        if (textureViewWidth == 0 || textureViewHeight == 0 || textureViewRotation == 0) {
            textureView.setTransform(null);
        } else {
            Matrix transformMatrix = new Matrix();
            float pivotX = textureViewWidth / 2;
            float pivotY = textureViewHeight / 2;
            transformMatrix.postRotate(textureViewRotation, pivotX, pivotY);
            // After rotation, scale the rotated texture to fit the TextureView size.
            RectF originalTextureRect = new RectF(0, 0, textureViewWidth, textureViewHeight);
            RectF rotatedTextureRect = new RectF();
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect);
            transformMatrix.postScale(
                    textureViewWidth / rotatedTextureRect.width(),
                    textureViewHeight / rotatedTextureRect.height(),
                    pivotX,
                    pivotY);
            textureView.setTransform(transformMatrix);
        }
    }

    private static long usToMs(long us) {
        if (us == C.TIME_UNSET || us == TIME_END_OF_SOURCE) return us;
        else return us / 1000;
    }

    private void hide() {
        if (mController.getVisibility() == View.VISIBLE) {
            mController.setVisibility(View.GONE);
            mHandler.removeCallbacks(mUpdateProgressAction);
            mHandler.removeCallbacks(mHideAction);
            hideSystemUI(this, false);
        }
    }

    private void hideController() {
        mHandler.postDelayed(mHideAction, DEFAULT_SHOW_TIMEOUT_MS);
    }

    private final StringBuilder mStringBuilder = new StringBuilder();
    private final Formatter mFormatter = new Formatter(mStringBuilder);
    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {

            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 1000 - (pos % 1000));
        }
    };

    private int setProgress() {
        int position = mTextureVideoView.getCurrentPosition();
        mExoDuration.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, mTextureVideoView.getDuration()));
        mExoPosition.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, position));
        mExoProgress.setPosition(position);
        return position;
    }

    private void initializePlayer() {
        hideController();
        mTextureVideoView.setVideoURI(getIntent().getData());
        Logger.e(String.format("initializePlayer, %s", getIntent().getData()));
        mTextureVideoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Logger.e(String.format("onPrepared, %sx%s %s", mp.getVideoWidth(), mp.getVideoHeight(),
                        mp.getVideoWidth() * 1.0f / mp.getVideoHeight()));
                mExoContentFrame.setAspectRatio(mp.getVideoWidth() * 1.0f / mp.getVideoHeight());
                mExoContentFrame.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                Logger.e(String.format("onPrepared, %sx%s", mExoContentFrame.getMeasuredWidth(), mExoContentFrame.getMeasuredHeight()));
                mExoProgress.setDuration(mp.getDuration());
                mHandler.post(mProgressChecker);
                mTextureVideoView.start();
            }
        });
        updateAll();
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

    private void setButtonEnabled(boolean enabled, View view) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.3f);
        view.setVisibility(View.VISIBLE);
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
        updateProgress();
        updateNavigation();
        showSystemUI(this, true);
        if (mIsHasBar) {
            PlayerHelper.adjustController(this, mController, mNavigationBarHeight, mNavigationBarWidth);
        }
        hideController();
    }

    private void updateAll() {
        updateProgress();
        updateNavigation();
        seekToLastedState();
    }

    private void updateNavigation() {
//
//        setButtonEnabled(enablePrevious, mExoPrev);
//        setButtonEnabled(enableNext, mExoNext);
//        setButtonEnabled(true, mExoRew);
//        mExoProgress.setEnabled(isSeekable);
    }

    private void updateProgress() {
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
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                               int oldTop, int oldRight, int oldBottom) {
        //applyTextureViewRotation((TextureView) v, mTextureViewRotation);
        Logger.e(String.format("onLayoutChange, %s %s %s %s", left, top, right, bottom));
    }

    //
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
