package euphoria.psycho.share;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.SystemClock;
import android.provider.MediaStore.Downloads;
import android.provider.SyncStateContract.Constants;
import android.provider.SyncStateContract.Helpers;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import androidx.core.math.MathUtils;
import androidx.core.util.Preconditions;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

public class DownloadThread extends Thread {

    public static final Uri ALL_DOWNLOADS_CONTENT_URI =
            Uri.parse("content://downloads/all_downloads");
    public static final String COLUMN_APP_DATA = "entity";
    public static final String COLUMN_CONTROL = "control";
    public static final String COLUMN_CURRENT_BYTES = "current_bytes";
    public static final String COLUMN_DELETED = "deleted";
    public static final String COLUMN_FAILED_CONNECTIONS = "numfailed";
    public static final String COLUMN_LAST_MODIFICATION = "lastmod";
    public static final String COLUMN_MIME_TYPE = "mimetype";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_TOTAL_BYTES = "total_bytes";
    public static final String COLUMN_URI = "uri";
    public static final int CONTROL_PAUSED = 1;
    //public static final String DEFAULT_USER_AGENT;
    public static final int DESTINATION_CACHE_PARTITION = 1;
    public static final int DESTINATION_CACHE_PARTITION_NOROAMING = 3;
    public static final int DESTINATION_CACHE_PARTITION_PURGEABLE = 2;
    public static final int DESTINATION_EXTERNAL = 0;
    public static final int DESTINATION_FILE_URI = 4;
    public static final String ETAG = "etag";
    public static final int MAX_REDIRECTS = 5; // can't be more than 7.
    public static final int MAX_RETRIES = 5;
    public static final int MAX_RETRY_AFTER = 24 * 60 * 60; // 24h
    public static final int MIN_ARTIFICIAL_ERROR_STATUS = 488;
    public static final int MIN_RETRY_AFTER = 30; // 30s
    public static final String RETRY_AFTER_X_REDIRECT_COUNT = "method";
    public static final int STATUS_BAD_REQUEST = 400;
    public static final int STATUS_BLOCKED = 498;
    public static final int STATUS_CANCELED = 490;
    public static final int STATUS_CANNOT_RESUME = 489;
    public static final int STATUS_DEVICE_NOT_FOUND_ERROR = 199;
    public static final int STATUS_FILE_ALREADY_EXISTS_ERROR = 488;
    public static final int STATUS_FILE_ERROR = 492;
    public static final int STATUS_HTTP_DATA_ERROR = 495;
    public static final int STATUS_HTTP_EXCEPTION = 496;
    public static final int STATUS_INSUFFICIENT_SPACE_ERROR = 198;
    public static final int STATUS_LENGTH_REQUIRED = 411;
    public static final int STATUS_NOT_ACCEPTABLE = 406;
    public static final int STATUS_PAUSED_BY_APP = 193;
    public static final int STATUS_PENDING = 190;
    public static final int STATUS_PRECONDITION_FAILED = 412;
    public static final int STATUS_QUEUED_FOR_WIFI = 196;
    public static final int STATUS_RUNNING = 192;
    public static final int STATUS_SUCCESS = 200;
    public static final int STATUS_TOO_MANY_REDIRECTS = 497;
    public static final int STATUS_UNHANDLED_HTTP_CODE = 494;
    public static final int STATUS_UNHANDLED_REDIRECT = 493;
    public static final int STATUS_UNKNOWN_ERROR = 491;
    public static final int STATUS_WAITING_FOR_NETWORK = 195;
    public static final int STATUS_WAITING_TO_RETRY = 194;
    public static final String TAG = "DownloadManager";
    public static final String _DATA = "_data";
    private static final int DEFAULT_TIMEOUT = (int) (20 * SECOND_IN_MILLIS);
    private static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private static final int HTTP_TEMP_REDIRECT = 307;
    private static final Pattern PATTERN_ANDROID_DIRS =
            Pattern.compile("(?i)^/storage/[^/]+(?:/[0-9]+)?/Android/(?:data|obb|media)/.+");

//    static {
//        final StringBuilder builder = new StringBuilder();
//        final boolean validRelease = !TextUtils.isEmpty(Build.VERSION.RELEASE_OR_CODENAME);
//        final boolean validId = !TextUtils.isEmpty(Build.ID);
//        final boolean includeModel = "REL".equals(Build.VERSION.CODENAME)
//                && !TextUtils.isEmpty(Build.MODEL);
//        builder.append("AndroidDownloadManager");
//        if (validRelease) {
//            builder.append("/").append(Build.VERSION.RELEASE_OR_CODENAME);
//        }
//        builder.append(" (Linux; U; Android");
//        if (validRelease) {
//            builder.append(" ").append(Build.VERSION.RELEASE_OR_CODENAME);
//        }
//        if (includeModel || validId) {
//            builder.append(";");
//            if (includeModel) {
//                builder.append(" ").append(Build.MODEL);
//            }
//            if (validId) {
//                builder.append(" Build/").append(Build.ID);
//            }
//        }
//        builder.append(")");
//        DEFAULT_USER_AGENT = builder.toString();
//    }

    private final Context mContext;
    private final long mId;
    private final DownloadInfo mInfo;
    private final DownloadInfoDelta mInfoDelta;
    private final DownloadNotifier mNotifier;
    private boolean mIgnoreBlocked;
    private boolean mMadeProgress = false;

    public DownloadThread(DownloadInfo info) {
        mInfo = info;
        mId = mInfo.mId;
        mInfoDelta = null;
        mNotifier = null;
        mContext = null;
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    public static long constrain(long amount, long low, long high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    public static File getRunningDestinationDirectory(Context context, int destination)
            throws IOException {
        return getDestinationDirectory(context, destination, true);
    }

    public static File getSuccessDestinationDirectory(Context context, int destination)
            throws IOException {
        return getDestinationDirectory(context, destination, false);
    }

    public static boolean isFileInExternalAndroidDirs(String filePath) {
        return PATTERN_ANDROID_DIRS.matcher(filePath).matches();
    }

    public static boolean isStatusError(int status) {
        return (status >= 400 && status < 600);
    }

    public static boolean isStatusRetryable(int status) {
        switch (status) {
            case STATUS_HTTP_DATA_ERROR:
            case HTTP_UNAVAILABLE:
            case HTTP_INTERNAL_ERROR:
            case STATUS_FILE_ERROR:
                return true;
            default:
                return false;
        }
    }

    public static boolean isStatusSuccess(int status) {
        return (status >= 200 && status < 300);
    }

    public static String statusToString(int status) {
        switch (status) {
            case STATUS_PENDING:
                return "PENDING";
            case STATUS_RUNNING:
                return "RUNNING";
            case STATUS_PAUSED_BY_APP:
                return "PAUSED_BY_APP";
            case STATUS_WAITING_TO_RETRY:
                return "WAITING_TO_RETRY";
            case STATUS_WAITING_FOR_NETWORK:
                return "WAITING_FOR_NETWORK";
            case STATUS_QUEUED_FOR_WIFI:
                return "QUEUED_FOR_WIFI";
            case STATUS_INSUFFICIENT_SPACE_ERROR:
                return "INSUFFICIENT_SPACE_ERROR";
            case STATUS_DEVICE_NOT_FOUND_ERROR:
                return "DEVICE_NOT_FOUND_ERROR";
            case STATUS_SUCCESS:
                return "SUCCESS";
            case STATUS_BAD_REQUEST:
                return "BAD_REQUEST";
            case STATUS_NOT_ACCEPTABLE:
                return "NOT_ACCEPTABLE";
            case STATUS_LENGTH_REQUIRED:
                return "LENGTH_REQUIRED";
            case STATUS_PRECONDITION_FAILED:
                return "PRECONDITION_FAILED";
            case STATUS_FILE_ALREADY_EXISTS_ERROR:
                return "FILE_ALREADY_EXISTS_ERROR";
            case STATUS_CANNOT_RESUME:
                return "CANNOT_RESUME";
            case STATUS_CANCELED:
                return "CANCELED";
            case STATUS_UNKNOWN_ERROR:
                return "UNKNOWN_ERROR";
            case STATUS_FILE_ERROR:
                return "FILE_ERROR";
            case STATUS_UNHANDLED_REDIRECT:
                return "UNHANDLED_REDIRECT";
            case STATUS_UNHANDLED_HTTP_CODE:
                return "UNHANDLED_HTTP_CODE";
            case STATUS_HTTP_DATA_ERROR:
                return "HTTP_DATA_ERROR";
            case STATUS_HTTP_EXCEPTION:
                return "HTTP_EXCEPTION";
            case STATUS_TOO_MANY_REDIRECTS:
                return "TOO_MANY_REDIRECTS";
            case STATUS_BLOCKED:
                return "BLOCKED";
            default:
                return Integer.toString(status);
        }
    }

    private static File getDestinationDirectory(Context context, int destination, boolean running)
            throws IOException {
        switch (destination) {
            case DESTINATION_CACHE_PARTITION:
            case DESTINATION_CACHE_PARTITION_PURGEABLE:
            case DESTINATION_CACHE_PARTITION_NOROAMING:
                if (running) {
                    return context.getFilesDir();
                } else {
                    return context.getCacheDir();
                }
            case DESTINATION_EXTERNAL:
                final File target = new File(
                        Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
                if (!target.isDirectory() && target.mkdirs()) {
                    throw new IOException("unable to create external downloads directory");
                }
                return target;
            default:
                throw new IllegalStateException("unexpected destination: " + destination);
        }
    }

    private void addRequestHeaders(HttpURLConnection conn, boolean resuming) {
        for (Pair<String, String> header : mInfo.getHeaders()) {
            conn.addRequestProperty(header.first, header.second);
        }
        // Only splice in user agent when not already defined
        if (conn.getRequestProperty("User-Agent") == null) {
            conn.addRequestProperty("User-Agent", mInfo.getUserAgent());
        }
        // Defeat transparent gzip compression, since it doesn't allow us to
        // easily resume partial downloads.
        conn.setRequestProperty("Accept-Encoding", "identity");
        // Defeat connection reuse, since otherwise servers may continue
        // streaming large downloads after cancelled.
        conn.setRequestProperty("Connection", "close");
        if (resuming) {
            if (mInfoDelta.mETag != null) {
                conn.addRequestProperty("If-Match", mInfoDelta.mETag);
            }
            conn.addRequestProperty("Range", "bytes=" + mInfoDelta.mCurrentBytes + "-");
        }
    }

    private void checkConnectivity() {
    }

    private void executeDownload() throws StopRequestException {
        final boolean resuming = mInfoDelta.mCurrentBytes != 0;
        URL url;
        try {
            // TODO: migrate URL sanity checking into client side of API
            url = new URL(mInfoDelta.mUri);
        } catch (MalformedURLException e) {
            throw new StopRequestException(STATUS_BAD_REQUEST, e);
        }
        int redirectionCount = 0;
        while (redirectionCount++ < MAX_REDIRECTS) {
            // Open connection and follow any redirects until we have a useful
            // response with body.
            HttpURLConnection conn = null;
            try {
                // Check that the caller is allowed to make network connections. If so, make one on
                // their behalf to open the url.
                checkConnectivity();
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);
                addRequestHeaders(conn, resuming);
                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                    case HTTP_OK:
                        if (resuming) {
                            throw new StopRequestException(
                                    STATUS_CANNOT_RESUME, "Expected partial, but received OK");
                        }
                        parseOkHeaders(conn);
                        transferData(conn);
                        return;
                    case HTTP_PARTIAL:
                        if (!resuming) {
                            throw new StopRequestException(
                                    STATUS_CANNOT_RESUME, "Expected OK, but received partial");
                        }
                        transferData(conn);
                        return;
                    case HTTP_MOVED_PERM:
                    case HTTP_MOVED_TEMP:
                    case HTTP_SEE_OTHER:
                    case HTTP_TEMP_REDIRECT:
                        final String location = conn.getHeaderField("Location");
                        url = new URL(url, location);
                        if (responseCode == HTTP_MOVED_PERM) {
                            // Push updated URL back to database
                            mInfoDelta.mUri = url.toString();
                        }
                        continue;
                    case HTTP_PRECON_FAILED:
                        throw new StopRequestException(
                                STATUS_CANNOT_RESUME, "Precondition failed");
                    case HTTP_REQUESTED_RANGE_NOT_SATISFIABLE:
                        throw new StopRequestException(
                                STATUS_CANNOT_RESUME, "Requested range not satisfiable");
                    case HTTP_UNAVAILABLE:
                        parseUnavailableHeaders(conn);
                        throw new StopRequestException(
                                HTTP_UNAVAILABLE, conn.getResponseMessage());
                    case HTTP_INTERNAL_ERROR:
                        throw new StopRequestException(
                                HTTP_INTERNAL_ERROR, conn.getResponseMessage());
                    default:
                        StopRequestException.throwUnhandledHttpError(
                                responseCode, conn.getResponseMessage());
                }

            } catch (IOException e) {
                if (e instanceof ProtocolException
                        && e.getMessage().startsWith("Unexpected status line")) {
                    throw new StopRequestException(STATUS_UNHANDLED_HTTP_CODE, e);
                } else {
                    // Trouble with low-level sockets
                    throw new StopRequestException(STATUS_HTTP_DATA_ERROR, e);
                }

            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        throw new StopRequestException(STATUS_TOO_MANY_REDIRECTS, "Too many redirects");
    }

    private void transferData(HttpURLConnection conn) {
    }

    private void parseOkHeaders(HttpURLConnection conn) throws StopRequestException {
        if (mInfoDelta.mFileName == null) {
            final String contentDisposition = conn.getHeaderField("Content-Disposition");
            final String contentLocation = conn.getHeaderField("Content-Location");
            mInfoDelta.mFileName = "";

        }
        if (mInfoDelta.mMimeType == null) {
            mInfoDelta.mMimeType = Intent.normalizeMimeType(conn.getContentType());
        }
        final String transferEncoding = conn.getHeaderField("Transfer-Encoding");
        if (transferEncoding == null) {
            mInfoDelta.mTotalBytes = getHeaderFieldLong(conn, "Content-Length", -1);
        } else {
            mInfoDelta.mTotalBytes = -1;
        }
        mInfoDelta.mETag = conn.getHeaderField("ETag");
        mInfoDelta.writeToDatabaseOrThrow();
        // Check connectivity again now that we know the total size
        checkConnectivity();
    }

    private static long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
        try {
            return Long.parseLong(conn.getHeaderField(field));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void finalizeDestination() {
        if (isStatusError(mInfoDelta.mStatus)) {
            // When error, free up any disk space
            try {
                final ParcelFileDescriptor target = mContext.getContentResolver()
                        .openFileDescriptor(mInfo.getAllDownloadsUri(), "rw");
                try {
                    Os.ftruncate(target.getFileDescriptor(), 0);
                } catch (ErrnoException ignored) {
                } finally {
                    closeQuietly(target);
                }
            } catch (FileNotFoundException ignored) {
            }
            // Delete if local file
            if (mInfoDelta.mFileName != null) {
                new File(mInfoDelta.mFileName).delete();
                mInfoDelta.mFileName = null;
            }

        } else if (isStatusSuccess(mInfoDelta.mStatus)) {
            // When success, open access if local file
            if (mInfoDelta.mFileName != null) {
                if (isFileInExternalAndroidDirs(mInfoDelta.mFileName)) {
                    // Files that are downloaded in Android/ may need fixing up
                    // of permissions on devices without sdcardfs; do so here,
                    // before we give the file back to the client
                    File file = new File(mInfoDelta.mFileName);
                    //mStorage.fixupAppDir(file.getParentFile());
                }
                if (mInfo.mDestination != DESTINATION_FILE_URI) {
                    try {
                        // Move into final resting place, if needed
                        final File before = new File(mInfoDelta.mFileName);
                        final File beforeDir = getRunningDestinationDirectory(
                                mContext, mInfo.mDestination);
                        final File afterDir = getSuccessDestinationDirectory(
                                mContext, mInfo.mDestination);
                        if (!beforeDir.equals(afterDir)
                                && before.getParentFile().equals(beforeDir)) {
                            final File after = new File(afterDir, before.getName());
                            if (before.renameTo(after)) {
                                mInfoDelta.mFileName = after.getAbsolutePath();
                            }
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private void logDebug(String msg) {
        Log.d(TAG, "[" + mId + "] " + msg);
    }

    private void logError(String msg, Throwable t) {
        Log.e(TAG, "[" + mId + "] " + msg, t);
    }

    private void logWarning(String msg) {
        Log.w(TAG, "[" + mId + "] " + msg);
    }

    private void parseUnavailableHeaders(HttpURLConnection conn) {
        long retryAfter = conn.getHeaderFieldInt("Retry-After", -1);
        retryAfter = constrain(retryAfter, MIN_RETRY_AFTER,
                MAX_RETRY_AFTER);
        mInfoDelta.mRetryAfter = (int) (retryAfter * SECOND_IN_MILLIS);
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        // Skip when download already marked as finished; this download was
        // probably started again while racing with UpdateThread.
        if (mInfo.queryDownloadStatus() == STATUS_SUCCESS) {
            logDebug("Already finished; skipping");
            return;
        }
        try {
            // while performing download, register for rules updates
            logDebug("Starting");
            mInfoDelta.mStatus = STATUS_RUNNING;
            mInfoDelta.writeToDatabase();
            executeDownload();
            mInfoDelta.mStatus = STATUS_SUCCESS;
            TrafficStats.incrementOperationCount(1);
            // If we just finished a chunked file, record total size
            if (mInfoDelta.mTotalBytes == -1) {
                mInfoDelta.mTotalBytes = mInfoDelta.mCurrentBytes;
            }

        } catch (StopRequestException e) {
            mInfoDelta.mStatus = e.getFinalStatus();
            mInfoDelta.mErrorMsg = e.getMessage();
            logWarning("Stop requested with status "
                    + statusToString(mInfoDelta.mStatus) + ": "
                    + mInfoDelta.mErrorMsg);
            // Nobody below our level should request retries, since we handle
            // failure counts at this level.
            if (mInfoDelta.mStatus == STATUS_WAITING_TO_RETRY) {
                throw new IllegalStateException("Execution should always throw final error codes");
            }
            // Some errors should be retryable, unless we fail too many times.
            if (isStatusRetryable(mInfoDelta.mStatus)) {
                if (mMadeProgress) {
                    mInfoDelta.mNumFailed = 1;
                } else {
                    mInfoDelta.mNumFailed += 1;
                }
                if (mInfoDelta.mNumFailed < MAX_RETRIES) {
                    mInfoDelta.mStatus = STATUS_WAITING_TO_RETRY;
                    if ((mInfoDelta.mETag == null && mMadeProgress)) {
                        // However, if we wrote data and have no ETag to verify
                        // contents against later, we can't actually resume.
                        mInfoDelta.mStatus = STATUS_CANNOT_RESUME;
                    }
                }
            }

        } catch (Throwable t) {
            mInfoDelta.mStatus = STATUS_UNKNOWN_ERROR;
            mInfoDelta.mErrorMsg = t.toString();
            logError("Failed: " + mInfoDelta.mErrorMsg, t);

        } finally {
            logDebug("Finished with status " + statusToString(mInfoDelta.mStatus));
            mNotifier.notifyDownloadSpeed(mId, 0);
            finalizeDestination();
            mInfoDelta.writeToDatabase();
        }
        boolean needsReschedule = false;
        if (mInfoDelta.mStatus == STATUS_WAITING_TO_RETRY
                || mInfoDelta.mStatus == STATUS_WAITING_FOR_NETWORK
                || mInfoDelta.mStatus == STATUS_QUEUED_FOR_WIFI) {
            needsReschedule = true;
        }
    }

    static class StopRequestException extends Exception {
        public static final int STATUS_UNHANDLED_HTTP_CODE = 494;
        private final int mFinalStatus;

        public StopRequestException(int finalStatus, String message) {
            super(message);
            mFinalStatus = finalStatus;
        }

        public StopRequestException(int finalStatus, Throwable t) {
            this(finalStatus, t.getMessage());
            initCause(t);
        }

        public StopRequestException(int finalStatus, String message, Throwable t) {
            this(finalStatus, message);
            initCause(t);
        }

        public int getFinalStatus() {
            return mFinalStatus;
        }

        public static StopRequestException throwUnhandledHttpError(int code, String message)
                throws StopRequestException {
            final String error = "Unhandled HTTP response: " + code + " " + message;
            if (code >= 400 && code < 600) {
                throw new StopRequestException(code, error);
            } else if (code >= 300 && code < 400) {
                throw new StopRequestException(STATUS_UNHANDLED_REDIRECT, error);
            } else {
                throw new StopRequestException(STATUS_UNHANDLED_HTTP_CODE, error);
            }
        }
    }

    public static class LongSparseLongArray implements Cloneable {
        private long[] mKeys;
        private long[] mValues;
        private int mSize;

        /**
         * Creates a new SparseLongArray containing no mappings.
         */
        public LongSparseLongArray() {
            this(10);
        }

        /**
         * Creates a new SparseLongArray containing no mappings that will not
         * require any additional memory allocation to store the specified
         * number of mappings.  If you supply an initial capacity of 0, the
         * sparse array will be initialized with a light-weight representation
         * not requiring any additional array allocations.
         */
        public LongSparseLongArray(int initialCapacity) {
            if (initialCapacity == 0) {
                mKeys = new long[0];
                mValues = new long[0];
            } else {
                mKeys = new long[initialCapacity];
                mValues = new long[mKeys.length];
            }
            mSize = 0;
        }

        /**
         * Puts a key/value pair into the array, optimizing for the case where
         * the key is greater than all existing keys in the array.
         */
        public void append(long key, long value) {
            if (mSize != 0 && key <= mKeys[mSize - 1]) {
                put(key, value);
                return;
            }
            mKeys = append(mKeys, mSize, key);
            mValues = append(mValues, mSize, value);
            mSize++;
        }

        public static long[] append(long[] array, int currentSize, long element) {
            assert currentSize <= array.length;
            if (currentSize + 1 > array.length) {
                long[] newArray = new long[growSize(currentSize)];
                System.arraycopy(array, 0, newArray, 0, currentSize);
                array = newArray;
            }
            array[currentSize] = element;
            return array;
        }

        /**
         * Removes all key-value mappings from this SparseIntArray.
         */
        public void clear() {
            mSize = 0;
        }

        /**
         * Removes the mapping from the specified key, if there was any.
         */
        public void delete(long key) {
            int i = binarySearch(mKeys, mSize, key);
            if (i >= 0) {
                removeAt(i);
            }
        }

        /**
         * Gets the long mapped from the specified key, or <code>0</code>
         * if no such mapping has been made.
         */
        public long get(long key) {
            return get(key, 0);
        }

        /**
         * Gets the long mapped from the specified key, or the specified value
         * if no such mapping has been made.
         */
        public long get(long key, long valueIfKeyNotFound) {
            int i = binarySearch(mKeys, mSize, key);
            if (i < 0) {
                return valueIfKeyNotFound;
            } else {
                return mValues[i];
            }
        }

        public static int growSize(int currentSize) {
            return currentSize <= 4 ? 8 : currentSize * 2;
        }

        /**
         * Returns the index for which {@link #keyAt} would return the
         * specified key, or a negative number if the specified
         * key is not mapped.
         */
        public int indexOfKey(long key) {
            return binarySearch(mKeys, mSize, key);
        }

        /**
         * Returns an index for which {@link #valueAt} would return the
         * specified key, or a negative number if no keys map to the
         * specified value.
         * Beware that this is a linear search, unlike lookups by key,
         * and that multiple keys can map to the same value and this will
         * find only one of them.
         */
        public int indexOfValue(long value) {
            for (int i = 0; i < mSize; i++)
                if (mValues[i] == value)
                    return i;
            return -1;
        }

        public static long[] insert(long[] array, int currentSize, int index, long element) {
            assert currentSize <= array.length;
            if (currentSize + 1 <= array.length) {
                System.arraycopy(array, index, array, index + 1, currentSize - index);
                array[index] = element;
                return array;
            }
            long[] newArray = new long[growSize(currentSize)];
            System.arraycopy(array, 0, newArray, 0, index);
            newArray[index] = element;
            System.arraycopy(array, index, newArray, index + 1, array.length - index);
            return newArray;
        }

        /**
         * Given an index in the range <code>0...size()-1</code>, returns
         * the key from the <code>index</code>th key-value mapping that this
         * SparseLongArray stores.
         *
         * <p>The keys corresponding to indices in ascending order are guaranteed to
         * be in ascending order, e.g., <code>keyAt(0)</code> will return the
         * smallest key and <code>keyAt(size()-1)</code> will return the largest
         * key.</p>
         *
         * <p>For indices outside of the range <code>0...size()-1</code>, the behavior is undefined for
         * apps targeting {@link android.os.Build.VERSION_CODES#P} and earlier, and an
         * {@link ArrayIndexOutOfBoundsException} is thrown for apps targeting
         * {@link android.os.Build.VERSION_CODES#Q} and later.</p>
         */
        public long keyAt(int index) {
            return mKeys[index];
        }

        /**
         * Adds a mapping from the specified key to the specified value,
         * replacing the previous mapping from the specified key if there
         * was one.
         */
        public void put(long key, long value) {
            int i = binarySearch(mKeys, mSize, key);
            if (i >= 0) {
                mValues[i] = value;
            } else {
                i = ~i;
                mKeys = insert(mKeys, mSize, i, key);
                mValues = insert(mValues, mSize, i, value);
                mSize++;
            }
        }

        /**
         * Removes the mapping at the given index.
         */
        public void removeAt(int index) {
            System.arraycopy(mKeys, index + 1, mKeys, index, mSize - (index + 1));
            System.arraycopy(mValues, index + 1, mValues, index, mSize - (index + 1));
            mSize--;
        }

        /**
         * Returns the number of key-value mappings that this SparseIntArray
         * currently stores.
         */
        public int size() {
            return mSize;
        }

        /**
         * Given an index in the range <code>0...size()-1</code>, returns
         * the value from the <code>index</code>th key-value mapping that this
         * SparseLongArray stores.
         *
         * <p>The values corresponding to indices in ascending order are guaranteed
         * to be associated with keys in ascending order, e.g.,
         * <code>valueAt(0)</code> will return the value associated with the
         * smallest key and <code>valueAt(size()-1)</code> will return the value
         * associated with the largest key.</p>
         *
         * <p>For indices outside of the range <code>0...size()-1</code>, the behavior is undefined for
         * apps targeting {@link android.os.Build.VERSION_CODES#P} and earlier, and an
         * {@link ArrayIndexOutOfBoundsException} is thrown for apps targeting
         * {@link android.os.Build.VERSION_CODES#Q} and later.</p>
         */
        public long valueAt(int index) {
            return mValues[index];
        }

        static int binarySearch(int[] array, int size, int value) {
            int lo = 0;
            int hi = size - 1;
            while (lo <= hi) {
                final int mid = (lo + hi) >>> 1;
                final int midVal = array[mid];
                if (midVal < value) {
                    lo = mid + 1;
                } else if (midVal > value) {
                    hi = mid - 1;
                } else {
                    return mid;  // value found
                }
            }
            return ~lo;  // value not present
        }

        static int binarySearch(long[] array, int size, long value) {
            int lo = 0;
            int hi = size - 1;
            while (lo <= hi) {
                final int mid = (lo + hi) >>> 1;
                final long midVal = array[mid];
                if (midVal < value) {
                    lo = mid + 1;
                } else if (midVal > value) {
                    hi = mid - 1;
                } else {
                    return mid;  // value found
                }
            }
            return ~lo;  // value not present
        }

        @Override
        public LongSparseLongArray clone() {
            LongSparseLongArray clone = null;
            try {
                clone = (LongSparseLongArray) super.clone();
                clone.mKeys = mKeys.clone();
                clone.mValues = mValues.clone();
            } catch (CloneNotSupportedException cnse) {
                /* ignore */
            }
            return clone;
        }

        /**
         * {@inheritDoc}
         *
         * <p>This implementation composes a string by iterating over its mappings.
         */
        @Override
        public String toString() {
            if (size() <= 0) {
                return "{}";
            }
            StringBuilder buffer = new StringBuilder(mSize * 28);
            buffer.append('{');
            for (int i = 0; i < mSize; i++) {
                if (i > 0) {
                    buffer.append(", ");
                }
                long key = keyAt(i);
                buffer.append(key);
                buffer.append('=');
                long value = valueAt(i);
                buffer.append(value);
            }
            buffer.append('}');
            return buffer.toString();
        }

    }

    public class DownloadInfo {
        private final Context mContext;
        public long mId;
        public String mUri;
        public String mHint;
        public String mFileName;
        public String mMimeType;
        public int mDestination;
        public int mVisibility;
        public int mControl;
        public int mStatus;
        public int mNumFailed;
        public int mRetryAfter;
        public long mLastMod;
        public String mPackage;
        public String mClass;
        public String mExtras;
        public String mCookies;
        public String mUserAgent;
        public String mReferer;
        public long mTotalBytes;
        public long mCurrentBytes;
        public String mETag;
        public int mUid;
        public int mMediaScanned;
        public boolean mDeleted;
        public String mMediaProviderUri;
        public String mMediaStoreUri;
        public boolean mIsPublicApi;
        public int mAllowedNetworkTypes;
        public boolean mAllowRoaming;
        public boolean mAllowMetered;
        public int mFlags;
        public String mTitle;
        public String mDescription;
        public int mBypassRecommendedSizeLimit;
        public boolean mIsVisibleInDownloadsUi;
        private List<Pair<String, String>> mRequestHeaders = new ArrayList<Pair<String, String>>();

        public DownloadInfo(Context context) {
            mContext = context;
        }

        public Uri getAllDownloadsUri() {
            return ContentUris.withAppendedId(ALL_DOWNLOADS_CONTENT_URI, mId);
        }

        public Collection<Pair<String, String>> getHeaders() {
            return Collections.unmodifiableList(mRequestHeaders);
        }

        public String getUserAgent() {
            if (mUserAgent != null) {
                return mUserAgent;
            } else {
                return null;//DEFAULT_USER_AGENT;
            }
        }

        public int queryDownloadInt(String columnName, int defaultValue) {
            try (Cursor cursor = mContext.getContentResolver().query(getAllDownloadsUri(),
                    new String[]{columnName}, null, null, null)) {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                } else {
                    return defaultValue;
                }
            }
        }

        public int queryDownloadStatus() {
            return queryDownloadInt(COLUMN_STATUS, STATUS_PENDING);
        }
    }

    private class DownloadInfoDelta {
        public static final String COLUMN_ERROR_MSG = "errorMsg";
        private static final String NOT_CANCELED = COLUMN_STATUS + " != '" + STATUS_CANCELED + "'";
        private static final String NOT_DELETED = COLUMN_DELETED + " == '0'";
        private static final String NOT_PAUSED = "(" + COLUMN_CONTROL + " IS NULL OR "
                + COLUMN_CONTROL + " != '" + CONTROL_PAUSED + "')";
        private static final String SELECTION_VALID = NOT_CANCELED + " AND " + NOT_DELETED + " AND "
                + NOT_PAUSED;
        public String mUri;
        public String mFileName;
        public String mMimeType;
        public int mStatus;
        public int mNumFailed;
        public int mRetryAfter;
        public long mTotalBytes;
        public long mCurrentBytes;
        public String mETag;
        public String mErrorMsg;

        public DownloadInfoDelta(DownloadInfo info) {
            mUri = info.mUri;
            mFileName = info.mFileName;
            mMimeType = info.mMimeType;
            mStatus = info.mStatus;
            mNumFailed = info.mNumFailed;
            mRetryAfter = info.mRetryAfter;
            mTotalBytes = info.mTotalBytes;
            mCurrentBytes = info.mCurrentBytes;
            mETag = info.mETag;
        }

        /**
         * Blindly push update of current delta values to provider.
         */
        public void writeToDatabase() {
//            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), buildContentValues(),
//                    null, null);
        }

        /**
         * Push update of current delta values to provider, asserting strongly
         * that we haven't been paused or deleted.
         */
        public void writeToDatabaseOrThrow() throws StopRequestException {
//            if (mContext.getContentResolver().update(mInfo.getAllDownloadsUri(),
//                    buildContentValues(), SELECTION_VALID, null) == 0) {
//                if (mInfo.queryDownloadControl() == CONTROL_PAUSED) {
//                    throw new StopRequestException(STATUS_PAUSED_BY_APP, "Download paused!");
//                } else {
//                    throw new StopRequestException(STATUS_CANCELED, "Download deleted or missing!");
//                }
//            }
        }

        private ContentValues buildContentValues() {
            final ContentValues values = new ContentValues();
            values.put(COLUMN_URI, mUri);
            values.put(_DATA, mFileName);
            values.put(COLUMN_MIME_TYPE, mMimeType);
            values.put(COLUMN_STATUS, mStatus);
            values.put(COLUMN_FAILED_CONNECTIONS, mNumFailed);
            values.put(RETRY_AFTER_X_REDIRECT_COUNT, mRetryAfter);
            values.put(COLUMN_TOTAL_BYTES, mTotalBytes);
            values.put(COLUMN_CURRENT_BYTES, mCurrentBytes);
            values.put(ETAG, mETag);
            values.put(COLUMN_LAST_MODIFICATION, System.currentTimeMillis());
            values.put(COLUMN_ERROR_MSG, mErrorMsg);
            return values;
        }
    }

    public class DownloadNotifier {
        private final LongSparseLongArray mDownloadSpeed = new LongSparseLongArray();
        private final LongSparseLongArray mDownloadTouch = new LongSparseLongArray();

        public void notifyDownloadSpeed(long id, long bytesPerSecond) {
            synchronized (mDownloadSpeed) {
                if (bytesPerSecond != 0) {
                    mDownloadSpeed.put(id, bytesPerSecond);
                    mDownloadTouch.put(id, SystemClock.elapsedRealtime());
                } else {
                    mDownloadSpeed.delete(id);
                    mDownloadTouch.delete(id);
                }
            }
        }
    }
}
/*

https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/provider/Downloads.java


*/