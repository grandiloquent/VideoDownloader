package euphoria.psycho;

public interface TaskStatus {
    //  emitSynchronizeTask(TaskStatus.PARSE_VIDEOS);
    int PARSE_VIDEOS = 1;
    //  emitSynchronizeTask(TaskStatus.CREATE_VIDEO_DIRECTORY);
    int CREATE_VIDEO_DIRECTORY = 2;
    //  emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEOS);
    int DOWNLOAD_VIDEOS = 3;
    //  emitSynchronizeTask(TaskStatus.PARSE_CONTENT_LENGTH);
    int PARSE_CONTENT_LENGTH = 4;
    //  emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEO_FINISHED);
    int DOWNLOAD_VIDEO_FINISHED = 5;
    //  emitSynchronizeTask(TaskStatus.MERGE_VIDEO);
    int MERGE_VIDEO = 6;
    //  emitSynchronizeTask(TaskStatus.MERGE_VIDEO_FINISHED);
    int MERGE_VIDEO_FINISHED = 7;
    //  emitSynchronizeTask(TaskStatus.PARSE_VIDEOS);
    int ERROR_CREATE_DIRECTORY = -1;
    //  emitSynchronizeTask(TaskStatus.CREATE_VIDEO_DIRECTORY);
    int ERROR_CREATE_LOG_FILE = -2;
    //  emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEOS);
    int ERROR_READ_M3U8 = -3;
    //  emitSynchronizeTask(TaskStatus.PARSE_CONTENT_LENGTH);
    int ERROR_DOWNLOAD_FILE = -4;
    //  emitSynchronizeTask(TaskStatus.DOWNLOAD_VIDEO_FINISHED);
    int ERROR_MERGE_VIDEO_FAILED = -5;
}