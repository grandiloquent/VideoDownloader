package euphoria.psycho.explorer;

import android.os.Process;

import java.io.File;

public interface DownloadNotifier {
    void downloadStart(String uri);

    void downloadFailed(String uri, String message);

    void downloadProgress(String uri, String fileName, int totalSize);

    void downloadProgress(String uri, long downloadBytes, float speed);

}
