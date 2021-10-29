package euphoria.psycho.bilibili;

import android.content.Context;
import android.icu.text.MeasureFormat;
import android.icu.text.MeasureFormat.FormatWidth;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.os.Environment;

import java.io.File;
import java.util.Locale;

import euphoria.psycho.share.KeyShare;

public class BilibiliUtils {

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
    public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;

    public static CharSequence formatDuration(long millis) {
        final FormatWidth width;
        width = FormatWidth.WIDE;
        final MeasureFormat formatter = MeasureFormat.getInstance(Locale.getDefault(), width);
        if (millis >= HOUR_IN_MILLIS) {
            final int hours = (int) ((millis + 1800000) / HOUR_IN_MILLIS);
            return formatter.format(new Measure(hours, MeasureUnit.HOUR));
        } else if (millis >= MINUTE_IN_MILLIS) {
            final int minutes = (int) ((millis + 30000) / MINUTE_IN_MILLIS);
            return formatter.format(new Measure(minutes, MeasureUnit.MINUTE));
        } else {
            final int seconds = (int) ((millis + 500) / SECOND_IN_MILLIS);
            return formatter.format(new Measure(seconds, MeasureUnit.SECOND));
        }
    }

    public static String getBilibiliDatabaseName(Context context) {
        return new File(getBilibiliDirectory(context), "Bilibili.db").getAbsolutePath();
    }

    public static File getBilibiliDirectory(Context context) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Bilibili");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getBilibiliThreadFile(Context context, String url) {
        String key = KeyShare.md5(url);
        File dir = new File(getBilibiliDirectory(context), key);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, key + "-01");
    }

    public static File getBilibiliVideoFile(Context context, String url) {
        String key = KeyShare.md5(url);
        File dir = new File(getBilibiliDirectory(context), key);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, key + ".mp4");
    }

}
