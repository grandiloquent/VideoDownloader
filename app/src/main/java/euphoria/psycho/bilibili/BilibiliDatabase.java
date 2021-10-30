package euphoria.psycho.bilibili;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BilibiliDatabase extends SQLiteOpenHelper {

    public BilibiliDatabase(Context context) {
        super(context, BilibiliUtils.getBilibiliDatabaseName(context), null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table tasks\n" +
                "(\n" +
                "    id        integer\n" +
                "        primary key autoincrement,\n" +
                "    url       text not null\n" +
                "        unique,\n" +
                "    title     text,\n" +
                "    filename  text not null,\n" +
                "    status integer,\n" +
                "    create_at integer,\n" +
                "    update_at integer\n" +
                ");");
        db.execSQL("create table threads\n" +
                "(\n" +
                "    id        integer\n" +
                "        constraint threads_pk\n" +
                "            primary key autoincrement,\n" +
                "    task_id   integer\n" +
                "        references tasks\n" +
                "            on delete cascade,\n" +
                "    url       text,\n" +
                "    filename  text not null,\n" +
                "    size      integer,\n" +
                "    create_at integer,\n" +
                "    update_at integer\n" +
                ");\n");
        db.execSQL("create unique index threads_url_uindex\n" +
                "    on threads (url);");
    }

    public void updateBilibiliTask(BilibiliTask bilibiliTask) {
        ContentValues values = new ContentValues();
        values.put("status", bilibiliTask.Status);
        getWritableDatabase().update("tasks", values, "id=?", new String[]{
                Integer.toString(bilibiliTask.Id)
        });
    }

    public void deleteBilibiliTask(BilibiliTask bilibiliTask) {
        ContentValues values = new ContentValues();
        getWritableDatabase().delete("tasks", "id=?", new String[]{
                Integer.toString(bilibiliTask.Id)
        });
    }

    public void insertBilibiliTask(BilibiliTask bilibiliTask) {
        Cursor cursor = getReadableDatabase().rawQuery("select id, filename from tasks where url = ?", new String[]{
                bilibiliTask.Url
        });
        if (cursor.moveToNext()) {
            if (!new File(cursor.getString(1)).exists()) {
                getWritableDatabase().delete("tasks", "id=?", new String[]{
                        Integer.toString(cursor.getInt(0))
                });
            }
        }
        cursor.close();
        ContentValues values = new ContentValues();
        values.put("url", bilibiliTask.Url);
        values.put("title", bilibiliTask.Title);
        values.put("filename", bilibiliTask.Filename);
        values.put("create_at", System.currentTimeMillis());
        values.put("update_at", System.currentTimeMillis());
        long id = getWritableDatabase().insert("tasks", null, values);
        if (id == -1) return;
        for (BilibiliThread thread : bilibiliTask.BilibiliThreads) {
            ContentValues v = new ContentValues();
            v.put("task_id", id);
            v.put("url", thread.Url);
            v.put("filename", thread.Filename);
            v.put("size", thread.Size);
            v.put("create_at", System.currentTimeMillis());
            v.put("update_at", System.currentTimeMillis());
            getWritableDatabase().insert("threads", null, v);
        }
    }

    public List<BilibiliTask> queryUnfinishedTasks() {
        List<BilibiliTask> tasks = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery("select * from tasks where status > -1 or status is null", null);
        while (cursor.moveToNext()) {
            BilibiliTask bilibiliTask = new BilibiliTask();
            bilibiliTask.Id = cursor.getInt(0);
            bilibiliTask.Url = cursor.getString(1);
            bilibiliTask.Title = cursor.getString(2);
            bilibiliTask.Filename = cursor.getString(3);
            bilibiliTask.Status = cursor.getInt(4);
            bilibiliTask.CreateAt = cursor.getInt(5);
            bilibiliTask.UpdateAt = cursor.getInt(6);
            String sql = "select * from threads where task_id = ?";
            Cursor threadCursor = getReadableDatabase().rawQuery(sql, new String[]{
                    Integer.toString(bilibiliTask.Id)
            });
            List<BilibiliThread> threads = new ArrayList<>();
            while (threadCursor.moveToNext()) {
                BilibiliThread thread = new BilibiliThread();
                thread.Id = threadCursor.getInt(0);
                thread.TaskId = threadCursor.getInt(1);
                thread.Url = threadCursor.getString(2);
                thread.Filename = threadCursor.getString(3);
                thread.Size = threadCursor.getInt(4);
                thread.CreateAt = threadCursor.getInt(5);
                thread.UpdateAt = threadCursor.getInt(6);
                threads.add(thread);
            }
            threadCursor.close();
            bilibiliTask.BilibiliThreads = threads.toArray(new BilibiliThread[0]);
            tasks.add(bilibiliTask);
        }
        cursor.close();
        return tasks;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
