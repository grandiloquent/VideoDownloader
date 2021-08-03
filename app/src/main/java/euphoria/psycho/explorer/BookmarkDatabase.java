package euphoria.psycho.explorer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BookmarkDatabase extends SQLiteOpenHelper {

    public static final String NAME = "name";
    public static final String TABLE = "bookmark";
    public static final String URL = "url";
    public static final String CREATE_AT = "createAt";
    public static final String UPDATE_AT = "updateAt";

    public class Bookmark {
        public int Id;
        public String Name;
        public String Url;
    }

    private static final int DATABASE_VERSION = 1;

    public BookmarkDatabase(Context context) {
        super(context, new File(context.getExternalCacheDir(), "bookmark.db").getPath(), null, DATABASE_VERSION);
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            Log.e("TAG/", "Debug: BookmarkDatabase, " + context.getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS)[0]);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " (id INTEGER PRIMARY KEY," +
                NAME + " TEXT," +
                URL + " TEXT," +
                CREATE_AT + " INTEGER," +
                UPDATE_AT + " INTEGER" +
                ")");
        insert("YouTube", "https://m.youtube.com",db);
        insert("回形针", "https://lucidu.cn",db);
        insert("XVideos", "https://xvideos.com",db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insert(String name, String url, SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(URL, url);
        contentValues.put(NAME, name);
        contentValues.put(CREATE_AT, System.currentTimeMillis() / 1000L);
        contentValues.put(UPDATE_AT, System.currentTimeMillis() / 1000L);
        db.insert(TABLE, null, contentValues);
    }

    public void insert(Bookmark bookmark) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(URL, bookmark.Url);
        contentValues.put(NAME, bookmark.Name);
        contentValues.put(CREATE_AT, System.currentTimeMillis() / 1000L);
        contentValues.put(UPDATE_AT, System.currentTimeMillis() / 1000L);
        getWritableDatabase().insert(TABLE, null, contentValues);
    }

    public List<Bookmark> getBookmarkList() {
        Cursor cursor = getReadableDatabase().query(TABLE,
                new String[]{
                        NAME,
                        URL,
                }, null, null, null, null, null);
        List<Bookmark> bookmarks = new ArrayList<>();
        while (cursor.moveToNext()) {
            Bookmark bookmark = new Bookmark();
            bookmark.Name = cursor.getString(0);
            bookmark.Url = cursor.getString(1);
            bookmarks.add(bookmark);
        }
        cursor.close();
        return bookmarks;
    }
}
