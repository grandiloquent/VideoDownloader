package euphoria.psycho.tasks;

import android.os.Process;

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

    public static final int STATUS_CONTENT_LENGTH = 2;
    public static final int STATUS_ERROR = -2;
    public static final int STATUS_ERROR_IO_INPUT = -3;
    public static final int STATUS_ERROR_IO_OUTPUT = -4;
    public static final int STATUS_MERGE_FAILED = -5;
    public static final int STATUS_FATAL_ERROR = 1;
    public static final int STATUS_FILE_CACHED = 1;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_MERGE_VIDEO = 4;
    public static final int STATUS_MERGE_COMPLETED = 5;

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

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        List<HLSDownloadTaskSegment> segments = mTask.getHLSDownloadTaskSegments();
        for (HLSDownloadTaskSegment ts : segments) {
            mTask.setSequence(ts.Sequence);
            File file = new File(mTask.getDirectory(), StringShare.substringBefore(ts.Uri, "?"));
            if (file.exists()) {
                if (ts.Total > 0 && file.length() == ts.Total) {
                    mStatus = STATUS_FILE_CACHED;
                    mListener.onProgress(this);
                    continue;
                }
            }
            try {
                URL url = new URL(mBaseUri + ts.Uri);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                int status = c.getResponseCode();
                if (status < 200 || status >= 400) {
                    mStatus = STATUS_FATAL_ERROR;
                    mListener.onProgress(this);
                    return;
                }
                ts.Total = c.getContentLengthLong();
                mStatus = STATUS_CONTENT_LENGTH;
                mListener.onProgress(this);
                if (file.exists()) {
                    if (ts.Total > 0 && file.length() == ts.Total) {
                        mStatus = STATUS_FILE_CACHED;
                        mListener.onProgress(this);
                        continue;
                    }
                }
                InputStream in = c.getInputStream();
                FileOutputStream out = new FileOutputStream(file);
                final byte[] buffer = new byte[8192];
                while (true) {
                    if (mPaused) {
                        mStatus = STATUS_PAUSED;
                        mListener.onProgress(this);
                        return;
                    }
                    int len;
                    try {
                        len = in.read(buffer);
                    } catch (IOException e) {
                        mStatus = STATUS_ERROR_IO_INPUT;
                        mListener.onProgress(this);
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
                        mStatus = STATUS_ERROR_IO_OUTPUT;
                        mListener.onProgress(this);
                        return;
                    }
                }

            } catch (Exception exc) {
                mStatus = STATUS_ERROR;
                mListener.onProgress(this);
            }
        }
        mStatus = STATUS_MERGE_VIDEO;
        mListener.onProgress(this);
        try {
            try (FileChannel fc = new FileOutputStream(mTask.getVideoFile()).getChannel()) {
                for (HLSDownloadTaskSegment ts : segments) {
                    File file = new File(mTask.getDirectory(), StringShare.substringBefore(ts.Uri, "?"));
                    try (FileChannel fci = new FileInputStream(file).getChannel()) {
                        fci.transferTo(0, fci.size(), fc);
                    }
                }
                fc.force(true);
            }
            mStatus = STATUS_MERGE_COMPLETED;
            mListener.onProgress(this);
        } catch (IOException e) {
            mStatus = STATUS_MERGE_FAILED;
            mListener.onProgress(this);
        }

    }
}
