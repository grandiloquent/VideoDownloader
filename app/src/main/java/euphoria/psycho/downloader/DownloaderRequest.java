package euphoria.psycho.downloader;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import euphoria.psycho.share.FileShare;

// The class that actually performs the download task
// Pass the status of the execution to other logic
// layers through the event handler
public class DownloaderRequest implements Comparable<DownloaderRequest> {

    public static final int BUFFER_SIZE = 8192;
    private final Handler mHandler;
    private final VideoTaskListener mListener;
    private final DownloaderTask mVideoTask;
    private Integer mSequence;
    private RequestQueue mRequestQueue;
    private long mSpeed;
    private long mSpeedSampleStart;
    private long mSpeedSampleBytes;

    public DownloaderRequest(DownloaderTask videoTask, VideoTaskListener listener, Handler handler) {
        mVideoTask = videoTask;
        mListener = listener;
        mHandler = handler;
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

    // Send the status of task to the task queue
    // to provide information for effective task management
    public void sendEvent(int event) {
        if (mRequestQueue != null) {
            mRequestQueue.sendRequestEvent(this, event);
        }
    }

    public void setRequestQueue(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
    }

    public void start() {
        emitSynchronizeTask(TaskStatus.START);
        try {
            downloadFile(new File(mVideoTask.Directory, mVideoTask.FileName));
        } catch (Exception e) {
            Log.e("B5aOx2", String.format("start, %s", e.getMessage()));
            emitSynchronizeTask(TaskStatus.ERROR_UNKONW);
        }
    }

    private void downloadFile(File videoFile) throws Exception {
        if (mVideoTask.IsPaused) {
            emitSynchronizeTask(TaskStatus.PAUSED);
            return;
        }
        if (videoFile.exists()) {
            long size = mVideoTask.TotalSize;
            if (videoFile.length() == size) {
                emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEO_FINISHED);
                return;
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
        if (statusCode >= 200 && statusCode < 400) {
            if (mVideoTask.TotalSize == 0) {
                mVideoTask.TotalSize = Long.parseLong(connection.getHeaderField("Content-Length"));
            }
            InputStream is = connection.getInputStream();
            RandomAccessFile out = new RandomAccessFile(videoFile, "rw");
            if (mVideoTask.DownloadedSize > 0) {
                out.seek(mVideoTask.DownloadedSize);
            }
            transferData(is, out);
            FileShare.closeSilently(is);
            FileShare.closeSilently(out);
        } else {
            emitSynchronizeTask(TaskStatus.ERROR_FATAL);
        }
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

    private void transferData(InputStream in, RandomAccessFile out) {
        emitSynchronizeTask(TaskStatus.DOWNLOADING);
        final byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            if (mVideoTask.IsPaused) {
                emitSynchronizeTask(TaskStatus.PAUSED);
                return;
            }
            int len;
            try {
                len = in.read(buffer);
            } catch (IOException e) {
                emitSynchronizeTask(TaskStatus.ERROR_READ_STREAM);
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
                emitSynchronizeTask(TaskStatus.ERROR_WRITE_STREAM);
                throw new Error(e);
            }
        }

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
