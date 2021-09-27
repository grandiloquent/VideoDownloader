package euphoria.psycho.share;


import android.content.Context;
import android.net.ConnectivityManager;

import org.brotli.dec.BrotliInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NetShare {

    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";
    private static final int[][] range = {{607649792, 608174079}, {1038614528, 1039007743}, {1783627776, 1784676351},
            {2035023872, 2035154943}, {2078801920, 2079064063}, {-1950089216, -1948778497}, {-1425539072, -1425014785},
            {-1236271104, -1235419137}, {-770113536, -768606209}, {-569376768, -564133889},};

    public static final String PC_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36";

    public static void addDefaultRequestHeaders(HttpURLConnection urlConnection) {
        urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        urlConnection.setRequestProperty("Accept-Encoding", "gzip");
        urlConnection.setRequestProperty("Connection", "keep-alive");
        urlConnection.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
        //urlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
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
            Logger.d(String.format("%s: %s", header.getKey(), header.getValue().get(0)));
        }
    }

    public static String randomIp() {
        int index = ThreadLocalRandom.current().nextInt(range.length);
        int ip = range[index][0] + ThreadLocalRandom.current().nextInt(range[index][1] - range[index][0]);
        int[] b = new int[4];
        b[0] = (ip >> 24) & 0xff;
        b[1] = (ip >> 16) & 0xff;
        b[2] = (ip >> 8) & 0xff;
        b[3] = ip & 0xff;
        return b[0] + "." + b[1] + "." + b[2] + "." + b[3];
    }

    public static String readString(HttpURLConnection connection) {
        InputStream in;
        BufferedReader reader = null;
        try {
            String contentEncoding = connection.getHeaderField("Content-Encoding");
            if (contentEncoding != null && contentEncoding.equals("gzip")) {
                in = new GZIPInputStream(connection.getInputStream());
            }
            /*
            "implementation group": "org.brotli', name: 'dec', version: '0.1.1",
            else if (contentEncoding != null && contentEncoding.equals("br")) {
                in = new BrotliInputStream(connection.getInputStream());
            } */
            if (contentEncoding != null && contentEncoding.equals("br")) {
                in = new BrotliInputStream(connection.getInputStream());
            } else {
                in = connection.getInputStream();
            }
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
            return sb.toString();
        } catch (Exception ignored) {
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static Map<String, List<String>> getHeaders(String uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
        return connection.getHeaderFields();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public static boolean isInternetAvailable() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            return address.toString().length() != 0;
        } catch (UnknownHostException e) {
            // Log error
        }
        return false;
    }

    public static void disableSSLCertificateChecking() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {
                        // not implemented
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {
                        // not implemented
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}

