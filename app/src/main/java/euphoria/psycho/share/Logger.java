package euphoria.psycho.share;

import android.util.Log;

public class Logger {
    private static boolean sDebug = true;

    public static void d(String message) {
        if (!sDebug) return;
        Log.e("TAG", message);
    }
}
