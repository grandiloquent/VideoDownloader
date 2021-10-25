package euphoria.psycho.player;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.DefaultControlDispatcher;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ForwardingPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.Events;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.ui.TrackNameProvider;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.RepeatModeUtil;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import euphoria.psycho.explorer.R;

import static com.google.android.exoplayer2.Player.COMMAND_SEEK_BACK;
import static com.google.android.exoplayer2.Player.COMMAND_SEEK_FORWARD;
import static com.google.android.exoplayer2.Player.COMMAND_SEEK_IN_CURRENT_WINDOW;
import static com.google.android.exoplayer2.Player.COMMAND_SEEK_TO_NEXT;
import static com.google.android.exoplayer2.Player.COMMAND_SEEK_TO_PREVIOUS;
import static com.google.android.exoplayer2.Player.EVENT_AVAILABLE_COMMANDS_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_IS_PLAYING_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_PLAYBACK_PARAMETERS_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_PLAYBACK_STATE_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_PLAY_WHEN_READY_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_POSITION_DISCONTINUITY;
import static com.google.android.exoplayer2.Player.EVENT_REPEAT_MODE_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_SEEK_BACK_INCREMENT_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_SEEK_FORWARD_INCREMENT_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_TIMELINE_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_TRACKS_CHANGED;
import static com.google.android.exoplayer2.util.Assertions.checkNotNull;

public class SimplePlayerControlView extends FrameLayout {
    public static final
    int DEFAULT_REPEAT_TOGGLE_MODES =
            RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE;

    public static final int DEFAULT_SHOW_TIMEOUT_MS = 5_000;

    public static final int DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS = 200;

    public static final int MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100;

    private static final int MAX_UPDATE_INTERVAL_MS = 1_000;
    private static final int SETTINGS_AUDIO_TRACK_SELECTION_POSITION = 1;
    private static final int SETTINGS_PLAYBACK_SPEED_POSITION = 0;

    static {
        ExoPlayerLibraryInfo.registerModule("goog.exo.ui");
    }

    private final float buttonAlphaDisabled;
    private final float buttonAlphaEnabled;
    private final ComponentListener componentListener;

    private final TextView durationView;

    private final View fastForwardButton;

    private final TextView fastForwardButtonTextView;
    private final StringBuilder formatBuilder;
    private final Formatter formatter;
    private final String fullScreenEnterContentDescription;
    private final Drawable fullScreenEnterDrawable;
    private final String fullScreenExitContentDescription;
    private final Drawable fullScreenExitDrawable;

    private final View nextButton;
    private final Timeline.Period period;

    private final View playPauseButton;

    private final TextView positionView;

    private final View previousButton;
    private final String repeatAllButtonContentDescription;
    private final Drawable repeatAllButtonDrawable;
    private final String repeatOffButtonContentDescription;
    private final Drawable repeatOffButtonDrawable;
    private final String repeatOneButtonContentDescription;
    private final Drawable repeatOneButtonDrawable;

    private final ImageView repeatToggleButton;

    private final View rewindButton;

    private final TextView rewindButtonTextView;

    private final ImageView shuffleButton;
    private final Drawable shuffleOffButtonDrawable;
    private final String shuffleOffContentDescription;
    private final Drawable shuffleOnButtonDrawable;
    private final String shuffleOnContentDescription;
    private final Drawable subtitleOffButtonDrawable;
    private final String subtitleOffContentDescription;
    private final Drawable subtitleOnButtonDrawable;
    private final String subtitleOnContentDescription;

    private final com.google.android.exoplayer2.ui.TimeBar timeBar;
    private final Runnable updateProgressAction;
    private final CopyOnWriteArrayList<VisibilityListener> visibilityListeners;

    private final Timeline.Window window;

    private Player player;
    private ControlDispatcher controlDispatcher;

    private ProgressUpdateListener progressUpdateListener;

    private OnFullScreenModeChangedListener onFullScreenModeChangedListener;
    private boolean isFullScreen;
    private boolean isAttachedToWindow;
    private boolean showMultiWindowTimeBar;
    private boolean multiWindowTimeBar;
    private boolean scrubbing;
    private int showTimeoutMs;
    private int timeBarMinUpdateIntervalMs;
    private
    int repeatToggleModes;
    private long currentWindowOffset;
    private SimplePlayerControlViewLayoutManager controlViewLayoutManager;
    private Resources resources;
    private RecyclerView settingsView;
    private SettingsAdapter settingsAdapter;
    private PlaybackSpeedAdapter playbackSpeedAdapter;
    private PopupWindow settingsWindow;
    private boolean needToHideBars;
    private int settingsWindowMargin;

    private DefaultTrackSelector trackSelector;
    private TrackSelectionAdapter textTrackSelectionAdapter;
    private TrackSelectionAdapter audioTrackSelectionAdapter;

    private TrackNameProvider trackNameProvider;

    private ImageView subtitleButton;

    private ImageView fullScreenButton;

    private ImageView minimalFullScreenButton;

    private View settingsButton;

    private View playbackSpeedButton;

    private View audioTrackButton;

    public SimplePlayerControlView(Context context) {
        this(context, null);
    }

    public SimplePlayerControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimplePlayerControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, attrs);
    }

    public SimplePlayerControlView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            AttributeSet playbackAttrs) {
        super(context, attrs, defStyleAttr);
        int controllerLayoutId = R.layout.exo_styled_player_control_view;
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS;
        repeatToggleModes = DEFAULT_REPEAT_TOGGLE_MODES;
        timeBarMinUpdateIntervalMs = DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS;
        boolean showRewindButton = true;
        boolean showFastForwardButton = true;
        boolean showPreviousButton = true;
        boolean showNextButton = true;
        boolean showShuffleButton = false;
        boolean showSubtitleButton = false;
        boolean animationEnabled = true;
        boolean showVrButton = false;
        if (playbackAttrs != null) {
            TypedArray a =
                    context
                            .getTheme()
                            .obtainStyledAttributes(
                                    playbackAttrs,
                                    com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView,
                                    defStyleAttr,
                                    0);
            try {
                controllerLayoutId =
                        a.getResourceId(
                                com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_controller_layout_id, controllerLayoutId);
                showTimeoutMs = a.getInt(com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_show_timeout, showTimeoutMs);
                repeatToggleModes = getRepeatToggleModes(a, repeatToggleModes);
                showRewindButton =
                        a.getBoolean(com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_show_rewind_button, showRewindButton);
                showFastForwardButton =
                        a.getBoolean(
                                com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_show_fastforward_button, showFastForwardButton);
                showPreviousButton =
                        a.getBoolean(
                                com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_show_previous_button, showPreviousButton);
                showNextButton =
                        a.getBoolean(com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_show_next_button, showNextButton);
                showShuffleButton =
                        a.getBoolean(
                                com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_show_shuffle_button, showShuffleButton);
                showSubtitleButton =
                        a.getBoolean(
                                com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_show_subtitle_button, showSubtitleButton);
                showVrButton =
                        a.getBoolean(com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_show_vr_button, showVrButton);
                setTimeBarMinUpdateInterval(
                        a.getInt(
                                com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_time_bar_min_update_interval,
                                timeBarMinUpdateIntervalMs));
                animationEnabled =
                        a.getBoolean(com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_animation_enabled, animationEnabled);
            } finally {
                a.recycle();
            }
        }
        LayoutInflater.from(context).inflate(controllerLayoutId, this);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        componentListener = new ComponentListener();
        visibilityListeners = new CopyOnWriteArrayList<>();
        period = new Timeline.Period();
        window = new Timeline.Window();
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        controlDispatcher = new DefaultControlDispatcher();
        updateProgressAction = this::updateProgress;
        durationView = findViewById(com.google.android.exoplayer2.ui.R.id.exo_duration);
        positionView = findViewById(com.google.android.exoplayer2.ui.R.id.exo_position);
        subtitleButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_subtitle);
        if (subtitleButton != null) {
            subtitleButton.setOnClickListener(componentListener);
        }
        fullScreenButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_fullscreen);
        initializeFullScreenButton(fullScreenButton, this::onFullScreenButtonClicked);
        minimalFullScreenButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_minimal_fullscreen);
        initializeFullScreenButton(minimalFullScreenButton, this::onFullScreenButtonClicked);
        settingsButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_settings);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(componentListener);
        }
        playbackSpeedButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_playback_speed);
        if (playbackSpeedButton != null) {
            playbackSpeedButton.setOnClickListener(componentListener);
        }
        audioTrackButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_audio_track);
        if (audioTrackButton != null) {
            audioTrackButton.setOnClickListener(componentListener);
        }
        com.google.android.exoplayer2.ui.TimeBar customTimeBar = findViewById(com.google.android.exoplayer2.ui.R.id.exo_progress);
        View timeBarPlaceholder = findViewById(com.google.android.exoplayer2.ui.R.id.exo_progress_placeholder);
        if (customTimeBar != null) {
            timeBar = customTimeBar;
        } else if (timeBarPlaceholder != null) {
            com.google.android.exoplayer2.ui.DefaultTimeBar defaultTimeBar =
                    new DefaultTimeBar(context, null, 0, playbackAttrs, com.google.android.exoplayer2.ui.R.style.ExoStyledControls_TimeBar);
            defaultTimeBar.setId(com.google.android.exoplayer2.ui.R.id.exo_progress);
            defaultTimeBar.setLayoutParams(timeBarPlaceholder.getLayoutParams());
            ViewGroup parent = ((ViewGroup) timeBarPlaceholder.getParent());
            int timeBarIndex = parent.indexOfChild(timeBarPlaceholder);
            parent.removeView(timeBarPlaceholder);
            parent.addView(defaultTimeBar, timeBarIndex);
            timeBar = defaultTimeBar;
        } else {
            timeBar = null;
        }
        if (timeBar != null) {
            timeBar.addListener(componentListener);
        }
        playPauseButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_play_pause);
        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(componentListener);
        }
        previousButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_prev);
        if (previousButton != null) {
            previousButton.setOnClickListener(componentListener);
        }
        nextButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_next);
        if (nextButton != null) {
            nextButton.setOnClickListener(componentListener);
        }
        Typeface typeface = ResourcesCompat.getFont(context, com.google.android.exoplayer2.ui.R.font.roboto_medium_numbers);
        View rewButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_rew);
        rewindButtonTextView = rewButton == null ? findViewById(com.google.android.exoplayer2.ui.R.id.exo_rew_with_amount) : null;
        if (rewindButtonTextView != null) {
            rewindButtonTextView.setTypeface(typeface);
        }
        rewindButton = rewButton == null ? rewindButtonTextView : rewButton;
        if (rewindButton != null) {
            rewindButton.setOnClickListener(componentListener);
        }
        View ffwdButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_ffwd);
        fastForwardButtonTextView = ffwdButton == null ? findViewById(com.google.android.exoplayer2.ui.R.id.exo_ffwd_with_amount) : null;
        if (fastForwardButtonTextView != null) {
            fastForwardButtonTextView.setTypeface(typeface);
        }
        fastForwardButton = ffwdButton == null ? fastForwardButtonTextView : ffwdButton;
        if (fastForwardButton != null) {
            fastForwardButton.setOnClickListener(componentListener);
        }
        repeatToggleButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_repeat_toggle);
        if (repeatToggleButton != null) {
            repeatToggleButton.setOnClickListener(componentListener);
        }
        shuffleButton = findViewById(com.google.android.exoplayer2.ui.R.id.exo_shuffle);
        if (shuffleButton != null) {
            shuffleButton.setOnClickListener(componentListener);
        }
        resources = context.getResources();
        buttonAlphaEnabled =
                (float) resources.getInteger(com.google.android.exoplayer2.ui.R.integer.exo_media_button_opacity_percentage_enabled) / 100;
        buttonAlphaDisabled =
                (float) resources.getInteger(com.google.android.exoplayer2.ui.R.integer.exo_media_button_opacity_percentage_disabled) / 100;
        controlViewLayoutManager = new SimplePlayerControlViewLayoutManager(this);
        controlViewLayoutManager.setAnimationEnabled(animationEnabled);
        String[] settingTexts = new String[2];
        Drawable[] settingIcons = new Drawable[2];
        settingTexts[SETTINGS_PLAYBACK_SPEED_POSITION] =
                resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_playback_speed);
        settingIcons[SETTINGS_PLAYBACK_SPEED_POSITION] =
                resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_speed);
        settingTexts[SETTINGS_AUDIO_TRACK_SELECTION_POSITION] =
                resources.getString(com.google.android.exoplayer2.ui.R.string.exo_track_selection_title_audio);
        settingIcons[SETTINGS_AUDIO_TRACK_SELECTION_POSITION] =
                resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_audiotrack);
        settingsAdapter = new SettingsAdapter(settingTexts, settingIcons);
        settingsWindowMargin = resources.getDimensionPixelSize(com.google.android.exoplayer2.ui.R.dimen.exo_settings_offset);
        settingsView =
                (RecyclerView)
                        LayoutInflater.from(context)
                                .inflate(com.google.android.exoplayer2.ui.R.layout.exo_styled_settings_list, null);
        settingsView.setAdapter(settingsAdapter);
        settingsView.setLayoutManager(new LinearLayoutManager(getContext()));
        settingsWindow =
                new PopupWindow(settingsView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
        if (Util.SDK_INT < 23) {
            settingsWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        settingsWindow.setOnDismissListener(componentListener);
        needToHideBars = true;
        trackNameProvider = new DefaultTrackNameProvider(getResources());
        subtitleOnButtonDrawable = resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_subtitle_on);
        subtitleOffButtonDrawable = resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_subtitle_off);
        subtitleOnContentDescription =
                resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_cc_enabled_description);
        subtitleOffContentDescription =
                resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_cc_disabled_description);
        textTrackSelectionAdapter = new TextTrackSelectionAdapter();
        audioTrackSelectionAdapter = new AudioTrackSelectionAdapter();
        playbackSpeedAdapter =
                new PlaybackSpeedAdapter(
                        resources.getStringArray(com.google.android.exoplayer2.ui.R.array.exo_playback_speeds),
                        resources.getIntArray(com.google.android.exoplayer2.ui.R.array.exo_speed_multiplied_by_100));
        fullScreenExitDrawable = resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_fullscreen_exit);
        fullScreenEnterDrawable =
                resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_fullscreen_enter);
        repeatOffButtonDrawable = resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_repeat_off);
        repeatOneButtonDrawable = resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_repeat_one);
        repeatAllButtonDrawable = resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_repeat_all);
        shuffleOnButtonDrawable = resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_shuffle_on);
        shuffleOffButtonDrawable = resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_shuffle_off);
        fullScreenExitContentDescription =
                resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_fullscreen_exit_description);
        fullScreenEnterContentDescription =
                resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_fullscreen_enter_description);
        repeatOffButtonContentDescription =
                resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_repeat_off_description);
        repeatOneButtonContentDescription =
                resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_repeat_one_description);
        repeatAllButtonContentDescription =
                resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_repeat_all_description);
        shuffleOnContentDescription = resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_shuffle_on_description);
        shuffleOffContentDescription =
                resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_shuffle_off_description);
        ViewGroup bottomBar = findViewById(com.google.android.exoplayer2.ui.R.id.exo_bottom_bar);
        controlViewLayoutManager.setShowButton(bottomBar, true);
        controlViewLayoutManager.setShowButton(fastForwardButton, showFastForwardButton);
        controlViewLayoutManager.setShowButton(rewindButton, showRewindButton);
        controlViewLayoutManager.setShowButton(previousButton, showPreviousButton);
        controlViewLayoutManager.setShowButton(nextButton, showNextButton);
        controlViewLayoutManager.setShowButton(shuffleButton, showShuffleButton);
        controlViewLayoutManager.setShowButton(subtitleButton, showSubtitleButton);
        controlViewLayoutManager.setShowButton(
                repeatToggleButton, repeatToggleModes != RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE);
        addOnLayoutChangeListener(this::onLayoutChange);
    }

    public void addVisibilityListener(VisibilityListener listener) {
        Assertions.checkNotNull(listener);
        visibilityListeners.add(listener);
    }

    public boolean dispatchMediaKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        Player player = this.player;
        if (player == null || !isHandledMediaKey(keyCode)) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                if (player.getPlaybackState() != Player.STATE_ENDED) {
                    controlDispatcher.dispatchFastForward(player);
                }
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                controlDispatcher.dispatchRewind(player);
            } else if (event.getRepeatCount() == 0) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        dispatchPlayPause(player);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        dispatchPlay(player);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        dispatchPause(player);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        controlDispatcher.dispatchNext(player);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        controlDispatcher.dispatchPrevious(player);
                        break;
                    default:
                        break;
                }
            }
        }
        return true;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper());
        Assertions.checkArgument(
                player == null || player.getApplicationLooper() == Looper.getMainLooper());
        if (this.player == player) {
            return;
        }
        if (this.player != null) {
            this.player.removeListener(componentListener);
        }
        this.player = player;
        if (player != null) {
            player.addListener(componentListener);
        }
        if (player instanceof ForwardingPlayer) {
            player = ((ForwardingPlayer) player).getWrappedPlayer();
        }
        if (player instanceof ExoPlayer) {
            TrackSelector trackSelector = ((ExoPlayer) player).getTrackSelector();
            if (trackSelector instanceof DefaultTrackSelector) {
                this.trackSelector = (DefaultTrackSelector) trackSelector;
            }
        } else {
            this.trackSelector = null;
        }
        updateAll();
    }

    public int getRepeatToggleModes() {
        return repeatToggleModes;
    }

    public void setRepeatToggleModes(int repeatToggleModes) {
        this.repeatToggleModes = repeatToggleModes;
        if (player != null) {
            int currentMode = player.getRepeatMode();
            if (repeatToggleModes == RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE
                    && currentMode != Player.REPEAT_MODE_OFF) {
                controlDispatcher.dispatchSetRepeatMode(player, Player.REPEAT_MODE_OFF);
            } else if (repeatToggleModes == RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE
                    && currentMode == Player.REPEAT_MODE_ALL) {
                controlDispatcher.dispatchSetRepeatMode(player, Player.REPEAT_MODE_ONE);
            } else if (repeatToggleModes == RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
                    && currentMode == Player.REPEAT_MODE_ONE) {
                controlDispatcher.dispatchSetRepeatMode(player, Player.REPEAT_MODE_ALL);
            }
        }
        controlViewLayoutManager.setShowButton(
                repeatToggleButton, repeatToggleModes != RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE);
        updateRepeatModeButton();
    }

    public boolean getShowShuffleButton() {
        return controlViewLayoutManager.getShowButton(shuffleButton);
    }

    public void setShowShuffleButton(boolean showShuffleButton) {
        controlViewLayoutManager.setShowButton(shuffleButton, showShuffleButton);
        updateShuffleButton();
    }

    public boolean getShowSubtitleButton() {
        return controlViewLayoutManager.getShowButton(subtitleButton);
    }

    public void setShowSubtitleButton(boolean showSubtitleButton) {
        controlViewLayoutManager.setShowButton(subtitleButton, showSubtitleButton);
    }

    public int getShowTimeoutMs() {
        return showTimeoutMs;
    }

    public void setShowTimeoutMs(int showTimeoutMs) {
        this.showTimeoutMs = showTimeoutMs;
        if (isFullyVisible()) {
            controlViewLayoutManager.resetHideCallbacks();
        }
    }

    public void hide() {
        controlViewLayoutManager.hide();
    }

    public void hideImmediately() {
        controlViewLayoutManager.hideImmediately();
    }

    public boolean isAnimationEnabled() {
        return controlViewLayoutManager.isAnimationEnabled();
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        controlViewLayoutManager.setAnimationEnabled(animationEnabled);
    }

    public boolean isFullyVisible() {
        return controlViewLayoutManager.isFullyVisible();
    }

    public boolean isVisible() {
        return getVisibility() == VISIBLE;
    }

    public void removeVisibilityListener(VisibilityListener listener) {
        visibilityListeners.remove(listener);
    }

    public void setControlDispatcher(ControlDispatcher controlDispatcher) {
        if (this.controlDispatcher != controlDispatcher) {
            this.controlDispatcher = controlDispatcher;
            updateNavigation();
        }
    }

    public void setOnFullScreenModeChangedListener(
            OnFullScreenModeChangedListener listener) {
        onFullScreenModeChangedListener = listener;
        updateFullScreenButtonVisibility(fullScreenButton, listener != null);
        updateFullScreenButtonVisibility(minimalFullScreenButton, listener != null);
    }

    public void setProgressUpdateListener(ProgressUpdateListener listener) {
        this.progressUpdateListener = listener;
    }

    public void setShowFastForwardButton(boolean showFastForwardButton) {
        controlViewLayoutManager.setShowButton(fastForwardButton, showFastForwardButton);
        updateNavigation();
    }

    public void setShowMultiWindowTimeBar(boolean showMultiWindowTimeBar) {
        this.showMultiWindowTimeBar = showMultiWindowTimeBar;
        updateTimeline();
    }

    public void setShowNextButton(boolean showNextButton) {
        controlViewLayoutManager.setShowButton(nextButton, showNextButton);
        updateNavigation();
    }

    public void setShowPreviousButton(boolean showPreviousButton) {
        controlViewLayoutManager.setShowButton(previousButton, showPreviousButton);
        updateNavigation();
    }

    public void setShowRewindButton(boolean showRewindButton) {
        controlViewLayoutManager.setShowButton(rewindButton, showRewindButton);
        updateNavigation();
    }

    public void setTimeBarMinUpdateInterval(int minUpdateIntervalMs) {
        timeBarMinUpdateIntervalMs =
                Util.constrainValue(minUpdateIntervalMs, 16, MAX_UPDATE_INTERVAL_MS);
    }

    public void show() {
        controlViewLayoutManager.show();
    }

    private static boolean canShowMultiWindowTimeBar(Timeline timeline, Timeline.Window window) {
        if (timeline.getWindowCount() > MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR) {
            return false;
        }
        int windowCount = timeline.getWindowCount();
        for (int i = 0; i < windowCount; i++) {
            if (timeline.getWindow(i, window).durationUs == C.TIME_UNSET) {
                return false;
            }
        }
        return true;
    }

    private static int getRepeatToggleModes(
            TypedArray a, int defaultValue) {
        return a.getInt(com.google.android.exoplayer2.ui.R.styleable.StyledPlayerControlView_repeat_toggle_modes, defaultValue);
    }

    private static void initializeFullScreenButton(View fullScreenButton, OnClickListener listener) {
        if (fullScreenButton == null) {
            return;
        }
        fullScreenButton.setVisibility(GONE);
        fullScreenButton.setOnClickListener(listener);
    }

    private static boolean isHandledMediaKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS;
    }

    private static void updateFullScreenButtonVisibility(
            View fullScreenButton, boolean visible) {
        if (fullScreenButton == null) {
            return;
        }
        if (visible) {
            fullScreenButton.setVisibility(VISIBLE);
        } else {
            fullScreenButton.setVisibility(GONE);
        }
    }

    private void dispatchPause(Player player) {
        controlDispatcher.dispatchSetPlayWhenReady(player, false);
    }

    private void dispatchPlay(Player player) {
        int state = player.getPlaybackState();
        if (state == Player.STATE_IDLE) {
            controlDispatcher.dispatchPrepare(player);
        } else if (state == Player.STATE_ENDED) {
            seekTo(player, player.getCurrentWindowIndex(), C.TIME_UNSET);
        }
        controlDispatcher.dispatchSetPlayWhenReady(player, true);
    }

    private void dispatchPlayPause(Player player) {
        int state = player.getPlaybackState();
        if (state == Player.STATE_IDLE || state == Player.STATE_ENDED || !player.getPlayWhenReady()) {
            dispatchPlay(player);
        } else {
            dispatchPause(player);
        }
    }

    private void displaySettingsWindow(RecyclerView.Adapter<?> adapter) {
        settingsView.setAdapter(adapter);
        updateSettingsWindowSize();
        needToHideBars = false;
        settingsWindow.dismiss();
        needToHideBars = true;
        int xoff = getWidth() - settingsWindow.getWidth() - settingsWindowMargin;
        int yoff = -settingsWindow.getHeight() - settingsWindowMargin;
        settingsWindow.showAsDropDown(this, xoff, yoff);
    }

    private void gatherTrackInfosForAdapter(
            MappedTrackInfo mappedTrackInfo, int rendererIndex, List<TrackInfo> tracks) {
        TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
        TrackSelectionArray trackSelections = checkNotNull(player).getCurrentTrackSelections();
        TrackSelection trackSelection = trackSelections.get(rendererIndex);
        for (int groupIndex = 0; groupIndex < trackGroupArray.length; groupIndex++) {
            TrackGroup trackGroup = trackGroupArray.get(groupIndex);
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                Format format = trackGroup.getFormat(trackIndex);
                if (mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex)
                        == C.FORMAT_HANDLED) {
                    boolean trackIsSelected =
                            trackSelection != null && trackSelection.indexOf(format) != C.INDEX_UNSET;
                    tracks.add(
                            new TrackInfo(
                                    rendererIndex,
                                    groupIndex,
                                    trackIndex,
                                    trackNameProvider.getTrackName(format),
                                    trackIsSelected));
                }
            }
        }
    }

    private void initTrackSelectionAdapter() {
        textTrackSelectionAdapter.clear();
        audioTrackSelectionAdapter.clear();
        if (player == null || trackSelector == null) {
            return;
        }
        DefaultTrackSelector trackSelector = this.trackSelector;
        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return;
        }
        List<TrackInfo> textTracks = new ArrayList<>();
        List<TrackInfo> audioTracks = new ArrayList<>();
        List<Integer> textRendererIndices = new ArrayList<>();
        List<Integer> audioRendererIndices = new ArrayList<>();
        for (int rendererIndex = 0;
             rendererIndex < mappedTrackInfo.getRendererCount();
             rendererIndex++) {
            if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT
                    && controlViewLayoutManager.getShowButton(subtitleButton)) {
                gatherTrackInfosForAdapter(mappedTrackInfo, rendererIndex, textTracks);
                textRendererIndices.add(rendererIndex);
            } else if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_AUDIO) {
                gatherTrackInfosForAdapter(mappedTrackInfo, rendererIndex, audioTracks);
                audioRendererIndices.add(rendererIndex);
            }
        }
        textTrackSelectionAdapter.init(textRendererIndices, textTracks, mappedTrackInfo);
        audioTrackSelectionAdapter.init(audioRendererIndices, audioTracks, mappedTrackInfo);
    }

    private void onFullScreenButtonClicked(View v) {
        if (onFullScreenModeChangedListener == null) {
            return;
        }
        isFullScreen = !isFullScreen;
        updateFullScreenButtonForState(fullScreenButton, isFullScreen);
        updateFullScreenButtonForState(minimalFullScreenButton, isFullScreen);
        if (onFullScreenModeChangedListener != null) {
            onFullScreenModeChangedListener.onFullScreenModeChanged(isFullScreen);
        }
    }

    private void onLayoutChange(
            View v,
            int left,
            int top,
            int right,
            int bottom,
            int oldLeft,
            int oldTop,
            int oldRight,
            int oldBottom) {
        int width = right - left;
        int height = bottom - top;
        int oldWidth = oldRight - oldLeft;
        int oldHeight = oldBottom - oldTop;
        if ((width != oldWidth || height != oldHeight) && settingsWindow.isShowing()) {
            updateSettingsWindowSize();
            int xOffset = getWidth() - settingsWindow.getWidth() - settingsWindowMargin;
            int yOffset = -settingsWindow.getHeight() - settingsWindowMargin;
            settingsWindow.update(v, xOffset, yOffset, -1, -1);
        }
    }

    private void onSettingViewClicked(int position) {
        if (position == SETTINGS_PLAYBACK_SPEED_POSITION) {
            displaySettingsWindow(playbackSpeedAdapter);
        } else if (position == SETTINGS_AUDIO_TRACK_SELECTION_POSITION) {
            displaySettingsWindow(audioTrackSelectionAdapter);
        } else {
            settingsWindow.dismiss();
        }
    }

    private boolean seekTo(Player player, int windowIndex, long positionMs) {
        return controlDispatcher.dispatchSeekTo(player, windowIndex, positionMs);
    }

    private void seekToTimeBarPosition(Player player, long positionMs) {
        int windowIndex;
        Timeline timeline = player.getCurrentTimeline();
        if (multiWindowTimeBar && !timeline.isEmpty()) {
            int windowCount = timeline.getWindowCount();
            windowIndex = 0;
            while (true) {
                long windowDurationMs = timeline.getWindow(windowIndex, window).getDurationMs();
                if (positionMs < windowDurationMs) {
                    break;
                } else if (windowIndex == windowCount - 1) {
                    positionMs = windowDurationMs;
                    break;
                }
                positionMs -= windowDurationMs;
                windowIndex++;
            }
        } else {
            windowIndex = player.getCurrentWindowIndex();
        }
        seekTo(player, windowIndex, positionMs);
        updateProgress();
    }

    private void setPlaybackSpeed(float speed) {
        if (player == null) {
            return;
        }
        controlDispatcher.dispatchSetPlaybackParameters(
                player, player.getPlaybackParameters().withSpeed(speed));
    }

    private boolean shouldShowPauseButton() {
        return player != null
                && player.getPlaybackState() != Player.STATE_ENDED
                && player.getPlaybackState() != Player.STATE_IDLE
                && player.getPlayWhenReady();
    }

    private void updateButton(boolean enabled, View view) {
        if (view == null) {
            return;
        }
        view.setEnabled(enabled);
        view.setAlpha(enabled ? buttonAlphaEnabled : buttonAlphaDisabled);
    }

    private void updateFastForwardButton() {
        long fastForwardMs =
                controlDispatcher instanceof DefaultControlDispatcher && player != null
                        ? ((DefaultControlDispatcher) controlDispatcher).getFastForwardIncrementMs(player)
                        : C.DEFAULT_SEEK_FORWARD_INCREMENT_MS;
        int fastForwardSec = (int) (fastForwardMs / 1_000);
        if (fastForwardButtonTextView != null) {
            fastForwardButtonTextView.setText(String.valueOf(fastForwardSec));
        }
        if (fastForwardButton != null) {
            fastForwardButton.setContentDescription(
                    resources.getQuantityString(
                            com.google.android.exoplayer2.ui.R.plurals.exo_controls_fastforward_by_amount_description,
                            fastForwardSec,
                            fastForwardSec));
        }
    }

    private void updateFullScreenButtonForState(
            ImageView fullScreenButton, boolean isFullScreen) {
        if (fullScreenButton == null) {
            return;
        }
        if (isFullScreen) {
            fullScreenButton.setImageDrawable(fullScreenExitDrawable);
            fullScreenButton.setContentDescription(fullScreenExitContentDescription);
        } else {
            fullScreenButton.setImageDrawable(fullScreenEnterDrawable);
            fullScreenButton.setContentDescription(fullScreenEnterContentDescription);
        }
    }

    private void updateNavigation() {
        if (!isVisible() || !isAttachedToWindow) {
            return;
        }
        Player player = this.player;
        boolean enableSeeking = false;
        boolean enablePrevious = false;
        boolean enableRewind = false;
        boolean enableFastForward = false;
        boolean enableNext = false;
        if (player != null) {
            enableSeeking = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_WINDOW);
            enablePrevious = player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS);
            enableRewind =
                    player.isCommandAvailable(COMMAND_SEEK_BACK) && controlDispatcher.isRewindEnabled();
            enableFastForward =
                    player.isCommandAvailable(COMMAND_SEEK_FORWARD)
                            && controlDispatcher.isFastForwardEnabled();
            enableNext = player.isCommandAvailable(COMMAND_SEEK_TO_NEXT);
        }
        if (enableRewind) {
            updateRewindButton();
        }
        if (enableFastForward) {
            updateFastForwardButton();
        }
        updateButton(enablePrevious, previousButton);
        updateButton(enableRewind, rewindButton);
        updateButton(enableFastForward, fastForwardButton);
        updateButton(enableNext, nextButton);
        if (timeBar != null) {
            timeBar.setEnabled(enableSeeking);
        }
    }

    private void updatePlayPauseButton() {
        if (!isVisible() || !isAttachedToWindow) {
            return;
        }
        if (playPauseButton != null) {
            if (shouldShowPauseButton()) {
                ((ImageView) playPauseButton)
                        .setImageDrawable(resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_pause));
                playPauseButton.setContentDescription(
                        resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_pause_description));
            } else {
                ((ImageView) playPauseButton)
                        .setImageDrawable(resources.getDrawable(com.google.android.exoplayer2.ui.R.drawable.exo_styled_controls_play));
                playPauseButton.setContentDescription(
                        resources.getString(com.google.android.exoplayer2.ui.R.string.exo_controls_play_description));
            }
        }
    }

    private void updatePlaybackSpeedList() {
        if (player == null) {
            return;
        }
        playbackSpeedAdapter.updateSelectedIndex(player.getPlaybackParameters().speed);
        settingsAdapter.setSubTextAtPosition(
                SETTINGS_PLAYBACK_SPEED_POSITION, playbackSpeedAdapter.getSelectedText());
    }

    private void updateProgress() {
        if (!isVisible() || !isAttachedToWindow) {
            return;
        }
        Player player = this.player;
        long position = 0;
        long bufferedPosition = 0;
        if (player != null) {
            position = currentWindowOffset + player.getContentPosition();
            bufferedPosition = currentWindowOffset + player.getContentBufferedPosition();
        }
        if (positionView != null && !scrubbing) {
            positionView.setText(Util.getStringForTime(formatBuilder, formatter, position));
        }
        if (timeBar != null) {
            timeBar.setPosition(position);
            timeBar.setBufferedPosition(bufferedPosition);
        }
        if (progressUpdateListener != null) {
            progressUpdateListener.onProgressUpdate(position, bufferedPosition);
        }
        removeCallbacks(updateProgressAction);
        int playbackState = player == null ? Player.STATE_IDLE : player.getPlaybackState();
        if (player != null && player.isPlaying()) {
            long mediaTimeDelayMs =
                    timeBar != null ? timeBar.getPreferredUpdateDelay() : MAX_UPDATE_INTERVAL_MS;
            long mediaTimeUntilNextFullSecondMs = 1000 - position % 1000;
            mediaTimeDelayMs = Math.min(mediaTimeDelayMs, mediaTimeUntilNextFullSecondMs);
            float playbackSpeed = player.getPlaybackParameters().speed;
            long delayMs =
                    playbackSpeed > 0 ? (long) (mediaTimeDelayMs / playbackSpeed) : MAX_UPDATE_INTERVAL_MS;
            delayMs = Util.constrainValue(delayMs, timeBarMinUpdateIntervalMs, MAX_UPDATE_INTERVAL_MS);
            postDelayed(updateProgressAction, delayMs);
        } else if (playbackState != Player.STATE_ENDED && playbackState != Player.STATE_IDLE) {
            postDelayed(updateProgressAction, MAX_UPDATE_INTERVAL_MS);
        }
    }

    private void updateRepeatModeButton() {
        if (!isVisible() || !isAttachedToWindow || repeatToggleButton == null) {
            return;
        }
        if (repeatToggleModes == RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE) {
            updateButton(false, repeatToggleButton);
            return;
        }
        Player player = this.player;
        if (player == null) {
            updateButton(false, repeatToggleButton);
            repeatToggleButton.setImageDrawable(repeatOffButtonDrawable);
            repeatToggleButton.setContentDescription(repeatOffButtonContentDescription);
            return;
        }
        updateButton(true, repeatToggleButton);
        switch (player.getRepeatMode()) {
            case Player.REPEAT_MODE_OFF:
                repeatToggleButton.setImageDrawable(repeatOffButtonDrawable);
                repeatToggleButton.setContentDescription(repeatOffButtonContentDescription);
                break;
            case Player.REPEAT_MODE_ONE:
                repeatToggleButton.setImageDrawable(repeatOneButtonDrawable);
                repeatToggleButton.setContentDescription(repeatOneButtonContentDescription);
                break;
            case Player.REPEAT_MODE_ALL:
                repeatToggleButton.setImageDrawable(repeatAllButtonDrawable);
                repeatToggleButton.setContentDescription(repeatAllButtonContentDescription);
                break;
            default:

        }
    }

    private void updateRewindButton() {
        long rewindMs =
                controlDispatcher instanceof DefaultControlDispatcher && player != null
                        ? ((DefaultControlDispatcher) controlDispatcher).getRewindIncrementMs(player)
                        : C.DEFAULT_SEEK_BACK_INCREMENT_MS;
        int rewindSec = (int) (rewindMs / 1_000);
        if (rewindButtonTextView != null) {
            rewindButtonTextView.setText(String.valueOf(rewindSec));
        }
        if (rewindButton != null) {
            rewindButton.setContentDescription(
                    resources.getQuantityString(
                            com.google.android.exoplayer2.ui.R.plurals.exo_controls_rewind_by_amount_description, rewindSec, rewindSec));
        }
    }

    private void updateSettingsWindowSize() {
        settingsView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        int maxWidth = getWidth() - settingsWindowMargin * 2;
        int itemWidth = settingsView.getMeasuredWidth();
        int width = Math.min(itemWidth, maxWidth);
        settingsWindow.setWidth(width);
        int maxHeight = getHeight() - settingsWindowMargin * 2;
        int totalHeight = settingsView.getMeasuredHeight();
        int height = Math.min(maxHeight, totalHeight);
        settingsWindow.setHeight(height);
    }

    private void updateShuffleButton() {
        if (!isVisible() || !isAttachedToWindow || shuffleButton == null) {
            return;
        }
        Player player = this.player;
        if (!controlViewLayoutManager.getShowButton(shuffleButton)) {
            updateButton(false, shuffleButton);
        } else if (player == null) {
            updateButton(false, shuffleButton);
            shuffleButton.setImageDrawable(shuffleOffButtonDrawable);
            shuffleButton.setContentDescription(shuffleOffContentDescription);
        } else {
            updateButton(true, shuffleButton);
            shuffleButton.setImageDrawable(
                    player.getShuffleModeEnabled() ? shuffleOnButtonDrawable : shuffleOffButtonDrawable);
            shuffleButton.setContentDescription(
                    player.getShuffleModeEnabled()
                            ? shuffleOnContentDescription
                            : shuffleOffContentDescription);
        }
    }

    private void updateTimeline() {
        Player player = this.player;
        if (player == null) {
            return;
        }
        multiWindowTimeBar =
                showMultiWindowTimeBar && canShowMultiWindowTimeBar(player.getCurrentTimeline(), window);
        currentWindowOffset = 0;
        long durationUs = 0;
        int adGroupCount = 0;
        Timeline timeline = player.getCurrentTimeline();
        if (!timeline.isEmpty()) {
            int currentWindowIndex = player.getCurrentWindowIndex();
            int firstWindowIndex = multiWindowTimeBar ? 0 : currentWindowIndex;
            int lastWindowIndex = multiWindowTimeBar ? timeline.getWindowCount() - 1 : currentWindowIndex;
            for (int i = firstWindowIndex; i <= lastWindowIndex; i++) {
                if (i == currentWindowIndex) {
                    currentWindowOffset = C.usToMs(durationUs);
                }
                timeline.getWindow(i, window);
                if (window.durationUs == C.TIME_UNSET) {
                    Assertions.checkState(!multiWindowTimeBar);
                    break;
                }
                for (int j = window.firstPeriodIndex; j <= window.lastPeriodIndex; j++) {
                    timeline.getPeriod(j, period);
                }
                durationUs += window.durationUs;
            }
        }
        long durationMs = C.usToMs(durationUs);
        if (durationView != null) {
            durationView.setText(Util.getStringForTime(formatBuilder, formatter, durationMs));
        }
        if (timeBar != null) {
            timeBar.setDuration(durationMs);
        }
        updateProgress();
    }

    private void updateTrackLists() {
        initTrackSelectionAdapter();
        updateButton(textTrackSelectionAdapter.getItemCount() > 0, subtitleButton);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        controlViewLayoutManager.onLayout(changed, left, top, right, bottom);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        controlViewLayoutManager.onAttachedToWindow();
        isAttachedToWindow = true;
        if (isFullyVisible()) {
            controlViewLayoutManager.resetHideCallbacks();
        }
        updateAll();
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        controlViewLayoutManager.onDetachedFromWindow();
        isAttachedToWindow = false;
        removeCallbacks(updateProgressAction);
        controlViewLayoutManager.removeHideCallbacks();
    }

    public interface VisibilityListener {
        void onVisibilityChange(int visibility);
    }

    public interface ProgressUpdateListener {
        void onProgressUpdate(long position, long bufferedPosition);
    }

    public interface OnFullScreenModeChangedListener {

        void onFullScreenModeChanged(boolean isFullScreen);
    }

    private static final class TrackInfo {

        public final int groupIndex;
        public final int rendererIndex;
        public final boolean selected;
        public final int trackIndex;
        public final String trackName;

        public TrackInfo(
                int rendererIndex, int groupIndex, int trackIndex, String trackName, boolean selected) {
            this.rendererIndex = rendererIndex;
            this.groupIndex = groupIndex;
            this.trackIndex = trackIndex;
            this.trackName = trackName;
            this.selected = selected;
        }
    }

    private static class SubSettingViewHolder extends RecyclerView.ViewHolder {

        public final View checkView;
        public final TextView textView;

        public SubSettingViewHolder(View itemView) {
            super(itemView);
            if (Util.SDK_INT < 26) {
                itemView.setFocusable(true);
            }
            textView = itemView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_text);
            checkView = itemView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_check);
        }
    }

    private final class ComponentListener
            implements Player.Listener,
            com.google.android.exoplayer2.ui.TimeBar.OnScrubListener,
            OnClickListener,
            PopupWindow.OnDismissListener {
        public void onClick(View view) {
            Player player = SimplePlayerControlView.this.player;
            if (player == null) {
                return;
            }
            controlViewLayoutManager.resetHideCallbacks();
            if (nextButton == view) {
                controlDispatcher.dispatchNext(player);
            } else if (previousButton == view) {
                controlDispatcher.dispatchPrevious(player);
            } else if (fastForwardButton == view) {
                if (player.getPlaybackState() != Player.STATE_ENDED) {
                    controlDispatcher.dispatchFastForward(player);
                }
            } else if (rewindButton == view) {
                controlDispatcher.dispatchRewind(player);
            } else if (playPauseButton == view) {
                dispatchPlayPause(player);
            } else if (repeatToggleButton == view) {
                controlDispatcher.dispatchSetRepeatMode(
                        player, RepeatModeUtil.getNextRepeatMode(player.getRepeatMode(), repeatToggleModes));
            } else if (shuffleButton == view) {
                controlDispatcher.dispatchSetShuffleModeEnabled(player, !player.getShuffleModeEnabled());
            } else if (settingsButton == view) {
                controlViewLayoutManager.removeHideCallbacks();
                displaySettingsWindow(settingsAdapter);
            } else if (playbackSpeedButton == view) {
                controlViewLayoutManager.removeHideCallbacks();
                displaySettingsWindow(playbackSpeedAdapter);
            } else if (audioTrackButton == view) {
                controlViewLayoutManager.removeHideCallbacks();
                displaySettingsWindow(audioTrackSelectionAdapter);
            } else if (subtitleButton == view) {
                controlViewLayoutManager.removeHideCallbacks();
                displaySettingsWindow(textTrackSelectionAdapter);
            }
        }

        public void onDismiss() {
            if (needToHideBars) {
                controlViewLayoutManager.resetHideCallbacks();
            }
        }

        public void onEvents(Player player, Events events) {
            if (events.containsAny(EVENT_PLAYBACK_STATE_CHANGED, EVENT_PLAY_WHEN_READY_CHANGED)) {
                updatePlayPauseButton();
            }
            if (events.containsAny(
                    EVENT_PLAYBACK_STATE_CHANGED, EVENT_PLAY_WHEN_READY_CHANGED, EVENT_IS_PLAYING_CHANGED)) {
                updateProgress();
            }
            if (events.contains(EVENT_REPEAT_MODE_CHANGED)) {
                updateRepeatModeButton();
            }
            if (events.contains(EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                updateShuffleButton();
            }
            if (events.containsAny(
                    EVENT_REPEAT_MODE_CHANGED,
                    EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    EVENT_POSITION_DISCONTINUITY,
                    EVENT_TIMELINE_CHANGED,
                    EVENT_SEEK_BACK_INCREMENT_CHANGED,
                    EVENT_SEEK_FORWARD_INCREMENT_CHANGED,
                    EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                updateNavigation();
            }
            if (events.containsAny(EVENT_POSITION_DISCONTINUITY, EVENT_TIMELINE_CHANGED)) {
                updateTimeline();
            }
            if (events.contains(EVENT_PLAYBACK_PARAMETERS_CHANGED)) {
                updatePlaybackSpeedList();
            }
            if (events.contains(EVENT_TRACKS_CHANGED)) {
                updateTrackLists();
            }
        }

        public void onScrubMove(com.google.android.exoplayer2.ui.TimeBar timeBar, long position) {
            if (positionView != null) {
                positionView.setText(Util.getStringForTime(formatBuilder, formatter, position));
            }
        }

        public void onScrubStart(com.google.android.exoplayer2.ui.TimeBar timeBar, long position) {
            scrubbing = true;
            if (positionView != null) {
                positionView.setText(Util.getStringForTime(formatBuilder, formatter, position));
            }
            controlViewLayoutManager.removeHideCallbacks();
        }

        public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
            scrubbing = false;
            if (!canceled && player != null) {
                seekToTimeBarPosition(player, position);
            }
            controlViewLayoutManager.resetHideCallbacks();
        }
    }

    private class SettingsAdapter extends RecyclerView.Adapter<SettingViewHolder> {

        private final Drawable[] iconIds;
        private final String[] mainTexts;
        private final String[] subTexts;

        public SettingsAdapter(String[] mainTexts, Drawable[] iconIds) {
            this.mainTexts = mainTexts;
            this.subTexts = new String[mainTexts.length];
            this.iconIds = iconIds;
        }

        public void setSubTextAtPosition(int position, String subText) {
            this.subTexts[position] = subText;
        }

        public int getItemCount() {
            return mainTexts.length;
        }

        public long getItemId(int position) {
            return position;
        }

        public void onBindViewHolder(SettingViewHolder holder, int position) {
            holder.mainTextView.setText(mainTexts[position]);
            if (subTexts[position] == null) {
                holder.subTextView.setVisibility(GONE);
            } else {
                holder.subTextView.setText(subTexts[position]);
            }
            if (iconIds[position] == null) {
                holder.iconView.setVisibility(GONE);
            } else {
                holder.iconView.setImageDrawable(iconIds[position]);
            }
        }

        public SettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v =
                    LayoutInflater.from(getContext())
                            .inflate(com.google.android.exoplayer2.ui.R.layout.exo_styled_settings_list_item, parent, false);
            return new SettingViewHolder(v);
        }
    }

    private final class SettingViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconView;
        private final TextView mainTextView;
        private final TextView subTextView;

        public SettingViewHolder(View itemView) {
            super(itemView);
            if (Util.SDK_INT < 26) {
                itemView.setFocusable(true);
            }
            mainTextView = itemView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_main_text);
            subTextView = itemView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_sub_text);
            iconView = itemView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_icon);
            itemView.setOnClickListener(v -> onSettingViewClicked(getAdapterPosition()));
        }
    }

    private final class PlaybackSpeedAdapter extends RecyclerView.Adapter<SubSettingViewHolder> {

        private final String[] playbackSpeedTexts;
        private final int[] playbackSpeedsMultBy100;
        private int selectedIndex;

        public PlaybackSpeedAdapter(String[] playbackSpeedTexts, int[] playbackSpeedsMultBy100) {
            this.playbackSpeedTexts = playbackSpeedTexts;
            this.playbackSpeedsMultBy100 = playbackSpeedsMultBy100;
        }

        public String getSelectedText() {
            return playbackSpeedTexts[selectedIndex];
        }

        public void updateSelectedIndex(float playbackSpeed) {
            int currentSpeedMultBy100 = Math.round(playbackSpeed * 100);
            int closestMatchIndex = 0;
            int closestMatchDifference = Integer.MAX_VALUE;
            for (int i = 0; i < playbackSpeedsMultBy100.length; i++) {
                int difference = Math.abs(currentSpeedMultBy100 - playbackSpeedsMultBy100[i]);
                if (difference < closestMatchDifference) {
                    closestMatchIndex = i;
                    closestMatchDifference = difference;
                }
            }
            selectedIndex = closestMatchIndex;
        }

        public int getItemCount() {
            return playbackSpeedTexts.length;
        }

        public void onBindViewHolder(SubSettingViewHolder holder, int position) {
            if (position < playbackSpeedTexts.length) {
                holder.textView.setText(playbackSpeedTexts[position]);
            }
            holder.checkView.setVisibility(position == selectedIndex ? VISIBLE : INVISIBLE);
            holder.itemView.setOnClickListener(
                    v -> {
                        if (position != selectedIndex) {
                            float speed = playbackSpeedsMultBy100[position] / 100.0f;
                            setPlaybackSpeed(speed);
                        }
                        settingsWindow.dismiss();
                    });
        }

        public SubSettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v =
                    LayoutInflater.from(getContext())
                            .inflate(
                                    com.google.android.exoplayer2.ui.R.layout.exo_styled_sub_settings_list_item, parent, false);
            return new SubSettingViewHolder(v);
        }
    }

    private final class TextTrackSelectionAdapter extends TrackSelectionAdapter {

        public void init(
                List<Integer> rendererIndices,
                List<TrackInfo> trackInfos,
                MappedTrackInfo mappedTrackInfo) {
            boolean subtitleIsOn = false;
            for (int i = 0; i < trackInfos.size(); i++) {
                if (trackInfos.get(i).selected) {
                    subtitleIsOn = true;
                    break;
                }
            }
            if (subtitleButton != null) {
                subtitleButton.setImageDrawable(
                        subtitleIsOn ? subtitleOnButtonDrawable : subtitleOffButtonDrawable);
                subtitleButton.setContentDescription(
                        subtitleIsOn ? subtitleOnContentDescription : subtitleOffContentDescription);
            }
            this.rendererIndices = rendererIndices;
            this.tracks = trackInfos;
            this.mappedTrackInfo = mappedTrackInfo;
        }

        public void onBindViewHolder(SubSettingViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            if (position > 0) {
                TrackInfo track = tracks.get(position - 1);
                holder.checkView.setVisibility(track.selected ? VISIBLE : INVISIBLE);
            }
        }

        public void onBindViewHolderAtZeroPosition(SubSettingViewHolder holder) {
            holder.textView.setText(com.google.android.exoplayer2.ui.R.string.exo_track_selection_none);
            boolean isTrackSelectionOff = true;
            for (int i = 0; i < tracks.size(); i++) {
                if (tracks.get(i).selected) {
                    isTrackSelectionOff = false;
                    break;
                }
            }
            holder.checkView.setVisibility(isTrackSelectionOff ? VISIBLE : INVISIBLE);
            holder.itemView.setOnClickListener(
                    v -> {
                        if (trackSelector != null) {
                            ParametersBuilder parametersBuilder = trackSelector.getParameters().buildUpon();
                            for (int i = 0; i < rendererIndices.size(); i++) {
                                int rendererIndex = rendererIndices.get(i);
                                parametersBuilder =
                                        parametersBuilder
                                                .clearSelectionOverrides(rendererIndex)
                                                .setRendererDisabled(rendererIndex, true);
                            }
                            checkNotNull(trackSelector).setParameters(parametersBuilder);
                            settingsWindow.dismiss();
                        }
                    });
        }

        public void onTrackSelection(String subtext) {
        }
    }

    private final class AudioTrackSelectionAdapter extends TrackSelectionAdapter {
        public void init(
                List<Integer> rendererIndices,
                List<TrackInfo> trackInfos,
                MappedTrackInfo mappedTrackInfo) {
            boolean hasSelectionOverride = false;
            for (int i = 0; i < rendererIndices.size(); i++) {
                int rendererIndex = rendererIndices.get(i);
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
                if (trackSelector != null
                        && trackSelector.getParameters().hasSelectionOverride(rendererIndex, trackGroups)) {
                    hasSelectionOverride = true;
                    break;
                }
            }
            if (trackInfos.isEmpty()) {
                settingsAdapter.setSubTextAtPosition(
                        SETTINGS_AUDIO_TRACK_SELECTION_POSITION,
                        getResources().getString(com.google.android.exoplayer2.ui.R.string.exo_track_selection_none));
            } else if (!hasSelectionOverride) {
                settingsAdapter.setSubTextAtPosition(
                        SETTINGS_AUDIO_TRACK_SELECTION_POSITION,
                        getResources().getString(com.google.android.exoplayer2.ui.R.string.exo_track_selection_auto));
            } else {
                for (int i = 0; i < trackInfos.size(); i++) {
                    TrackInfo track = trackInfos.get(i);
                    if (track.selected) {
                        settingsAdapter.setSubTextAtPosition(
                                SETTINGS_AUDIO_TRACK_SELECTION_POSITION, track.trackName);
                        break;
                    }
                }
            }
            this.rendererIndices = rendererIndices;
            this.tracks = trackInfos;
            this.mappedTrackInfo = mappedTrackInfo;
        }

        public void onBindViewHolderAtZeroPosition(SubSettingViewHolder holder) {
            holder.textView.setText(com.google.android.exoplayer2.ui.R.string.exo_track_selection_auto);
            boolean hasSelectionOverride = false;
            DefaultTrackSelector.Parameters parameters = checkNotNull(trackSelector).getParameters();
            for (int i = 0; i < rendererIndices.size(); i++) {
                int rendererIndex = rendererIndices.get(i);
                TrackGroupArray trackGroups = checkNotNull(mappedTrackInfo).getTrackGroups(rendererIndex);
                if (parameters.hasSelectionOverride(rendererIndex, trackGroups)) {
                    hasSelectionOverride = true;
                    break;
                }
            }
            holder.checkView.setVisibility(hasSelectionOverride ? INVISIBLE : VISIBLE);
            holder.itemView.setOnClickListener(
                    v -> {
                        if (trackSelector != null) {
                            ParametersBuilder parametersBuilder = trackSelector.getParameters().buildUpon();
                            for (int i = 0; i < rendererIndices.size(); i++) {
                                int rendererIndex = rendererIndices.get(i);
                                parametersBuilder = parametersBuilder.clearSelectionOverrides(rendererIndex);
                            }
                            checkNotNull(trackSelector).setParameters(parametersBuilder);
                        }
                        settingsAdapter.setSubTextAtPosition(
                                SETTINGS_AUDIO_TRACK_SELECTION_POSITION,
                                getResources().getString(com.google.android.exoplayer2.ui.R.string.exo_track_selection_auto));
                        settingsWindow.dismiss();
                    });
        }

        public void onTrackSelection(String subtext) {
            settingsAdapter.setSubTextAtPosition(SETTINGS_AUDIO_TRACK_SELECTION_POSITION, subtext);
        }
    }

    private abstract class TrackSelectionAdapter extends RecyclerView.Adapter<SubSettingViewHolder> {

        protected List<Integer> rendererIndices;
        protected List<TrackInfo> tracks;
        protected
        MappedTrackInfo mappedTrackInfo;

        public TrackSelectionAdapter() {
            this.rendererIndices = new ArrayList<>();
            this.tracks = new ArrayList<>();
            this.mappedTrackInfo = null;
        }

        public void clear() {
            tracks = Collections.emptyList();
            mappedTrackInfo = null;
        }

        public abstract void init(
                List<Integer> rendererIndices, List<TrackInfo> trackInfos, MappedTrackInfo mappedTrackInfo);

        public abstract void onBindViewHolderAtZeroPosition(SubSettingViewHolder holder);

        public abstract void onTrackSelection(String subtext);

        public int getItemCount() {
            return tracks.isEmpty() ? 0 : tracks.size() + 1;
        }

        public void onBindViewHolder(SubSettingViewHolder holder, int position) {
            if (trackSelector == null || mappedTrackInfo == null) {
                return;
            }
            if (position == 0) {
                onBindViewHolderAtZeroPosition(holder);
            } else {
                TrackInfo track = tracks.get(position - 1);
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(track.rendererIndex);
                boolean explicitlySelected =
                        checkNotNull(trackSelector)
                                .getParameters()
                                .hasSelectionOverride(track.rendererIndex, trackGroups)
                                && track.selected;
                holder.textView.setText(track.trackName);
                holder.checkView.setVisibility(explicitlySelected ? VISIBLE : INVISIBLE);
                holder.itemView.setOnClickListener(
                        v -> {
                            if (mappedTrackInfo != null && trackSelector != null) {
                                ParametersBuilder parametersBuilder = trackSelector.getParameters().buildUpon();
                                for (int i = 0; i < rendererIndices.size(); i++) {
                                    int rendererIndex = rendererIndices.get(i);
                                    if (rendererIndex == track.rendererIndex) {
                                        parametersBuilder =
                                                parametersBuilder
                                                        .setSelectionOverride(
                                                                rendererIndex,
                                                                checkNotNull(mappedTrackInfo).getTrackGroups(rendererIndex),
                                                                new SelectionOverride(track.groupIndex, track.trackIndex))
                                                        .setRendererDisabled(rendererIndex, false);
                                    } else {
                                        parametersBuilder =
                                                parametersBuilder
                                                        .clearSelectionOverrides(rendererIndex)
                                                        .setRendererDisabled(rendererIndex, true);
                                    }
                                }
                                checkNotNull(trackSelector).setParameters(parametersBuilder);
                                onTrackSelection(track.trackName);
                                settingsWindow.dismiss();
                            }
                        });
            }
        }

        public SubSettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v =
                    LayoutInflater.from(getContext())
                            .inflate(
                                    com.google.android.exoplayer2.ui.R.layout.exo_styled_sub_settings_list_item, parent, false);
            return new SubSettingViewHolder(v);
        }
    }

    void notifyOnVisibilityChange() {
        for (VisibilityListener visibilityListener : visibilityListeners) {
            visibilityListener.onVisibilityChange(getVisibility());
        }
    }

    void updateAll() {
        updatePlayPauseButton();
        updateNavigation();
        updateRepeatModeButton();
        updateShuffleButton();
        updateTrackLists();
        updatePlaybackSpeedList();
        updateTimeline();
    }

    void requestPlayPauseFocus() {
        if (playPauseButton != null) {
            playPauseButton.requestFocus();
        }
    }
}
