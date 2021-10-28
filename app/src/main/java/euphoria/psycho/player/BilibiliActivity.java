package euphoria.psycho.player;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;

import euphoria.psycho.downloader.DownloadTaskDatabase;
import euphoria.psycho.downloader.DownloaderActivity;
import euphoria.psycho.downloader.DownloaderTask;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.DateTimeShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.videos.Iqiyi;

import static euphoria.psycho.player.PlayerHelper.getNavigationBarHeight;
import static euphoria.psycho.player.PlayerHelper.getNavigationBarSize;
import static euphoria.psycho.player.PlayerHelper.hideSystemUI;
import static euphoria.psycho.player.PlayerHelper.rotateScreen;
import static euphoria.psycho.player.PlayerHelper.showSystemUI;
import static euphoria.psycho.player.PlayerHelper.switchPlayState;
//
public class BilibiliActivity extends BaseVideoActivity implements
        GestureDetector.OnGestureListener,
        TimeBar.OnScrubListener,
        OnPreparedListener,
        OnCompletionListener,
        OnBufferingUpdateListener,
        OnSeekCompleteListener,
        OnInfoListener,
        Iqiyi.Callback {
    public static final long DEFAULT_SHOW_TIMEOUT_MS = 5000L;
    public static final String EXTRA_PLAYLSIT = "extra.PLAYLSIT";

    private final Handler mHandler = new Handler();
    private final HashMap<String, Integer> mHashMap = new HashMap<>();
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
    private int mNavigationBarHeight;
    private int mNavigationBarWidth;
    private boolean mScrubbing;
    private GestureDetector mVideoTouchHelper;
    private int mCurrentPlaybackIndex;
    private String[] mPlayList;
    private boolean mAudioPrepared;
    private MediaPlayer mAudio;
    private boolean mVideoPrepared;
    private final Runnable mPlay = new Runnable() {
        @Override
        public void run() {
            if (mVideoPrepared && mAudioPrepared) {
                mProgress.setDuration(mPlayer.getDuration());
                mHandler.post(mProgressChecker);
                mExoPlay.setImageResource(R.drawable.exo_controls_pause);
                mPlayer.start();
                mAudio.start();
                mHandler.removeCallbacks(mPlay);
            } else {
                mHandler.postDelayed(mPlay, 50);
            }
        }
    };

    public static void startDownloadActivity(Context context) {
        Intent starter = new Intent(context, DownloaderActivity.class);
        context.startActivity(starter);
    }

    private String createDownloadDirectory() {
        // File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Bilibili");
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    private void downloadVideo(View v) {
        String dir = createDownloadDirectory();
        String fileName = KeyShare.md5(mPlayList[0]);
        DownloaderTask videoTask = new DownloaderTask();
        videoTask.Directory = dir;
        videoTask.FileName = fileName + ".mp4";
        videoTask.Uri = mPlayList[0];
        DownloaderTask audioTask = new DownloaderTask();
        audioTask.Directory = dir;
        audioTask.FileName = fileName + ".mp3";
        audioTask.Uri = mPlayList[1];
        DownloadTaskDatabase.getInstance(this).insertDownloadTask(videoTask);
        DownloadTaskDatabase.getInstance(this).insertDownloadTask(audioTask);
        startDownloadActivity(this);
    }

    private HashMap<String, String> getRequestHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Referer", "https://www.bilibili.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Safari/537.36");
        return headers;
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

    private void initializeAudioPlayer() {
        if (mPlayList.length < 2) {
            return;
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, Uri.parse(mPlayList[1]), getRequestHeaders());
        } catch (IOException ignored) {
        }
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(mp -> {
            mAudioPrepared = true;
        });
        mAudio = mediaPlayer;
    }

    private void initializePlayer() {
        mExoDelete.setVisibility(View.GONE);
        hideController();
        if (!loadPlayList())
            return;
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener((mp, what, extra) -> false);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
        initializeAudioPlayer();
        play();
    }

    private boolean loadPlayList() {
        mPlayList = getIntent().getStringArrayExtra(EXTRA_PLAYLSIT);
        if (mPlayList != null) {
            playPlayList(mCurrentPlaybackIndex);
            return true;
        }
        return false;
    }

    private void play() {
        mHandler.postDelayed(mPlay, 50);
    }

    private void playPlayList(int index) {
        HashMap<String, String> headers = getRequestHeaders();
        mPlayer.setVideoURI(Uri.parse(mPlayList[index]), headers);

    }

    private int setProgress() {
        int position = mPlayer.getCurrentPosition();
        mExoDuration.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, mPlayer.getDuration()));
        mExoPosition.setText(DateTimeShare.getStringForTime(mStringBuilder, mFormatter, position));
        mProgress.setPosition(position);
        return position;
    }

    private void setupView() {
        mFileDownload.setVisibility(View.VISIBLE);
        mRootView.setOnTouchListener((v, event) -> {
            mVideoTouchHelper.onTouchEvent(event);
            return true;
        });
        mProgress.addListener(this);
        mExoPlay.setOnClickListener(v -> {
            switchPlayState(mPlayer, mExoPlay);
        });
        mExoPrev.setOnClickListener(v -> {
            mCurrentPlaybackIndex = previous(mCurrentPlaybackIndex);
        });
        mExoPrev.setVisibility(View.GONE);
        mExoNext.setOnClickListener(v -> {
            mCurrentPlaybackIndex = next(mCurrentPlaybackIndex);
        });
        mExoNext.setVisibility(View.GONE);
        mExoRew.setOnClickListener(v -> {
            rotateScreen(this);
        });
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        }
        mFileDownload.setOnClickListener(this::downloadVideo);
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
        if (mAudio != null) {
            mAudio.stop();
            mAudio.release();
        }
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
        mProgress.setBufferedPosition((long) (mPlayer.getDuration() * (1f * percent / 100)));
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
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mVideoPrepared = true;
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
            mAudio.seekTo(positionMs);
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
        mAudio.seekTo((int) position);
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
