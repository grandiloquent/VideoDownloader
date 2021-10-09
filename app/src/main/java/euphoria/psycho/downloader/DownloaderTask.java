package euphoria.psycho.downloader;

import java.util.Objects;

public class DownloaderTask {
    // The task Id
    public long Id;
    // The video uri address
    public String Uri;
    // The video save directory
    public String Directory;
    public String FileName;
    public int Status;
    public long DownloadedSize;
    public long TotalSize;
    public long CreateAt;
    public long UpdateAt;
    public Request Request;
    public boolean IsPaused;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloaderTask videoTask = (DownloaderTask) o;
        return
                Objects.equals(FileName, videoTask.FileName);
    }

    @Override
    public int hashCode() {
        return FileName.hashCode();
    }

    @Override
    public String toString() {
        return "VideoTask{" +
                "Id=" + Id +
                ", Uri='" + Uri + '\'' +
                ", Directory='" + Directory + '\'' +
                ", FileName='" + FileName + '\'' +
                ", Status=" + Status +
                ", DownloadedSize=" + DownloadedSize +
                ", TotalSize=" + TotalSize +
                ", CreateAt=" + CreateAt +
                ", UpdateAt=" + UpdateAt +
                '}';
    }
}


