package euphoria.psycho.explorer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class DownloadTaskDatabase extends SQLiteOpenHelper {
    public static final int STATUS_FATAL = -1;
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_ERROR_CREATE_CACHE_FILES = 1;
    public static final int STATUS_ERROR_DOWNLOAD_FILE = 2;


    public DownloadTaskDatabase(@Nullable Context context, @Nullable String name) {
        super(context, name, null, 1);
    }

    public DownloadTaskInfo getDownloadTaskInfo(String uri) {
        Cursor cursor = getReadableDatabase().query("tasks", new String[]{
                "_id",
                "filename",
                "status",
                "create_at",
                "update_at"
        }, "uri=?", new String[]{
                uri
        }, null, null, null);
        DownloadTaskInfo downloadTaskInfo = null;
        if (cursor.moveToNext()) {
            downloadTaskInfo = new DownloadTaskInfo();
            downloadTaskInfo.Id = cursor.getLong(0);
            downloadTaskInfo.FileName = cursor.getString(1);
            downloadTaskInfo.Status = cursor.getInt(2);
            downloadTaskInfo.CreateAt = cursor.getLong(3);
            downloadTaskInfo.UpdateAt = cursor.getLong(4);
            downloadTaskInfo.Uri = uri;
        }
        cursor.close();
        return downloadTaskInfo;
    }

    public List<DownloadTaskInfo> getDownloadTaskInfos(int minStatus, int maxStatus) {
        Cursor cursor = getReadableDatabase().rawQuery("select uri,filename from tasks where status >= ? or status <= ? ", new String[]{
                Integer.toString(minStatus),
                Integer.toString(maxStatus)
        });
        List<DownloadTaskInfo> taskInfos = new ArrayList<>();
        while (cursor.moveToNext()) {
            DownloadTaskInfo taskInfo = new DownloadTaskInfo();
            taskInfo.Uri = cursor.getString(0);
            taskInfo.FileName = cursor.getString(1);
            taskInfos.add(taskInfo);
        }
        cursor.close();
        return taskInfos;
    }

    public long insertDownloadTaskInfo(DownloadTaskInfo downloadTaskInfo) {
        ContentValues values = new ContentValues();
        values.put("uri", downloadTaskInfo.Uri);
        values.put("filename", downloadTaskInfo.FileName);
        values.put("create_at", System.currentTimeMillis());
        values.put("update_at", System.currentTimeMillis());
        return getWritableDatabase().insert("tasks", null, values);
    }

    public long updateDownloadTaskInfo(DownloadTaskInfo downloadTaskInfo) {
        ContentValues values = new ContentValues();
        if (downloadTaskInfo.Uri != null)
            values.put("uri", downloadTaskInfo.Uri);
        if (downloadTaskInfo.FileName != null)
            values.put("filename", downloadTaskInfo.FileName);
        values.put("update_at", System.currentTimeMillis());
        if (downloadTaskInfo.Status > 0)
            values.put("status", downloadTaskInfo.Status);
        return getWritableDatabase().update("tasks", values, "_id=?", new String[]{
                Long.toString(downloadTaskInfo.Id)
        });
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists tasks(_id integer PRIMARY KEY,uri text unique,filename text,status integer, create_at INTEGER,update_at integer) ");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public static class DownloadTaskInfo {
        public long Id;
        public String Uri;
        public String FileName;
        public long CreateAt;
        public long UpdateAt;
        public int Status;

        @Override
        public String toString() {
            return "DownloadTaskInfo{" +
                    "Id=" + Id +
                    ", Uri='" + Uri + '\'' +
                    ", FileName='" + FileName + '\'' +
                    ", CreateAt=" + CreateAt +
                    ", UpdateAt=" + UpdateAt +
                    ", Status=" + Status +
                    '}';
        }
    }
}