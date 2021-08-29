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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.share.WebViewShare;
import euphoria.psycho.tasks.VideoActivity;

public abstract class BaseVideoExtractor<T> {
    public static String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";
    private final String mInputUri;
    protected final MainActivity mMainActivity;

    protected BaseVideoExtractor(String inputUri, MainActivity mainActivity) {
        mInputUri = inputUri;
        mMainActivity = mainActivity;
    }

    public String getLocation(String uri, String[][] headers) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (headers != null) {
            for (String[] header : headers) {
                urlConnection.setRequestProperty(header[0], header[1]);
            }
        }
        urlConnection.setRequestProperty("User-Agent", BaseVideoExtractor.USER_AGENT);
        urlConnection.setInstanceFollowRedirects(false);
        Logger.d(String.format("getLocation: %d", urlConnection.getResponseCode()));
        return urlConnection.getHeaderField("Location");
    }

    public String[] getLocationAddCookie(String uri, String[][] headers) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (headers != null) {
            for (String[] header : headers) {
                urlConnection.setRequestProperty(header[0], header[1]);
            }
        }
        urlConnection.setRequestProperty("User-Agent", BaseVideoExtractor.USER_AGENT);
        urlConnection.setInstanceFollowRedirects(false);
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
        return new String[]{urlConnection.getHeaderField("Location"), stringBuilder.toString()};
    }

    public static void launchDialog(MainActivity mainActivity, List<Pair<String, String>> videoList) throws IOException {
        String[] names = new String[videoList.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = videoList.get(i).first;
        }
        new AlertDialog.Builder(mainActivity)
                .setItems(names, (dialog, which) -> {
                    viewVideoBetter(mainActivity, videoList.get(which).second);
                })
                .show();
        //
    }

    public void parsingVideo() {
        ProgressDialog progressDialog = DialogShare.createProgressDialog(mMainActivity);
        progressDialog.show();
        String uri = processUri(mInputUri);
        performTask(uri, progressDialog);
    }

    public static void viewVideoBetter(MainActivity mainActivity, String videoUri) {
        try {
            String uri = "https://hxz315.com/?v=" + URLEncoder.encode(videoUri, "UTF-8");
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
                if (videoUri.contains("m3u8")) {
                    Intent intent = new Intent(mainActivity, VideoActivity.class);
                    intent.setData(Uri.parse(videoUri));
                    mainActivity.startActivity(intent);
                } else {
                    WebViewShare.downloadFile(mainActivity, KeyShare.toHex(videoUri.getBytes(StandardCharsets.UTF_8)), videoUri, BaseVideoExtractor.USER_AGENT);
                }
            })
                    .setMessage("是否使用浏览器打开视频链接")
                    .show();
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    protected static AlertDialog.Builder createAlertDialogBuilder(Context context, String title, DialogInterface.OnClickListener p, DialogInterface.OnClickListener n) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, p)
                .setNegativeButton("下载", n);
    }

    protected abstract T fetchVideoUri(String uri);

    protected String[] getResponse(String uri, String[][] headers) {
        try {
            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", BaseVideoExtractor.USER_AGENT);
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

    protected String getString(String uri, String[][] headers) {
        try {
            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", BaseVideoExtractor.USER_AGENT);
            if (headers != null) {
                for (String[] header : headers) {
                    urlConnection.setRequestProperty(header[0], header[1]);
                }
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

    protected String postFormUrlencoded(String uri, String[][] headers, String[][] values) {
        try {
            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("User-Agent", BaseVideoExtractor.USER_AGENT);
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

    protected abstract String processUri(String inputUri);

    protected abstract void processVideo(T videoUri);

    private void performTask(String uri, ProgressDialog progressDialog) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            T videoUri = fetchVideoUri(uri);
            mMainActivity.runOnUiThread(() -> {
                if (videoUri != null) {
                    processVideo(videoUri);
                } else {
                    Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
            });
        }).start();
    }
}
//