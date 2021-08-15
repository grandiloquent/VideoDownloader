package euphoria.psycho.videos;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.CookieManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.videos.XVideosRedShare.Callback;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;

public class Iqiyi extends BaseVideoExtractor<List<Pair<String, String>>> {
    public static Pattern MATCH_IQIYI = Pattern.compile("\\.iqiyi\\.com/v_");

    public Iqiyi(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    private static String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                if (h.length() < 2)
                    hexString.append("0");
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String getMagicId() {
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        byte[] bytes = new byte[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122};
        byte[] buffer = new byte[32];
        int length = bytes.length;
        for (int i = 0; i < 32; i++) {
            buffer[i] = bytes[random.nextInt(length)];
        }
        return new String(buffer);
    }

    private static String findJSONValue(String pattern, String response, boolean isString) {
        int start = response.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = response.indexOf(",", start);
        if (end != -1) {
            if (isString) {
                start += 1;
                end -= 1;
            }
            return response.substring(start, end);
        }
        return null;
    }

    private String getHtml(String uri) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        NetShare.addDefaultRequestHeaders(urlConnection);
        urlConnection.setRequestProperty("Referer", "https://m.iqiyi.com/");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }
    }

    @Override
    protected List<Pair<String, String>> fetchVideoUri(String uri) {
        try {
            String response = getHtml(uri);
            if (response == null) {
                return null;
            }
            String title = StringShare.substring(response, "<title>", "-");
            String tvid = findJSONValue("\"tvid\":", response, false);
            String vid = findJSONValue("\"vid\":", response, true);
            String params = String.format("/vps?tvid=%s&vid=%s&v=0&qypid=%s_12&src=01012001010000000000&t=%d&k_tag=1&k_uid=%s&rs=1",
                    tvid, vid, tvid, System.currentTimeMillis(), getMagicId());
            String hash = md5(params + "1j2k2k3l3l4m4m5n5n6o6o7p7p8q8q9r");
            String url = String.format("%s%s&vf=%s", "http://cache.video.qiyi.com", params, hash);
            response = getHtml(url);
            if (response == null) {
                return null;
            }
            JSONObject obj = new JSONObject(response);
            if (obj.get("code").equals("A00000")) {
                String prefix = obj.getJSONObject("data").getJSONObject("vp").getString("du");
                JSONArray videos = obj.getJSONObject("data").getJSONObject("vp").getJSONArray("tkl").getJSONObject(0).getJSONArray("vs");
                List<Pair<Integer, JSONArray>> list = new ArrayList<>();
                for (int i = 0; i < videos.length(); i++) {
                    list.add(Pair.create(
                            Integer.parseInt(videos.getJSONObject(i).getString("scrsz").split("x")[0]),
                            videos.getJSONObject(i).getJSONArray("fs")
                    ));
                }
                Collections.sort(list, (o1, o2) -> o2.first - o1.first);
                JSONArray largestResolution = list.get(0).second;
                List<Pair<String, String>> videoList = new ArrayList<>();
                for (int i = 0; i < largestResolution.length(); i++) {
                    String uriPart = largestResolution.getJSONObject(i).getString("l");
                    String videoInfo = getHtml(prefix + uriPart);
                    if (videoInfo == null) {
                        return null;
                    }
                    String realVideoUri = findJSONValue("\"l\":", videoInfo, true);
                    videoList.add(Pair.create(title + i, realVideoUri));
                }
                return videoList;
            }

        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(List<Pair<String, String>> videoUri) {
        AlertDialog alertDialog =
                new AlertDialog.Builder(mMainActivity)
                        .setTitle("询问")
                        .setMessage("确定要下载此视频吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            dialog.dismiss();
                            executeTask(videoUri);
                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .create();
        alertDialog.show();
    }

    private void executeTask(List<Pair<String, String>> videoUri) {
        DownloadManager manager = (DownloadManager) mMainActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        for (int i = 0; i < videoUri.size(); i++) {
            downloadFile(manager, videoUri.get(i).second, videoUri.get(i).first + ".f4v", "video/x-f4v");
        }
    }

    private void downloadFile(DownloadManager manager, String url, String filename, String mimetype) {
        final DownloadManager.Request request;
        Uri uri = Uri.parse(url);
        request = new DownloadManager.Request(uri);
        request.setMimeType(mimetype);
        // set downloaded file destination to /sdcard/Download.
        // or, should it be set to one of several Environment.DIRECTORY* dirs depending on mimetype?
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        // let this downloaded file be scanned by MediaScanner - so that it can
        // show up in Gallery app, for example.
        request.allowScanningByMediaScanner();
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
//        String cookies = CookieManager.getInstance().getCookie(url, privateBrowsing);
//        request.addRequestHeader("cookie", cookies);
//        request.addRequestHeader("User-Agent", userAgent);
//        request.addRequestHeader("Referer", referer);
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        manager.enqueue(request);
    }
}
