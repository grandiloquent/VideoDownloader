package euphoria.psycho.explorer;

import android.os.Process;

import java.io.File;

import euphoria.psycho.explorer.DownloadTaskDatabase.DownloadTaskInfo;

public interface DownloadNotifier {
    void downloadStart(DownloadTaskInfo downloadTaskInfo);

    void downloadFailed(String uri, String message);

    void downloadProgress(String uri, String fileName);

    void downloadProgress(String uri, int currentSize, int total, long downloadBytes, long speed);

    void downloadCompleted(String uri, String directory);

    void mergeVideoCompleted(String outPath);

    void mergeVideoFailed(String message);
}
