package euphoria.psycho.videos;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.DownloadActivity;
import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.videos.XVideosRedShare.Callback;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.NetShare;

public class Porn91Share extends BaseVideoExtractor<String> {

    public Porn91Share(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        String response = getString(uri, new String[][]{
                {"Referer", "https://91porn.com"},
                {"X-Forwarded-For", NetShare.randomIp()}
        });
        if (response == null) {
            return null;
        }
        String encoded = StringShare.substring(response, "document.write(strencode2(\"", "\"));");
        String htm = null;
        try {
            htm = URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (htm != null) {
            return StringShare.substring(htm, "src='", "'");
        }
        return null;
    }

    public static AlertDialog.Builder createAlertDialogBuilder(Context context, String title, DialogInterface.OnClickListener p, DialogInterface.OnClickListener n) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, p)
                .setNegativeButton("下载", n);
    }

    public static void viewVideo(MainActivity mainActivity, String value) {
        try {
            String uri = "https://hxz315.com/?v=" + URLEncoder.encode(value, "UTF-8");
            createAlertDialogBuilder(mainActivity, "询问", (dialog, which) -> {
                dialog.dismiss();
                if (PreferenceShare.getPreferences().getBoolean("chrome", false)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setPackage("com.android.chrome");
                    intent.setData(Uri.parse(uri));
                    mainActivity.startActivity(intent);
                } else {
                    Helper.videoChooser(mainActivity, uri);
                }
            }, (dialog, which) -> {
                dialog.dismiss();
                Intent intent = new Intent(mainActivity, DownloadActivity.class);
                intent.setData(Uri.parse(value));
                mainActivity.startActivity(intent);
            })
                    .setMessage("是否使用浏览器打开视频链接")
                    .show();
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    @Override
    protected void processVideo(String videoUri) {
        viewVideo(mMainActivity, videoUri);
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("91porn.com/view_video.php\\?viewkey=[a-zA-Z0-9]+");
        if (pattern.matcher(uri).find()) {
            new Porn91Share(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}