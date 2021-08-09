package euphoria.psycho.explorer;

import android.content.Context;
import android.os.Environment;
import android.os.Process;
import android.os.SystemClock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;


public class DownloadThread extends Thread {
    public static final int BUFFER_SIZE = 8192;
    private final Context mContext;
    private final DownloadNotifier mDownloadNotifier;
    private final String mUri;
    private BlobCache mBlobCache;
    private File mDirectory;
    private volatile boolean mShutdownRequested;
    private long mSpeedSampleStart;
    private long mCurrentBytes;
    private long mSpeedSampleBytes;
    private long mSpeed;

    public DownloadThread(String uri, Context context, DownloadNotifier downloadNotifier) {
        mUri = uri;
        mContext = context;
        mDownloadNotifier = downloadNotifier;
        try {
            mBlobCache = new BlobCache(mDirectory + "/log",
                    100, 1024 * 1024, false,
                    1);
        } catch (IOException e) {
            Logger.d(String.format("onCreate: %s", e.getMessage()));
        }
        initializeRootDirectory();
        initializeTaskDirectory();
    }

    private void downloadFile(String ts) throws IOException {
        String tsUri = StringShare.substringBeforeLast(mUri, "/")
                + "/"
                + ts;
        final String fileName = StringShare.substringBeforeLast(ts, "?");
        File tsFile = new File(mDirectory, fileName);
        if (tsFile.exists()) {
            int size = getBookmark(tsUri);
            if (tsFile.length() == size) {
                mDownloadNotifier.downloadProgress(tsUri, fileName, size);
                return;
            } else {
                tsFile.delete();
            }
        }
        mDownloadNotifier.downloadProgress(tsUri, fileName, 0);
        HttpURLConnection connection = (HttpURLConnection) new URL(tsUri).openConnection();
        int statusCode = connection.getResponseCode();
        if (statusCode >= 200 && statusCode < 400) {
            int size = Integer.parseInt(connection.getHeaderField("Content-Length"));
            setBookmark(tsUri, size);
            mDownloadNotifier.downloadProgress(tsUri, fileName, size);
            InputStream is = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(tsFile);
            transferData(is, out);
            FileShare.closeSilently(is);
            FileShare.closeSilently(out);
        }
    }

    private int getBookmark(String uri) {
        try {
            byte[] data = mBlobCache.lookup(uri.hashCode());
            if (data == null) return 0;
            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));
            return dis.readInt();
        } catch (Throwable t) {
            Logger.d(String.format("getBookmark: %s", t.getMessage()));
        }
        return 0;
    }

    private void initializeRootDirectory() {
        mDirectory = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "视频");
        if (!mDirectory.exists()) {
            mDirectory.mkdirs();
        }
    }

    private void initializeTaskDirectory() {
        String directoryName = Long.toString(KeyShare.crc64Long(StringShare.substringBeforeLast(mUri, "?")));
        mDirectory = new File(mDirectory, directoryName);
        if (!mDirectory.exists()) {
            mDirectory.mkdirs();
        }
    }

    private List<String> parseM3u8File() {
        try {
            String response = M3u8Share.getString(mUri);
            if (response == null) {
                return null;
            }
            String[] segments = response.split("\n");
            List<String> tsList = new ArrayList<>();
            for (int i = 0; i < segments.length; i++) {
                if (segments[i].startsWith("#EXTINF:")) {
                    String uri = segments[i + 1];
                    tsList.add(uri);
                    i++;
                }
            }
            return tsList;
        } catch (IOException e) {
            Logger.d(String.format("parseM3u8File: %s", e.getMessage()));

        }
        return null;
    }

    private void setBookmark(String uri, int size) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(uri);
            dos.writeInt(size);
            dos.flush();
            mBlobCache.insert(uri.hashCode(), bos.toByteArray());
        } catch (Throwable t) {
        }
    }

    private void transferData(InputStream in, OutputStream out) {
        final byte buffer[] = new byte[BUFFER_SIZE];
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
                updateProgress();

            } catch (IOException e) {
                throw new Error(e);
            }
        }

    }

    private void updateProgress() {
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
                mDownloadNotifier.downloadProgress(mUri, mCurrentBytes, mSpeed);
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
        mDownloadNotifier.downloadStart(mUri);
        List<String> tsList = parseM3u8File();
        if (tsList == null) {
            mDownloadNotifier.downloadFailed(mUri, "解析m3u8文件失败");
            return;
        }
        for (String ts : tsList) {
            try {
                downloadFile(ts);
            } catch (IOException e) {
                mDownloadNotifier.downloadFailed(mUri, e.getMessage());
            }
        }
    }

}
