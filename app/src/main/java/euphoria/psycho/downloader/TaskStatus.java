package euphoria.psycho.downloader;

public interface TaskStatus {
    int DOWNLOADING = 1;
    //  emitSynchronizeTask(TaskStatus.CREATE_VIDEO_DIRECTORY);
    int CREATE_VIDEO_DIRECTORY = 2;
    //  emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEOS);
    int DOWNLOAD_VIDEOS = 3;
    //  emitSynchronizeTask(TaskStatus.PARSE_CONTENT_LENGTH);
    int PARSE_CONTENT_LENGTH = 4;
    //  emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEO_FINISHED);
    int DOWNLOAD_VIDEO_FINISHED = 5;
    //  emitSynchronizeTask(TaskStatus.MERGE_VIDEO);
    int RANGE = 6;
    //  emitSynchronizeTask(TaskStatus.MERGE_VIDEO_FINISHED);
    int MERGE_VIDEO_FINISHED = 7;
    int START = 8;
    int PAUSED = 9;
    //  emitSynchronizeTask(TaskStatus.PARSE_VIDEOS);
    int ERROR_CREATE_DIRECTORY = -1;
    //  emitSynchronizeTask(TaskStatus.CREATE_VIDEO_DIRECTORY);
    int ERROR_CREATE_LOG_FILE = -2;
    //  emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEOS);
    int ERROR_UNKONW = -3;
    //  emitSynchronizeTask(TaskStatus.PARSE_CONTENT_LENGTH);
    int ERROR_DOWNLOAD_FILE = -4;
    //  emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEO_FINISHED);
    int ERROR_READ_STREAM = -5;

    int ERROR_DELETE_FILE_FAILED = -6;

    int ERROR_STATUS_CODE = -7;
    int ERROR_WRITE_STREAM = -8;

    int ERROR_FATAL = -9;


}