package euphoria.psycho;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import android.widget.ImageButton;
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
    private ImageButton mActionFullscreen;
    private ImageButton mActionFileDownload;
    private Button mExoFfwdWithAmount;
    private Button mExoRewWithAmount;

    static int calculateScreenOrientation(Activity activity) {
        int displayRotation = getDisplayRotation(activity);
        boolean standard = displayRotation < 180;
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (standard)
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            else return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        } else {
            if (displayRotation == 90 || displayRotation == 270) {
                standard = !standard;
            }
            return standard ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
    }

    static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    static int getNavigationBarHeight(Context context) {
        Resources res = context.getResources();
        int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return res.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    static void hideSystemUI(Activity activity, boolean toggleActionBarVisibility) {
        if (toggleActionBarVisibility && activity.getActionBar() != null) {
            activity.getActionBar().hide();
        }
        activity.getWindow().getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                                View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    static void rotateScreen(Activity activity) {
        int orientation = calculateScreenOrientation(activity);
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    static void showSystemUI(Activity activity, boolean toggleActionBarVisibility) {
        if (toggleActionBarVisibility) {
            android.app.ActionBar actionBar = activity.getActionBar();
            if (actionBar != null)
                actionBar.show();
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

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

    private void onActionFileDownload(View view) {
    }

    private void onActionFullscreen(View view) {
        int orientation = calculateScreenOrientation(this);
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            int videoWidth = mMediaPlayer.getVideoWidth();
            int videoHeight = mMediaPlayer.getVideoHeight();
            double ratio = mRoot.getMeasuredWidth() / (videoHeight * 1.0);
            int width = (int) (ratio * videoWidth);
            int left = (mRoot.getMeasuredHeight() - width) >> 1;
            FrameLayout.LayoutParams layoutParams = new LayoutParams(width, getResources().getDisplayMetrics().widthPixels);
            layoutParams.leftMargin = left;
//        Log.e("B5aOx2", String.format("onActionFullscreen, mMediaPlayer.getVideoWidth() = %s;\n mMediaPlayer.getVideoHeight() = %s;\n getResources().getDisplayMetrics().widthPixels = %s;\n getResources().getDisplayMetrics().heightPixels = %s;\n mRoot.getMeasuredWidth() = %s;\n mRoot.getMeasuredHeight() = %s;\n ratio = %s\n left = %s",
//                mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight(), getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels, mRoot.getMeasuredWidth(), mRoot.getMeasuredHeight(), ratio, left
//        ));
            mTextureView.setLayoutParams(layoutParams);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            int videoWidth = mMediaPlayer.getVideoWidth();
            int videoHeight = mMediaPlayer.getVideoHeight();
            double ratio = mRoot.getMeasuredHeight() / (videoWidth * 1.0);
            int height = (int) (ratio * videoHeight);
            int top = (mRoot.getMeasuredWidth() - height) >> 1;
            FrameLayout.LayoutParams layoutParams = new LayoutParams(mRoot.getMeasuredHeight(), height);
            layoutParams.topMargin = top;
            mTextureView.setLayoutParams(layoutParams);
            Log.e("B5aOx2", String.format("onActionFullscreen, mMediaPlayer.getVideoWidth() = %s;\n mMediaPlayer.getVideoHeight() = %s;\n getResources().getDisplayMetrics().widthPixels = %s;\n getResources().getDisplayMetrics().heightPixels = %s;\n mRoot.getMeasuredWidth() = %s;\n mRoot.getMeasuredHeight() = %s;\n ratio = %s\n left = %s",
                    mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight(), getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels, mRoot.getMeasuredWidth(), mRoot.getMeasuredHeight(), ratio, top
            ));
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
        mRoot = findViewById(R.id.root);
        mRoot.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE);
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // TODO: The system bars are visible. Make any desired
                            // adjustments to your UI, such as showing the action bar or
                            // other navigational controls.
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mRoot.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                            View.SYSTEM_UI_FLAG_LOW_PROFILE |
                                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                                            View.SYSTEM_UI_FLAG_IMMERSIVE);
                                }
                            }, 5000);
                        } else {
                            // TODO: The system bars are NOT visible. Make any desired
                            // adjustments to your UI, such as hiding the action bar or
                            // other navigational controls.
                        }
                    }
                });
        mTextureView = findViewById(R.id.texture_view);
        mPosition = findViewById(R.id.position);
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
            public void onScrubMove(TimeBar timeBar, long position) {
                mPosition.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, position));
            }

            @Override
            public void onScrubStart(TimeBar timeBar, long position) {
                mHandler.removeCallbacks(null);
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
        mActionFileDownload = findViewById(R.id.action_file_download);
        mActionFileDownload.setOnClickListener(this::onActionFileDownload);
        mActionFileDownload.setAlpha(75);
        mActionFullscreen = findViewById(R.id.action_fullscreen);
        mActionFullscreen.setOnClickListener(this::onActionFullscreen);

    }

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
 