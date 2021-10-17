package euphoria.psycho.explorer;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

import java.net.HttpURLConnection;
import java.net.URL;

import euphoria.psycho.share.NetShare;

public class UpdateUtils {

    private static final String APK_URI = "https://lucidu.cn/api/obs/%E8%A7%86%E9%A2%91%E6%B5%8F%E8%A7%88%E5%99%A8.apk";

    public static String getApplicationVersionName(Context context) {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (NameNotFoundException ignored) {
        }
        return null;
    }

    public static void launchDialog(Context context, DialogInterface.OnClickListener listener) {
        new Builder(context)
                .setMessage("程序有新版本，是否现在下载更新？")
                .setPositiveButton("确定", listener)
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void launchDownloadActivity(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(APK_URI));
        try {
            intent.setPackage("com.android.browser");
            context.startActivity(intent);
        } catch (Exception e) {
            context.startActivity(Intent.createChooser(intent, "下载最新版本"));
        }
    }
}