package euphoria.psycho.tasks;

import android.content.Context;
import android.text.Html;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import euphoria.psycho.share.KeyShare;

import static euphoria.psycho.tasks.HLSDownloadHelpers.createVideoDownloadDirectory;
import static euphoria.psycho.tasks.HLSDownloadHelpers.createVideoFile;

public class HLSDownloadTask {

    private final Context mContext;
    private File mDirectory;
    private File mVideoFile;
    private String mUniqueId;
    private String mFileName;
    private int mId;
    private String mUri;
    private int mStatus;
    private long mCreateAt;
    private long mUpdateAt;
    private int mSequence;
    private List<HLSDownloadTaskSegment> mHLSDownloadTaskSegments = new ArrayList<>();

    public HLSDownloadTask(Context context) {
        mContext = context;
    }

    public HLSDownloadTask build(String uri) throws IOException {
        mUri = uri;
        String m3u8Content;
        try {
            m3u8Content = HLSDownloadHelpers.getString(uri);
        } catch (Exception e) {
            return null;
        }
        if (m3u8Content == null) {
            return null;
        }
        mUniqueId = KeyShare.md5(m3u8Content);
        mDirectory = createVideoDownloadDirectory(mContext, mUniqueId);
        mVideoFile = createVideoFile(mContext, mUniqueId, Html.fromHtml(mFileName,Html.FROM_HTML_MODE_LEGACY).toString());
        HLSDownloadTask task = HLSDownloadManager.getInstance(getContext()).getDatabase().getTask(mUniqueId);
        if (task != null) {
            return task;
        }
        String[] segments = getSplit(m3u8Content);
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

    public HLSDownloadTask setFileName(String fileName) {
        mFileName = fileName;
        return this;
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

    public int getSequence() {
        return mSequence;
    }

    public void setSequence(int sequence) {
        mSequence = sequence;
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

    public String getFileName() {
        return mFileName;
    }

    @NonNull
    private String[] getSplit(String m3u8Content) {
        return m3u8Content.split("\n");
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
