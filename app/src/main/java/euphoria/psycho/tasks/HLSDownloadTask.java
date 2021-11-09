package euphoria.psycho.tasks;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.share.KeyShare;

public class HLSDownloadTask {

    private final Context mContext;
    private File mDirectory;
    private File mVideoFile;
    private String mUniqueId;
    private int mId;
    private String mUri;
    private int mStatus;
    private long mCreateAt;
    private long mUpdateAt;
    private List<HLSDownloadTaskSegment> mHLSDownloadTaskSegments = new ArrayList<>();

    public HLSDownloadTask(Context context) {
        mContext = context;
    }

    public HLSDownloadTask build(String uri) throws IOException {
        mUri = uri;
        String m3u8Content = HLSDownloadHelpers.getString(uri);
        if (m3u8Content == null) {
            return null;
        }
        mUniqueId = KeyShare.md5(m3u8Content);
        HLSDownloadTask task = HLSDownloadManager.getInstance(getContext()).getDatabase().getTask(mUniqueId);
        if (task != null) {
            return task;
        }
        mDirectory = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), mUniqueId);
        if (!mDirectory.exists())
            mDirectory.mkdirs();
        mVideoFile = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), mUniqueId + ".mp4");
        String[] segments = m3u8Content.split("\n");
        int sequence = 0;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].startsWith("#EXTINF:")) {
                String segment = segments[i + 1];
                HLSDownloadTaskSegment taskSegment = new HLSDownloadTaskSegment();
                taskSegment.Uri = segment;
                taskSegment.UniqueId = mUniqueId;
                taskSegment.Sequence = sequence++;
                mHLSDownloadTaskSegments.add(taskSegment);
                i++;
            }
        }
        HLSDownloadManager.getInstance(getContext())
                .getDatabase().insertTask(this);
        return this;
    }

    public Context getContext() {
        return mContext;
    }

    public long getCreateAt() {
        return mCreateAt;
    }

    public void setCreateAt(long createAt) {
        mCreateAt = createAt;
    }

    public File getDirectory() {
        return mDirectory;
    }

    public void setDirectory(File directory) {
        mDirectory = directory;
    }

    public List<HLSDownloadTaskSegment> getHLSDownloadTaskSegments() {
        return mHLSDownloadTaskSegments;
    }

    public void setHLSDownloadTaskSegments(List<HLSDownloadTaskSegment> HLSDownloadTaskSegments) {
        mHLSDownloadTaskSegments = HLSDownloadTaskSegments;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        mStatus = status;
    }

    public String getUniqueId() {
        return mUniqueId;
    }

    public void setUniqueId(String uniqueId) {
        mUniqueId = uniqueId;
    }

    public long getUpdateAt() {
        return mUpdateAt;
    }

    public void setUpdateAt(long updateAt) {
        mUpdateAt = updateAt;
    }

    public String getUri() {
        return mUri;
    }

    public void setUri(String uri) {
        mUri = uri;
    }

    public File getVideoFile() {
        return mVideoFile;
    }

    public void setVideoFile(File videoFile) {
        mVideoFile = videoFile;
    }

    @Override
    public String toString() {
        return "HLSDownloadTask{" +
                "mDirectory=" + mDirectory +
                ", mVideoFile=" + mVideoFile +
                ", mUniqueId='" + mUniqueId + '\'' +
                '}';
    }
}
