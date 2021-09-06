package euphoria.psycho.player;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import euphoria.psycho.explorer.R;

public class PlayerHelper {
    static void adjustController(AppCompatActivity activity, View view, int navigationBarHeight, int navigationBarWidth) {
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        int orientation = calculateScreenOrientation(activity);
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            bottom += navigationBarHeight;
        } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            right += navigationBarWidth;
        } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            left += navigationBarWidth;
        }
        view.setPadding(left, top, right, bottom);
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

    static void deleteVideo(Context context, Player player, MediaSource mediaSource, List<File> files) {
        openDeleteVideoDialog(context, unused -> {
            int index = player.getCurrentWindowIndex();
            removeFromPlaylist(mediaSource, index);
            if (!files.remove(index).delete()) {
                throw new IllegalStateException();
            }
            return null;
        });
    }

    static Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
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

    static Point getNavigationBarSize(Context context) {
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

    static Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return size;
    }

    static boolean hasNavBar(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics realMetrics = new DisplayMetrics();
        display.getRealMetrics(realMetrics);
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return realMetrics.widthPixels - metrics.widthPixels > 0
                || realMetrics.heightPixels - metrics.heightPixels > 0;
    }

    static void hideSystemUI(AppCompatActivity activity, boolean toggleActionBarVisibility) {
        if (toggleActionBarVisibility) {
            activity.getSupportActionBar().hide();
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

    static void openDeleteVideoDialog(Context context, Function<Void, Void> function) {
        new Builder(context)
                .setTitle("确定删除吗？")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    function.apply(null);
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .show();
    }

    static int playNextVideo(int currentPlaybackIndex, TextureVideoView textureVideoView, File[] files) {
        int nextPlaybackIndex = files.length > currentPlaybackIndex + 1 ? currentPlaybackIndex + 1 : 0;
        textureVideoView.setVideoPath(files[nextPlaybackIndex].getAbsolutePath());
        return nextPlaybackIndex;
    }

    static int playPreviousVideo(int currentPlaybackIndex, TextureVideoView textureVideoView, File[] files) {
        int nextPlaybackIndex = currentPlaybackIndex - 1 > -1 ? currentPlaybackIndex - 1 : files.length - 1;
        textureVideoView.setVideoPath(files[nextPlaybackIndex].getAbsolutePath());
        return nextPlaybackIndex;
    }

    static void removeFromPlaylist(MediaSource mediaSource, int index) {
        ((ConcatenatingMediaSource) mediaSource).removeMediaSource(index);
    }

    static void rotateScreen(AppCompatActivity activity) {
        int orientation = calculateScreenOrientation(activity);
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    static void showSystemUI(AppCompatActivity activity, boolean toggleActionBarVisibility) {
        if (toggleActionBarVisibility) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null)
                actionBar.show();
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    static void switchPlayState(TextureVideoView textureVideoView, ImageButton playButton) {
        if (textureVideoView.isPlaying()) {
            textureVideoView.pause();
            playButton.setImageResource(R.drawable.exo_controls_play);
        } else {
            textureVideoView.start();
            playButton.setImageResource(R.drawable.exo_controls_pause);
        }
    }
}