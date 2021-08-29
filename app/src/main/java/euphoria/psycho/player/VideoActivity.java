package euphoria.psycho.player;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultControlDispatcher;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;

import java.io.File;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import static com.google.android.exoplayer2.C.INDEX_UNSET;
import static com.google.android.exoplayer2.C.TIME_END_OF_SOURCE;
import static com.google.android.exoplayer2.C.TIME_UNSET;

// https://github.com/google/ExoPlayer
public class VideoActivity extends BaseVideoActivity implements
        Player.EventListener,
        VideoListener, TextOutput,
        View.OnLayoutChangeListener,
        VideoTouchHelper.Listener,
        TimeBar.OnScrubListener {
    public static final long DEFAULT_SHOW_TIMEOUT_MS = 5000L;
    public static final String EXTRA_REFRESH = "refresh";
    public static final String KEY_SORT_BY = "sort_by";
    public static final String KEY_SORT_DIRECTION = "sort_direction_video";
    private static final String TAG = "VideoActivity";
    private final DefaultControlDispatcher mControlDispatcher = new DefaultControlDispatcher();
    private final FileDataSourceFactory mFileDataSourceFactory = new FileDataSourceFactory();
    private final Handler mHandler = new Handler();
    private final Format mSrtFormat = Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP, Format.NO_VALUE, "en");
    private final StringBuilder mStringBuilder = new StringBuilder();
    private Bookmarker mBookmarker;
    private File[] mFiles = null;
    private Formatter mFormatter = new Formatter(mStringBuilder);
    private int mNavigationBarHeight;
    private int mNavigationBarWidth;
    private SimpleExoPlayer mPlayer;
    private boolean mScrubbing;
    private int mStartWindow;
    private int mTextureViewRotation;
    private VideoTouchHelper mVideoTouchHelper;
    private Timeline.Window mWindow = new Timeline.Window();
    private Runnable mUpdateProgressAction = this::updateProgress;
    private Runnable mHideAction = this::hide;

    private void actionDeleteVideo() {
        if (isPlaying()) {
            mControlDispatcher.dispatchSetPlayWhenReady(mPlayer, false);
        }
        String fileName = getCurrentUri();

        openDeleteVideoDialog(this, fileName, new OperationResultCallback() {
            @Override
            public void onCancel() {
            }

            @Override
            public void onFinished(boolean result) {
                Context context = VideoActivity.this;

                new File(fileName).delete();
                mPlayer.stop();
                if (mStartWindow + 1 < mFiles.length) {
                    MediaSource mediaSource = generateMediaSource(Uri.fromFile(mFiles[mStartWindow + 1]));
                    mPlayer.prepare(mediaSource);
                    seekToFile(mFiles[mStartWindow]);

                } else if (mStartWindow - 1 >= 0) {
                    MediaSource mediaSource = generateMediaSource(Uri.fromFile(mFiles[mStartWindow + -1]));
                    mPlayer.prepare(mediaSource);
                    seekToFile(mFiles[mStartWindow]);
                }
                setRefreshResult();

            }
        });
    }

    private void actionRenamFile() {
        if (isPlaying()) {
            mControlDispatcher.dispatchSetPlayWhenReady(mPlayer, false);
        }
        String fileName = getCurrentUri();
//        FileUtils.showFileDialog(this, FileUtils.getFileName(fileName),
//                "重命名", new FileUtils.DialogListener() {
//                    @Override
//                    public void onConfirmed(CharSequence value) {
//                        Context context = VideoActivity.this;
//                        File targetFile = new File(FileUtils.getDirectoryName(fileName), value.toString());
//                        if (fileName.startsWith(FileUtils.getRemovableStoragePath())) {
//                            String treeUri = mPreferences
//                                    .getString(KEY_TREE_URI, "");
//                            if (!isNullOrEmpty(treeUri)) {
//                                DocumentFile documentFile = FileUtils.getDocumentFile(context,
//                                        fileName, Uri.parse(treeUri));
//                                try {
//                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                                        DocumentsContract.renameDocument(context.getContentResolver(), documentFile.getUri(), value.toString());
//                                    }
//                                } catch (FileNotFoundException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        } else {
//                            new File(fileName).renameTo(targetFile);
//                        }
//
//                        seekToFile(targetFile);
//                        setRefreshResult();
//                    }
//
//                    @Override
//                    public void onDismiss() {
//                    }
//                });
    }

    private SingleSampleMediaSource buildSubtitleMediaSource(File file) {
        File subtitleFile = new File(Utils.changeExtension(file.getAbsolutePath(), "srt"));
        if (subtitleFile.exists()) {
            return new SingleSampleMediaSource.Factory(mFileDataSourceFactory).createMediaSource(Uri.fromFile(subtitleFile),
                    mSrtFormat,
                    C.TIME_UNSET);
        }
        return null;
    }

    private void fastForward() {
        if (mPlayer == null) return;
        float speed = mPlayer.getPlaybackParameters().speed;
        float targetSpeed = (((int) speed + 2) / 2) * 2f;
        targetSpeed = Math.min(targetSpeed, 8);
        mPlayer.setPlaybackParameters(new PlaybackParameters(targetSpeed, targetSpeed));
        Toast.makeText(this, targetSpeed + " ", Toast.LENGTH_SHORT).show();
    }

    private MediaSource generateMediaSource(Uri uri) {
        String sourcePath = uri.getPath();
        File[] files = listVideoFiles(new File(sourcePath).getParent());
        mFiles = files;
        if (files == null) return null;
        int length = files.length;
        MediaSource[] mediaSources = new MediaSource[length];
        FileDataSourceFactory fileDataSourceFactory = new FileDataSourceFactory();
        for (int i = 0; i < length; i++) {
            if (files[i].getAbsolutePath().equals(sourcePath)) {
                Log.e(TAG, files[i].getAbsolutePath() + " " + i);
                mStartWindow = i;
            }
            MediaSource mediaSource = new ExtractorMediaSource.Factory(fileDataSourceFactory).createMediaSource(Uri.fromFile(files[i]));
            SingleSampleMediaSource singleSampleMediaSource = buildSubtitleMediaSource(files[i]);
            if (singleSampleMediaSource == null) {
                mediaSources[i] = mediaSource;
            } else {
                mediaSources[i] = new MergingMediaSource(mediaSource, singleSampleMediaSource);
            }
        }
        if (files.length > 1) {
            ConcatenatingMediaSource mediaSource = new ConcatenatingMediaSource();
            for (int i = 0; i < length; i++) {
                mediaSource.addMediaSource(mediaSources[i]);
            }
            return mediaSource;
        } else {
            return mediaSources[0];
        }
    }

    private String getCurrentUri() {
        mStartWindow = mPlayer.getCurrentWindowIndex();
        if (mFiles == null)
            return null;
        if (mStartWindow >= 0 && mStartWindow < mFiles.length)
            return mFiles[mStartWindow].getAbsolutePath();
        return null;
    }

    private void hide() {
        if (mController.getVisibility() == View.VISIBLE) {
            mController.setVisibility(View.GONE);
            mHandler.removeCallbacks(mUpdateProgressAction);
            mHandler.removeCallbacks(mHideAction);
            hideSystemUI(false);
        }
    }

    private void hideController() {
        mHandler.postDelayed(mHideAction, DEFAULT_SHOW_TIMEOUT_MS);
    }

    private void initializePlayer() {
        hideController();
        if (mPlayer == null) {
            TrackSelector trackSelector = new DefaultTrackSelector();
            mPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
            mPlayer.addListener(this);
            mPlayer.setPlayWhenReady(true);
            Player.VideoComponent videoComponent = mPlayer.getVideoComponent();
            videoComponent.setVideoTextureView(mTextureView);
            videoComponent.addVideoListener(this);
            mPlayer.getTextComponent().addTextOutput(this);
            Uri uri = getIntent().getData();
            MediaSource mediaSource = null;
            if (uri == null) {
                String videoUri = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                uri = Uri.parse(videoUri);
                DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
                String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";
                DefaultDataSourceFactory mediaDataSourceFactory = new DefaultDataSourceFactory(this, BANDWIDTH_METER,
                        new DefaultHttpDataSourceFactory(userAgent, BANDWIDTH_METER));

                if (videoUri.contains(".m3u8"))
                    mediaSource = new HlsMediaSource.Factory(mediaDataSourceFactory)
                            .createMediaSource(uri);
                else{
                    mediaSource=new ExtractorMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
                }

            } else {
                mediaSource = generateMediaSource(uri);
            }


            mPlayer.prepare(mediaSource);
            if (mStartWindow > 0) {
                seekTo(mStartWindow, C.TIME_UNSET);
            }
        }
        updateAll();
    }

    private boolean isPlaying() {
        SimpleExoPlayer player = mPlayer;
        if (player == null) return false;
        int state = player.getPlaybackState();
        return state != Player.STATE_ENDED
                && state != Player.STATE_IDLE
                && player.getPlayWhenReady();
    }

    private File[] listVideoFiles(String dir) {
        File directory = new File(dir);
        Pattern pattern = Pattern.compile("\\.(?:mp4|vm|crdownload)$");
        File[] files = directory.listFiles(file ->
        {
            return file.isFile() && pattern.matcher(file.getName()).find();
        });
        if (files == null || files.length == 0) return null;
        Collator collator = Collator.getInstance(Locale.CHINA);
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
//                final long result =o2.lastModified()-o1.lastModified();
//                if (result < 0) {
//                    return -1;
//                } else if (result > 0) {
//                    return 1;
//                } else {
//                    return 0;
//                }
                return collator.compare(o1.getName(), o2.getName());


            }
        });
        return files;
    }

    private void next() {
        if (mPlayer == null) return;
        savePosition();
        if (mPlayer.getCurrentTimeline().isEmpty()) return;
        if (mPlayer.getNextWindowIndex() != INDEX_UNSET) {
            seekTo(mPlayer.getNextWindowIndex(), TIME_UNSET);
        } else {
            seekTo(mPlayer.getCurrentWindowIndex(), C.TIME_UNSET);
        }
        seekToLastedState();
    }

    private void preparePlayback() {
    }

    private void previous() {
        if (mPlayer == null || mPlayer.getCurrentTimeline() == null) return;
        savePosition();
        mPlayer.getCurrentTimeline().getWindow(mPlayer.getCurrentWindowIndex(), mWindow);
        if (mPlayer.getPreviousWindowIndex() != INDEX_UNSET
//                && mPlayer.getCurrentPosition() <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
                || (mWindow.isDynamic && !mWindow.isSeekable)) {
            seekTo(mPlayer.getPreviousWindowIndex(), C.TIME_UNSET);
        } else {
            seekTo(mPlayer.getCurrentWindowIndex(), 0L);
        }
        seekToLastedState();
    }

    private void releasePlayer() {
        updateStartPosition();
        if (mPlayer != null) {
            savePosition();
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void requestPlayPauseFocus() {
        boolean playing = isPlaying();
        if (!playing) {
            mExoPlay.requestFocus();
        } else {
            mExoPause.requestFocus();
        }
    }

    private void rewind() {
        if (mPlayer == null) return;
        float speed = mPlayer.getPlaybackParameters().speed;
        float targetSpeed = 0f;
        if (speed <= 1.0f) {
            targetSpeed = speed - 0.25f;
            if (targetSpeed <= 0f) {
                targetSpeed = 0.25f;
            }
        } else {
            targetSpeed = (((int) speed - 2) / 2) * 2f;
            targetSpeed = Math.max(targetSpeed, 1);
        }


        mPlayer.setPlaybackParameters(new PlaybackParameters(targetSpeed, targetSpeed));
        Toast.makeText(this, targetSpeed + " ", Toast.LENGTH_SHORT).show();
    }

    private void savePosition() {
        String uri = getCurrentUri();
        if (uri == null || mPlayer == null) return;
        mBookmarker.setBookmark(uri, (int) mPlayer.getCurrentPosition());
    }

    private void seekTo(int windowIndex, long position) {
        if (mPlayer != null) {
            boolean dispatched = mControlDispatcher.dispatchSeekTo(mPlayer, windowIndex, position);
            if (!dispatched) {
                updateProgress();
            }
        }
    }

    private void seekToFile(File file) {
        Uri uri = Uri.fromFile(file);
        mPlayer.stop();

        MediaSource mediaSource = generateMediaSource(uri);
        mPlayer.prepare(mediaSource);
        mPlayer.seekTo(mStartWindow, C.TIME_UNSET);

        getSupportActionBar().setTitle(file.getName());
        long bookmark = mBookmarker.getBookmark(file.getAbsolutePath());
        if (bookmark > 0) {
            seekTo(mPlayer.getCurrentWindowIndex(), bookmark);
        }
        mControlDispatcher.dispatchSetPlayWhenReady(mPlayer, true);
        updateNavigation();

    }

    private void seekToLastedState() {
        mStartWindow = mPlayer.getCurrentWindowIndex();
        String uri = getCurrentUri();
        if (uri == null) return;
        long bookmark = mBookmarker.getBookmark(uri);
        if (bookmark > 0) {
            seekTo(mPlayer.getCurrentWindowIndex(), bookmark);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(Utils.getFileName(uri));
        }
        updateNavigation();
    }

    private void seekToTimeBarPosition(long position) {
        if (mPlayer != null) {
            mPlayer.seekTo(mPlayer.getCurrentWindowIndex(), position);
        }
    }

    private void setButtonEnabled(boolean enabled, View view) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.3f);
        view.setVisibility(View.VISIBLE);
    }

    private void setRefreshResult() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_REFRESH, true);
        setResult(RESULT_OK, intent);
    }

    private void setupView() {
        mRootView.setOnTouchListener((v, event) -> mVideoTouchHelper.onTouch(event));
        mExoProgress.addListener(this);
        mExoPlay.setOnClickListener(v -> {
            if (mPlayer == null) return;
            int playbackState = mPlayer.getPlaybackState();
            if (playbackState == Player.STATE_IDLE) preparePlayback();
            else if (playbackState == Player.STATE_ENDED)
                mControlDispatcher.dispatchSeekTo(mPlayer, mPlayer.getCurrentWindowIndex(), C.TIME_UNSET);
            else mControlDispatcher.dispatchSetPlayWhenReady(mPlayer, true);
            hideController();
        });
        mExoPause.setOnClickListener(v -> {
            mControlDispatcher.dispatchSetPlayWhenReady(mPlayer, false);
            hideController();
        });
        mExoPrev.setOnClickListener(v -> {
            previous();
            hideController();
        });
        mExoNext.setOnClickListener(v -> {
            next();
            hideController();
        });
        mExoFfwd.setOnClickListener(v -> {
            fastForward();
        });
        mExoRew.setOnClickListener(v -> {
            rewind();
        });
    }

    private void show() {
        if (mController.getVisibility() == View.VISIBLE) return;
        mController.setVisibility(View.VISIBLE);
        updateProgress();
        updateNavigation();
        requestPlayPauseFocus();
        showSystemUI(true);
        if (mIsHasBar) {
            int left = 0;
            int top = 0;
            int right = 0;
            int bottom = 0;
            int orientation = calculateScreenOrientation();
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                bottom += mNavigationBarHeight;
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                right += mNavigationBarWidth;
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                left += mNavigationBarWidth;
            }
            mController.setPadding(left, top, right, bottom);
        }
        hideController();
    }

    private void updateAll() {
        updatePlayPauseButton();
        updateProgress();
        updateNavigation();
        seekToLastedState();
    }

    private void updateNavigation() {
        if (mController.getVisibility() != View.VISIBLE) return;
        Timeline timeline = mPlayer.getCurrentTimeline();
        boolean haveNonEmptyTimeline = timeline != null && !timeline.isEmpty();
        boolean isSeekable = false;
        boolean enablePrevious = false;
        boolean enableNext = false;
        if (haveNonEmptyTimeline) {
            timeline.getWindow(mPlayer.getCurrentWindowIndex(), mWindow);
            isSeekable = mWindow.isSeekable;
            enablePrevious = isSeekable || !mWindow.isDynamic || mPlayer.getPreviousWindowIndex() != INDEX_UNSET;
            enableNext = mWindow.isDynamic || mPlayer.getNextWindowIndex() != INDEX_UNSET;
        }
        setButtonEnabled(enablePrevious, mExoPrev);
        setButtonEnabled(enableNext, mExoNext);
        setButtonEnabled(isSeekable, mExoFfwd);
        setButtonEnabled(isSeekable, mExoRew);
        mExoProgress.setEnabled(isSeekable);
    }

    private void updatePlayPauseButton() {
        boolean requestFocus = false;
        boolean playing = isPlaying();
        mExoPlay.setVisibility(playing ? View.GONE : View.VISIBLE);
        requestFocus = requestFocus | (playing && mExoPlay.isFocused());
        mExoPause.setVisibility(!playing ? View.GONE : View.VISIBLE);
        requestFocus = requestFocus | (!playing && mExoPause.isFocused());
        if (requestFocus)
            requestPlayPauseFocus();
    }

    private void updateProgress() {
        if (mController.getVisibility() != View.VISIBLE) return;
        long position = 0L;
        long duration = 0L;
        int playbackState = 0;
        if (mPlayer != null) {
            Timeline timeline = mPlayer.getCurrentTimeline();
            if (!timeline.isEmpty())
                timeline.getWindow(mPlayer.getCurrentWindowIndex(), mWindow);


            duration = usToMs(mWindow.getDurationUs());

//            Log.e("TAG/VideoActivity", "[ERROR] updateProgress: " + mPlayer.getDuration() + " "
//                    + mPlayer.getContentDuration()
//                    + " " + duration
//            + mPlayer.du);

            position = mPlayer.getCurrentPosition();
            playbackState = mPlayer.getPlaybackState();
        }
        mExoDuration.setText(Util.getStringForTime(mStringBuilder, mFormatter, duration));
        mExoPosition.setText(Util.getStringForTime(mStringBuilder, mFormatter, position));
        mExoProgress.setDuration(duration);
        mExoProgress.setPosition(position);
        mHandler.removeCallbacks(mUpdateProgressAction);
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            long delayMs = 0L;
            if (mPlayer != null) {
                if (mPlayer.getPlayWhenReady() && playbackState == Player.STATE_READY) {
                    float playbackSpeed = mPlayer.getPlaybackParameters().speed;
                    if (playbackSpeed <= 0.1f) {
                        delayMs = 1000L;
                    } else if (playbackSpeed < 5f) {
                        float mediaTimeUpdatePeriodMs = 1000 / Math.max(1f, Math.round(1 / playbackSpeed));
                        float mediaTimeDelayMs = mediaTimeUpdatePeriodMs - (position % mediaTimeUpdatePeriodMs);
                        if (mediaTimeDelayMs < (mediaTimeUpdatePeriodMs / 5)) {
                            mediaTimeDelayMs += mediaTimeUpdatePeriodMs;
                        }
                        delayMs = (playbackSpeed == 1f) ? (long) mediaTimeDelayMs : (long) (mediaTimeDelayMs / playbackSpeed);
                    } else {
                        delayMs = 200L;
                    }
                } else {
                    delayMs = 1000L;
                }
                mHandler.postDelayed(mUpdateProgressAction, delayMs);
            }
        }
    }

    private void updateStartPosition() {
        if (mPlayer == null) return;
        mStartWindow = mPlayer.getCurrentWindowIndex();
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

    public static void openDeleteVideoDialog(Context context, String
            fileName, OperationResultCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("询问")
                .setMessage(String.format("确定永久删除 %s 吗？", fileName))
                .setNegativeButton(android.R.string.cancel, ((dialog, which) -> {
                    dialog.dismiss();
                    if (callback != null) {
                        callback.onCancel();
                    }
                }))
                .setPositiveButton(android.R.string.ok, ((dialog, which) -> {
                    callback.onFinished(true);
                })).show();
    }

    private static long usToMs(long us) {
        if (us == C.TIME_UNSET || us == TIME_END_OF_SOURCE) return us;
        else return us / 1000;
    }

    @Override
    void initialize() {
        super.initialize();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Point point = getNavigationBarSize(this);
        mNavigationBarHeight = point.y;
        mNavigationBarWidth = point.x;
        mBookmarker = new Bookmarker(this);
        setupView();
        mVideoTouchHelper = new VideoTouchHelper(this, this);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //getMenuInflater().inflate(R.menu.video, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCues(List<Cue> cues) {
        mExoSubtitles.setCues(cues);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                               int oldTop, int oldRight, int oldBottom) {
        applyTextureViewRotation((TextureView) v, mTextureViewRotation);
    }

    /**
     * Called when the player starts or stops loading the source.
     *
     * @param isLoading Whether the source is currently being loaded.
     */
    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_rename: {
//                actionRenamFile();
//                return true;
//            }
//            case R.id.action_delete: {
//                actionDeleteVideo();
//                return true;
//
//            }
//            case R.id.action_full_screen: {
//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
//            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//        if (Player.STATE_READY == playbackState) {
//            seekToLastedState();
//        }
        updatePlayPauseButton();
        updateProgress();
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        if (mFiles != null)
            getSupportActionBar().setTitle(mFiles[mPlayer.getCurrentWindowIndex()].getName());
    }

    /**
     * Called when a frame is rendered for the first time since setting the surface, and when a frame
     * is rendered for the first time since a video track was selected.
     */
    @Override
    public void onRenderedFirstFrame() {

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public boolean onScroll(float distanceX, float distanceY) {
        int delta = (int) distanceX * -100;
        if (Math.abs(delta) > 1000)
            mPlayer.seekTo(delta + mPlayer.getCurrentPosition());


        return true;
    }

    @Override
    public void onScrubMove(TimeBar timeBar, long position) {
    }

    @Override
    public void onScrubStart(TimeBar timeBar, long position) {
        mHandler.removeCallbacks(mHideAction);
        mScrubbing = true;
    }

    @Override
    public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
        mScrubbing = false;
        seekToTimeBarPosition(position);
        hideController();
    }

    /**
     * Called when all pending seek requests have been processed by the player. This is guaranteed
     * to happen after any necessary changes to the player state were reported to
     * {@link #onPlayerStateChanged(boolean, int)}.
     */
    @Override
    public void onSeekProcessed() {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onSingleTapConfirmed() {

        show();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {
        updateProgress();
        updateNavigation();
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {
        float ratio = (height == 0 || width == 0) ? 1f : (width * pixelWidthHeightRatio) / height;
        if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
            ratio = 1 / ratio;
        }
        if (mTextureViewRotation != 0) {
            mTextureView.removeOnLayoutChangeListener(this);
        }
        mTextureViewRotation = unappliedRotationDegrees;
        if (mTextureViewRotation != 0) {
            mTextureView.addOnLayoutChangeListener(this);
        }
        applyTextureViewRotation(mTextureView, mTextureViewRotation);
        mExoContentFrame.setAspectRatio(ratio);
    }

    public interface OperationResultCallback {
        void onCancel();

        void onFinished(boolean result);
    }
}
