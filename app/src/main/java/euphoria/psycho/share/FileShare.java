package euphoria.psycho.share;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import euphoria.psycho.explorer.BuildConfig;

import static android.os.Build.VERSION.SDK_INT;

public class FileShare {

    public static void appendAllText(File file, String contents) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
        writer.write(contents);
        writer.close();
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException t) {
        }
    }

    public static void closeSilently(ParcelFileDescriptor fd) {
        try {
            if (fd != null) fd.close();
        } catch (Throwable t) {
        }
    }

    public static void closeSilently(Cursor cursor) {
        try {
            if (cursor != null) cursor.close();
        } catch (Throwable t) {
        }
    }

    /**
     * Performs a simple copy of inputStream to outputStream.
     */
    public static void copyStream(InputStream inputStream, OutputStream outputStream)
            throws IOException {
        byte[] buffer = new byte[8192];
        int amountRead;
        while ((amountRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, amountRead);
        }
    }

    /**
     * Atomically copies the data from an input stream into an output file.
     *
     * @param is      Input file stream to read data from.
     * @param outFile Output file path.
     * @throws IOException in case of I/O error.
     */
    public static void copyStreamToFile(InputStream is, File outFile) throws IOException {
        File tmpOutputFile = new File(outFile.getPath() + ".tmp");
        try (OutputStream os = new FileOutputStream(tmpOutputFile)) {
            copyStream(is, os);
        }
        if (!tmpOutputFile.renameTo(outFile)) {
            throw new IOException();
        }
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

    /**
     * Extracts an asset from the app's APK to a file.
     *
     * @param context
     * @param assetName Name of the asset to extract.
     * @param outFile   File to extract the asset to.
     * @return true on success.
     */
    public static boolean extractAsset(Context context, String assetName, File outFile) {
        try (InputStream inputStream = context.getAssets().open(assetName)) {
            copyStreamToFile(inputStream, outFile);
            return true;
        } catch (IOException e) {
            return false;
        }
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

    public static String getExtension(String file) {
        int lastSep = file.lastIndexOf('/');
        int lastDot = file.lastIndexOf('.');
        if (lastSep >= lastDot) return ""; // Subsumes |lastDot == -1|.
        return file.substring(lastDot + 1).toLowerCase(Locale.US);
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

    /**
     * Get file size. If it is a directory, recursively get the size of all files within it.
     *
     * @param file The file or directory.
     * @return The size in bytes.
     */
    public static long getFileSizeBytes(File file) {
        if (file == null) return 0L;
        if (file.isDirectory()) {
            long size = 0L;
            final File[] files = file.listFiles();
            if (files == null) {
                return size;
            }
            for (File f : files) {
                size += getFileSizeBytes(f);
            }
            return size;
        } else {
            return file.length();
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

    public static String readAllText(File filename) throws IOException {
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filename), "UTF-8");
        String s = readText(reader);
        reader.close();
        return s;
    }

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

    public static byte[] readBytes(InputStream in, int estimatedSize) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.max(estimatedSize, in.available()));
        copyTo(in, buffer, 8 * 1024);
        return buffer.toByteArray();
    }

    /**
     * Reads inputStream into a byte array.
     */
    @NonNull
    public static byte[] readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        copyStream(inputStream, data);
        return data.toByteArray();
    }

    public static String readText(Reader in) throws IOException {
        StringWriter buffer = new StringWriter();
        copyTo(in, buffer, 8 * 1024);
        return buffer.toString();
    }

    public static String readText(InputStream in) throws IOException {
        return readText(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * Delete the given File and (if it's a directory) everything within it.
     *
     * @param currentFile The file or directory to delete. Does not need to exist.
     * @param canDelete   the {@link Function} function used to check if the file can be deleted.
     * @return True if the files are deleted, or files reserved by |canDelete|, false if failed to
     * delete files.
     * @note Caveat: Return values from recursive deletes are ignored.
     * @note Caveat: |canDelete| is not robust; see https://crbug.com/1066733.
     */
    public static boolean recursivelyDeleteFile(
            File currentFile, Function<String, Boolean> canDelete) {
        if (!currentFile.exists()) {
            // This file could be a broken symlink, so try to delete. If we don't delete a broken
            // symlink, the directory containing it cannot be deleted.
            currentFile.delete();
            return true;
        }
        if (canDelete != null && !canDelete.apply(currentFile.getPath())) {
            return true;
        }
        if (currentFile.isDirectory()) {
            File[] files = currentFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    recursivelyDeleteFile(file, canDelete);
                }
            }
        }
        boolean ret = currentFile.delete();
        if (!ret) {
        }
        return ret;
    }

    public static void requestManageAllFilePermission() {
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

    public static List<File> recursivelyListFiles(File directory) {
        ArrayList<File> results = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    results.addAll(recursivelyListFiles(file));
                } else {
                    results.add(file);
                }
            }
        }
        return results;
    }


}
// https://referencesource.microsoft.com/#mscorlib/system/io/file.cs
// https://referencesource.microsoft.com/#mscorlib/system/io/path.cs
