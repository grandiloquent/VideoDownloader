package euphoria.psycho.player;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.Logger;

import static euphoria.psycho.player.PlayerHelper.hasNavBar;

public abstract class BaseVideoActivity extends AppCompatActivity {
    LinearLayout mController;
    AspectRatioFrameLayout mExoContentFrame;
    TextView mExoDuration;
    TextView mExoErrorMessage;
    ImageButton mExoNext;
    ImageButton mExoPause;
    ImageButton mExoPlay;
    TextView mExoPosition;
    ImageButton mExoPrev;
    DefaultTimeBar mExoProgress;
    ImageButton mExoRew;
    boolean mIsHasBar = false;
    SharedPreferences mPreferences;
    FrameLayout mRootView;
    ImageButton mExoDelete;
    TextureVideoView mTextureVideoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.e(String.format("onCreate, %s", ""));
        initialize();
    }

    void initialize() {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mIsHasBar = hasNavBar(this);
        setContentView(R.layout.activity_video);
        mRootView = findViewById(R.id.root_view);
        mExoContentFrame = findViewById(R.id.exo_content_frame);
        mExoErrorMessage = findViewById(R.id.exo_error_message);
        mController = findViewById(R.id.controller);
        mExoPrev = findViewById(R.id.exo_prev);
        mExoRew = findViewById(R.id.exo_rew);
        mExoDelete = findViewById(R.id.exo_delete);
        mExoPlay = findViewById(R.id.exo_play);
        mExoPause = findViewById(R.id.exo_pause);
        mExoNext = findViewById(R.id.exo_next);
        mExoPosition = findViewById(R.id.exo_position);
        mExoProgress = findViewById(R.id.exo_progress);
        mExoDuration = findViewById(R.id.exo_duration);
        mTextureVideoView = findViewById(R.id.texture_video_view);
    }
}