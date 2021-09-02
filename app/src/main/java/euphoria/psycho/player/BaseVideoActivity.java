package euphoria.psycho.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import euphoria.psycho.explorer.R;


public abstract class BaseVideoActivity extends AppCompatActivity {
    static final String KEY_TREE_URI = "tree_uri";
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
    SubtitleView mExoSubtitles;
    boolean mIsHasBar = false;
    SharedPreferences mPreferences;
    FrameLayout mRootView;
    TextureView mTextureView;

    int calculateScreenOrientation() {
        int displayRotation = getDisplayRotation();
        boolean standard = displayRotation < 180;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
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


    int getDisplayRotation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
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

    boolean hasNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display display = getWindowManager().getDefaultDisplay();
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            return realMetrics.widthPixels - metrics.widthPixels > 0
                    || realMetrics.heightPixels - metrics.heightPixels > 0;
        } else {
            boolean hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey();
            boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
            return !hasMenuKey && !hasBackKey;
        }
    }

    void hideSystemUI(boolean toggleActionBarVisibility) {
        if (toggleActionBarVisibility) {
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LOW_PROFILE |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    void initialize() {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mIsHasBar = hasNavBar();
        setContentView(R.layout.activity_video);
        mRootView = findViewById(R.id.root_view);
        mExoContentFrame = findViewById(R.id.exo_content_frame);
        mExoSubtitles = findViewById(R.id.exo_subtitles);
        mExoErrorMessage = findViewById(R.id.exo_error_message);
        mController = findViewById(R.id.controller);
        mExoPrev = findViewById(R.id.exo_prev);
        mExoRew = findViewById(R.id.exo_rew);
        mExoPlay = findViewById(R.id.exo_play);
        mExoPause = findViewById(R.id.exo_pause);
        mExoNext = findViewById(R.id.exo_next);
        mExoPosition = findViewById(R.id.exo_position);
        mExoProgress = findViewById(R.id.exo_progress);
        mExoDuration = findViewById(R.id.exo_duration);
        mTextureView = new TextureView(this);
        mTextureView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mExoContentFrame.addView(mTextureView, 0);

    }

    void showSystemUI(boolean toggleActionBarVisibility) {
        if (toggleActionBarVisibility) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.show();
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public static Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static Point getNavigationBarSize(Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize = getRealScreenSize(context);
        // navigation bar on the side
        if (appUsableSize.x < realScreenSize.x) {
            return new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
        }
        // navigation bar at the bottom
        if (appUsableSize.y < realScreenSize.y) {
            return new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
        }
        // navigation bar is not present
        return new Point();
    }

    public static Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            } catch (NoSuchMethodException e) {
            }
        }
        return size;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }
}
