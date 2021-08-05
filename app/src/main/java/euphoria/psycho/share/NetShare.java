package euphoria.psycho.share;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import euphoria.psycho.explorer.Share;

public class NetShare {
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

    public static boolean iterateResponseHeader(HttpURLConnection connection) {
        Map<String, List<String>> listMap = connection.getHeaderFields();
        for (Entry<String, List<String>> header : listMap.entrySet()) {
        }
        return false;
    }

 // 
}

