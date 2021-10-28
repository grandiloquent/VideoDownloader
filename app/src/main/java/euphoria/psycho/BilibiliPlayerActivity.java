package euphoria.psycho;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Pair;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource.Factory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.EventLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.WebViewShare;

//

public class BilibiliPlayerActivity extends Activity implements SimplePlayerControlView.VisibilityListener {
    public static final long DEFAULT_SHOW_TIMEOUT_MS = 5000L;
    public static final String EXTRA_TYPE = "extra.TYPE";
    public static final String KEY_PLAYLIST = "playlist";
    public static final String KEY_REQUEST_HEADERS = "requestHeaders";
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
    private static final String KEY_AUTO_PLAY = "auto_play";
    private static final String KEY_POSITION = "position";
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";

    protected SimplePlayerView mPlayerView;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private boolean mStartAutoPlay;
    private int mStartWindow;
    private long mStartPosition;
    protected SimpleExoPlayer mPlayer;
    private DefaultTrackSelector mDefaultTrackSelector;
    private TrackGroupArray mLastSeenTrackGroupArray;
    private SimpleCache mCache;


    protected void clearStartPosition() {
        mStartAutoPlay = true;
        mStartWindow = C.INDEX_UNSET;
        mStartPosition = C.TIME_UNSET;
    }

    protected boolean initializePlayer() {
        Intent intent = getIntent();
        List<MediaItem> mediaItems = new ArrayList<>();
        String[] strings = intent.getStringArrayExtra(KEY_PLAYLIST);
        for (String s : strings) {
            mediaItems.add(MediaItem.fromUri(s));
            MediaItem mediaItem = MediaItem.fromUri(s);
            MediaItem.Builder builder = mediaItem.buildUpon();
        }
        if (mPlayer == null) {
            boolean preferExtensionDecoders = true;
            RenderersFactory renderersFactory =
                    PlayerUtils.buildRenderersFactory(/* context= */ this, preferExtensionDecoders);
            Factory httpFactory = PlayerUtils.getHttpFactory(new String[]{"Referer", "https://www.bilibili.com/"});
            DefaultDataSourceFactory upstreamFactory =
                    new DefaultDataSourceFactory(this, httpFactory);
            File downloadContentDirectory =
                    new File(getExternalFilesDir(/* type= */ null), DOWNLOAD_CONTENT_DIRECTORY);
            mCache = new SimpleCache(
                    downloadContentDirectory, new NoOpCacheEvictor(), new ExoDatabaseProvider(this));
            MediaSourceFactory mediaSourceFactory =
                    new DefaultMediaSourceFactory(buildReadOnlyCacheDataSource(upstreamFactory,
                            mCache));
            mDefaultTrackSelector = new DefaultTrackSelector(/* context= */ this);
            mDefaultTrackSelector.setParameters(trackSelectorParameters);
            mLastSeenTrackGroupArray = null;
            mPlayer =
                    new SimpleExoPlayer.Builder(/* context= */ this, renderersFactory)
                            .setMediaSourceFactory(mediaSourceFactory)
                            .setTrackSelector(mDefaultTrackSelector)
                            .build();
            mPlayer.addListener(new PlayerEventListener());
            mPlayer.addAnalyticsListener(new EventLogger(mDefaultTrackSelector));
            mPlayer.setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true);
            mPlayer.setPlayWhenReady(mStartAutoPlay);
            mPlayerView.setPlayer(mPlayer);

        }
        boolean haveStartPosition = mStartWindow != C.INDEX_UNSET;
        if (haveStartPosition) {
            mPlayer.seekTo(mStartWindow, mStartPosition);
        }
        mPlayer.setMediaItems(mediaItems, /* resetPosition= */ !haveStartPosition);
        mPlayer.prepare();
        return true;
    }

    protected void releasePlayer() {
        if (mPlayer != null) {
            updateTrackSelectorParameters();
            updateStartPosition();
            mPlayer.release();
            mCache.release();
            mCache = null;
            mPlayer = null;
            mDefaultTrackSelector = null;
        }

    }

    private static CacheDataSource.Factory buildReadOnlyCacheDataSource(
            DataSource.Factory upstreamFactory, Cache cache) {
        return new CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
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
        activity.getWindow().getDecorView().setSystemUiVisibility(
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


    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void updateStartPosition() {
        if (mPlayer != null) {
            mStartAutoPlay = mPlayer.getPlayWhenReady();
            mStartWindow = mPlayer.getCurrentWindowIndex();
            mStartPosition = Math.max(0, mPlayer.getContentPosition());
        }
    }

    private void updateTrackSelectorParameters() {
        if (mDefaultTrackSelector != null) {
            trackSelectorParameters = mDefaultTrackSelector.getParameters();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);
        mPlayerView = findViewById(R.id.player_view);
        mPlayerView.setControllerVisibilityListener(this);
        mPlayerView.setErrorMessageProvider(new PlayerErrorMessageProvider());
        mPlayerView.requestFocus();
        mPlayerView.setControllerOnFullScreenModeChangedListener(isFullScreen -> {
            if (isFullScreen) {
                hideSystemUI(BilibiliPlayerActivity.this, true);
                rotateScreen(BilibiliPlayerActivity.this);
                mPlayerView.setPadding(0, 0, 0, 0);
            } else {
                showSystemUI(BilibiliPlayerActivity.this, true);
                rotateScreen(BilibiliPlayerActivity.this);
                mPlayerView.setPadding(0, 0, 0, getNavigationBarHeight(BilibiliPlayerActivity.this));
            }
        });
        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
            mStartAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            mStartWindow = savedInstanceState.getInt(KEY_WINDOW);
            mStartPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            DefaultTrackSelector.ParametersBuilder builder =
                    new DefaultTrackSelector.ParametersBuilder(/* context= */ this);
            trackSelectorParameters = builder.build();
            clearStartPosition();
        }
        mPlayerView.setOnDownloadListener(this::onDownload);
    }

    private void onDownload(View view) {
        String[] strings = getIntent().getStringArrayExtra(KEY_PLAYLIST);
        int i = 1;
        for (String s : strings) {
            WebViewShare.downloadFile(this,
                    KeyShare.md5(s) + "-" + (i++) + ".f4v", s, "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Safari/537.36");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPlayer == null) {
            initializePlayer();
            if (mPlayerView != null) {
                mPlayerView.onResume();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
        if (mPlayerView != null) {
            mPlayerView.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mPlayerView != null) {
            mPlayerView.onPause();
        }
        releasePlayer();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        releasePlayer();
        clearStartPosition();
        setIntent(intent);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        updateTrackSelectorParameters();
        updateStartPosition();
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters);
        outState.putBoolean(KEY_AUTO_PLAY, mStartAutoPlay);
        outState.putInt(KEY_WINDOW, mStartWindow);
        outState.putLong(KEY_POSITION, mStartPosition);
    }

    @Override
    public void onVisibilityChange(int visibility) {
    }

    private class PlayerEventListener implements Player.Listener {

        @Override
        public void onPlaybackStateChanged(@Player.State int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
            }
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                mPlayer.seekToDefaultPosition();
                mPlayer.prepare();
            } else {
            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(
                @NonNull TrackGroupArray trackGroups, @NonNull TrackSelectionArray trackSelections) {
            if (trackGroups != mLastSeenTrackGroupArray) {
                MappedTrackInfo mappedTrackInfo = mDefaultTrackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_video);
                    }
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_audio);
                    }
                }
                mLastSeenTrackGroupArray = trackGroups;
            }
        }
    }

    private class PlayerErrorMessageProvider implements ErrorMessageProvider<PlaybackException> {

        @Override
        @NonNull
        public Pair<Integer, String> getErrorMessage(@NonNull PlaybackException e) {
            String errorString = getString(R.string.error_generic);
            Throwable cause = e.getCause();
            if (cause instanceof DecoderInitializationException) {
                // Special case for decoder initialization failures.
                DecoderInitializationException decoderInitializationException =
                        (DecoderInitializationException) cause;
                if (decoderInitializationException.codecInfo == null) {
                    if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
                        errorString = getString(R.string.error_querying_decoders);
                    } else if (decoderInitializationException.secureDecoderRequired) {
                        errorString =
                                getString(
                                        R.string.error_no_secure_decoder, decoderInitializationException.mimeType);
                    } else {
                        errorString =
                                getString(R.string.error_no_decoder, decoderInitializationException.mimeType);
                    }
                } else {
                    errorString =
                            getString(
                                    R.string.error_instantiating_decoder,
                                    decoderInitializationException.codecInfo.name);
                }
            }
            return Pair.create(0, errorString);
        }
    }
}
