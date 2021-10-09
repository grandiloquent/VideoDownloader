package euphoria.psycho.downloader;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import euphoria.psycho.share.FileShare;

public class DownloaderRequest implements Comparable<DownloaderRequest> {

    public static final int BUFFER_SIZE = 8192;
    private final Context mContext;
    private final Handler mHandler;
    private final VideoTaskListener mListener;
    private final DownloaderTask mVideoTask;
    private Integer mSequence;
    private RequestQueue mRequestQueue;
    private long mSpeed;
    private long mSpeedSampleStart;
    private long mSpeedSampleBytes;

    public DownloaderRequest(Context context, DownloaderTask videoTask, VideoTaskListener listener, Handler handler) {
        mVideoTask = videoTask;
        mListener = listener;
        mHandler = handler;
        mContext = context;
        mVideoTask.Request = this;
    }

    public DownloaderTask getDownloaderTask() {
        return mVideoTask;
    }

    public final int getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        }
        return mSequence;
    }

    public final DownloaderRequest setSequence(int sequence) {
        mSequence = sequence;
        return this;
    }

    public void sendEvent(int event) {
        if (mRequestQueue != null) {
            mRequestQueue.sendRequestEvent(this, event);
        }
    }

    public void setRequestQueue(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
    }

    public void start() {
        // TaskStatus\.([A-Z-]+)
        emitSynchronizeTask(TaskStatus.START);
        try {
            downloadFile(new File(mVideoTask.Directory, mVideoTask.FileName));
        } catch (Exception e) {
            emitSynchronizeTask(TaskStatus.ERROR_UNKONW);
        }
    }

    private boolean downloadFile(File videoFile) throws Exception {
        if (mVideoTask.IsPaused) {
            emitSynchronizeTask(TaskStatus.PAUSED);
            return false;
        }
        if (videoFile.exists()) {
            long size = mVideoTask.TotalSize;
            if (videoFile.length() == size) {
                emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEO_FINISHED);
                return true;
            } else {
                mVideoTask.DownloadedSize = videoFile.length();
                emitSynchronizeTask(TaskStatus.RANGE);
            }
        } else {
            mVideoTask.DownloadedSize = 0;
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(mVideoTask.Uri).openConnection();
        if (mVideoTask.DownloadedSize > 0) {
            connection.addRequestProperty("Range", "bytes=" + mVideoTask.DownloadedSize + "-");
        }
        int statusCode = connection.getResponseCode();
        boolean result = false;
        if (statusCode >= 200 && statusCode < 400) {
            mVideoTask.TotalSize = Long.parseLong(connection.getHeaderField("Content-Length"));
            emitSynchronizeTask(TaskStatus.DOWNLOADING);
            InputStream is = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(videoFile);
            result = transferData(is, out);
            FileShare.closeSilently(is);
            FileShare.closeSilently(out);
        } else {
            emitSynchronizeTask(TaskStatus.ERROR_STATUS_CODE);
        }
        return result;
    }

    private void emitSynchronizeTask(int status) {
        mVideoTask.Status = status;
        mHandler.post(() -> {
            mListener.synchronizeTask(mVideoTask);
        });
    }

    private void emitTaskProgress() {
        mHandler.post(() -> {
            mListener.taskProgress(mVideoTask);
        });
    }

    private boolean transferData(InputStream in, OutputStream out) {
        final byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            if (mVideoTask.IsPaused) {
                emitSynchronizeTask(TaskStatus.PAUSED);
                return false;
            }
            int len;
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
                mVideoTask.DownloadedSize += len;
                updateProgress();
            } catch (IOException e) {
                throw new Error(e);
            }
        }
        return true;

    }

    private void updateProgress() {
        final long now = SystemClock.elapsedRealtime();
        final long currentBytes = mVideoTask.DownloadedSize;
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
                mVideoTask.Speed = mSpeed;
                emitTaskProgress();
            }
            mSpeedSampleStart = now;
            mSpeedSampleBytes = currentBytes;
        }

    }

    @Override
    public int compareTo(DownloaderRequest other) {
        return this.mSequence - other.mSequence;
    }

    void finish() {
        if (mRequestQueue != null) {
            mRequestQueue.finish(this);
        }
    }
}
