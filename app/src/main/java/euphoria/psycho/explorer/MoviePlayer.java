package euphoria.psycho.explorer;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Virtualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.VideoView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import euphoria.psycho.share.DateTimeShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.utils.ApiHelper;
import euphoria.psycho.utils.BlobCache;
import euphoria.psycho.utils.CacheManager;

public class MoviePlayer implements
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
        ControllerOverlay.Listener {
    @SuppressWarnings("unused")
    private static final String TAG = "MoviePlayer";

    private static final String KEY_VIDEO_POSITION = "video-position";
    private static final String KEY_RESUMEABLE_TIME = "resumeable-timeout";

    // These are constants in KeyEvent, appearing on API level 11.
    private static final int KEYCODE_MEDIA_PLAY = 126;
    private static final int KEYCODE_MEDIA_PAUSE = 127;

    // Copied from MediaPlaybackService in the Music Player app.
    private static final String SERVICECMD = "com.android.music.musicservicecommand";
    private static final String CMDNAME = "command";
    private static final String CMDPAUSE = "pause";
    private static final int TOUCH_FLAG_SEEK = 1 << 2;

    private static final String VIRTUALIZE_EXTRA = "virtualize";
    private static final long BLACK_TIMEOUT = 500;

    // If we resume the acitivty with in RESUMEABLE_TIMEOUT, we will keep playing.
    // Otherwise, we pause the player.
    private static final long RESUMEABLE_TIMEOUT = 3 * 60 * 1000; // 3 mins

    private Context mContext;
    private final VideoView mVideoView;
    private final Bookmarker mBookmarker;
    private final Uri mUri;
    private final Handler mHandler = new Handler();
    private final AudioBecomingNoisyReceiver mAudioBecomingNoisyReceiver;
    private final MovieControllerOverlay mController;

    private long mResumeableTime = Long.MAX_VALUE;
    private int mVideoPosition = 0;
    private boolean mHasPaused = false;
    private int mLastSystemUiVis = 0;

    // If the time bar is being dragged.
    private boolean mDragging;

    // If the time bar is visible.
    private boolean mShowing;

    private Virtualizer mVirtualizer;

    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_VOLUME = 1;
    private static final int TOUCH_BRIGHTNESS = 2;
    private static final int TOUCH_MOVE = 3;
    private static final int TOUCH_SEEK = 4;
    private static final int TOUCH_IGNORE = 5;


    private final Runnable mPlayingChecker = new Runnable() {
        @Override
        public void run() {
            if (mVideoView.isPlaying()) {
                mController.showPlaying();
            } else {
                mHandler.postDelayed(mPlayingChecker, 250);
            }
        }
    };

    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 1000 - (pos % 1000));
        }
    };

    private float mNumberOfTaps;
    private float mLastTapTimeMs;
    private float mTouchDownMs;
    private int mTouchAction;
    private float mInitTouchY;
    private float mInitTouchX;
    private float mTouchY;
    private float mTouchX;
    private boolean mVerticalTouchActive;
    private float mLastMove;

    public MoviePlayer(View rootView, final MovieActivity movieActivity,
                       Uri videoUri, Bundle savedInstance, boolean canReplay) {
        mContext = movieActivity.getApplicationContext();
        mVideoView = rootView.findViewById(R.id.surface_view);
        DisplayMetrics metrics = movieActivity.getResources().getDisplayMetrics();
        mBookmarker = new Bookmarker(movieActivity);
        mUri = videoUri;
        mController = new MovieControllerOverlay(mContext);
        mController.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float xChanged = (mTouchX != -1f && mTouchY != -1f) ? event.getX() - mTouchX : 0f;
                float yChanged = (mTouchX != -1f && mTouchY != -1f) ? event.getY() - mTouchY : 0f;
                float coef = Math.abs(yChanged / xChanged);
                float xgesturesize = xChanged / metrics.xdpi * 2.54f;
                float deltaY = Math.max(((Math.abs(mInitTouchY - event.getY()) / metrics.xdpi + 0.5f) * 2f), 1f);
                int xTouch = (int) event.getX();
                int yTouch = (int) event.getY();
                long now = System.currentTimeMillis();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Logger.d(String.format("onTouch: %s", "MotionEvent.ACTION_DOWN"));
                        mTouchDownMs = now;
                        mVerticalTouchActive = false;
                        // Audio;
                        mInitTouchY = event.getY();
                        mInitTouchX = event.getX();
                        mTouchY = mInitTouchY;
                        //player.initAudioVolume();
                        mTouchAction = TOUCH_NONE;
                        // Seek;
                        mTouchX = event.getX();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Logger.d(String.format("onTouch: %s", "MotionEvent.ACTION_MOVE"));
                        if (mTouchAction == TOUCH_IGNORE) return false;
                        // Mouse events for the core;
                        // player.sendMouseEvent(MotionEvent.ACTION_MOVE, xTouch, yTouch);
                        // No volume/brightness action if coef < 2 or a secondary display is connected;
                        //TODO : Volume action when a secondary display is connected;
                        Logger.d(String.format("onTouch: %s", coef));
                        if (mTouchAction != TOUCH_SEEK && coef > 2) {
                            if (!mVerticalTouchActive) {
//                                    if (Math.abs(yChanged / yRange) >= 0.05) {
//                                        mVerticalTouchActive = true;
//                                        mTouchY = event.getY();
//                                        mTouchX = event.getX();
//                                    }
                                return false;
                            }
                            mTouchY = event.getY();
                            mTouchX = event.getX();
                            //doVerticalTouchAction(yChanged);
                        } else if (mInitTouchX < metrics.widthPixels * 0.95) {
                            // Seek (Right or Left move);
                            doSeekTouch((int) deltaY, xgesturesize, false);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        Logger.d(String.format("onTouch: %s", "MotionEvent.ACTION_UP"));
                        float touchSlop = ViewConfiguration.get(movieActivity).getScaledTouchSlop();
                        if (mTouchAction == TOUCH_IGNORE) mTouchAction = TOUCH_NONE;
                        // Mouse events for the core;
                        //player.sendMouseEvent(MotionEvent.ACTION_UP, xTouch, yTouch);
                        mTouchX = -1f;
                        mTouchY = -1f;
                        // Seek;
                        if (mTouchAction == TOUCH_SEEK) {
                            doSeekTouch((int) deltaY, xgesturesize, true);
                            return true;
                        }
                        // Vertical actions;
                        if (mTouchAction == TOUCH_VOLUME || mTouchAction == TOUCH_BRIGHTNESS) {
                            // doVerticalTouchAction(yChanged);
                            return true;
                        }
                        mHandler.removeCallbacksAndMessages(null);
                        if (now - mTouchDownMs > ViewConfiguration.getDoubleTapTimeout()) {
                            ;
                            //it was not a tap;
                            mNumberOfTaps = 0;
                            mLastTapTimeMs = 0;
                        }
                        ;
                        //verify that the touch coordinate distance did not exceed the touchslop to increment the count tap;
                        if (Math.abs(event.getX() - mInitTouchX) < touchSlop && Math.abs(event.getY() - mInitTouchY) < touchSlop) {
                            if (mNumberOfTaps > 0 && now - mLastTapTimeMs < ViewConfiguration.getDoubleTapTimeout()) {
                                mNumberOfTaps += 1;
                            } else {
                                mNumberOfTaps = 1;
                            }
                        }
                        mLastTapTimeMs = now;
                        //handle multi taps;
                        if (mNumberOfTaps > 1) {
                            if (TOUCH_FLAG_SEEK == 0) {
                                // player.doPlayPause();
                            } else {
//                                val range = (if (screenConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) screenConfig.xRange else screenConfig.yRange).toFloat();
//                                if (BuildConfig.DEBUG) Log.d("VideoTouchDelegate", "Landscape: ${screenConfig.orientation == Configuration.ORIENTATION_LANDSCAPE} range: $range eventx: ${event.x}");
//                                when {;
//                                    event.x < range / 4f -> seekDelta(-10000);
//                                    event.x > range * 0.75 -> seekDelta(10000);
//                                    else -> player.doPlayPause();
//                                };
                            }
                        }
                        if (mNumberOfTaps == 1) {
                            Logger.d(String.format("onTouch: %s", mNumberOfTaps));
                            mHandler.sendMessageDelayed(Message.obtain(), ViewConfiguration.getDoubleTapTimeout());
                        }
                        break;

                }
                // mController.show();
                return true;//mTouchAction != TOUCH_NONE;
            }
        });
        ((ViewGroup) rootView).addView(mController.getView());
        mController.setListener(this);
        mController.setCanReplay(canReplay);
        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setVideoURI(mUri);
        Intent ai = movieActivity.getIntent();
        boolean virtualize = ai.getBooleanExtra(VIRTUALIZE_EXTRA, false);
        if (virtualize) {
            int session = mVideoView.getAudioSessionId();
            if (session != 0) {
                mVirtualizer = new Virtualizer(0, session);
                mVirtualizer.setEnabled(true);
            } else {
                Log.w(TAG, "no audio session to virtualize");
            }
        }
        mVideoView.setOnPreparedListener(player -> {
            mController.setSeekable(mVideoView.canSeekForward() && mVideoView.canSeekBackward());
            setProgress();
        });
        // The SurfaceView is transparent before drawing the first frame.
        // This makes the UI flashing when open a video. (black -> old screen
        // -> video) However, we have no way to know the timing of the first
        // frame. So, we hide the VideoView for a while to make sure the
        // video has been drawn on it.
        mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mVideoView.setVisibility(View.VISIBLE);
            }
        }, BLACK_TIMEOUT);
        setOnSystemUiVisibilityChangeListener();
        // Hide system UI by default
        showSystemUi(false);
        mAudioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();
        mAudioBecomingNoisyReceiver.register();
        Intent i = new Intent(SERVICECMD);
        i.putExtra(CMDNAME, CMDPAUSE);
        movieActivity.sendBroadcast(i);
        if (savedInstance != null) { // this is a resumed activity
            mVideoPosition = savedInstance.getInt(KEY_VIDEO_POSITION, 0);
            mResumeableTime = savedInstance.getLong(KEY_RESUMEABLE_TIME, Long.MAX_VALUE);
            mVideoView.start();
            mVideoView.suspend();
            mHasPaused = true;
        } else {
            final Integer bookmark = mBookmarker.getBookmark(mUri);
            if (bookmark != null) {
                mVideoView.seekTo(bookmark);
                startVideo();
                //showResumeDialog(movieActivity, bookmark);
            } else {
                startVideo();
            }
        }
    }

    private void doSeekTouch(int coef, float gesturesize, boolean seek) {
        if (coef == 0) coef = 1;
        // No seek action if coef > 0.5 and gesturesize < 1cm;
        if (Math.abs(gesturesize) < 1 || !(mVideoView.canSeekForward() || mVideoView.canSeekBackward()))
            return;
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_SEEK) return;
        mTouchAction = TOUCH_SEEK;
        int length = mVideoView.getDuration();
        int time = mVideoView.getCurrentPosition();
        float sign;
        if (gesturesize > 0) {
            sign = 1f;
        } else if (gesturesize < 0) {
            sign = -1f;
        } else {
            sign = 0;
        }
        int jump = (int) (sign * (600000 * Math.pow((gesturesize / 8), 4.0) + 3000) / coef);
        if (jump > 0 && time + jump > length) jump = (length - time);
        if (jump < 0 && time + jump < 0) jump = (-time);
        if (seek && length > 0) mVideoView.seekTo(time + jump);

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setOnSystemUiVisibilityChangeListener() {
        if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION) return;
        // When the user touches the screen or uses some hard key, the framework
        // will change system ui visibility from invisible to visible. We show
        // the media control and enable system UI (e.g. ActionBar) to be visible at this point
        mVideoView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        int diff = mLastSystemUiVis ^ visibility;
                        mLastSystemUiVis = visibility;
                        if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                                && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                            Logger.d(String.format("onSystemUiVisibilityChange: %s", ""));
                            mController.show();
                        }
                    }
                });
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showSystemUi(boolean visible) {
        if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) return;
        int flag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (!visible) {
            // We used the deprecated "STATUS_BAR_HIDDEN" for unbundling
            flag |= View.STATUS_BAR_HIDDEN | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        mVideoView.setSystemUiVisibility(flag);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_VIDEO_POSITION, mVideoPosition);
        outState.putLong(KEY_RESUMEABLE_TIME, mResumeableTime);
    }


    private void showResumeDialog(Context context, final int bookmark) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.resume_playing_title);
        builder.setMessage(String.format(
                context.getString(R.string.resume_playing_message),
                DateTimeShare.formatDuration(context, bookmark / 1000)));
        builder.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onCompletion();
            }
        });
        builder.setPositiveButton(
                R.string.resume_playing_resume, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mVideoView.seekTo(bookmark);
                        startVideo();
                    }
                });
        builder.setNegativeButton(
                R.string.resume_playing_restart, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startVideo();
                    }
                });
        builder.show();
    }

    public void onPause() {
        mHasPaused = true;
        mHandler.removeCallbacksAndMessages(null);
        mVideoPosition = mVideoView.getCurrentPosition();
        mBookmarker.setBookmark(mUri, mVideoPosition, mVideoView.getDuration());
        mVideoView.suspend();
        mResumeableTime = System.currentTimeMillis() + RESUMEABLE_TIMEOUT;
    }

    public void onResume() {
        if (mHasPaused) {
            mVideoView.seekTo(mVideoPosition);
            mVideoView.resume();
            // If we have slept for too long, pause the play
            if (System.currentTimeMillis() > mResumeableTime) {
                pauseVideo();
            }
        }
        mHandler.post(mProgressChecker);
    }

    public void onDestroy() {
        if (mVirtualizer != null) {
            mVirtualizer.release();
            mVirtualizer = null;
        }
        mVideoView.stopPlayback();
        mAudioBecomingNoisyReceiver.unregister();
    }

    // This updates the time bar display (if necessary). It is called every
    // second by mProgressChecker and also from places where the time bar needs
    // to be updated immediately.
    private int setProgress() {
        if (mDragging || !mShowing) {
            return 0;
        }
        int position = mVideoView.getCurrentPosition();
        int duration = mVideoView.getDuration();
        mController.setTimes(position, duration, 0, 0);
        return position;
    }

    private void startVideo() {
        // For streams that we expect to be slow to start up, show a
        // progress spinner until playback starts.
        String scheme = mUri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "rtsp".equalsIgnoreCase(scheme)) {
            mController.showLoading();
            mHandler.removeCallbacks(mPlayingChecker);
            mHandler.postDelayed(mPlayingChecker, 250);
        } else {
            mController.showPlaying();
            mController.hide();
        }
        mVideoView.start();
        setProgress();
    }

    private void playVideo() {
        mVideoView.start();
        mController.showPlaying();
        setProgress();
    }

    private void pauseVideo() {
        mVideoView.pause();
        mController.showPaused();
    }

    // Below are notifications from VideoView
    @Override
    public boolean onError(MediaPlayer player, int arg1, int arg2) {
        mHandler.removeCallbacksAndMessages(null);
        // VideoView will show an error dialog if we return false, so no need
        // to show more message.
        mController.showErrorMessage("");
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mController.showEnded();
        onCompletion();
    }

    public void onCompletion() {
    }

    // Below are notifications from ControllerOverlay
    @Override
    public void onPlayPause() {
        if (mVideoView.isPlaying()) {
            pauseVideo();
        } else {
            playVideo();
        }
    }

    @Override
    public void onSeekStart() {
        mDragging = true;
    }

    @Override
    public void onSeekMove(int time) {
        mVideoView.seekTo(time);
    }

    @Override
    public void onSeekEnd(int time, int start, int end) {
        mDragging = false;
        mVideoView.seekTo(time);
        setProgress();
    }

    @Override
    public void onShown() {
        mShowing = true;
        setProgress();
        showSystemUi(true);
    }

    @Override
    public void onHidden() {
        mShowing = false;
        showSystemUi(false);
    }

    @Override
    public void onReplay() {
        startVideo();
    }

    // Below are key events passed from MovieActivity.
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Some headsets will fire off 7-10 events on a single click
        if (event.getRepeatCount() > 0) {
            return isMediaKey(keyCode);
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mVideoView.isPlaying()) {
                    pauseVideo();
                } else {
                    playVideo();
                }
                return true;
            case KEYCODE_MEDIA_PAUSE:
                if (mVideoView.isPlaying()) {
                    pauseVideo();
                }
                return true;
            case KEYCODE_MEDIA_PLAY:
                if (!mVideoView.isPlaying()) {
                    playVideo();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                // TODO: Handle next / previous accordingly, for now we're
                // just consuming the events.
                return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return isMediaKey(keyCode);
    }

    private static boolean isMediaKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE;
    }

    // We want to pause when the headset is unplugged.
    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {

        public void register() {
            mContext.registerReceiver(this,
                    new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }

        public void unregister() {
            mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mVideoView.isPlaying()) pauseVideo();
        }
    }
}

class Bookmarker {
    private static final String TAG = "Bookmarker";

    private static final String BOOKMARK_CACHE_FILE = "bookmark";
    private static final int BOOKMARK_CACHE_MAX_ENTRIES = 100;
    private static final int BOOKMARK_CACHE_MAX_BYTES = 10 * 1024;
    private static final int BOOKMARK_CACHE_VERSION = 1;

    private static final int HALF_MINUTE = 30 * 1000;
    private static final int TWO_MINUTES = 4 * HALF_MINUTE;

    private final Context mContext;

    public Bookmarker(Context context) {
        mContext = context;
    }

    public void setBookmark(Uri uri, int bookmark, int duration) {
        try {
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(uri.toString());
            dos.writeInt(bookmark);
            dos.writeInt(duration);
            dos.flush();
            cache.insert(uri.hashCode(), bos.toByteArray());
        } catch (Throwable t) {
            Log.w(TAG, "setBookmark failed", t);
        }
    }

    public Integer getBookmark(Uri uri) {
        try {
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);
            byte[] data = cache.lookup(uri.hashCode());
            if (data == null) return null;
            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));
            String uriString = DataInputStream.readUTF(dis);
            int bookmark = dis.readInt();
            int duration = dis.readInt();
            if (!uriString.equals(uri.toString())) {
                return null;
            }
            if ((bookmark < HALF_MINUTE) || (duration < TWO_MINUTES)
                    || (bookmark > (duration - HALF_MINUTE))) {
                return null;
            }
            return Integer.valueOf(bookmark);
        } catch (Throwable t) {
            Log.w(TAG, "getBookmark failed", t);
        }
        return null;
    }
}
