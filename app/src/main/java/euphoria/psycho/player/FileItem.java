package euphoria.psycho.player;

import android.util.SparseArray;

import java.util.Objects;

public class FileItem {
    private String description;
    private long mLastModified;
    private String mName;
    private String mPath;
    private long mSize;
    private FileType mType;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getLastModified() {
        return mLastModified;
    }

    public void setLastModified(long lastModified) {
        mLastModified = lastModified;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public long getSize() {
        return mSize;
    }

    public void setSize(long size) {
        mSize = size;
    }

    public FileType getType() {
        return mType;
    }

    public void setType(FileType type) {
        mType = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileItem fileItem = (FileItem) o;
        return mLastModified == fileItem.mLastModified &&
                mSize == fileItem.mSize &&
                Objects.equals(mName, fileItem.mName) &&
                Objects.equals(mPath, fileItem.mPath) &&
                mType == fileItem.mType;
    }

    @Override
    public int hashCode() {

        return Objects.hash(mLastModified, mName, mPath, mSize, mType);
    }

    public enum FileType {
        VIDEO,
        AUDIO,
        DIRECTORY,
        TEXT,
        OTHER,
    }

    public enum FileSort {
        NAME(1), LAST_MODIFED(2), SIZE(3);

        private static final SparseArray<FileSort> mMap = new SparseArray<>();

        static {
            for (FileSort fileSort : FileSort.values()) {
                mMap.put(fileSort.getValue(), fileSort);
            }
        }

        private final int mValue;

        private FileSort(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static FileSort get(int value) {
            return mMap.get(value);
        }
    }

}
