package euphoria.psycho.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class HLSDownloadDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private Context mContext;

    public HLSDownloadDatabase(@Nullable Context context, @Nullable String name) {
        super(context, name, null, DATABASE_VERSION);
        mContext = context;
    }

    public void insertTask(HLSDownloadTask downloadTask) {
        ContentValues values = new ContentValues();
        values.put("uri", downloadTask.getUri());
        values.put("status", downloadTask.getStatus());
        values.put("create_at", System.currentTimeMillis());
        values.put("update_at", System.currentTimeMillis());
        values.put("unique_id", downloadTask.getUniqueId());
        getWritableDatabase().insert("task", null, values);
        downloadTask.getHLSDownloadTaskSegments().forEach(
                ts -> {
                    ContentValues v = new ContentValues();
                    v.put("unique_id", downloadTask.getUniqueId());
                    v.put("uri", ts.Uri);
                    v.put("sequence", ts.Sequence);
                    v.put("total", ts.Total);
                    v.put("status", ts.Status);
                    v.put("create_at", System.currentTimeMillis());
                    v.put("update_at", System.currentTimeMillis());
                    getWritableDatabase().insert("task_segment", null, v);
                }
        );
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table task\n" +
                "(\n" +
                "    id        integer primary key,\n" +
                "    uri       text not null unique,\n" +
                "    unique_id text not null unique,\n" +
                "    status    integer,\n" +
                "    create_at integer,\n" +
                "    update_at integer" +
                ");");
        sqLiteDatabase.execSQL("create table if not exists task_segment(\n" +
                "    id integer primary key ,\n" +
                "    unique_id text not null,\n" +
                "    uri text not null unique ,\n" +
                "    sequence integer,\n" +
                "    total integer,\n" +
                "    status integer,\n" +
                "    create_at integer,\n" +
                "    update_at integer\n" +
                ")");
    }

    public HLSDownloadTask getTask(String uniqueId) {
        Cursor cursor = getReadableDatabase().rawQuery("select * from task where unique_id = ?", new String[]{uniqueId});
        HLSDownloadTask downloadTask = null;
        if (cursor.moveToNext()) {
            downloadTask = new HLSDownloadTask(mContext);
            downloadTask.setId(cursor.getInt(0));
            downloadTask.setUri(cursor.getString(1));
            downloadTask.setUniqueId(cursor.getString(2));
            downloadTask.setStatus(cursor.getInt(3));
            downloadTask.setCreateAt(cursor.getLong(4));
            downloadTask.setUpdateAt(cursor.getLong(5));
            List<HLSDownloadTaskSegment> segments = new ArrayList<>();
            Cursor c = getReadableDatabase().rawQuery("select * from task_segment where unique_id = ? order by sequence", new String[]{uniqueId});
            while (c.moveToNext()) {
                HLSDownloadTaskSegment segment = new HLSDownloadTaskSegment();
                segment.Id = c.getInt(0);
                segment.UniqueId = c.getString(1);
                segment.Uri = c.getString(2);
                segment.Sequence = c.getInt(3);
                segment.Total = c.getLong(4);
                segment.Status = c.getInt(5);
                segment.CreateAt = c.getLong(6);
                segment.UpdateAt = c.getLong(7);
                segments.add(segment);
            }
            c.close();
            downloadTask.setHLSDownloadTaskSegments(segments);
        }
        cursor.close();
        return downloadTask;
    }

    public void updateTaskSegment(HLSDownloadTaskSegment taskSegment) {
        ContentValues values = new ContentValues();
        values.put("status", taskSegment.Status);
        values.put("total", taskSegment.Total);
        getWritableDatabase().update("task_segment", values, " unique_id = ?  and uri = ?", new String[]{
                taskSegment.UniqueId,
                taskSegment.Uri
        });
    }

    public void updateTask(HLSDownloadTask task) {
        ContentValues values = new ContentValues();
        values.put("status", task.getStatus());
        getWritableDatabase().update("task", values, " unique_id = ?", new String[]{
                task.getUniqueId()});
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }
}
