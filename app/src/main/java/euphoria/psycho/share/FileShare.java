package euphoria.psycho.share;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.util.Xml.Encoding;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.explorer.BuildConfig;

import static android.os.Build.VERSION.SDK_INT;

public class FileShare {

    public static String readAssetString(Context context, String fileName) {
        InputStream in = null;
        try {
            in = context.getAssets().open(fileName);
            return readText(in);
        } catch (IOException ignored) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;

    }

    public static void appendAllText(File file, String contents) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
        writer.write(contents);
        writer.close();
    }

    public static long copyTo(Reader in, Writer out, int bufferSize) throws IOException {
        //  8 * 1024
        long charsCopied = 0;
        char[] buffer = new char[bufferSize];
        int chars = in.read(buffer);
        while (chars >= 0) {
            out.write(buffer, 0, chars);
            charsCopied += chars;
            chars = in.read(buffer);
        }
        return charsCopied;
    }

    public static long copyTo(InputStream in, OutputStream out, int bufferSize) throws IOException {
        long bytesCopied = 0;
        byte[] buffer = new byte[bufferSize];
        int bytes = in.read(buffer);
        while (bytes >= 0) {
            out.write(buffer, 0, bytes);
            bytesCopied += bytes;
            bytes = in.read(buffer);
        }
        return bytesCopied;
    }

    public static byte[] readBytes(InputStream in, int estimatedSize) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.max(estimatedSize, in.available()));
        copyTo(in, buffer, 8 * 1024);
        return buffer.toByteArray();
    }

    public static String readText(Reader in) throws IOException {
        StringWriter buffer = new StringWriter();
        copyTo(in, buffer, 8 * 1024);
        return buffer.toString();
    }

    public static String readText(InputStream in) throws IOException {
        return readText(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public static String readAllText(File filename) throws IOException {
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filename), "UTF-8");
        String s = readText(reader);
        reader.close();
        return s;
    }

    public static String formatFileSize(long number) {
        float result = number;
        String suffix = "";
        if (result > 900) {
            suffix = " KB";
            result = result / 1024;
        }
        if (result > 900) {
            suffix = " MB";
            result = result / 1024;
        }
        if (result > 900) {
            suffix = " GB";
            result = result / 1024;
        }
        if (result > 900) {
            suffix = " TB";
            result = result / 1024;
        }
        if (result > 900) {
            suffix = " PB";
            result = result / 1024;
        }
        String value;
        if (result < 1) {
            value = String.format("%.2f", result);
        } else if (result < 10) {
            value = String.format("%.1f", result);
        } else if (result < 100) {
            value = String.format("%.0f", result);
        } else {
            value = String.format("%.0f", result);
        }
        return value + suffix;
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException t) {
        }
    }

    public static List<String> readAllLines(File file) throws IOException {
        String line;
        List<String> lines = new ArrayList<>();
        BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        while ((line = sr.readLine()) != null)
            lines.add(line);
        return lines;
    }

    public static String getFileNameFromUri(String path) {
        int end = path.indexOf('?');
        if (end == -1) {
            end = path.length();
        }
        int start = path.lastIndexOf('/', end);
        start = start == -1 ? 0 : start + 1;
        return path.substring(start, end);
    }

    public static void requestManageAllFilePermission(){
//        if (SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
//            try {
//                Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
//                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
//                startActivityForResult(intent, 1);
//            } catch (Exception ex) {
//                Intent intent = new Intent();
//                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                startActivityForResult(intent, 1);
//            }
//            return;
//        }
    }
}
// https://referencesource.microsoft.com/#mscorlib/system/io/file.cs
// https://referencesource.microsoft.com/#mscorlib/system/io/path.cs
