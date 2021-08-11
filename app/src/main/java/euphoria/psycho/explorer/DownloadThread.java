package euphoria.psycho.explorer;

import android.content.Context;
import android.os.Environment;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

import com.jeffmony.ffmpeglib.LogUtils;
import com.jeffmony.ffmpeglib.VideoProcessor;
import com.jeffmony.ffmpeglib.listener.OnVideoCompositeListener;

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
    private long mTotalSize;

    public DownloadThread(String uri, Context context, DownloadNotifier downloadNotifier) {
        mUri = uri;
        mContext = context;
        mDownloadNotifier = downloadNotifier;
        initializeRootDirectory();
        initializeTaskDirectory();
        try {
            mBlobCache = new BlobCache(mDirectory + "/log",
                    100, 1024 * 1024, false,
                    1);
        } catch (IOException e) {
            Logger.d(String.format("onCreate: %s", e.getMessage()));
        }

    }

    private void downloadFile(String ts) throws IOException {
        String tsUri = StringShare.substringBeforeLast(mUri, "/")
                + "/"
                + ts;
        final String fileName = StringShare.substringAfterLast(StringShare.substringBeforeLast(ts, "?"), "/");
        File tsFile = new File(mDirectory, fileName);
        if (tsFile.exists()) {
            long size = getBookmark(tsUri);
            Logger.d(String.format("downloadFile: %d %d", tsFile.length(), size));
            if (tsFile.length() == size) {
                Logger.d(String.format("downloadFile: %s", "cached"));
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
            long size = Long.parseLong(connection.getHeaderField("Content-Length"));
            mTotalSize = size;
            setBookmark(tsUri, size);
            mDownloadNotifier.downloadProgress(tsUri, fileName, size);
            InputStream is = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(tsFile);
            transferData(is, out);
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
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                if (segments[i].startsWith("#EXTINF:")) {
                    String uri = segments[i + 1];
                    tsList.add(uri);
                    final String fileName = StringShare.substringAfterLast(StringShare.substringBeforeLast(uri, "?"), "/");
                    sb.append(fileName).append('\n');
                    i++;
                }
            }
            OutputStream outputStream = new FileOutputStream(new File(mDirectory, "list.txt"));
            outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.close();
            return tsList;
        } catch (IOException e) {
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

    private void transferData(InputStream in, OutputStream out) {
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
                updateProgress();

            } catch (IOException e) {
                throw new Error(e);
            }
        }
        mDownloadNotifier.downloadProgress(mUri, mTotalSize, mCurrentBytes, 0);

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
                mDownloadNotifier.downloadProgress(mUri, mTotalSize, mCurrentBytes, mSpeed);
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
                mCurrentBytes = 0;
                mSpeedSampleStart = 0;
                mSpeedSampleBytes = 0;
                downloadFile(ts);
            } catch (IOException e) {
                mDownloadNotifier.downloadFailed(mUri, e.getMessage());
            }
        }
        mDownloadNotifier.downloadCompleted(mUri, mDirectory.getName());
        VideoProcessor videoProcessor = new VideoProcessor();
        List<String> inputVideos = null;
        try {
            String[] files = FileShare.readAllText(new File(mDirectory, "list.txt"))
                    .split("\n");
            inputVideos = new ArrayList<>();
            for (String file : files) {
                inputVideos.add(new File(mDirectory, file).getAbsolutePath());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        String outputPath = new File(mDirectory, mDirectory.getName() + ".mp4")
                .getAbsolutePath();
        Logger.d(String.format("transferData: %s", outputPath));
        videoProcessor.compositeVideos(outputPath, inputVideos, new OnVideoCompositeListener() {
            @Override
            public void onComplete() {
                mDownloadNotifier.mergeVideoCompleted(outputPath);
            }

            @Override
            public void onError(int errCode) {
                mDownloadNotifier.mergeVideoFailed(Integer.toString(errCode));
            }
        });
    }

}
