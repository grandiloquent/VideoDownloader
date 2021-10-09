package euphoria.psycho.downloader;



public interface VideoTaskListener {
    void synchronizeTask(DownloadTask videoTask);

    void taskProgress(DownloadTask videoTask);

}

