package euphoria.psycho.player;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.VideoSize;

import java.util.List;

import euphoria.psycho.explorer.R;

import static com.google.android.exoplayer2.Player.COMMAND_GET_TEXT;
import static com.google.android.exoplayer2.Player.COMMAND_SET_VIDEO_SURFACE;

public class SimplePlayerView extends FrameLayout {
    public static final int SHOW_BUFFERING_ALWAYS = 2;
    public static final int SHOW_BUFFERING_NEVER = 0;

    public static final int SHOW_BUFFERING_WHEN_PLAYING = 1;
    private static final int PICTURE_TYPE_FRONT_COVER = 3;
    private static final int PICTURE_TYPE_NOT_SET = -1;
    private final View bufferingView;
    private final ComponentListener componentListener;
    private final com.google.android.exoplayer2.ui.AspectRatioFrameLayout contentFrame;
    private final SimplePlayerControlView controller;
    private final TextView errorMessageView;
    private final FrameLayout overlayFrameLayout;
    private final SubtitleView subtitleView;
    private final View surfaceView;
    private final boolean surfaceViewIgnoresVideoAspectRatio;
    private Player player;
    private boolean useController;
    private SimplePlayerControlView.VisibilityListener controllerVisibilityListener;
    private
    int showBuffering;
    private boolean keepContentOnPlayerReset;
    private ErrorMessageProvider<? super PlaybackException> errorMessageProvider;
    private CharSequence customErrorMessage;
    private int controllerShowTimeoutMs;
    private boolean controllerAutoShow;
    private boolean controllerHideOnTouch;
    private int textureViewRotation;
    private boolean isTouching;

    public SimplePlayerView(Context context) {
        this(context, null);
    }

    public SimplePlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimplePlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        componentListener = new ComponentListener();
        int playerLayoutId = R.layout.exo_styled_player_view;
        boolean useController = true;
        int resizeMode = com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT;
        int controllerShowTimeoutMs = SimplePlayerControlView.DEFAULT_SHOW_TIMEOUT_MS;
        boolean controllerHideOnTouch = true;
        boolean controllerAutoShow = true;
        int showBuffering = SHOW_BUFFERING_NEVER;
        LayoutInflater.from(context).inflate(playerLayoutId, this);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        contentFrame = findViewById(R.id.exo_content_frame);
        if (contentFrame != null) {
            setResizeModeRaw(contentFrame, resizeMode);
        }
        boolean surfaceViewIgnoresVideoAspectRatio = false;
        if (contentFrame != null) {
            ViewGroup.LayoutParams params =
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            surfaceView = new TextureView(context);
            surfaceView.setLayoutParams(params);
            surfaceView.setOnClickListener(componentListener);
            surfaceView.setClickable(false);
            contentFrame.addView(surfaceView, 0);
        } else {
            surfaceView = null;
        }
        this.surfaceViewIgnoresVideoAspectRatio = surfaceViewIgnoresVideoAspectRatio;
        overlayFrameLayout = findViewById(com.google.android.exoplayer2.ui.R.id.exo_overlay);
        subtitleView = findViewById(com.google.android.exoplayer2.ui.R.id.exo_subtitles);
        if (subtitleView != null) {
            subtitleView.setUserDefaultStyle();
            subtitleView.setUserDefaultTextSize();
        }
        bufferingView = findViewById(com.google.android.exoplayer2.ui.R.id.exo_buffering);
        if (bufferingView != null) {
            bufferingView.setVisibility(View.GONE);
        }
        this.showBuffering = showBuffering;
        errorMessageView = findViewById(com.google.android.exoplayer2.ui.R.id.exo_error_message);
        if (errorMessageView != null) {
            errorMessageView.setVisibility(View.GONE);
        }
        SimplePlayerControlView customController = findViewById(com.google.android.exoplayer2.ui.R.id.exo_controller);
        View controllerPlaceholder = findViewById(com.google.android.exoplayer2.ui.R.id.exo_controller_placeholder);
        if (customController != null) {
            this.controller = customController;
        } else if (controllerPlaceholder != null) {
            this.controller = new SimplePlayerControlView(context, null, 0, attrs);
            controller.setId(com.google.android.exoplayer2.ui.R.id.exo_controller);
            controller.setLayoutParams(controllerPlaceholder.getLayoutParams());
            controller.setOnFullScreenModeChangedListener(new SimplePlayerControlView.OnFullScreenModeChangedListener() {

                public void onFullScreenModeChanged(boolean isFullScreen) {
                }
            });
            ViewGroup parent = ((ViewGroup) controllerPlaceholder.getParent());
            int controllerIndex = parent.indexOfChild(controllerPlaceholder);
            parent.removeView(controllerPlaceholder);
            parent.addView(controller, controllerIndex);
        } else {
            this.controller = null;
        }
        this.controllerShowTimeoutMs = controller != null ? controllerShowTimeoutMs : 0;
        this.controllerHideOnTouch = controllerHideOnTouch;
        this.controllerAutoShow = controllerAutoShow;
        this.useController = useController && controller != null;
        if (controller != null) {
            controller.hideImmediately();
            controller.addVisibilityListener(componentListener);
        }
        updateContentDescription();
    }

    public boolean dispatchMediaKeyEvent(KeyEvent event) {
        return useController() && controller.dispatchMediaKeyEvent(event);
    }

    public boolean getControllerAutoShow() {
        return controllerAutoShow;
    }

    public void setControllerAutoShow(boolean controllerAutoShow) {
        this.controllerAutoShow = controllerAutoShow;
    }

    public boolean getControllerHideOnTouch() {
        return controllerHideOnTouch;
    }

    public void setControllerHideOnTouch(boolean controllerHideOnTouch) {
        Assertions.checkStateNotNull(controller);
        this.controllerHideOnTouch = controllerHideOnTouch;
        updateContentDescription();
    }

    public int getControllerShowTimeoutMs() {
        return controllerShowTimeoutMs;
    }

    public void setControllerShowTimeoutMs(int controllerShowTimeoutMs) {
        Assertions.checkStateNotNull(controller);
        this.controllerShowTimeoutMs = controllerShowTimeoutMs;
        if (controller.isFullyVisible()) {
            showController();
        }
    }

    public FrameLayout getOverlayFrameLayout() {
        return overlayFrameLayout;
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
        Player oldPlayer = this.player;
        if (oldPlayer != null) {
            oldPlayer.removeListener(componentListener);
            if (surfaceView instanceof TextureView) {
                oldPlayer.clearVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                oldPlayer.clearVideoSurfaceView((SurfaceView) surfaceView);
            }
        }
        if (subtitleView != null) {
            subtitleView.setCues(null);
        }
        this.player = player;
        if (useController()) {
            controller.setPlayer(player);
        }
        updateBuffering();
        updateErrorMessage();
        updateForCurrentTrackSelections(true);
        if (player != null) {
            if (player.isCommandAvailable(COMMAND_SET_VIDEO_SURFACE)) {
                if (surfaceView instanceof TextureView) {
                    player.setVideoTextureView((TextureView) surfaceView);
                } else if (surfaceView instanceof SurfaceView) {
                    player.setVideoSurfaceView((SurfaceView) surfaceView);
                }
                updateAspectRatio();
            }
            if (subtitleView != null && player.isCommandAvailable(COMMAND_GET_TEXT)) {
                subtitleView.setCues(player.getCurrentCues());
            }
            player.addListener(componentListener);
            maybeShowController(false);
        } else {
            hideController();
        }
    }

    public int getResizeMode() {
        Assertions.checkStateNotNull(contentFrame);
        return contentFrame.getResizeMode();
    }

    public void setResizeMode(int resizeMode) {
        Assertions.checkStateNotNull(contentFrame);
        contentFrame.setResizeMode(resizeMode);
    }

    public SubtitleView getSubtitleView() {
        return subtitleView;
    }

    public boolean getUseController() {
        return useController;
    }

    public void setUseController(boolean useController) {
        Assertions.checkState(!useController || controller != null);
        if (this.useController == useController) {
            return;
        }
        this.useController = useController;
        if (useController()) {
            controller.setPlayer(player);
        } else if (controller != null) {
            controller.hide();
            controller.setPlayer(null);
        }
        updateContentDescription();
    }

    public View getVideoSurfaceView() {
        return surfaceView;
    }

    public void hideController() {
        if (controller != null) {
            controller.hide();
        }
    }

    public boolean isControllerFullyVisible() {
        return controller != null && controller.isFullyVisible();
    }

    public void onPause() {
        if (surfaceView instanceof GLSurfaceView) {
            ((GLSurfaceView) surfaceView).onPause();
        }
    }

    public void onResume() {
        if (surfaceView instanceof GLSurfaceView) {
            ((GLSurfaceView) surfaceView).onResume();
        }
    }

    public void setAspectRatioListener(
            com.google.android.exoplayer2.ui.AspectRatioFrameLayout.AspectRatioListener listener) {
        Assertions.checkStateNotNull(contentFrame);
        contentFrame.setAspectRatioListener(listener);
    }

    public void setControlDispatcher(ControlDispatcher controlDispatcher) {
        Assertions.checkStateNotNull(controller);
        controller.setControlDispatcher(controlDispatcher);
    }

    public void setControllerOnFullScreenModeChangedListener(
            SimplePlayerControlView.OnFullScreenModeChangedListener listener) {
        Assertions.checkStateNotNull(controller);
        controller.setOnFullScreenModeChangedListener(listener);
    }

    public void setControllerVisibilityListener(
            SimplePlayerControlView.VisibilityListener listener) {
        Assertions.checkStateNotNull(controller);
        if (this.controllerVisibilityListener == listener) {
            return;
        }
        if (this.controllerVisibilityListener != null) {
            controller.removeVisibilityListener(this.controllerVisibilityListener);
        }
        this.controllerVisibilityListener = listener;
        if (listener != null) {
            controller.addVisibilityListener(listener);
        }
    }

    public void setCustomErrorMessage(CharSequence message) {
        Assertions.checkState(errorMessageView != null);
        customErrorMessage = message;
        updateErrorMessage();
    }

    public void setErrorMessageProvider(
            ErrorMessageProvider<? super PlaybackException> errorMessageProvider) {
        if (this.errorMessageProvider != errorMessageProvider) {
            this.errorMessageProvider = errorMessageProvider;
            updateErrorMessage();
        }
    }

    public void setKeepContentOnPlayerReset(boolean keepContentOnPlayerReset) {
        if (this.keepContentOnPlayerReset != keepContentOnPlayerReset) {
            this.keepContentOnPlayerReset = keepContentOnPlayerReset;
            updateForCurrentTrackSelections(false);
        }
    }

    public void setOnDownloadListener(OnClickListener clickListener) {
        controller.getDownloadButton().setOnClickListener(clickListener);
    }

    public void setShowBuffering(int showBuffering) {
        if (this.showBuffering != showBuffering) {
            this.showBuffering = showBuffering;
            updateBuffering();
        }
    }

    public void setShowFastForwardButton(boolean showFastForwardButton) {
        Assertions.checkStateNotNull(controller);
        controller.setShowFastForwardButton(showFastForwardButton);
    }

    public void setShowMultiWindowTimeBar(boolean showMultiWindowTimeBar) {
        Assertions.checkStateNotNull(controller);
        controller.setShowMultiWindowTimeBar(showMultiWindowTimeBar);
    }

    public void setShowNextButton(boolean showNextButton) {
        Assertions.checkStateNotNull(controller);
        controller.setShowNextButton(showNextButton);
    }

    public void setShowPreviousButton(boolean showPreviousButton) {
        Assertions.checkStateNotNull(controller);
        controller.setShowPreviousButton(showPreviousButton);
    }

    public void setShowRewindButton(boolean showRewindButton) {
        Assertions.checkStateNotNull(controller);
        controller.setShowRewindButton(showRewindButton);
    }

    public void showController() {
        showController(shouldShowControllerIndefinitely());
    }

    public static void switchTargetView(
            Player player,
            StyledPlayerView oldPlayerView,
            StyledPlayerView newPlayerView) {
        if (oldPlayerView == newPlayerView) {
            return;
        }
        if (newPlayerView != null) {
            newPlayerView.setPlayer(player);
        }
        if (oldPlayerView != null) {
            oldPlayerView.setPlayer(null);
        }
    }

    protected void onContentAspectRatioChanged(
            com.google.android.exoplayer2.ui.AspectRatioFrameLayout contentFrame, float aspectRatio) {
        if (contentFrame != null) {
            contentFrame.setAspectRatio(aspectRatio);
        }
    }

    private static void applyTextureViewRotation(TextureView textureView, int textureViewRotation) {
        Matrix transformMatrix = new Matrix();
        float textureViewWidth = textureView.getWidth();
        float textureViewHeight = textureView.getHeight();
        if (textureViewWidth != 0 && textureViewHeight != 0 && textureViewRotation != 0) {
            float pivotX = textureViewWidth / 2;
            float pivotY = textureViewHeight / 2;
            transformMatrix.postRotate(textureViewRotation, pivotX, pivotY);
            RectF originalTextureRect = new RectF(0, 0, textureViewWidth, textureViewHeight);
            RectF rotatedTextureRect = new RectF();
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect);
            transformMatrix.postScale(
                    textureViewWidth / rotatedTextureRect.width(),
                    textureViewHeight / rotatedTextureRect.height(),
                    pivotX,
                    pivotY);
        }
        textureView.setTransform(transformMatrix);
    }

    private static void setResizeModeRaw(AspectRatioFrameLayout aspectRatioFrame, int resizeMode) {
        aspectRatioFrame.setResizeMode(resizeMode);
    }

    private boolean isDpadKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_UP_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER;
    }

    private void maybeShowController(boolean isForced) {
        if (useController()) {
            boolean wasShowingIndefinitely =
                    controller.isFullyVisible() && controller.getShowTimeoutMs() <= 0;
            boolean shouldShowIndefinitely = shouldShowControllerIndefinitely();
            if (isForced || wasShowingIndefinitely || shouldShowIndefinitely) {
                showController(shouldShowIndefinitely);
            }
        }
    }

    private boolean shouldShowControllerIndefinitely() {
        if (player == null) {
            return true;
        }
        int playbackState = player.getPlaybackState();
        return controllerAutoShow
                && !player.getCurrentTimeline().isEmpty()
                && (playbackState == Player.STATE_IDLE
                || playbackState == Player.STATE_ENDED
                || !player.getPlayWhenReady());
    }

    private void showController(boolean showIndefinitely) {
        if (!useController()) {
            return;
        }
        controller.setShowTimeoutMs(showIndefinitely ? 0 : controllerShowTimeoutMs);
        controller.show();
    }

    private boolean toggleControllerVisibility() {
        if (!useController() || player == null) {
            return false;
        }
        if (!controller.isFullyVisible()) {
            maybeShowController(true);
            return true;
        } else if (controllerHideOnTouch) {
            controller.hide();
            return true;
        }
        return false;
    }

    private void updateAspectRatio() {
        VideoSize videoSize = player != null ? player.getVideoSize() : VideoSize.UNKNOWN;
        int width = videoSize.width;
        int height = videoSize.height;
        int unappliedRotationDegrees = videoSize.unappliedRotationDegrees;
        float videoAspectRatio =
                (height == 0 || width == 0) ? 0 : (width * videoSize.pixelWidthHeightRatio) / height;
        if (surfaceView instanceof TextureView) {
            if (videoAspectRatio > 0
                    && (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270)) {
                videoAspectRatio = 1 / videoAspectRatio;
            }
            if (textureViewRotation != 0) {
                surfaceView.removeOnLayoutChangeListener(componentListener);
            }
            textureViewRotation = unappliedRotationDegrees;
            if (textureViewRotation != 0) {
                surfaceView.addOnLayoutChangeListener(componentListener);
            }
            applyTextureViewRotation((TextureView) surfaceView, textureViewRotation);
        }
        onContentAspectRatioChanged(
                contentFrame, surfaceViewIgnoresVideoAspectRatio ? 0 : videoAspectRatio);
    }

    private void updateBuffering() {
        if (bufferingView != null) {
            boolean showBufferingSpinner =
                    player != null
                            && player.getPlaybackState() == Player.STATE_BUFFERING
                            && (showBuffering == SHOW_BUFFERING_ALWAYS
                            || (showBuffering == SHOW_BUFFERING_WHEN_PLAYING && player.getPlayWhenReady()));
            bufferingView.setVisibility(showBufferingSpinner ? View.VISIBLE : View.GONE);
        }
    }

    private void updateContentDescription() {
        if (controller == null || !useController) {
            setContentDescription(null);
        } else if (controller.isFullyVisible()) {
            setContentDescription(
                    controllerHideOnTouch
                            ? getResources().getString(com.google.android.exoplayer2.ui.R.string.exo_controls_hide)
                            : null);
        } else {
            setContentDescription(
                    getResources().getString(com.google.android.exoplayer2.ui.R.string.exo_controls_show));
        }
    }

    private void updateControllerVisibility() {
        maybeShowController(false);
    }

    private void updateErrorMessage() {
        if (errorMessageView != null) {
            if (customErrorMessage != null) {
                errorMessageView.setText(customErrorMessage);
                errorMessageView.setVisibility(View.VISIBLE);
                return;
            }
            PlaybackException error = player != null ? player.getPlayerError() : null;
            if (error != null && errorMessageProvider != null) {
                CharSequence errorMessage = errorMessageProvider.getErrorMessage(error).second;
                errorMessageView.setText(errorMessage);
                errorMessageView.setVisibility(View.VISIBLE);
            } else {
                errorMessageView.setVisibility(View.GONE);
            }
        }
    }

    private void updateForCurrentTrackSelections(boolean isNewPlayer) {
        Player player = this.player;
        if (player == null || player.getCurrentTrackGroups().isEmpty()) {
            if (!keepContentOnPlayerReset) {
            }
            return;
        }
        if (isNewPlayer && !keepContentOnPlayerReset) {
        }
        TrackSelectionArray trackSelections = player.getCurrentTrackSelections();
        for (int i = 0; i < trackSelections.length; i++) {
            TrackSelection trackSelection = trackSelections.get(i);
            if (trackSelection != null) {
                for (int j = 0; j < trackSelection.length(); j++) {
                    Format format = trackSelection.getFormat(j);
                    if (MimeTypes.getTrackType(format.sampleMimeType) == C.TRACK_TYPE_VIDEO) {
                        return;
                    }
                }
            }
        }

    }

    private boolean useController() {
        if (useController) {
            Assertions.checkStateNotNull(controller);
            return true;
        }
        return false;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (player != null && player.isPlayingAd()) {
            return super.dispatchKeyEvent(event);
        }
        boolean isDpadKey = isDpadKey(event.getKeyCode());
        boolean handled = false;
        if (isDpadKey && useController() && !controller.isFullyVisible()) {
            maybeShowController(true);
            handled = true;
        } else if (dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event)) {
            maybeShowController(true);
            handled = true;
        } else if (isDpadKey && useController()) {
            maybeShowController(true);
        }
        return handled;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!useController() || player == null) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouching = true;
                return true;
            case MotionEvent.ACTION_UP:
                if (isTouching) {
                    isTouching = false;
                    return performClick();
                }
                return false;
            default:
                return false;
        }
    }

    public boolean onTrackballEvent(MotionEvent ev) {
        if (!useController() || player == null) {
            return false;
        }
        maybeShowController(true);
        return true;
    }

    public boolean performClick() {
        super.performClick();
        return toggleControllerVisibility();
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (surfaceView instanceof SurfaceView) {
            surfaceView.setVisibility(visibility);
        }
    }

    public interface ShowBuffering {
    }

    private final class ComponentListener
            implements Player.Listener,
            OnLayoutChangeListener,
            OnClickListener,
            SimplePlayerControlView.VisibilityListener {

        private final Period period;
        private
        Object lastPeriodUidWithTracks;

        public ComponentListener() {
            period = new Period();
        }

        public void onClick(View view) {
            toggleControllerVisibility();
        }

        public void onCues(List<Cue> cues) {
            if (subtitleView != null) {
                subtitleView.onCues(cues);
            }
        }

        public void onLayoutChange(
                View view,
                int left,
                int top,
                int right,
                int bottom,
                int oldLeft,
                int oldTop,
                int oldRight,
                int oldBottom) {
            applyTextureViewRotation((TextureView) view, textureViewRotation);
        }

        public void onPlayWhenReadyChanged(
                boolean playWhenReady, int reason) {
            updateBuffering();
            updateControllerVisibility();
        }

        public void onPlaybackStateChanged(int playbackState) {
            updateBuffering();
            updateErrorMessage();
            updateControllerVisibility();
        }

        public void onPositionDiscontinuity(
                Player.PositionInfo oldPosition,
                Player.PositionInfo newPosition,
                int reason) {
        }

        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray selections) {
            Player player = SimplePlayerView.this.player;
            Timeline timeline = player.getCurrentTimeline();
            if (timeline.isEmpty()) {
                lastPeriodUidWithTracks = null;
            } else if (!player.getCurrentTrackGroups().isEmpty()) {
                lastPeriodUidWithTracks =
                        timeline.getPeriod(player.getCurrentPeriodIndex(), period, true).uid;
            } else if (lastPeriodUidWithTracks != null) {
                int lastPeriodIndexWithTracks = timeline.getIndexOfPeriod(lastPeriodUidWithTracks);
                if (lastPeriodIndexWithTracks != C.INDEX_UNSET) {
                    int lastWindowIndexWithTracks =
                            timeline.getPeriod(lastPeriodIndexWithTracks, period).windowIndex;
                    if (player.getCurrentWindowIndex() == lastWindowIndexWithTracks) {
                        return;
                    }
                }
                lastPeriodUidWithTracks = null;
            }
            updateForCurrentTrackSelections(false);
        }

        public void onVideoSizeChanged(VideoSize videoSize) {
            updateAspectRatio();
        }

        public void onVisibilityChange(int visibility) {
            updateContentDescription();
        }
    }
}
