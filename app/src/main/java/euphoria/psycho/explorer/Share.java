package euphoria.psycho.explorer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import euphoria.psycho.share.FileShare;

public class Share {

    public static boolean downloadFile(String url, String fileName) {
        HttpURLConnection connection = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            URL u = new URL(url);
            connection = (HttpURLConnection) u.openConnection();
            List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
            if (cookies != null && cookies.size() > 0) {
                Log.e("TAG/", "[DEBUG][download]: "
                        + url + "\n"
                        + cookies.get(0) + "\n");
            }
            byte[] buffer = new byte[8192];
            File targetFile = new File(fileName);
            int count = 1;
            while (targetFile.isFile()) {
                fileName = fileName + count;
                count++;
                targetFile = new File(fileName);
            }
            output = new FileOutputStream(fileName);
            int byteCount;
            input = new BufferedInputStream(u.openStream(), 8192);
            while ((byteCount = input.read(buffer)) != -1) {
                output.write(buffer, 0, byteCount);
            }
            output.close();
            output = null;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG/Strings", "[ERROR][download]: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (output != null) {
                    output.close();
                    output = null;
                }
                if (input != null) {
                    input.close();
                    input = null;
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException e) {
                Log.e("TAG/Strings", "Error: download, " + e.getMessage() + " " + e.getCause());
            }
        }
        return false;
    }

    public static String getClipboardText(Context context) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            ClipData clipData = clipboardManager.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                CharSequence c = clipData.getItemAt(0).getText();
                if (c != null)
                    return c.toString();
            }
        }
        return null;
    }

    public static void setClipboardText(Context context, String s) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null)
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, s));
    }

    public static String touchServer(String url, String method, String accessToken, String jsonBody) {
        Log.e("TAG/Utils", "[ERROR][touch]: " + url);
        disableSSLCertificateChecking();
        HttpURLConnection connection = null;
        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            Log.e("TAG/Utils", "[ERROR][touch]: " + e.getMessage());
            return e.getMessage();
        }
        try {
            connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod(method);
            if (accessToken != null)
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            connection.setRequestProperty("Cache-Control", "max-age=0");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.56 Mobile Safari/537.36");
            if (jsonBody != null) {
                connection.addRequestProperty("Content-Type", "application/json");
                connection.addRequestProperty("Content-Encoding", "gzip");
                GZIPOutputStream outGZIP;
                outGZIP = new GZIPOutputStream(connection.getOutputStream());
                byte[] body = jsonBody.getBytes("utf-8");
                outGZIP.write(body, 0, body.length);
                outGZIP.close();
            }
            int code = connection.getResponseCode();
            StringBuilder sb = new StringBuilder();
//            sb.append("ResponseCode: ").append(code).append("\r\n");
//
//
//            Set<String> keys = connection.getHeaderFields().keySet();
//            for (String key : keys) {
//                sb.append(key).append(": ").append(connection.getHeaderField(key)).append("\r\n");
//            }
            if (code < 400 && code >= 200) {
                //sb.append("\r\n\r\n");
                InputStream in;
                String contentEncoding = connection.getHeaderField("Content-Encoding");
                if (contentEncoding != null && contentEncoding.equals("gzip")) {
                    in = new GZIPInputStream(connection.getInputStream());
                } else {
                    in = connection.getInputStream();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\r\n");
                }
                reader.close();
            } else {
                Log.e("TAG/Utils", "[ERROR][touch]: " + code);
                sb.append("Method: ").append(method).append(";\n")
                        .append("ResponseCode: ").append(code).append(";\n")
                        .append("Error: ").append(FileShare.toString(connection.getErrorStream()));
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e("TAG/Utils", "[ERROR][touch]: " + e.getMessage());
            return e.getMessage();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void disableSSLCertificateChecking() {
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