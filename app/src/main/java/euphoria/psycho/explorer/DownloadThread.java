package euphoria.psycho.explorer;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.os.Process;
import android.os.SystemClock;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.explorer.DownloadTaskDatabase.DownloadTaskInfo;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.utils.BlobCache;
import euphoria.psycho.utils.M3u8Utils;

import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_ERROR_CREATE_CACHE_FILES;
import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_ERROR_DOWNLOAD_FILE;
import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_FATAL;

public class DownloadThread extends Thread {
    public static final int BUFFER_SIZE = 8192;
    private final String mBaseUri;
    private final Context mContext;
    private final DownloadNotifier mDownloadNotifier;
    private final DownloadTaskInfo mDownloadTaskInfo;
    private final String mUri;
    private BlobCache mBlobCache;
    private volatile boolean mShutdownRequested;
    private long mSpeedSampleStart;
    private long mCurrentBytes;
    private long mSpeedSampleBytes;
    private long mSpeed;
    private int mTotalSize;
    private int mCurrentSize;
    private List<String> mVideos = new ArrayList<>();

    public DownloadThread(Context context, DownloadTaskInfo downloadTaskInfo, DownloadNotifier downloadNotifier) {
        mContext = context;
        mDownloadTaskInfo = downloadTaskInfo;
        mUri = mDownloadTaskInfo.Uri;
        mBaseUri = StringShare.substringBeforeLast(mUri, "/")
                + "/";
        mDownloadNotifier = downloadNotifier;

    }

    private void downloadFile(String ts, File tsFile) throws IOException {
        if (tsFile.exists()) {
            long size = getBookmark(tsFile.getName());
            if (tsFile.length() == size) {
                return;
            } else {
                tsFile.delete();
            }
        }
        String tsUri = mBaseUri + ts;
        HttpURLConnection connection = (HttpURLConnection) new URL(tsUri).openConnection();
        int statusCode = connection.getResponseCode();
        if (statusCode >= 200 && statusCode < 400) {
            long size = Long.parseLong(connection.getHeaderField("Content-Length"));
            setBookmark(tsFile.getName(), size);
            InputStream is = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(tsFile);
            transferData(is, out, tsFile.getName());
            FileShare.closeSilently(is);
            FileShare.closeSilently(out);
        }
    }

    private long getBookmark(String uri) {
        try {
            byte[] data = mBlobCache.lookup(uri.hashCode());
            if (data == null) return 0;
            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));
            dis.readUTF();
            return dis.readLong();
        } catch (Throwable t) {
            Logger.d(String.format("getBookmark: %s", t.getMessage()));
        }
        return 0;
    }


    private List<String> parseM3u8File() {
        try {
            String response = M3u8Utils.getString(mUri);
            if (response == null) {
                mDownloadTaskInfo.Status = STATUS_FATAL;
                mDownloadNotifier.downloadFailed(mDownloadTaskInfo);
                return null;
            } else {
                File directory = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), KeyShare.toHex(KeyShare.md5encode(response)));
                mDownloadTaskInfo.FileName = directory.getAbsolutePath();
                Logger.d(String.format("parseM3u8File: %s", mDownloadTaskInfo.FileName));
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                mDownloadNotifier.updateDatabase(mDownloadTaskInfo);
            }
            try {
                mBlobCache = new BlobCache(mDownloadTaskInfo.FileName + "/log",
                        100, 1024 * 1024, false,
                        1);
            } catch (IOException e) {
                mDownloadTaskInfo.Status = STATUS_ERROR_CREATE_CACHE_FILES;
                mDownloadNotifier.downloadFailed(mDownloadTaskInfo);
                return null;
            }
            String[] segments = response.split("\n");
            List<String> tsList = new ArrayList<>();
            for (int i = 0; i < segments.length; i++) {
                if (segments[i].startsWith("#EXTINF:")) {
                    String uri = segments[i + 1];
                    tsList.add(uri);
                    mVideos.add(FileShare.getFileNameFromUri(uri));
                    i++;
                }
            }
            return tsList;
        } catch (IOException | NoSuchAlgorithmException e) {
            Logger.d(String.format("parseM3u8File: %s", e.getMessage()));
        }
        return null;
    }



    private void setBookmark(String uri, long size) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(uri);
            dos.writeLong(size);
            dos.flush();
            mBlobCache.insert(uri.hashCode(), bos.toByteArray());
        } catch (Throwable t) {
            Logger.d(String.format("setBookmark: %s", t.getMessage()));
        }
    }

    private void transferData(InputStream in, OutputStream out, String fileName) {
        mDownloadNotifier.downloadProgress(mDownloadTaskInfo, mCurrentSize, mTotalSize, mCurrentBytes, 0, fileName);
        final byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            if (mShutdownRequested) {
                throw new Error("Local halt requested; job probably timed out");
            }
            int len = -1;
            try {
                len = in.read(buffer);
            } catch (IOException e) {
                throw new Error("Failed reading response: " + e, e);
            }
            if (len == -1) {
                break;
            }
            try {
                out.write(buffer, 0, len);
                mCurrentBytes += len;
                //updateProgress(fileName);

            } catch (IOException e) {
                throw new Error(e);
            }
        }

    }

    private void updateProgress(String fileName) {
        final long now = SystemClock.elapsedRealtime();
        final long currentBytes = mCurrentBytes;
        final long sampleDelta = now - mSpeedSampleStart;
        if (sampleDelta > 500) {
            final long sampleSpeed = ((currentBytes - mSpeedSampleBytes) * 1000)
                    / sampleDelta;
            if (mSpeed == 0) {
                mSpeed = sampleSpeed;
            } else {
                mSpeed = ((mSpeed * 3) + sampleSpeed) / 4;
            }
            // Only notify once we have a full sample window
            if (mSpeedSampleStart != 0) {
                mDownloadNotifier.downloadProgress(mDownloadTaskInfo, mCurrentSize, mTotalSize, mCurrentBytes, mSpeed, fileName);
            }
            mSpeedSampleStart = now;
            mSpeedSampleBytes = currentBytes;
        }
//        final long bytesDelta = currentBytes - mLastUpdateBytes;
//        final long timeDelta = now - mLastUpdateTime;
//        if (bytesDelta > MIN_PROGRESS_STEP && timeDelta > MIN_PROGRESS_TIME) {
//            // fsync() to ensure that current progress has been flushed to disk,
//            // so we can always resume based on latest database information.
//            out.flush();
//            mLastUpdateBytes = currentBytes;
//            mLastUpdateTime = now;
//        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        List<String> tsList = parseM3u8File();
        if (tsList == null) {
            return;
        }
        mTotalSize = tsList.size();
        mDownloadNotifier.downloadStart(mDownloadTaskInfo);
        for (String ts : tsList) {
            final String fileName = FileShare.getFileNameFromUri(ts);
            File tsFile = new File(mDownloadTaskInfo.FileName, fileName);
            try {
                downloadFile(ts, tsFile);
                mCurrentSize++;
            } catch (IOException e) {
                mDownloadTaskInfo.Status = STATUS_ERROR_DOWNLOAD_FILE;
                mDownloadNotifier.downloadFailed(mDownloadTaskInfo);
                return;
            }

        }
        mDownloadNotifier.downloadCompleted(mDownloadTaskInfo);
        try {
            String outputPath = new File(mDownloadTaskInfo.FileName, StringShare.substringAfterLast(mDownloadTaskInfo.FileName, "/") + ".mp4")
                    .getAbsolutePath();
            OutputStream fileOutputStream = new FileOutputStream(outputPath);
            byte[] b = new byte[4096];
            for (String video : mVideos) {
                FileInputStream fileInputStream = new FileInputStream(new File(mDownloadTaskInfo.FileName, video));
                int len;
                while ((len = fileInputStream.read(b)) != -1) {
                    fileOutputStream.write(b, 0, len);
                }
                fileInputStream.close();
                fileOutputStream.flush();
            }
            fileOutputStream.close();
            mDownloadNotifier.mergeVideoCompleted(mDownloadTaskInfo, outputPath);
        } catch (IOException e) {
            mDownloadNotifier.mergeVideoFailed(mDownloadTaskInfo, e.getMessage());
            Logger.d(String.format("run: %s", e.getMessage()));
        }


    }

}
