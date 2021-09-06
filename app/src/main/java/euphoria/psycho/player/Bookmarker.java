package euphoria.psycho.player;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import euphoria.psycho.utils.BlobCache;
import euphoria.psycho.utils.CacheManager;

import static euphoria.psycho.share.KeyShare.crc64Long;

public class Bookmarker {
    private static final String TAG = "Bookmarker";
    private static final String BOOKMARK_CACHE_FILE = "bookmark";
    private static final int BOOKMARK_CACHE_MAX_ENTRIES = 100;
    private static final int BOOKMARK_CACHE_MAX_BYTES = 10 * 1024;
    private static final int BOOKMARK_CACHE_VERSION = 1;
    private static final int HALF_MINUTE = 30 * 1000;
    private static final int TWO_MINUTES = 4 * HALF_MINUTE;
    private final Context mContext;

    public Bookmarker(Context context) {
        mContext = context;
    }

    public void setBookmark(String path, int bookmark/*, int duration*/) {
        try {
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(path);
            dos.writeInt(bookmark);
            //dos.writeInt(duration);
            dos.flush();
            cache.insert(crc64Long(path), bos.toByteArray());
        } catch (Throwable t) {
            Log.w(TAG, "setBookmark failed", t);
        }
    }

    public Integer getBookmark(String path) {
        try {
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);
            byte[] data = cache.lookup(crc64Long(path));
            if (data == null) return 0;
            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));
            String uriString = DataInputStream.readUTF(dis);
            int bookmark = dis.readInt();
            //int duration = dis.readInt();
            if (!uriString.equals(path)) {
                return 0;
            }
//            if ((bookmark < HALF_MINUTE) || (duration < TWO_MINUTES)
//                    || (bookmark > (duration - HALF_MINUTE))) {
//                return null;
//            }
            return bookmark;
        } catch (Throwable t) {
            Log.w(TAG, "getBookmark failed", t);
        }
        return null;
    }
}