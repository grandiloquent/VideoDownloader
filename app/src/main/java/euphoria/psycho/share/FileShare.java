package euphoria.psycho.share;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.documentfile.provider.DocumentFile;


public class FileShare {
    public static final String KEY_TREE_URI = "tree_uri";
    public static String sSDPath;
    private static boolean sIsHasSD;

    public static void appendAllText(File file, String contents) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
        writer.write(contents);
        writer.close();
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }

    public static void closeSilently(ParcelFileDescriptor fd) {
        try {
            if (fd != null) fd.close();
        } catch (Throwable ignored) {
        }
    }

    public static void closeSilently(Cursor cursor) {
        try {
            if (cursor != null) cursor.close();
        } catch (Throwable ignored) {
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

    public static byte[] createChecksum(InputStream fis) throws Exception {
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    public static void createDirectoryIfNotExists(String dirPath) {
        createDirectoryIfNotExists(new File(dirPath));
    }

    public static void createDirectoryIfNotExists(File dir) {
         /*
         Tests whether the file denoted by this abstract pathname is a
         directory.
         Where it is required to distinguish an I/O exception from the case
         that the file is not a directory, or where several attributes of the
         same file are required at the same time, then the java.nio.file.Files#readAttributes(Path,Class,LinkOption[])
         Files.readAttributes method may be used.
         @return true if and only if the file denoted by this
         abstract pathname exists and is a directory;
         false otherwise
         @throws  SecurityException
         If a security manager exists and its java.lang.SecurityManager#checkRead(java.lang.String)
         method denies read access to the file
         */
        if (!dir.isDirectory()) {
         /*
         Creates the directory named by this abstract pathname, including any
         necessary but nonexistent parent directories.  Note that if this
         operation fails it may have succeeded in creating some of the necessary
         parent directories.
         @return  true if and only if the directory was created,
         along with all necessary parent directories; false
         otherwise
         @throws  SecurityException
         If a security manager exists and its java.lang.SecurityManager#checkRead(java.lang.String)
         method does not permit verification of the existence of the
         named directory and all necessary parent directories; or if
         the java.lang.SecurityManager#checkWrite(java.lang.String)
         method does not permit the named directory and all necessary
         parent directories to be created
         */
            dir.mkdirs();
        }
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

    /**
     * Returns the file extension, or an empty string if none.
     *
     * @param file Name of the file, with or without the full path.
     * @return empty string if no extension, extension otherwise.
     */
    public static String getExtension(String file) {
        int index = file.lastIndexOf('.');
        if (index == -1) return "";
        return file.substring(index + 1).toLowerCase(Locale.US);
    }

    public static String getExternalStoragePath(Context context) {
        StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            if (result == null) return null;
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                Object removableObject = isRemovable.invoke(storageVolumeElement);
                if (removableObject == null) return null;
                boolean removable = (Boolean) removableObject;
                if (removable) {
                    return path;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    public static String getMD5Checksum(InputStream fis) throws Exception {
        byte[] b = createChecksum(fis);
        StringBuilder result = new StringBuilder();
        for (byte value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    public static String getMD5Checksum(String filename) throws Exception {
        return getMD5Checksum(new FileInputStream(filename));
    }

    //    public static boolean extractAsset(Context context, String assetName, File dest) {
//        InputStream inputStream = null;
//        OutputStream outputStream = null;
//        try {
//            inputStream = context.getAssets().open(assetName);
//            outputStream = new BufferedOutputStream(new FileOutputStream(dest));
//            byte[] buffer = new byte[8192];
//            int c;
//            while ((c = inputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, c);
//            }
//            inputStream.close();
//            outputStream.close();
//            return true;
//        } catch (IOException e) {
//            if (inputStream != null) {
//                try {
//                    inputStream.close();
//                } catch (IOException ex) {
//                }
//            }
//            if (outputStream != null) {
//                try {
//                    outputStream.close();
//                } catch (IOException ex) {
//                }
//            }
//        }
//        return false;
//    }
    public static String getSDPath() {
        return sSDPath;
    }

    public static void initialize(Context context) {
        sIsHasSD = (sSDPath = FileShare.getExternalStoragePath(context)) != null;
    }

    public static boolean isFile(String path) {
        return new File(path).isFile();
    }

    public static boolean isHasSD() {
        return sIsHasSD;
    }
//    public static String getExtension(String file) {
//        int lastSep = file.lastIndexOf('/');
//        int lastDot = file.lastIndexOf('.');
//        if (lastSep >= lastDot) return ""; // Subsumes |lastDot == -1|.
//        return file.substring(lastDot + 1).toLowerCase(Locale.US);
//    }

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
     */
    public static void recursivelyDeleteFile(File currentFile) {
        if (currentFile.isDirectory()) {
            File[] files = currentFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    recursivelyDeleteFile(file);
                }
            }
        } else {
            currentFile.delete();
        }

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

    public static List<File> recursivelyListFiles(File directory, String extension) {
        ArrayList<File> results = new ArrayList<>();
        File[] files = directory.listFiles(file -> file.isDirectory() || (file.isFile() && file.getName().endsWith(extension)));
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    results.addAll(recursivelyListFiles(file, extension));
                } else {
                    results.add(file);
                }
            }
        }
        return results;
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

    public static boolean rename(String src) {
        if (sIsHasSD && src.startsWith(sSDPath)) {
            Logger.d(String.format("rename: %s", DocumentFile.fromFile(new File(src)).getUri()));
        }
        return true;
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

    public static void writeAllBytes(String path, byte[] bytes) throws IOException {
        FileOutputStream fs = new FileOutputStream(path);
        fs.write(bytes, 0, bytes.length);
        fs.close();
    }

    public static void writeAllText(File file, String contents) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        writer.write(contents);
        writer.close();
    }

    static Intent getStoragePermissionIntent(Context context) {
        Intent intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");//Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        if (context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        }
        return intent;
    }
}
// https://referencesource.microsoft.com/#mscorlib/system/io/file.cs
// https://referencesource.microsoft.com/#mscorlib/system/io/path.cs
