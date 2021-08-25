package euphoria.psycho.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.io.File;

import androidx.annotation.Nullable;

public class VideoTaskDatabase extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    private static final String CREATE_AT = "create_at";
    private static final String DIRECTORY = "directory";
    private static final String DOWNLOADED_FILES = "downloaded_files";
    private static final String DOWNLOADED_SIZE = "downloaded_size";
    private static final String ID = "id";
    private static final String STATUS = "status";
    public static final String TABLE = "tasks";
    private static final String TOTAL_FILES = "total_files";
    private static final String TOTAL_SIZE = "total_size";
    private static final String UPDATE_AT = "update_at";
    private static final String URI = "uri";

    public VideoTaskDatabase(@Nullable Context context, @Nullable String name) {
        super(context, name, null, DATABASE_VERSION);
    }

    public static VideoTaskDatabase getInstance(Context context) {
        File database = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "" +
                "tasks.db");
        return new VideoTaskDatabase(context, database.getAbsolutePath());
    }

    public VideoTask getVideoTask(String uri) {
        Cursor cursor = getReadableDatabase().rawQuery("select * from " + TABLE + " where uri = ? limit 1",
                new String[]{uri});
        VideoTask videoTask = null;
        if (cursor.moveToNext()) {
            videoTask = new VideoTask();
            videoTask.Uri = cursor.getString(0);
            videoTask.Status = cursor.getInt(1);
            videoTask.Directory = cursor.getString(2);
            videoTask.TotalFiles = cursor.getInt(3);
            videoTask.Id = cursor.getLong(4);
            videoTask.CreateAt = cursor.getLong(5);
            videoTask.UpdateAt = cursor.getLong(6);
            videoTask.DownloadedFiles = cursor.getInt(7);
            videoTask.TotalSize = cursor.getLong(8);
            videoTask.DownloadedSize = cursor.getLong(9);
        }
        cursor.close();
        return videoTask;
    }

    public long insertVideoTask(VideoTask videoTask) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("uri", videoTask.Uri);
        contentValues.put("status", videoTask.Status);
        contentValues.put("directory", videoTask.Directory);
        contentValues.put("total_files", videoTask.TotalFiles);
        contentValues.put("create_at", System.currentTimeMillis());
        contentValues.put("update_at", System.currentTimeMillis());
        contentValues.put("downloaded_files", videoTask.DownloadedFiles);
        contentValues.put("total_size", videoTask.TotalSize);
        contentValues.put("downloaded_size", videoTask.DownloadedSize);
        return getWritableDatabase().insert(TABLE, null, contentValues);
    }

    public int updateVideoTask(VideoTask videoTask) {
        ContentValues contentValues = new ContentValues();
        if (videoTask.Uri != null) {
            contentValues.put("uri", videoTask.Uri);
        }
        contentValues.put("status", videoTask.Status);
        if (videoTask.Directory != null) {
            contentValues.put("directory", videoTask.Directory);
        }
        if (videoTask.TotalFiles != 0) {
            contentValues.put("total_files", videoTask.TotalFiles);
        }
        if (videoTask.CreateAt != 0) {
            contentValues.put("create_at", videoTask.CreateAt);
        }
        if (videoTask.UpdateAt != 0) {
            contentValues.put("update_at", videoTask.UpdateAt);
        } else {
            contentValues.put("update_at", System.currentTimeMillis());
        }
        if (videoTask.DownloadedFiles != 0) {
            contentValues.put("downloaded_files", videoTask.DownloadedFiles);
        }
        if (videoTask.TotalSize != 0) {
            contentValues.put("total_size", videoTask.TotalSize);
        }
        if (videoTask.DownloadedSize != 0) {
            contentValues.put("downloaded_size", videoTask.DownloadedSize);
        }
        return getWritableDatabase().update(TABLE, contentValues, "id=?", new String[]{
                Long.toString(videoTask.Id)
        });
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sb = "CREATE TABLE IF NOT EXISTS " + TABLE + "(" +
                URI + " TEXT NOT NULL UNIQUE," +
                STATUS + " INTEGER," +
                DIRECTORY + " TEXT UNIQUE," +
                TOTAL_FILES + " INTEGER," +
                ID + " INTEGER PRIMARY KEY," +
                CREATE_AT + " INTEGER," +
                UPDATE_AT + " INTEGER," +
                DOWNLOADED_FILES + " INTEGER," +
                TOTAL_SIZE + " INTEGER," +
                DOWNLOADED_SIZE + " INTEGER" +
                ")";
        db.execSQL(sb);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

}