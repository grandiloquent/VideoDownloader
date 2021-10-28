package euphoria.psycho;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.TimedMetaData;
import android.net.Uri;
import android.opengl.GLES20;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.WebViewShare;
import euphoria.psycho.tasks.HLSDownloadActivity;

import static euphoria.psycho.videos.VideosHelper.USER_AGENT;

public class PlayerActivity extends Activity {

    public static final int DEFAULT_HIDE_TIME_DELAY = 5000;
    public static final String KEY_M3U8 = "m3u8";
    public static final String KEY_REQUEST_HEADERS = "requestHeaders";
    public static final String KEY_VIDEO_FILE = "VideoFile";
    public static final String KEY_WEB_VIDEO = "WebVideo";
    private final Handler mHandler = new Handler();
    private final StringBuilder mStringBuilder = new StringBuilder();
    private final Formatter mFormatter = new Formatter(mStringBuilder);
    private TextureView mTextureView;
    private MediaPlayer mMediaPlayer;
    private Surface mSurface;
    private FrameLayout mRoot;
    private boolean mLayout = false;
    private FrameLayout mBottomBar;
    private TextView mDuration;
    private SimpleTimeBar mTimeBar;
    private TextView mPosition;
    private LinearLayout mCenterControls;
    private final Runnable mHideAction = this::hiddenControls;
    private List<String> mPlayList;
    private int mPlayIndex;
    private ImageButton mPlayPause;

    public static void launchActivity(Context context, File videoFile) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(KEY_VIDEO_FILE, videoFile.getAbsolutePath());
        context.startActivity(intent);
    }

    public static void launchActivity(Context context, String webVideo, boolean isM3u8) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(KEY_WEB_VIDEO, webVideo);
        if (isM3u8) {
            intent.putExtra(KEY_M3U8, isM3u8);
        }
        context.startActivity(intent);
    }

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

    static File[] getVideos(String videoPath) {
        if (videoPath == null) {
            return null;
        }
        File dir = new File(videoPath).getParentFile();
        if (dir == null) {
            return null;
        }
        return listVideoFiles(dir.getAbsolutePath());
    }

    static File[] listVideoFiles(String dir) {
        File directory = new File(dir);
        Pattern pattern = Pattern.compile("\\.(?:mp4|vm|crdownload)$");
        File[] files = directory.listFiles(file ->
                file.isFile() && pattern.matcher(file.getName()).find());
        if (files == null || files.length == 0) return null;
        Arrays.sort(files, (o1, o2) -> {
            final long result = o2.lastModified() - o1.lastModified();
            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        });
        return files;
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

    private void hiddenControls() {
        mTimeBar.setVisibility(View.GONE);
        mBottomBar.setVisibility(View.GONE);
        mCenterControls.setVisibility(View.GONE);
    }

    private void hideSystemUI() {
        mRoot.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void initializePlayer() {
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setOnBufferingUpdateListener(this::onBufferingUpdate);
            mMediaPlayer.setOnCompletionListener(this::onCompletion);
            mMediaPlayer.setOnErrorListener(this::onError);
            mMediaPlayer.setOnInfoListener(this::onInfo);
            mMediaPlayer.setOnPreparedListener(this::onPrepared);
            mMediaPlayer.setOnSeekCompleteListener(this::onSeekComplete);
            mMediaPlayer.setOnTimedMetaDataAvailableListener(this::onTimedMetaDataAvailable);
            mMediaPlayer.setOnVideoSizeChangedListener(this::onVideoSizeChanged);
            mMediaPlayer.setSurface(mSurface);
            play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onActionFileDownload(View view) {
        boolean isM3u8 = getIntent().getBooleanExtra(KEY_M3U8, false);
        if (!isM3u8) {
            WebViewShare.downloadFile(this,
                    KeyShare.toHex(mPlayList.get(mPlayIndex).getBytes(StandardCharsets.UTF_8))
                            + ".mp4"
                    , mPlayList.get(mPlayIndex), USER_AGENT);
        } else {
            Intent intent = new Intent(this, HLSDownloadActivity.class);
            intent.setData(Uri.parse(mPlayList.get(mPlayIndex)));
            startActivity(intent);
        }
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
        }

    }

    private void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        mTimeBar.setBufferedPosition(i);
    }

    private void onCompletion(MediaPlayer mediaPlayer) {
        Log.e("B5aOx2", "onCompletion");

    }

    private boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        Log.e("B5aOx2", "onError " + i);
        return true;
    }

    private boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        Log.e("B5aOx2", "onInfo");
        return true;
    }

    private void onNext(View view) {
        if (mPlayList.size() < 2) return;
        mLayout = false;
        if (mPlayIndex + 1 < mPlayList.size()) {
            mPlayIndex++;
        } else {
            mPlayIndex = 0;
        }
        mMediaPlayer.reset();
        try {
            play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onPlayPause(View view) {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.exo_ic_play_circle_filled));
        } else {
            mMediaPlayer.start();
            mPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.exo_ic_pause_circle_filled));
        }
    }

    private void onPrepared(MediaPlayer mediaPlayer) {
        Log.e("B5aOx2", "onPrepared");
        mDuration.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, mediaPlayer.getDuration()));
        mTimeBar.setDuration(mediaPlayer.getDuration());
        mMediaPlayer.start();
        mPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.exo_ic_pause_circle_filled));
        updateProgress();
        hiddenControls();
    }

    private void onPrev(View view) {
        if (mPlayList.size() < 2) return;
        mLayout = false;
        if (mPlayIndex - 1 > -1) {
            mPlayIndex--;
        } else {
            mPlayIndex = 0;
        }
        mMediaPlayer.reset();
        try {
            play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onRoot(View view) {
        showControls();
        scheduleHideControls();
    }

    private void onSeekComplete(MediaPlayer mediaPlayer) {
        Log.e("B5aOx2", "onSeekComplete");
    }

    private void onTimedMetaDataAvailable(MediaPlayer mediaPlayer, TimedMetaData timedMetaData) {
        Log.e("B5aOx2", "onTimedMetaDataAvailable");

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

    private void play() throws IOException {
        if (getIntent().getIntExtra(KEY_REQUEST_HEADERS, 0) == 1) {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://www.mgtv.com/");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36");
            mMediaPlayer.setDataSource(this, Uri.parse(mPlayList.get(mPlayIndex)), headers);
            mMediaPlayer.prepareAsync();
        } else {
            Log.e("B5aOx2", String.format("play, %s", mPlayList.get(mPlayIndex)));
            mMediaPlayer.setDataSource(mPlayList.get(mPlayIndex));
            mMediaPlayer.prepareAsync();
        }

    }

    private void scheduleHideControls() {
        mHandler.removeCallbacks(mHideAction);
        mHandler.postDelayed(mHideAction, DEFAULT_HIDE_TIME_DELAY);
    }

    private void showControls() {
        mTimeBar.setVisibility(View.VISIBLE);
        mBottomBar.setVisibility(View.VISIBLE);
        mCenterControls.setVisibility(View.VISIBLE);
        updateProgress();
    }

    private void updateProgress() {
        if (mMediaPlayer == null || mBottomBar.getVisibility() != View.VISIBLE) {
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
        hideSystemUI();
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (visibility -> {
                    // Note that system bars will only be "visible" if none of the
                    // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        // TODO: The system bars are visible. Make any desired
                        // adjustments to your UI, such as showing the action bar or
                        // other navigational controls.
                        mHandler.postDelayed(this::hideSystemUI, DEFAULT_HIDE_TIME_DELAY);
                    } else {
                        // TODO: The system bars are NOT visible. Make any desired
                        // adjustments to your UI, such as hiding the action bar or
                        // other navigational controls.
                    }
                });
        mCenterControls = findViewById(R.id.exo_center_controls);
        mTextureView = findViewById(R.id.texture_view);
        mPosition = findViewById(R.id.position);
        mBottomBar = findViewById(R.id.exo_bottom_bar);
        mDuration = findViewById(R.id.duration);
        mTextureView.setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
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
                mHandler.removeCallbacks(mHideAction);
                mPosition.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, position));
            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                mMediaPlayer.seekTo((int) position);
                updateProgress();
                scheduleHideControls();
            }

        });
        Button rewWithAmount = findViewById(R.id.exo_rew_with_amount);
        Typeface typeface = ResourcesCompat.getFont(this, com.google.android.exoplayer2.ui.R.font.roboto_medium_numbers);
        rewWithAmount.setTypeface(typeface);
        rewWithAmount.setText("10");
        rewWithAmount.setOnClickListener(v -> {
            int dif = mMediaPlayer.getCurrentPosition() - 10000;
            if (dif < 0) {
                dif = 0;
            }
            mMediaPlayer.seekTo(dif);
            scheduleHideControls();
            updateProgress();
        });
        Button ffwdWithAmount = findViewById(R.id.exo_ffwd_with_amount);
        ffwdWithAmount.setTypeface(typeface);
        ffwdWithAmount.setText("10");
        ffwdWithAmount.setOnClickListener(v -> {
            int dif = mMediaPlayer.getCurrentPosition() + 10000;
            if (dif > mMediaPlayer.getDuration()) {
                dif = mMediaPlayer.getDuration();
            }
            mMediaPlayer.seekTo(dif);
            scheduleHideControls();
            updateProgress();
        });
        mPlayPause = findViewById(R.id.play_pause);
        mPlayPause.setOnClickListener(this::onPlayPause);
        ImageButton actionFileDownload = findViewById(R.id.action_file_download);
        ImageButton actionFullscreen = findViewById(R.id.action_fullscreen);
        actionFullscreen.setOnClickListener(this::onActionFullscreen);
        mRoot.setOnClickListener(this::onRoot);
        ImageButton prev = findViewById(R.id.prev);
        ImageButton next = findViewById(R.id.next);
        String videoFile = getIntent().getStringExtra(KEY_VIDEO_FILE);
        if (videoFile != null) {
            actionFileDownload.setAlpha(75);
            mPlayList = Arrays.stream(getVideos(videoFile)).map(File::getAbsolutePath)
                    .collect(Collectors.toList());
            if (mPlayList.size() < 2) {
                prev.setAlpha(75);
                next.setAlpha(75);
                mPlayIndex = 0;
            } else {
                prev.setOnClickListener(this::onPrev);
                next.setOnClickListener(this::onNext);
                mPlayIndex = mPlayList.indexOf(videoFile);
            }
            return;
        }
        String webVideo = getIntent().getStringExtra(KEY_WEB_VIDEO);
        if (webVideo != null) {
            prev.setAlpha(75);
            next.setAlpha(75);
            actionFileDownload.setOnClickListener(this::onActionFileDownload);
            mPlayList = new ArrayList<>();
            mPlayList.add(webVideo);
            mPlayIndex = 0;
        }
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
