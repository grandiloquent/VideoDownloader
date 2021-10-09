package euphoria.psycho.downloader;



public interface VideoTaskListener {
    void synchronizeTask(DownloaderTask videoTask);

    void taskProgress(DownloaderTask videoTask);

}

