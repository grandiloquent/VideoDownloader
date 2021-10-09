package euphoria.psycho.downloader;

import java.util.Objects;

public class DownloadTask {
    public long Id;
    public String Uri;
    public String Directory;
    public String FileName;
    public String Content;
    public int Status;
    public int DownloadedFiles;
    public int TotalFiles;
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
        DownloadTask videoTask = (DownloadTask) o;
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
                ", DownloadedFiles=" + DownloadedFiles +
                ", TotalFiles=" + TotalFiles +
                ", DownloadedSize=" + DownloadedSize +
                ", TotalSize=" + TotalSize +
                ", CreateAt=" + CreateAt +
                ", UpdateAt=" + UpdateAt +
                '}';
    }
}


