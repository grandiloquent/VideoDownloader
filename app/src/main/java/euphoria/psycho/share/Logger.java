package euphoria.psycho.share;

import android.util.Log;

import java.util.List;

public class Logger {
    private static boolean sDebug = true;

    public static void d(String message) {
        if (!sDebug) return;
        Log.e("TAG", message);
    }
    public static void e(String message) {
        if (!sDebug) return;
        Log.e("apzDJY", message);
    }
    public static <T> void d(List<T> list) {
        if (!sDebug) return;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(list.size()).append('\n');
        for (T t : list) {
            stringBuilder.append(t).append('\n');
        }
        Log.e("TAG", stringBuilder.toString());
    }
}
//