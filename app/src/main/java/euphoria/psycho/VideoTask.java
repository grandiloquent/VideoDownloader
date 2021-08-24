package euphoria.psycho;

public class VideoTask {
    public String Uri;
    public int Status;
    public String Directory;
    public int TotalFiles;
    public long Id;
    public long CreateAt;
    public long UpdateAt;
    public int DownloadedFiles;
    public long TotalSize;
    public long DownloadedSize;

    public interface TaskStatus {
        int ERROR_CREATE_DIRECTORY = -2;
        int ERROR_CREATE_LOG_FILE = -3;
        int ERROR_READ_M3U8 = -1;
    }
}


