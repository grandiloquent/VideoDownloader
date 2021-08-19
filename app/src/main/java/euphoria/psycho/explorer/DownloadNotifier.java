package euphoria.psycho.explorer;

import euphoria.psycho.explorer.DownloadTaskDatabase.DownloadTaskInfo;

public interface DownloadNotifier {
    void downloadStart(DownloadTaskInfo downloadTaskInfo);

    void downloadFailed(String uri, String message);

    void downloadProgress(DownloadTaskInfo taskInfo, int currentSize, int total, long downloadBytes, long speed, String fileName);

    void downloadCompleted(DownloadTaskInfo taskInfo);

    void mergeVideoCompleted(DownloadTaskInfo downloadTaskInfo,String outPath);

    void mergeVideoFailed(DownloadTaskInfo taskInfo, String message);
}
