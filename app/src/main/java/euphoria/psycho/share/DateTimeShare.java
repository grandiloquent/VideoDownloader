package euphoria.psycho.share;


import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;


public class DateTimeShare {
    public static int DurationToSeconds(String duration) {
        String[] pieces = duration.split(":");
        int total = 0;
        for (int i = pieces.length - 1, j = 0; i > -1; i--, j++) {
            total += Integer.parseInt(pieces[i]) * Math.pow(60, j);
        }
        return total;
    }

    // Returns a (localized) string for the given duration (in seconds).
    public static String formatDuration(final Context context, int duration) {
        int h = duration / 3600;
        int m = (duration - h * 3600) / 60;
        int s = duration - (h * 3600 + m * 60);
        String durationValue;
        if (h == 0) {
            durationValue = String.format("%02d:%02d", m, s);
        } else {
            durationValue = String.format("%d:%02d:%02d", h, m, s);
        }
        return durationValue;
    }

    public static String getShortDateString() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        return simpleDateFormat.format(new Date());

    }

    public static String getStringForTime(StringBuilder builder, Formatter formatter, long timeMs) {
        if (timeMs == Long.MIN_VALUE + 1) {
            timeMs = 0;
        }
        long totalSeconds = (timeMs + 500) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        builder.setLength(0);
        return hours > 0 ? formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
                : formatter.format("%02d:%02d", minutes, seconds).toString();
    }
}
