package euphoria.psycho.explorer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import euphoria.psycho.share.StringShare;
import euphoria.psycho.share.ThreadShare;

public class Share {

    public static void startYouTubeActivity(Context context, WebView webView) {
        Intent intent = new Intent(context, SampleDownloadActivity.class);
        intent.setAction(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
        context.startActivity(intent);
    }
    public static List<Long> collectLongs(String s) {
        if (TextUtils.isEmpty(s)) {
            return null;
        }
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(s);
        List<Long> integers = new ArrayList<>();
        while (matcher.find()) {
            integers.add(Long.parseLong(matcher.group()));
        }
        return integers;
    }


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

    public static String getFileNameFromURL(String url) {
        String uri = StringShare.substringAfterLast(url, '/');
        uri = StringShare.substringBefore(uri, '?');
        if (uri.length() == 0) {
            uri = "filename";
        }
        if (uri.length() > 100) {
            uri = uri.substring(0, 100);
        }
        return uri;
    }

    public static String getShortDateString() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        return simpleDateFormat.format(new Date());
    }

    public static boolean isDigit(String s) {
        if (isNullOrWhiteSpace(s))
            return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    public static boolean isLetterOrDigit(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isLetterOrDigit(value.charAt(i))) return false;
        }
        return true;
    }

    public static boolean isNullOrWhiteSpace(String value) {
        return value == null || value.trim().length() == 0;
    }

    public static String join(String separator, List<String> value) {
        if (value == null || value.size() == 0) return null;
        if (separator == null) separator = "";
        StringBuilder sb = new StringBuilder();
        sb.append(value.get(0));
        int length = value.size();
        if (length > 1) {
            for (int i = 1; i < length; i++) {
                sb.append(separator).append(value.get(i));
            }
        }
        return sb.toString();
    }

    public static void launchDialog(Context context, String title, String message, View view, DialogInterface.OnClickListener positiveCallback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(view)
                .setPositiveButton(android.R.string.ok, positiveCallback)
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public static void launchAskDialog(Context context, String message, DialogInterface.OnClickListener positiveCallback) {
        new AlertDialog.Builder(context)
                .setTitle("询问")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, positiveCallback)
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public static ProgressDialog launchProgressDialog(Context context, String message) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    public static void message(final Context context, final String message) {
        ThreadShare.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static String padStart(String s, int length, char padChar) {
        if (length < 0)
            throw new IllegalArgumentException(String.format("Desired length %d is less than zero.", length));
        if (length <= s.length())
            return s;
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length - s.length(); i++) {
            sb.append(padChar);
        }
        sb.append(s);
        return sb.toString();
    }


    public static void setClipboardText(Context context, String s) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null)
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, s));
    }


    public static String toString(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "utf-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\r\n");
        }
        reader.close();
        return sb.toString();
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
                        .append("Error: ").append(toString(connection.getErrorStream()));
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

    public static void writeAllText(File file, String contents) throws IOException {

        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");

        writer.write(contents);

        writer.close();
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
