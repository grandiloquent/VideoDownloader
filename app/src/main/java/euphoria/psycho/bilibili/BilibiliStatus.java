package euphoria.psycho.bilibili;

public class BilibiliStatus {
    // When the server returns a http code other than 200 or 206,
    // we should consider the problem to be fatal
    // and should not re-execute the download task
    public static final int ERROR_STATUS_CODE = -1;
    public static final int ERROR_STATUS_HTTP_DATA = -2;
    public static final int ERROR_STATUS_FILE = -3;
    // FILE
    // STATUS_HTTP_DATA_ERROR
}
