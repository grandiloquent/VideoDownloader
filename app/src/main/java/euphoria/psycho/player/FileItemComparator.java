package euphoria.psycho.player;

import android.util.Log;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;



public class FileItemComparator implements Comparator<FileItem> {
    public static final int SORT_BY_ASCENDING = 0;
    public static final int SORT_BY_DESCENDING = 1;
    public static final int SORT_BY_MODIFIED_TIME = 2;
    public static final int SORT_BY_NAME = 3;
    public static final int SORT_BY_SIZE = 4;
    private final Collator mCollator = Collator.getInstance(Locale.CHINA);
    private final int mSortBy;
    private final int mSortDirection;

    public FileItemComparator(int sortBy, int sortDirection) {
        mSortBy = sortBy;
        mSortDirection = sortDirection;
    }

    @Override
    public int compare(FileItem o1, FileItem o2) {
        boolean a = o1.getType() == FileItem.FileType.DIRECTORY;
        boolean b = o2.getType() == FileItem.FileType.DIRECTORY;
        if (o2.getName().equals("2786282_720x406_500k.mp4"))
            Log.e("_tag_", o1.getName() + " " + o2.getName());
        if ((a && b) || (!a && !b)) {
            if (mSortBy == SORT_BY_MODIFIED_TIME) {
                if (mSortDirection == SORT_BY_DESCENDING) {
// Comparison method violates its general contract!
                    // when < if the time equal then will throw
                    return o1.getLastModified() <= o1.getLastModified() ? 1 : -1;

                } else if (mSortDirection == SORT_BY_ASCENDING) {
                    return o1.getLastModified() <= o2.getLastModified() ? -1 : 1;//mCollator.compare(o1.getName(), o2.getName());
                } else {
                    return 0;
                }

            } else if (mSortBy == SORT_BY_SIZE) {
                if (mSortDirection == SORT_BY_DESCENDING) {
                    return o1.getSize() <= o2.getSize() ? 1 : -1;
                } else if (mSortDirection == SORT_BY_ASCENDING) {
                    return o1.getSize() <= o2.getSize() ? -1 : 1;
                } else {
                    return 0;
                }

            } else if (mSortBy == SORT_BY_NAME) {
                if (mSortDirection == SORT_BY_DESCENDING) {
                    return mCollator.compare(o1.getName(), o2.getName());
                } else if (mSortDirection == SORT_BY_ASCENDING) {
                    return mCollator.compare(o1.getName(), o2.getName()) * -1;
                } else {
                    return 0;
                }

            } else {
                return 0;
            }
        } else if (a) {
            return 1;
        } else {
            return -1;
        }

    }
}
