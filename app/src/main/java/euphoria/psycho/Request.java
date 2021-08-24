package euphoria.psycho;


import android.content.Context;
import android.os.Environment;
import android.os.Handler;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.VideoTask.TaskStatus;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.utils.BlobCache;
import euphoria.psycho.utils.M3u8Utils;

import static euphoria.psycho.explorer.DownloadTaskDatabase.STATUS_ERROR_DOWNLOAD_FILE;

public class Request {

    private final Handler mHandler;
    private final VideoTaskListener mListener;
    private final VideoTask mVideoTask;
    private Context mContext;
    private BlobCache mBlobCache;
    private List<String> mVideos;

    public Request(VideoTask videoTask, VideoTaskListener listener, Handler handler) {
        mVideoTask = videoTask;
        mListener = listener;
        mHandler = handler;
    }

    public boolean createLogFile(File directory) {
        try {
            mBlobCache = new BlobCache(directory.getAbsolutePath() + "/log",
                    100, 1024 * 1024, false,
                    1);
        } catch (IOException e) {
            mVideoTask.Status = TaskStatus.ERROR_CREATE_LOG_FILE;
            emitSynchronizeTask();
            return false;
        }
        return true;
    }

    public File createVideoDirectory(String m3u8String) {
        File directory;
        try {
            directory = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    KeyShare.toHex(KeyShare.md5encode(m3u8String)));
        } catch (Exception e) {
            mVideoTask.Status = TaskStatus.ERROR_CREATE_DIRECTORY;
            emitSynchronizeTask();
            return null;
        }
        mVideoTask.Directory = directory.getAbsolutePath();
        emitSynchronizeTask();
        if (!directory.exists()) {
            boolean result = directory.mkdirs();
            if (!result) {
                mVideoTask.Status = TaskStatus.ERROR_CREATE_DIRECTORY;
                emitSynchronizeTask();
                return null;
            }
        }
        return directory;
    }

    public void sendEvent(int requestNetworkDispatchStarted) {
    }

    public void setRequestQueue(RequestQueue requestQueue) {
    }

    public void start() {
        emitTaskStarted();
        String m3u8String = getM3u8File();
        if (m3u8String == null) return;
        File directory = createVideoDirectory(m3u8String);
        if (directory == null) return;
        if (!createLogFile(directory)) return;
        parseVideos(m3u8String);
        for (String video : mVideos) {
            final String fileName = FileShare.getFileNameFromUri(video);
            File videoFile = new File(mVideoTask.Directory, fileName);
//            try {
//            } catch (IOException e) {
//                return;
//            }

        }
    }

    //
    private void emitSynchronizeTask() {
        mHandler.post(() -> {
            mListener.synchronizeTask(mVideoTask);
        });
    }

    private void emitTaskStarted() {
        mHandler.post(() -> {
            mListener.taskStarted(mVideoTask);
        });
    }

    private String getM3u8File() {
        String m3u8String = null;
        try {
            m3u8String = M3u8Utils.getString(mVideoTask.Uri);
        } catch (IOException ignored) {
        }
        if (m3u8String == null) {
            mVideoTask.Status = TaskStatus.ERROR_READ_M3U8;
            emitSynchronizeTask();
            return null;
        }
        return m3u8String;
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
        emitSynchronizeTask();
    }

}
