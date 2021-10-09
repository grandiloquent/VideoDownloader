package euphoria.psycho.share;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.List;

public class PackageShare {
    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            Log.e("B5aOx2", String.format("isAppInstalled, %s", e.getMessage()));
            return false;
        }
    }

    public static void listAllInstalledPackages(Context context) {
        final PackageManager pm = context.getPackageManager();
        if (pm == null) return;
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            Log.e("TAG", "Installed package :" + packageInfo.packageName);
            //Log.e("TAG", "Source dir : " + packageInfo.sourceDir);
            //Log.e("TAG", "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
        }
    }
}
