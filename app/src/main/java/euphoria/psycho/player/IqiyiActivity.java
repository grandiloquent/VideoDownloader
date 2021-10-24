package euphoria.psycho.player;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.EventLogger;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;
import euphoria.psycho.videos.Iqiyi;

public class IqiyiActivity extends Activity implements
        Iqiyi.Callback, StyledPlayerControlView.VisibilityListener {
    public static final long DEFAULT_SHOW_TIMEOUT_MS = 5000L;
    public static final String EXTRA_PLAYLSIT = "extra.PLAYLSIT";
    public static final String EXTRA_TYPE = "extra.TYPE";
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
    private static final String KEY_AUTO_PLAY = "auto_play";
    private static final String KEY_POSITION = "position";
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";
    private static final String USER_AGENT =
            "ExoPlayerDemo/"
                    + ExoPlayerLibraryInfo.VERSION
                    + " (Linux; Android "
                    + Build.VERSION.RELEASE
                    + ") "
                    + ExoPlayerLibraryInfo.VERSION_SLASHY;
    private static DataSource.Factory dataSourceFactory;
    private static DatabaseProvider databaseProvider;
    private static Cache downloadCache;
    private static File downloadDirectory;
    private static HttpDataSource.Factory httpDataSourceFactory;
    protected StyledPlayerView playerView;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private boolean startAutoPlay;
    private int startWindow;
    private long startPosition;
    protected @Nullable
    SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private TrackGroupArray lastSeenTrackGroupArray;

    public static RenderersFactory buildRenderersFactory(
            Context context, boolean preferExtensionRenderer) {
        @DefaultRenderersFactory.ExtensionRendererMode
        int extensionRendererMode =
                true
                        ? (preferExtensionRenderer
                        ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
        return new DefaultRenderersFactory(context.getApplicationContext())
                .setExtensionRendererMode(extensionRendererMode);
    }

    public static synchronized DataSource.Factory getDataSourceFactory(Context context) {
        if (dataSourceFactory == null) {
            context = context.getApplicationContext();
            DefaultDataSourceFactory upstreamFactory =
                    new DefaultDataSourceFactory(context, getHttpDataSourceFactory(context));
            dataSourceFactory = buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache(context));
        }
        return dataSourceFactory;
    }

    public static synchronized HttpDataSource.Factory getHttpDataSourceFactory(Context context) {
        if (httpDataSourceFactory == null) {
            if (httpDataSourceFactory == null) {
                // We don't want to use Cronet, or we failed to instantiate a CronetEngine.
                CookieManager cookieManager = new CookieManager();
                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
                CookieHandler.setDefault(cookieManager);
                httpDataSourceFactory = new DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT);
            }
        }
        return httpDataSourceFactory;
    }

    protected void clearStartPosition() {
        startAutoPlay = true;
        startWindow = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    protected boolean initializePlayer() {
        if (player == null) {
            Intent intent = getIntent();
            Iqiyi.getVideoAddress(intent.getStringArrayExtra(EXTRA_PLAYLSIT)[0], this);
            boolean preferExtensionDecoders = true;
            RenderersFactory renderersFactory =
                    buildRenderersFactory(/* context= */ this, preferExtensionDecoders);
            MediaSourceFactory mediaSourceFactory =
                    new DefaultMediaSourceFactory(dataSourceFactory)
                            .setAdViewProvider(playerView);
            trackSelector = new DefaultTrackSelector(/* context= */ this);
            trackSelector.setParameters(trackSelectorParameters);
            lastSeenTrackGroupArray = null;
            player =
                    new SimpleExoPlayer.Builder(/* context= */ this, renderersFactory)
                            .setMediaSourceFactory(mediaSourceFactory)
                            .setTrackSelector(trackSelector)
                            .build();
            player.addListener(new PlayerEventListener());
            player.addAnalyticsListener(new EventLogger(trackSelector));
            player.setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true);
            player.setPlayWhenReady(startAutoPlay);
            playerView.setPlayer(player);

        }
        boolean haveStartPosition = startWindow != C.INDEX_UNSET;
        if (haveStartPosition) {
            player.seekTo(startWindow, startPosition);
        }
        //player.setMediaItems(mediaItems, /* resetPosition= */ !haveStartPosition);
        updateButtonVisibility();
        return true;
    }

    protected void releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters();
            updateStartPosition();
            player.release();
            player = null;
            trackSelector = null;
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


    private static synchronized DatabaseProvider getDatabaseProvider(Context context) {
        if (databaseProvider == null) {
            databaseProvider = new ExoDatabaseProvider(context);
        }
        return databaseProvider;
    }

    private static synchronized Cache getDownloadCache(Context context) {
        if (downloadCache == null) {
            File downloadContentDirectory =
                    new File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache =
                    new SimpleCache(
                            downloadContentDirectory, new NoOpCacheEvictor(), getDatabaseProvider(context));
        }
        return downloadCache;
    }

    private static synchronized File getDownloadDirectory(Context context) {
        if (downloadDirectory == null) {
            downloadDirectory = context.getExternalFilesDir(/* type= */ null);
            if (downloadDirectory == null) {
                downloadDirectory = context.getFilesDir();
            }
        }
        return downloadDirectory;
    }

    @Nullable
    private static Map<String, String> getDrmRequestHeaders(MediaItem item) {
        MediaItem.DrmConfiguration drmConfiguration = item.playbackProperties.drmConfiguration;
        return drmConfiguration != null ? drmConfiguration.requestHeaders : null;
    }

    private void showControls() {
    }

    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void updateButtonVisibility() {
    }

    private void updateStartPosition() {
        if (player != null) {
            startAutoPlay = player.getPlayWhenReady();
            startWindow = player.getCurrentWindowIndex();
            startPosition = Math.max(0, player.getContentPosition());
        }
    }

    private void updateTrackSelectorParameters() {
        if (trackSelector != null) {
            trackSelectorParameters = trackSelector.getParameters();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataSourceFactory = getDataSourceFactory(this);
        setContentView(R.layout.player_activity);
        playerView = findViewById(R.id.player_view);
        playerView.setControllerVisibilityListener(this);
        playerView.setErrorMessageProvider(new PlayerErrorMessageProvider());
        playerView.requestFocus();
        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            DefaultTrackSelector.ParametersBuilder builder =
                    new DefaultTrackSelector.ParametersBuilder(/* context= */ this);
            trackSelectorParameters = builder.build();
            clearStartPosition();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player == null) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
        if (playerView != null) {
            playerView.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (playerView != null) {
            playerView.onPause();
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
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        outState.putInt(KEY_WINDOW, startWindow);
        outState.putLong(KEY_POSITION, startPosition);
    }

    @Override
    public void onVideoUri(String uri) {
        runOnUiThread(() -> {
            if (uri != null) {
                Log.e("B5aOx2", String.format("onVideoUri, %s", uri));
                player.setMediaSource(new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri)));
                player.prepare();
            } else {
                Toast.makeText(IqiyiActivity.this, "无法解析视频", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    public void onVisibilityChange(int visibility) {
    }

    private class PlayerEventListener implements Player.Listener {

        @Override
        public void onPlaybackStateChanged(@Player.State int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                showControls();
            }
            updateButtonVisibility();
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition();
                player.prepare();
            } else {
                updateButtonVisibility();
                showControls();
            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(
                @NonNull TrackGroupArray trackGroups, @NonNull TrackSelectionArray trackSelections) {
            updateButtonVisibility();
            if (trackGroups != lastSeenTrackGroupArray) {
                MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
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
                lastSeenTrackGroupArray = trackGroups;
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
            Log.e("B5aOx2", String.format("getErrorMessage, %s", errorString));
            return Pair.create(0, errorString);
        }
    }
}
