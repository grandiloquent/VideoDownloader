package euphoria.psycho.tasks;

import android.content.Context;
import android.os.Handler;

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
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.utils.BlobCache;

public class HLSDownloadRequest implements Comparable<HLSDownloadRequest> {

    public static final int BUFFER_SIZE = 8192;
    private final String mBaseUri;
    private final Context mContext;
    private final Handler mHandler;
    private final VideoTaskListener mListener;
    private final List<File> mVideoFiles = new ArrayList<>();
    private final VideoTask mVideoTask;
    private BlobCache mBlobCache;
    private List<String> mVideos;
    private Integer mSequence;
    private RequestQueue mRequestQueue;

    public HLSDownloadRequest(Context context, VideoTask videoTask, VideoTaskListener listener, Handler handler) {
        mVideoTask = videoTask;
        mListener = listener;
        mHandler = handler;
        mContext = context;
        mBaseUri = StringShare.substringBeforeLast(videoTask.Uri, "/")
                + "/";
        mVideoTask.Request = this;
    }

    public boolean createLogFile(File directory) {
        try {
            mBlobCache = new BlobCache(directory.getAbsolutePath() + "/log",
                    100, 1024 * 1024, false,
                    1);
        } catch (IOException e) {
            emitSynchronizeTask(TaskStatus.ERROR_CREATE_LOG_FILE);
            return false;
        }
        return true;
    }

    public File createVideoDirectory() {
        File directory;
        try {
            directory = new File(mVideoTask.Directory);
        } catch (Exception e) {
            emitSynchronizeTask(TaskStatus.ERROR_CREATE_DIRECTORY);
            return null;
        }
        emitSynchronizeTask(TaskStatus.CREATE_VIDEO_DIRECTORY);
        if (!directory.exists()) {
            boolean result = directory.mkdirs();
            if (!result) {
                emitSynchronizeTask(TaskStatus.ERROR_CREATE_DIRECTORY);
                return null;
            }
        }
        return directory;
    }

    public final int getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        }
        return mSequence;
    }

    public final HLSDownloadRequest setSequence(int sequence) {
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
        emitSynchronizeTask(TaskStatus.START);
        if (mVideoTask.Content == null) {
            try {
                mVideoTask.Content = HLSUtils.getString(mVideoTask.Uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String m3u8String = mVideoTask.Content;
        if (m3u8String == null || m3u8String.length() == 0) {
            try {
                m3u8String = HLSUtils.getString(mVideoTask.Uri);
                mVideoTask.Content = m3u8String;
            } catch (IOException e) {
                emitSynchronizeTask(TaskStatus.ERROR_FETCH_M3U8);
                return;
            }

        }
        if (m3u8String == null || m3u8String.length() == 0) {
            emitSynchronizeTask(TaskStatus.ERROR_MISSING_M3U8);
            return;
        }
        File directory = createVideoDirectory();
        // if \(!*?([a-zA-Z0-9]+)\(\w*?\)\)(?= return;)
        if (directory == null) return;
        if (!createLogFile(directory)) {
            return;
        }
        if (!parseVideos(m3u8String)) {
            return;
        }
        if (!downloadVideos()) {
            return;
        }
        if (!mergeVideo()) {
            return;
        }
        deleteCacheFiles(directory);
    }

    private void deleteCacheFiles(File directory) {
        File videoFile = new File(directory, directory.getName() + ".mp4");
        File destinationFile = new File(directory.getParentFile(), directory.getName() + ".mp4");
        boolean result = videoFile.renameTo(destinationFile);
        if (!result) {
            return;
        }
        FileShare.recursivelyDeleteFile(directory, f -> true);
    }

    private boolean downloadFile(String videoUri, File videoFile) throws IOException {
        if (mVideoTask.IsPaused) {
            emitSynchronizeTask(TaskStatus.PAUSED);
            return false;
        }
        if (videoFile.exists()) {
            long size = getBookmark(videoFile.getName());
            if (videoFile.length() == size) {
                return true;
            } else {
                boolean result = videoFile.delete();
                if (!result) {
                    emitSynchronizeTask(TaskStatus.ERROR_DELETE_FILE_FAILED);
                    return false;
                }
            }
        }
        String tsUri = mBaseUri + videoUri;
        HttpURLConnection connection = (HttpURLConnection) new URL(tsUri).openConnection();
        int statusCode = connection.getResponseCode();
        boolean result = false;
        if (statusCode >= 200 && statusCode < 400) {
            long size = Long.parseLong(connection.getHeaderField("Content-Length"));
            setBookmark(videoFile.getName(), size);
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

    private boolean downloadVideos() {
        for (String video : mVideos) {
            final String fileName = FileShare.getFileNameFromUri(video);
            File videoFile = new File(mVideoTask.Directory, fileName);
            mVideoFiles.add(videoFile);
            mVideoTask.DownloadedFiles++;
            emitTaskProgress();
            emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEOS);
            try {
                if (!downloadFile(video, videoFile)) {
                    return false;
                }
                emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEO_FINISHED);
            } catch (IOException e) {
                emitSynchronizeTask(TaskStatus.ERROR_DOWNLOAD_FILE);
                return false;
            }
        }
        return true;
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


    private long getBookmark(String uri) {
        try {
            byte[] data = mBlobCache.lookup(uri.hashCode());
            if (data == null) return 0;
            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));
            dis.readUTF();
            return dis.readLong();
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private boolean mergeVideo() {
        emitSynchronizeTask(TaskStatus.MERGE_VIDEO);
        try {
            String outputPath = HLSUtils.createVideoFile(mVideoTask)
                    .getAbsolutePath();
            try (FileChannel fc = new FileOutputStream(outputPath).getChannel()) {
                for (File video : mVideoFiles) {
                    try (FileChannel fci = new FileInputStream(video).getChannel()) {
                        fci.transferTo(0, fci.size(), fc);
                    }
                }
                fc.force(true);
            }
            FileShare.recursivelyDeleteFile(new File(mVideoTask.Directory));
            emitSynchronizeTask(TaskStatus.MERGE_VIDEO_FINISHED);
            return true;
        } catch (IOException e) {
            emitSynchronizeTask(TaskStatus.ERROR_MERGE_VIDEO_FAILED);
            return false;
        }
    }

    private boolean parseVideos(String m3u8String) {
        String[] segments = m3u8String.split("\n");
        mVideos = new ArrayList<>();
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].startsWith("#EXTINF:")) {
                String uri = segments[i + 1];
                mVideos.add(uri);
                i++;
            }
        }
        if (mVideos.size() > 0) {
            mVideoTask.TotalFiles = mVideos.size();
            emitSynchronizeTask(TaskStatus.PARSE_VIDEOS);
            return true;
        }
        return false;

    }

    private void setBookmark(String uri, long size) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(uri);
            dos.writeLong(size);
            dos.flush();
            mBlobCache.insert(uri.hashCode(), bos.toByteArray());
        } catch (Throwable ignored) {
        }
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
                //mVideoTask.DownloadedSize += len;
                //updateProgress(fileName);
            } catch (IOException e) {
                throw new Error(e);
            }
        }
        return true;

    }

    @Override
    public int compareTo(HLSDownloadRequest other) {
        return this.mSequence - other.mSequence;
    }

    void finish() {
        if (mRequestQueue != null) {
            mRequestQueue.finish(this);
        }
    }

    public VideoTask getVideoTask() {
        return mVideoTask;
    }
}
