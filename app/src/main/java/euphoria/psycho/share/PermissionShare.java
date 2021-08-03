package euphoria.psycho.share;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

public class PermissionShare{
    public static boolean checkSelfPermission(Context context, String permission) {
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        } else {
            return false;
        }
    }
}
