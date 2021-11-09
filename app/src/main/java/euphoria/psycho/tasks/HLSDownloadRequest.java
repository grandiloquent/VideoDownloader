package euphoria.psycho.tasks;

import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.List;

import euphoria.psycho.share.StringShare;

public class HLSDownloadRequest implements Runnable {

    public static final int STATUS_ERROR = -2;
    public static final int STATUS_ERROR_IO_INPUT = -3;
    public static final int STATUS_ERROR_IO_OUTPUT = -4;
    public static final int STATUS_MERGE_FAILED = -5;
    public static final int STATUS_FATAL_ERROR = -1;
    public static final int STATUS_FILE_CACHED = 1;
    public static final int STATUS_CONTENT_LENGTH = 2;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_MERGE_VIDEO = 4;
    public static final int STATUS_MERGE_COMPLETED = 5;
    public static final int STATUS_START = 6;

    private final String mBaseUri;
    private final HLSDownloadRequestListener mListener;
    private final HLSDownloadTask mTask;
    private volatile boolean mPaused;
    private int mStatus;

    public HLSDownloadRequest(HLSDownloadTask task, HLSDownloadRequestListener listener) {
        mTask = task;
        mListener = listener;
        mBaseUri = StringShare.substringBeforeLast(mTask.getUri(), "/") + "/";
    }

    public int getStatus() {
        return mStatus;
    }

    public HLSDownloadTask getTask() {
        return mTask;
    }

    public void setPaused(boolean paused) {
        mPaused = paused;
    }

    private void emitSynchronizationEvent(int status) {
        mStatus = status;
        mListener.onProgress(this);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        emitSynchronizationEvent(STATUS_START);
        List<HLSDownloadTaskSegment> segments = mTask.getHLSDownloadTaskSegments();
        for (HLSDownloadTaskSegment ts : segments) {
            if (mPaused) {
                emitSynchronizationEvent(STATUS_PAUSED);
                return;
            }
            mTask.setSequence(ts.Sequence);
            File file = new File(mTask.getDirectory(), StringShare.substringBefore(ts.Uri, "?"));
            if (file.exists()) {
                if (ts.Total > 0 && file.length() == ts.Total) {
                    emitSynchronizationEvent(STATUS_FILE_CACHED);
                    continue;
                }
            }
            try {
                URL url = new URL(mBaseUri + ts.Uri);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                int status = c.getResponseCode();
                if (status < 200 || status >= 400) {
                    emitSynchronizationEvent(STATUS_FATAL_ERROR);
                    return;
                }
                ts.Total = c.getContentLengthLong();
                emitSynchronizationEvent(STATUS_CONTENT_LENGTH);
                if (file.exists()) {
                    if (ts.Total > 0 && file.length() == ts.Total) {
                        emitSynchronizationEvent(STATUS_FILE_CACHED);
                        continue;
                    }
                }
                InputStream in = c.getInputStream();
                FileOutputStream out = new FileOutputStream(file);
                final byte[] buffer = new byte[8192];
                while (true) {
                    if (mPaused) {
                        emitSynchronizationEvent(STATUS_PAUSED);
                        return;
                    }
                    int len;
                    try {
                        len = in.read(buffer);
                    } catch (IOException e) {
                        emitSynchronizationEvent(STATUS_ERROR_IO_INPUT);
                        return;
                    }
                    if (len == -1) {
                        break;
                    }
                    try {
                        out.write(buffer, 0, len);
                        //mVideoTask.DownloadedSize += len;
                        //updateProgress(fileName);
                    } catch (IOException e) {
                        emitSynchronizationEvent(STATUS_ERROR_IO_OUTPUT);
                        return;
                    }
                }

            } catch (Exception exc) {
                emitSynchronizationEvent(STATUS_ERROR);
                return;
            }
        }
        emitSynchronizationEvent(STATUS_MERGE_VIDEO);
        try {
            if (mPaused) {
                emitSynchronizationEvent(STATUS_PAUSED);
                return;
            }
            try (FileChannel fc = new FileOutputStream(mTask.getVideoFile()).getChannel()) {
                for (HLSDownloadTaskSegment ts : segments) {
                    if (mPaused) {
                        emitSynchronizationEvent(STATUS_PAUSED);
                        return;
                    }
                    File file = new File(mTask.getDirectory(), StringShare.substringBefore(ts.Uri, "?"));
                    try (FileChannel fci = new FileInputStream(file).getChannel()) {
                        fci.transferTo(0, fci.size(), fc);
                    }
                }
                fc.force(true);
            }
            emitSynchronizationEvent(STATUS_MERGE_COMPLETED);
        } catch (IOException e) {
            Log.e("B5aOx2", String.format("run, %s", ""));
            emitSynchronizationEvent(STATUS_MERGE_FAILED);
        }

    }
}
