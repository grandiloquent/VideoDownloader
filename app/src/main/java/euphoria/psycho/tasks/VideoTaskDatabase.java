package euphoria.psycho.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class VideoTaskDatabase extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String TABLE = "tasks";
    private static final String ID = "id";
    private static final String URI = "uri";
    private static final String DIRECTORY = "directory";
    private static final String FILE_NAME = "file_name";
    private static final String CONTENT = "content";
    private static final String STATUS = "status";
    private static final String DOWNLOADED_FILES = "downloaded_files";
    private static final String TOTAL_FILES = "total_files";
    private static final String DOWNLOADED_SIZE = "downloaded_size";
    private static final String TOTAL_SIZE = "total_size";
    private static final String CREATE_AT = "create_at";
    private static final String UPDATE_AT = "update_at";

    public VideoTaskDatabase(@Nullable Context context, @Nullable String name) {
        super(context, name, null, DATABASE_VERSION);
    }

    public static VideoTaskDatabase getInstance(Context context) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File database = new File(dir,
                "tasks.db");
        // adb pull /storage/emulated/0/Android/data/euphoria.psycho.explorer/files/Download/tasks.db
        return new VideoTaskDatabase(context, database.getAbsolutePath());
    }

    public VideoTask getVideoTask(String fileName) {
        Cursor cursor = getReadableDatabase().rawQuery("select * from " + TABLE + " where file_name = ? limit 1",
                new String[]{fileName});
        VideoTask videoTask = null;
        if (cursor.moveToNext()) {
            videoTask = createVideoTaskFromCursor(cursor);
        }
        cursor.close();
        return videoTask;
    }

    public List<VideoTask> getPendingVideoTasks() {
        Cursor cursor = getReadableDatabase().rawQuery("select * from " + TABLE + " where status != 7 and status >-1 ",
                null);
        List<VideoTask> videoTasks = new ArrayList<>();
        while (cursor.moveToNext()) {
            videoTasks.add(createVideoTaskFromCursor(cursor));
        }
        cursor.close();
        return videoTasks;
    }

    private static VideoTask createVideoTaskFromCursor(Cursor cursor) {
        VideoTask videoTask = new VideoTask();
        videoTask.Id = cursor.getLong(0);
        videoTask.Uri = cursor.getString(1);
        videoTask.Directory = cursor.getString(2);
        videoTask.FileName = cursor.getString(3);
        videoTask.Content = cursor.getString(4);
        videoTask.Status = cursor.getInt(5);
        videoTask.DownloadedFiles = cursor.getInt(6);
        videoTask.TotalFiles = cursor.getInt(7);
        videoTask.DownloadedSize = cursor.getLong(8);
        videoTask.TotalSize = cursor.getLong(9);
        videoTask.CreateAt = cursor.getLong(10);
        videoTask.UpdateAt = cursor.getLong(11);
        return videoTask;
    }


    public long insertVideoTask(VideoTask videoTask) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("uri", videoTask.Uri);
        contentValues.put("directory", videoTask.Directory);
        contentValues.put("file_name", videoTask.FileName);
        contentValues.put("content", videoTask.Content);
        contentValues.put("status", videoTask.Status);
        contentValues.put("downloaded_files", videoTask.DownloadedFiles);
        contentValues.put("total_files", videoTask.TotalFiles);
        contentValues.put("downloaded_size", videoTask.DownloadedSize);
        contentValues.put("total_size", videoTask.TotalSize);
        contentValues.put("create_at", System.currentTimeMillis());
        contentValues.put("update_at", System.currentTimeMillis());
        return getWritableDatabase().insert(TABLE, null, contentValues);
    }

    public int updateVideoTask(VideoTask videoTask) {
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
        if (videoTask.Content != null) {
            contentValues.put("content", videoTask.Content);
        }
        if (videoTask.Status != 0) {
            contentValues.put("status", videoTask.Status);
        }
        if (videoTask.DownloadedFiles != 0) {
            contentValues.put("downloaded_files", videoTask.DownloadedFiles);
        }
        if (videoTask.TotalFiles != 0) {
            contentValues.put("total_files", videoTask.TotalFiles);
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
                CONTENT + " TEXT NOT NULL," +
                STATUS + " INTEGER," +
                DOWNLOADED_FILES + " INTEGER," +
                TOTAL_FILES + " INTEGER," +
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
