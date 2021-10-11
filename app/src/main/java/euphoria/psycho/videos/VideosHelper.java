package euphoria.psycho.videos;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.R;
import euphoria.psycho.player.VideoActivity;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.StringShare;

public class VideosHelper {
    public static String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";

    public static AlertDialog.Builder createAlertDialogBuilder(Context context, String title, DialogInterface.OnClickListener p, DialogInterface.OnClickListener n) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, p)
                .setNegativeButton(R.string.download, n);
    }


    public static String getLocation(String uri, String[][] headers) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (headers != null) {
            for (String[] header : headers) {
                urlConnection.setRequestProperty(header[0], header[1]);
            }
        }
        urlConnection.setRequestProperty("User-Agent", USER_AGENT);
        urlConnection.setInstanceFollowRedirects(false);
        return urlConnection.getHeaderField("Location");
    }

    public static String[] getLocationAddCookie(String uri, String[][] headers) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        //urlConnection.setRequestProperty("User-Agent", USER_AGENT);
        if (headers != null) {
            for (String[] header : headers) {
                urlConnection.setRequestProperty(header[0], header[1]);
            }
        }
        urlConnection.setInstanceFollowRedirects(false);
        Map<String, List<String>> listMap = urlConnection.getHeaderFields();
        StringBuilder stringBuilder = new StringBuilder();
        for (Entry<String, List<String>> header : listMap.entrySet()) {
            if (header.getKey() != null && header.getKey().toLowerCase().equals("set-cookie")) {
                for (String s : header.getValue()) {
                    stringBuilder.append(StringShare.substringBefore(s, "; "))
                            .append("; ");
                }
            }
        }
        Log.e("B5aOx2", String.format("getLocationAddCookie, %s", urlConnection.getResponseCode()));
        return new String[]{urlConnection.getHeaderField("Location"), stringBuilder.toString()};
    }

    public static String[] getResponse(String uri, String[][] headers) {
        try {
            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            if (headers != null) {
                for (String[] header : headers) {
                    urlConnection.setRequestProperty(header[0], header[1]);
                }
            }
            Map<String, List<String>> listMap = urlConnection.getHeaderFields();
            StringBuilder stringBuilder = new StringBuilder();
            for (Entry<String, List<String>> header : listMap.entrySet()) {
                if (header.getKey() != null && header.getKey().equals("set-cookie")) {
                    for (String s : header.getValue()) {
                        stringBuilder.append(StringShare.substringBefore(s, "; "))
                                .append("; ");
                    }
                }
            }
            int code = urlConnection.getResponseCode();
            if (code < 400 && code >= 200) {
                return new String[]{NetShare.readString(urlConnection), stringBuilder.toString()};
            } else {
                return null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    //
    public static String getString(String uri, String[][] headers) {
        try {
            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            if (headers != null) {
                for (String[] header : headers) {
                    urlConnection.setRequestProperty(header[0], header[1]);
                }
            }
            int code = urlConnection.getResponseCode();
            if (code < 400 && code >= 200) {
                return NetShare.readString(urlConnection);
            } else {
                Log.e("B5aOx2", String.format("An error occurred while requesting %s, %s", uri, code));
                return null;
            }
        } catch (Exception exception) {
            Log.e("B5aOx2", String.format("An error occurred while requesting %s, %s", uri, exception.getMessage()));
        }
        return null;
    }

    public static void invokeVideoPlayer(Context context, Uri videoUri) {
        Intent intent = new Intent(context, VideoActivity.class);
        intent.setData(videoUri);
        context.startActivity(intent);
    }

    public static void useChromeLoad(Context context, String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage("com.android.chrome");
        intent.setData(Uri.parse(uri));
        context.startActivity(intent);
    }

    public static void viewerChooser(Context context, String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        context.startActivity(Intent.createChooser(intent, "打开视频链接"));
    }

    public static void launchDialog(MainActivity mainActivity, List<Pair<String, String>> videoList) throws IOException {
        String[] names = new String[videoList.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = videoList.get(i).first;
        }
        new AlertDialog.Builder(mainActivity)
                .setItems(names, (dialog, which) -> {
                    String videoUri = videoList.get(which).second;
                    if (PreferenceShare.getPreferences().getBoolean("chrome", false)) {
                        useChromeLoad(mainActivity, videoUri);
                    } else {
                        viewerChooser(mainActivity, videoUri);
                    }
                })
                .show();
        //
    }

    public static String postFormUrlencoded(String uri, String[][] headers, String[][] values) {
        try {
            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            if (headers != null) {
                for (String[] header : headers) {
                    urlConnection.setRequestProperty(header[0], header[1]);
                }
            }
            if (values != null) {
                OutputStream outputStream = urlConnection.getOutputStream();
                for (int i = 0, j = values.length; i < j; i++) {
                    outputStream.write(values[i][0].getBytes(StandardCharsets.UTF_8));
                    outputStream.write(61);
                    outputStream.write(URLEncoder.encode(values[i][1], "UTF-8").getBytes(StandardCharsets.UTF_8));
                    if (i + 1 < j)
                        outputStream.write(38);
                }
                outputStream.close();
            }
            int code = urlConnection.getResponseCode();
            if (code < 400 && code >= 200) {
                return NetShare.readString(urlConnection);
            } else {
                return null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

}
