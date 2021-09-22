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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.IntentShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.share.WebViewShare;
import euphoria.psycho.tasks.VideoActivity;

public class VideosHelper {
    public static String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";

    public static AlertDialog.Builder createAlertDialogBuilder(Context context, String title, DialogInterface.OnClickListener p, DialogInterface.OnClickListener n) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, p)
                .setNegativeButton(R.string.download, n);
    }

    public static String extract91PornVideoAddress(String uri) {
        String[] headers = null;
        try {
            headers = getLocationAddCookie(
                    "https://91porn.com/index.php",
                    null
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (headers == null) {
            return null;
        }
        // We need to use fake IPs to
        // get around the 50 views per day
        // for non members limitation
        String response = getString(uri, new String[][]{
                {"Referer", "https://91porn.com"},
                {"X-Forwarded-For", NetShare.randomIp()},
                {"Cookie", headers[1]}
        });
        if (response == null) {
//        byte[] buffer = new byte[128];
//        byte[] buf = uri.getBytes(StandardCharsets.UTF_8);
//        int result = NativeShare.get91Porn(buf, buf.length, buffer, 128);
//        if (result == 0) {
            return null;
        }
        /*try {
            FileShare.writeAllText(
                    new File(App.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                            "1.txt"),
                    response
            );
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        // maybe that is the fast way to
        // extract the encoded code which
        // contains the real video uri
        String encoded = StringShare.substring(response, "document.write(strencode2(\"", "\"));");
        if (encoded == null) {
            Logger.e(String.format("extract91PornVideoAddress, %s", encoded));
            return null;
        }
        String htm = null;
        try {
            // translate from the javascript code 'window.unescape'
            htm = URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (htm != null) {
            // the decoded code is some html
            // we need to locate the video uri
            return StringShare.substring(htm, "src='", "'");
        }
        return null;
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
        if (headers != null) {
            for (String[] header : headers) {
                urlConnection.setRequestProperty(header[0], header[1]);
            }
        }
        urlConnection.setRequestProperty("User-Agent", USER_AGENT);
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
        Intent intent = new Intent(context, euphoria.psycho.player.VideoActivity.class);
        intent.setData(videoUri);
        context.startActivity(intent);
    }

    public static void launchDialog(MainActivity mainActivity, List<Pair<String, String>> videoList) throws IOException {
        String[] names = new String[videoList.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = videoList.get(i).first;
        }
        new AlertDialog.Builder(mainActivity)
                .setItems(names, (dialog, which) -> {
                    invokeVideoPlayer(mainActivity, Uri.parse(videoList.get(which).second));
                    //viewVideoBetter(mainActivity, videoList.get(which).second);
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
            Logger.d(String.format("getString: %s", ignored.getMessage()));

        }
        return null;
    }

    public static void useChromeLoad(Context context, String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage("com.android.chrome");
        intent.setData(Uri.parse(uri));
        context.startActivity(intent);
    }

    public static void viewVideo(MainActivity mainActivity, String uri) {
        //String uri = URLEncoder.encode(value, "UTF-8");
        DialogShare.createAlertDialogBuilder(mainActivity, "询问", (dialog, which) -> {
            dialog.dismiss();
            if (PreferenceShare.getPreferences().getBoolean("chrome", false)) {
                IntentShare.launchChrome(mainActivity, uri);
            } else {
                VideosHelper.viewerChooser(mainActivity, uri);
            }
        }, (dialog, which) -> {
            mainActivity.getWebView().loadUrl(uri);
            dialog.dismiss();
        })
                .setMessage("是否使用浏览器打开视频链接")
                .show();
    }

    public static void viewVideoBetter(MainActivity mainActivity, String videoUri) {
        try {
            String uri = "https://hxz315.com/?v=" + URLEncoder.encode(videoUri, "UTF-8");
            createAlertDialogBuilder(mainActivity, mainActivity.getString(R.string.ask), (dialog, which) -> {
                dialog.dismiss();
                if (PreferenceShare.getPreferences().getBoolean("chrome", false)) {
                    VideosHelper.useChromeLoad(mainActivity, "https://hxz315.com?v=" + videoUri);
                } else {
                    VideosHelper.viewerChooser(mainActivity, "https://hxz315.com?v=" + videoUri);
                }
            }, (dialog, which) -> {
                dialog.dismiss();
                if (videoUri.contains("m3u8")) {
                    Intent intent = new Intent(mainActivity, VideoActivity.class);
                    intent.setData(Uri.parse(videoUri));
                    mainActivity.startActivity(intent);
                } else {
                    WebViewShare.downloadFile(mainActivity, KeyShare.toHex(videoUri.getBytes(StandardCharsets.UTF_8)), videoUri, USER_AGENT);
                }
            })
                    .setMessage(R.string.whether_to_use_the_browser_to_open_the_video_link)
                    .show();
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    public static void viewerChooser(Context context, String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        context.startActivity(Intent.createChooser(intent, "打开视频链接"));
    }
}
