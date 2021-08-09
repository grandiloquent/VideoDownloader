package euphoria.psycho.explorer;

import android.content.Context;
import android.os.Environment;
import android.os.Process;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

public class DownloadThread extends Thread {
    private final String mUri;
    private BlobCache mBlobCache;
    private File mDirectory;
    private final Context mContext;

    public DownloadThread(String uri, Context context) {
        mUri = uri;
        mContext = context;
        try {
            mBlobCache = new BlobCache(mDirectory + "/log",
                    100, 1024 * 1024, false,
                    1);
        } catch (IOException e) {
            Logger.d(String.format("onCreate: %s", e.getMessage()));
        }
        initializeRootDirectory();
        initializeTaskDirectory();
    }


    private void initializeTaskDirectory() {
        String directoryName = Long.toString(KeyShare.crc64Long(StringShare.substringBeforeLast(mUri, "?")));
        mDirectory = new File(mDirectory, directoryName);
        if (!mDirectory.exists()) {
            mDirectory.mkdirs();
        }
    }

    private void initializeRootDirectory() {
        mDirectory = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "视频");
        if (!mDirectory.exists()) {
            mDirectory.mkdirs();
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

    }

    private void setBookmark(String uri, String fileName, String path, int size) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(uri);
            dos.writeUTF(fileName);
            dos.writeUTF(path);
            dos.writeInt(size);
            dos.flush();
            mBlobCache.insert(uri.hashCode(), bos.toByteArray());
        } catch (Throwable t) {
        }
    }

    private void getBookmark(String uri) {
        try {
            byte[] data = mBlobCache.lookup(uri.hashCode());
            if (data == null) return;
            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));
            String uriString = DataInputStream.readUTF(dis);
            String fileName = dis.readUTF();
            String path = dis.readUTF();
            int size = dis.readInt();
            Logger.d(String.format("getBookmark: %s %s %s %d", uriString, fileName, path, size));


        } catch (Throwable t) {
        }
    }

    private List<String> parseM3u8File() {
        try {
            String response = M3u8Share.getString(mUri);
            if (response == null) {
                return null;
            }
            String[] segments = response.split("\n");
            List<String> tsList = new ArrayList<>();
            for (int i = 0; i < segments.length; i++) {
                if (segments[i].startsWith("#EXTINF:")) {
                    String uri = segments[i + 1];
                    tsList.add(uri);
                    i++;
                }
            }
            return tsList;
        } catch (IOException e) {
            Logger.d(String.format("parseM3u8File: %s", e.getMessage()));

        }
        return null;
    }

}
