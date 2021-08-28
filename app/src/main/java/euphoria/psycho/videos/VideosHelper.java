package euphoria.psycho.videos;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

public class VideosHelper {

    public static String extract91PornVideoAddress(String uri) {
        // We need to use fake IPs to
        // get around the 50 views per day
        // for non members limitation
        String response = getString(uri, new String[][]{
                {"Referer", "https://91porn.com"},
                {"X-Forwarded-For", NetShare.randomIp()}
        });
        if (response == null) {
//        byte[] buffer = new byte[128];
//        byte[] buf = uri.getBytes(StandardCharsets.UTF_8);
//        int result = NativeShare.get91Porn(buf, buf.length, buffer, 128);
//        if (result == 0) {
            return null;
        }
        // maybe that is the fast way to
        // extract the encoded code which
        // contains the real video uri
        String encoded = StringShare.substring(response, "document.write(strencode2(\"", "\"));");
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

    public static String getString(String uri, String[][] headers) {
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

}
