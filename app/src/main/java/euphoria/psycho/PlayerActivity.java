package euphoria.psycho;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.DrmInfo;
import android.media.MediaTimestamp;
import android.media.SubtitleData;
import android.media.TimedMetaData;
import android.media.TimedText;
import android.opengl.GLES20;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

import java.io.IOException;
import java.util.Formatter;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import euphoria.psycho.explorer.R;
import euphoria.psycho.player.TimeBar;
import euphoria.psycho.player.TimeBar.OnScrubListener;
import euphoria.psycho.share.DateTimeShare;

public class PlayerActivity extends Activity {

    TextureView mTextureView;
    MediaPlayer mMediaPlayer;
    Surface mSurface;
    SurfaceTexture mSurfaceTexture;
    private FrameLayout mRoot;
    private boolean mLayout = false;
    StringBuilder mStringBuilder = new StringBuilder();
    Formatter mFormatter = new Formatter(mStringBuilder);
    private FrameLayout mExoBottomBar;
    private TextView mDuration;
    private SimpleTimeBar mTimeBar;
    private Handler mHandler = new Handler();
    private TextView mPosition;

    private void clearSurface() {
        if (mSurface == null) {
            return;
        }
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);
        int[] attribList = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, EGL10.EGL_WINDOW_BIT,
                EGL10.EGL_NONE, 0,      // placeholder for recordable [@-3]
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        egl.eglChooseConfig(display, attribList, configs, configs.length, numConfigs);
        EGLConfig config = configs[0];
        EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, new int[]{
                12440, 2, EGL10.EGL_NONE
        });
        EGLSurface eglSurface = egl.eglCreateWindowSurface(display, config, mSurface, new int[]{
                EGL10.EGL_NONE
        });
        egl.eglMakeCurrent(display, eglSurface, eglSurface, context);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        egl.eglSwapBuffers(display, eglSurface);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroyContext(display, context);
        egl.eglTerminate(display);
    }

    private void initializePlayer() {
        mMediaPlayer = new MediaPlayer();
        try {
            if (VERSION.SDK_INT >= VERSION_CODES.P) {
                mMediaPlayer.setOnMediaTimeDiscontinuityListener(this::onMediaTimeDiscontinuity);
                mMediaPlayer.setOnSubtitleDataListener(this::onSubtitleData);
                mMediaPlayer.setOnDrmInfoListener(this::onDrmInfo);
                mMediaPlayer.setOnDrmPreparedListener(this::onDrmPrepared);
            }
            mMediaPlayer.setOnBufferingUpdateListener(this::onBufferingUpdate);
            mMediaPlayer.setOnCompletionListener(this::onCompletion);
            mMediaPlayer.setOnErrorListener(this::onError);
            mMediaPlayer.setOnInfoListener(this::onInfo);
            mMediaPlayer.setOnPreparedListener(this::onPrepared);
            mMediaPlayer.setOnSeekCompleteListener(this::onSeekComplete);
            mMediaPlayer.setOnTimedMetaDataAvailableListener(this::onTimedMetaDataAvailable);
            mMediaPlayer.setOnTimedTextListener(this::onTimedText);
            mMediaPlayer.setOnVideoSizeChangedListener(this::onVideoSizeChanged);
            mMediaPlayer.setDataSource(this, getIntent().getData());
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        Log.e("B5aOx2", "onBufferingUpdate");

    }

    private void onCompletion(MediaPlayer mediaPlayer) {
        Log.e("B5aOx2", "onCompletion");

    }

    private void onDrmInfo(MediaPlayer mediaPlayer, DrmInfo drmInfo) {
        Log.e("B5aOx2", "onDrmInfo");

    }

    private void onDrmPrepared(MediaPlayer mediaPlayer, int i) {
        Log.e("B5aOx2", "onDrmPrepared");

    }

    private boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        Log.e("B5aOx2", "onError");
        return true;
    }

    private boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        Log.e("B5aOx2", "onInfo");
        return true;
    }

    private void onMediaTimeDiscontinuity(MediaPlayer mediaPlayer, MediaTimestamp mediaTimestamp) {
    }

    private void onPrepared(MediaPlayer mediaPlayer) {
        Log.e("B5aOx2", "onPrepared");
        mDuration.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, mediaPlayer.getDuration()));
        mTimeBar.setDuration(mediaPlayer.getDuration());
        mMediaPlayer.start();
        updateProgress();
    }

    private static ObjectAnimator ofTranslationY(float startValue, float endValue, View target) {
        return ObjectAnimator.ofFloat(target, "translationY", startValue, endValue);
    }

    private void onSeekComplete(MediaPlayer mediaPlayer) {
        Log.e("B5aOx2", "onSeekComplete");

    }

    private void onSubtitleData(MediaPlayer mediaPlayer, SubtitleData subtitleData) {
        Log.e("B5aOx2", "onSubtitleData");

    }

    private void onTimedMetaDataAvailable(MediaPlayer mediaPlayer, TimedMetaData timedMetaData) {
        Log.e("B5aOx2", "onTimedMetaDataAvailable");

    }

    private void onTimedText(MediaPlayer mediaPlayer, TimedText timedText) {
        Log.e("B5aOx2", "onTimedText");

    }

    private void onVideoSizeChanged(MediaPlayer mediaPlayer, int videoWidth, int videoHeight) {
        if (mLayout) return;
        mLayout = true;
        double ratio = mRoot.getMeasuredWidth() / (videoWidth * 1.0);
        int height = (int) (ratio * videoHeight);
        int top = (mRoot.getMeasuredHeight() - height) >> 1;
        FrameLayout.LayoutParams layoutParams = new LayoutParams(mRoot.getMeasuredWidth(), height);
        layoutParams.topMargin = top;
        mTextureView.setLayoutParams(layoutParams);
    }

    private void updateProgress() {
        if (mMediaPlayer == null) {
            return;
        }
        mTimeBar.setPosition(mMediaPlayer.getCurrentPosition());
        mPosition.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, mMediaPlayer.getCurrentPosition()));
        mHandler.postDelayed(this::updateProgress, 1000);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        mTextureView = findViewById(R.id.texture_view);
        mPosition = findViewById(R.id.position);
        mRoot = findViewById(R.id.root);
        mExoBottomBar = findViewById(R.id.exo_bottom_bar);
        mDuration = findViewById(R.id.duration);
        mTextureView.setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                mSurfaceTexture = surface;
                mSurface = new Surface(surface);
                initializePlayer();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });
        mTimeBar = findViewById(R.id.timebar);
        mTimeBar.addListener(new OnScrubListener() {
            @Override
            public void onScrubStart(TimeBar timeBar, long position) {
                mHandler.removeCallbacks(null);
                mPosition.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, position));
            }

            @Override
            public void onScrubMove(TimeBar timeBar, long position) {
                mPosition.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, position));
            }


            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                mMediaPlayer.seekTo((int) position);
                updateProgress();
            }
        });
        mExoRewWithAmount = findViewById(R.id.exo_rew_with_amount);
        Typeface typeface = ResourcesCompat.getFont(this, com.google.android.exoplayer2.ui.R.font.roboto_medium_numbers);
        mExoRewWithAmount.setTypeface(typeface);
        mExoRewWithAmount.setText("5");
        mExoRewWithAmount.setOnClickListener(v -> {
            mHandler.removeCallbacks(null);
            int dif = mMediaPlayer.getCurrentPosition() - 5000;
            if (dif < 0) {
                dif = 0;
            }
            mMediaPlayer.seekTo(dif);
            updateProgress();
        });
        mExoFfwdWithAmount = findViewById(R.id.exo_ffwd_with_amount);
        mExoFfwdWithAmount.setTypeface(typeface);
        mExoFfwdWithAmount.setText("15");
        mExoFfwdWithAmount.setOnClickListener(v -> {
            mHandler.removeCallbacks(null);
            int dif = mMediaPlayer.getCurrentPosition() + 15000;
            if (dif > mMediaPlayer.getDuration()) {
                dif = mMediaPlayer.getDuration();
            }
            mMediaPlayer.seekTo(dif);
            updateProgress();
        });

    }

    private Button mExoFfwdWithAmount;

    private Button mExoRewWithAmount;

    @Override
    protected void onStart() {
        super.onStart();
        if (mSurface != null)
            initializePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeCallbacks(null);
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        clearSurface();
    }
}
