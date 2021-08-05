package euphoria.psycho.share;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

public class NetShare {

    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";

    public static void addDefaultRequestHeaders(HttpURLConnection urlConnection) {
        urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        urlConnection.setRequestProperty("Accept-Encoding", "gzip");
        urlConnection.setRequestProperty("Connection", "keep-alive");
        urlConnection.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
        urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        urlConnection.setRequestProperty("Cache-Control", "max-age=0");
        urlConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        urlConnection.setRequestProperty("Sec-Fetch-Dest", "document");
        urlConnection.setRequestProperty("Sec-Fetch-Mode", "navigate");
        urlConnection.setRequestProperty("Sec-Fetch-Site", "cross-site");
        urlConnection.setRequestProperty("Sec-Fetch-User", "?1");

    }

    public static void iterateResponseHeader(HttpURLConnection connection) {
        Map<String, List<String>> listMap = connection.getHeaderFields();
        for (Entry<String, List<String>> header : listMap.entrySet()) {
        }
    }

    public static String readString(HttpURLConnection connection) {
        StringBuilder sb = new StringBuilder();
        InputStream in;
        BufferedReader reader = null;
        try {
            String contentEncoding = connection.getHeaderField("Content-Encoding");
            if (contentEncoding != null && contentEncoding.equals("gzip")) {
                in = new GZIPInputStream(connection.getInputStream());
            } else {
                in = connection.getInputStream();
            }
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (Exception ignored) {
            }
        }
        return sb.toString();
    }
}

