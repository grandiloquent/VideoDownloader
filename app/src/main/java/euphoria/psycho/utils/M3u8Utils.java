package euphoria.psycho.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

public class M3u8Utils {
    private static String URI = "https://hls-hw.xvideos-cdn.com/videos/hls/29/2f/cd/292fcd03310b0126b55dbd3ee0e47cce/hls-720p-e4013.m3u8?e=1628509853&l=0&h=2dfabad566e13a1e6b8c3c58a4959baa";
    private static String TS_MIMETYPE = "video/mp2t";
    private static String M3U8_MIME_TYPE = "application/x-mpegURL";

    //
    public static void parseM3u8File() {
        try {
            String response = getString(URI);
            if (response == null) {
                return;
            }
            String[] segments = response.split("\n");
            List<String> tsList = new ArrayList<>();
            for (int i = 0; i < segments.length; i++) {
                if (segments[i].startsWith("#EXTINF:")) {
                    tsList.add(segments[i + 1]);
                    i++;
                }
            }
            getHeaders(StringShare.substringBeforeLast(URI, "/") + "/" + tsList.get(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getHeaders(String uri) throws IOException {
        Logger.d(String.format("getHeaders: %s", uri));
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        urlConnection.setRequestProperty("Cache-Control", "max-age=0");
        urlConnection.setRequestProperty("Connection", "keep-alive");
        urlConnection.setRequestProperty("Referer", "https://www.xvideos.cn/");
        urlConnection.setRequestProperty("sec-ch-ua", "Chromium\";v=\"92\", \" Not A;Brand\";v=\"99\", \"Google Chrome\";v=\"92");
        urlConnection.setRequestProperty("sec-ch-ua-mobile", "?0");
        urlConnection.setRequestProperty("Sec-Fetch-Dest", "document");
        urlConnection.setRequestProperty("Sec-Fetch-Mode", "navigate");
        urlConnection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        urlConnection.setRequestProperty("Sec-Fetch-User", "?1");
        urlConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36");

    }

    public static String getString(String uri) throws IOException {
        URL url = new URL(uri);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, null, new java.security.SecureRandom());
            sc.createSSLEngine();
            urlConnection.setSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return NetShare.readString(urlConnection);
        } else {
            return null;
        }
    }

}

 