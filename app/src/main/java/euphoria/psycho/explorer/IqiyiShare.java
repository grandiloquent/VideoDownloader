package euphoria.psycho.explorer;

import android.os.Process;
import android.util.Pair;

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

import euphoria.psycho.explorer.XVideosRedShare.Callback;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;

public class IqiyiShare {
    public static String md5(final String s) {
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


    public static void performTask(String uri, Callback callback) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String response = null;
            try {
                response = getUrl(uri);
                if (response != null) {
                    String tvid = findJSONValue("\"tvid\":", response, false);
                    String vid = findJSONValue("\"vid\":", response, true);
                    Logger.d(String.format("performTask: tvid = %s, vid = %s", tvid, vid));
                    String params = String.format("/vps?tvid=%s&vid=%s&v=0&qypid=%s_12&src=01012001010000000000&t=%d&k_tag=1&k_uid=%s&rs=1",
                            tvid, vid, tvid, System.currentTimeMillis(), getMagicId());
                    String hash = md5(params + "1j2k2k3l3l4m4m5n5n6o6o7p7p8q8q9r");
                    String url = String.format("%s%s&vf=%s", "http://cache.video.qiyi.com", params, hash);
                    response = getUrl(url);
                    JSONObject obj = new JSONObject(response);
                    if (obj.get("code").equals("A00000")) {
                        String prefix = obj.getJSONObject("data").getJSONObject("vp").getString("du");
                        Logger.d(String.format("performTask: %s", prefix));
                        JSONArray videos = obj.getJSONObject("data").getJSONObject("vp").getJSONArray("tkl").getJSONObject(0).getJSONArray("vs");
                        List<Pair<Integer, String>> list = new ArrayList<>();
                        for (int i = 0; i < videos.length(); i++) {
                            list.add(Pair.create(
                                    Integer.parseInt(videos.getJSONObject(i).getString("scrsz").split("x")[0]),
                                    videos.getJSONObject(i).getJSONArray("fs").getJSONObject(0).getString("l")
                            ));
                        }
                        Collections.sort(list, (o1, o2) -> o2.first - o1.first);
                        response = getUrl(prefix + list.get(0).second);
                        if (response != null)
                            response = findJSONValue("\"l\":", response, true);
                    }

                }

            } catch (Exception e) {
                Logger.d(String.format("performTask: %s", e.getMessage()));

            }
            if (callback != null)
                callback.run(response);
        }).start();
    }

    private static String getUrl(String uri) throws IOException {
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
}
