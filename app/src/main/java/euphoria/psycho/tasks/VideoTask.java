package euphoria.psycho.tasks;

import java.util.Objects;

public class VideoTask {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoTask videoTask = (VideoTask) o;
        return
                Objects.equals(FileName, videoTask.FileName);
    }

    @Override
    public int hashCode() {
        return FileName.hashCode();
    }
}


