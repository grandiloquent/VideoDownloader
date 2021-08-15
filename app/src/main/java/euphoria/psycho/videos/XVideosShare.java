package euphoria.psycho.videos;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Process;
import android.util.Pair;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.DownloadActivity;
import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.StringShare;

public class XVideosShare extends BaseVideoExtractor<List<Pair<String, String>>> {


    public XVideosShare(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    private void parseHls(String hlsUri, List<Pair<String, String>> videoList) {
        String hls = getString(hlsUri,null);
        if (hls == null) return;
        String[] pieces = hls.split("\n");
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].startsWith("#EXT-X-STREAM-INF")) {
                String name = StringShare.substring(pieces[i], "NAME=\"", "\"");
                String url = StringShare.substringBeforeLast(hlsUri, "/") + "/" + pieces[i + 1];
                videoList.add(Pair.create(name, url));
                i++;
            }
        }

    }

    @Override
    protected List<Pair<String, String>> fetchVideoUri(String uri) {
        List<Pair<String, String>> videoList = new ArrayList<>();
        String htmlCode = getString(uri,null);
        if (htmlCode == null) return null;
        String low = StringShare.substring(htmlCode, "html5player.setVideoUrlLow('", "'");
        if (low != null) {
            videoList.add(Pair.create("标清", low));
        }
        String high = StringShare.substring(htmlCode, "html5player.setVideoUrlHigh('", "'");
        if (high != null) {
            videoList.add(Pair.create("高清", high));
        }
        String hls = StringShare.substring(htmlCode, "html5player.setVideoHLS('", "'");
        if (hls != null) {
            parseHls(hls, videoList);
        }
        return videoList;
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

    private static void launchDialog(MainActivity mainActivity, List<Pair<String, String>> videoList) throws IOException {
        String[] names = new String[videoList.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = videoList.get(i).first;
        }
        new AlertDialog.Builder(mainActivity)
                .setItems(names, (dialog, which) -> {
                    viewVideo(mainActivity, videoList.get(which).second);
                })
                .show();

    }

    @Override
    protected void processVideo(List<Pair<String, String>> videoUriList) {
        try {
            launchDialog(mMainActivity, videoUriList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("xvideos\\.com/video\\d+");
        if (pattern.matcher(uri).find()) {
            new XVideosShare(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}

