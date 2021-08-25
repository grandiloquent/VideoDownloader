package euphoria.psycho.tasks;

import android.content.Context;
import android.os.Environment;
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
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.utils.BlobCache;
import euphoria.psycho.utils.M3u8Utils;

public class Request implements Comparable<Request> {

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

    public Request(Context context, VideoTask videoTask, VideoTaskListener listener, Handler handler) {
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

    public File createVideoDirectory(String m3u8String) {
        File directory;
        try {
            mVideoTask.FileName = KeyShare.toHex(KeyShare.md5encode(m3u8String));
            directory = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    mVideoTask.FileName);
        } catch (Exception e) {
            emitSynchronizeTask(TaskStatus.ERROR_CREATE_DIRECTORY);
            return null;
        }
        mVideoTask.Directory = directory.getAbsolutePath();
        emitSynchronizeTask(TaskStatus.CREATE_VIDEO_DIRECTORY);
        if (!directory.exists()) {
            Logger.d(String.format("createVideoDirectory: %s", directory));
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

    public final Request setSequence(int sequence) {
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
        emitTaskStart();
        String m3u8String = getM3u8String();
        if (m3u8String == null) return;
        File directory = createVideoDirectory(m3u8String);
        if (directory == null) return;
        if (!createLogFile(directory)) return;
        parseVideos(m3u8String);
        if (!downloadVideos()) return;
        if (!mergeVideo()) return;
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

    private void downloadFile(String videoUri, File videoFile) throws IOException {
        if (videoFile.exists()) {
            long size = getBookmark(videoFile.getName());
            if (videoFile.length() == size) {
                return;
            } else {
                videoFile.delete();
            }
        }
        String tsUri = mBaseUri + videoUri;
        HttpURLConnection connection = (HttpURLConnection) new URL(tsUri).openConnection();
        int statusCode = connection.getResponseCode();
        if (statusCode >= 200 && statusCode < 400) {
            long size = Long.parseLong(connection.getHeaderField("Content-Length"));
            //mVideoTask.TotalSize += size;
            emitSynchronizeTask(TaskStatus.PARSE_CONTENT_LENGTH);
            setBookmark(videoFile.getName(), size);
            InputStream is = connection.getInputStream();
            FileOutputStream out = new FileOutputStream(videoFile);
            transferData(is, out);
            FileShare.closeSilently(is);
            FileShare.closeSilently(out);
        }
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
                downloadFile(video, videoFile);
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

    private void emitTaskStart() {
        mHandler.post(() -> {
            mListener.taskStart(mVideoTask);
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
        } catch (Throwable t) {
            Logger.d(String.format("getBookmark: %s", t.getMessage()));
        }
        return 0;
    }

    private String getM3u8String() {
        String m3u8String = null;
        try {
            m3u8String = M3u8Utils.getString(mVideoTask.Uri);
        } catch (IOException ignored) {
        }
        if (m3u8String == null) {
            emitSynchronizeTask(TaskStatus.ERROR_READ_M3U8);
            return null;
        }
        return m3u8String;
    }

    private boolean mergeVideo() {
        emitSynchronizeTask(TaskStatus.MERGE_VIDEO);
        try {
            String outputPath = new File(mVideoTask.Directory,
                    StringShare.substringAfterLast(mVideoTask.Directory, "/") + ".mp4")
                    .getAbsolutePath();
            OutputStream fileOutputStream = new FileOutputStream(outputPath);
            byte[] b = new byte[4096];
            for (File video : mVideoFiles) {
                FileInputStream fileInputStream = new FileInputStream(video);
                int len;
                while ((len = fileInputStream.read(b)) != -1) {
                    fileOutputStream.write(b, 0, len);
                }
                fileInputStream.close();
                fileOutputStream.flush();
            }
            fileOutputStream.close();
            emitSynchronizeTask(TaskStatus.MERGE_VIDEO_FINISHED);
            return true;
        } catch (IOException e) {
            emitSynchronizeTask(TaskStatus.ERROR_MERGE_VIDEO_FAILED);
            return false;
        }
    }

    private void parseVideos(String m3u8String) {
        String[] segments = m3u8String.split("\n");
        mVideos = new ArrayList<>();
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].startsWith("#EXTINF:")) {
                String uri = segments[i + 1];
                mVideos.add(uri);
                i++;
            }
        }
        mVideoTask.TotalFiles = mVideos.size();
        emitSynchronizeTask(TaskStatus.PARSE_VIDEOS);
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

    }

    @Override
    public int compareTo(Request other) {
        return this.mSequence - other.mSequence;
    }

    void finish() {
        if (mRequestQueue != null) {
            mRequestQueue.finish(this);
        }
    }

}
