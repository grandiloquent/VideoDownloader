package euphoria.psycho.downloader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class DownloadTaskDatabase extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String TABLE = "tasks";
    private static final String CREATE_AT = "create_at";
    private static final String DIRECTORY = "directory";
    private static final String DOWNLOADED_SIZE = "downloaded_size";
    private static final String FILE_NAME = "file_name";
    private static final String ID = "id";
    private static final String STATUS = "status";
    private static final String TOTAL_SIZE = "total_size";
    private static final String UPDATE_AT = "update_at";
    private static final String URI = "uri";

    public DownloadTaskDatabase(@Nullable Context context, @Nullable String name) {
        super(context, name, null, DATABASE_VERSION);
    }

    public DownloaderTask getDownloadTask(String fileName) {
        Cursor cursor = getReadableDatabase().rawQuery("select * from " + TABLE + " where file_name = ? limit 1",
                new String[]{fileName});
        DownloaderTask videoTask = null;
        if (cursor.moveToNext()) {
            videoTask = createDownloadTaskFromCursor(cursor);
        }
        cursor.close();
        return videoTask;
    }

    public static DownloadTaskDatabase getInstance(Context context) {
        // /data/user/0/euphoria.psycho.explorer/databases/goods
        return new DownloadTaskDatabase(context, "tasks.db");
    }

    public List<DownloaderTask> getPendingDownloadTasks() {
        Cursor cursor = getReadableDatabase().rawQuery("select * from " + TABLE + " where status != 7 and status != -9",
                null);
        List<DownloaderTask> videoTasks = new ArrayList<>();
        while (cursor.moveToNext()) {
            videoTasks.add(createDownloadTaskFromCursor(cursor));
        }
        cursor.close();
        return videoTasks;
    }

    public long insertDownloadTask(DownloaderTask videoTask) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("uri", videoTask.Uri);
        contentValues.put("directory", videoTask.Directory);
        contentValues.put("file_name", videoTask.FileName);
        contentValues.put("status", videoTask.Status);
        contentValues.put("downloaded_size", videoTask.DownloadedSize);
        contentValues.put("total_size", videoTask.TotalSize);
        contentValues.put("create_at", System.currentTimeMillis());
        contentValues.put("update_at", System.currentTimeMillis());
        return getWritableDatabase().insert(TABLE, null, contentValues);
    }

    public int updateDownloadTask(DownloaderTask videoTask) {
        ContentValues contentValues = new ContentValues();
        if (videoTask.Uri != null) {
            contentValues.put("uri", videoTask.Uri);
        }
        if (videoTask.Directory != null) {
            contentValues.put("directory", videoTask.Directory);
        }
        if (videoTask.FileName != null) {
            contentValues.put("file_name", videoTask.FileName);
        }
        if (videoTask.Status != 0) {
            contentValues.put("status", videoTask.Status);
        }
        if (videoTask.DownloadedSize != 0) {
            contentValues.put("downloaded_size", videoTask.DownloadedSize);
        }
        if (videoTask.TotalSize != 0) {
            contentValues.put("total_size", videoTask.TotalSize);
        }
        if (videoTask.UpdateAt != 0) {
            contentValues.put("update_at", System.currentTimeMillis());
        }
        return getWritableDatabase().update(TABLE, contentValues, "id=?", new String[]{
                Long.toString(videoTask.Id)
        });
    }

    private static DownloaderTask createDownloadTaskFromCursor(Cursor cursor) {
        DownloaderTask videoTask = new DownloaderTask();
        videoTask.Id = cursor.getLong(0);
        videoTask.Uri = cursor.getString(1);
        videoTask.Directory = cursor.getString(2);
        videoTask.FileName = cursor.getString(3);
        videoTask.Status = cursor.getInt(4);
        videoTask.DownloadedSize = cursor.getLong(5);
        videoTask.TotalSize = cursor.getLong(6);
        videoTask.CreateAt = cursor.getLong(7);
        videoTask.UpdateAt = cursor.getLong(8);
        return videoTask;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
         /* URI + " TEXT NOT NULL UNIQUE," +
                STATUS + " INTEGER," +
                DIRECTORY + " TEXT," +
                TOTAL_FILES + " INTEGER," +
                ID + " INTEGER PRIMARY KEY," +
                CREATE_AT + " INTEGER," +
                UPDATE_AT + " INTEGER," +
                DOWNLOADED_FILES + " INTEGER," +
                TOTAL_SIZE + " INTEGER," +
                DOWNLOADED_SIZE + " INTEGER" +
                */
        String sb = "CREATE TABLE IF NOT EXISTS " + TABLE + "(" +
                ID + " INTEGER PRIMARY KEY," +
                URI + " TEXT NOT NULL UNIQUE," +
                DIRECTORY + " TEXT," +
                FILE_NAME + " TEXT NOT NULL UNIQUE," +
                STATUS + " INTEGER," +
                DOWNLOADED_SIZE + " INTEGER," +
                TOTAL_SIZE + " INTEGER," +
                CREATE_AT + " INTEGER," +
                UPDATE_AT + " INTEGER" +
                ")";
        db.execSQL(sb);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

}
