package euphoria.psycho.explorer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.GridView;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import euphoria.psycho.PlayerActivity;
import euphoria.psycho.share.ContextShare;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.StringShare;

public class VideoListActivity extends Activity {
    public static final String EXTRA_LOAD_EXTERNAL_STORAGE_CARD = "load_external_storage_card";
    private VideoAdapter mVideoAdapter;
    private GridView mGridView;

    public static String getExternalStoragePath(Context context) {
        StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            if (result == null) return null;
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                Object removableObject = isRemovable.invoke(storageVolumeElement);
                if (removableObject == null) return null;
                boolean removable = (Boolean) removableObject;
                if (removable) {
                    return path;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Delete the entire directory where the video file is
    private void actionDelete(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        File videoFile = new File(mVideoAdapter.getItem(info.position).Directory,
                mVideoAdapter.getItem(info.position).Filename);
        File directory = videoFile.getParentFile();
        if (directory == null) {
            return;
        }
        String videoFileName = StringShare.substringBeforeLast(videoFile.getName(), ".");
        if (videoFileName.equals(directory.getName())) {
            FileShare.recursivelyDeleteFile(directory, input -> true);
        } else {
            videoFile.delete();
        }
        List<Video> videos = getVideos();
        mVideoAdapter.update(videos);
    }

    // Share the video
    private void actionShare(MenuItem item) {
//        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
//                .getMenuInfo();
//        File videoFile = mVideoAdapter.getItem(info.position);
//        startActivity(IntentShare.createShareVideoIntent(Uri.fromFile(videoFile)));
    }

    private List<Video> getVideos() {
        List<Video> videos = mVideoDatabase.queryDirectory(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), true);
        if (isLoadExternalStorageCard()) {
            String sdcard = getExternalStoragePath(this);
            if (sdcard != null) {
                videos.addAll(mVideoDatabase.queryDirectory(sdcard, false));
            }
        }
        return videos;
//        List<File> files = FileShare.recursivelyListFiles(
//                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
//                ".mp4"
//        );
//        files.sort((o1, o2) -> {
//            final long result = o2.lastModified() - o1.lastModified();
//            if (result < 0) {
//                return -1;
//            } else if (result > 0) {
//                return 1;
//            } else {
//                return 0;
//            }
//        });
//        String sdcard = getExternalStoragePath(this);
//        if (sdcard != null) {
//            File videoDirectory = new File(sdcard, "Videos");
//            if (videoDirectory.exists()) {
//                List<File> videos = files = FileShare.recursivelyListFiles(
//                        videoDirectory,
//                        ".mp4"
//                );
//                videos.sort((o1, o2) -> {
//                    final long result = o2.length() - o1.length();
//                    if (result < 0) {
//                        return -1;
//                    } else if (result > 0) {
//                        return 1;
//                    } else {
//                        return 0;
//                    }
//                });
//                files.addAll(videos);
//            }
//        }
//        return files;
    }

    private void initialize() {
        setContentView(R.layout.activity_video_list);
        mGridView = findViewById(R.id.grid_view);
        mGridView.setNumColumns(2);
        registerForContextMenu(mGridView);
        mVideoAdapter = new VideoAdapter(this);
        mGridView.setAdapter(mVideoAdapter);
//        List<File> videos = getVideos();
//        mVideoAdapter.update(videos);
        ContextShare.initialize(this);
        mGridView.setOnItemClickListener((parent, view, position, id) -> {
            PlayerActivity.launchActivity(VideoListActivity.this, new File(mVideoAdapter.getItem(position).Directory, mVideoAdapter.getItem(position).Filename));
        });
    }

    private boolean isLoadExternalStorageCard() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(EXTRA_LOAD_EXTERNAL_STORAGE_CARD, false);
    }

    private VideoDatabase mVideoDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        if (getIntent().getBooleanExtra("update", false)) {
            List<Video> videos = getVideos();
            mVideoAdapter.update(videos);
        }
        if (isLoadExternalStorageCard() && VERSION.SDK_INT >= VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
        mVideoDatabase = new VideoDatabase(VideoListActivity.this,
                new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath())
        ;
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("加载...");
        dialog.show();
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            mVideoDatabase.scanDirectory(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
            if (isLoadExternalStorageCard()) {
                String sdcard = getExternalStoragePath(this);
                if (sdcard != null) {
                    mVideoDatabase.scanDirectory(sdcard);
                }
            }
            runOnUiThread(() -> dialog.dismiss());
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Video> videos = getVideos();
        mVideoAdapter.update(videos);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            actionDelete(item);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.videos, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    private static class VideoDatabase extends SQLiteOpenHelper {

        public VideoDatabase(@Nullable Context context, @Nullable String name) {
            super(context, name, null, 1);
        }

        public void scanDirectory(String directory) {
            File[] videos = new File(directory)
                    .listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".mp4"));
            if (videos == null || videos.length == 0) return;
            for (File video : videos) {
                Cursor cursor = getReadableDatabase().rawQuery("select * from video where directory = ? and filename = ?", new String[]{
                        video.getParentFile().getAbsolutePath(),
                        video.getName()
                });
                if (cursor.moveToNext()) {
                    cursor.close();
                    continue;
                }
                cursor.close();
                try {
                    ContentValues values = new ContentValues();
                    values.put("directory", video.getParentFile().getAbsolutePath());
                    values.put("filename", video.getName());
                    values.put("length", video.length());
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(video.getAbsolutePath());
                    String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    values.put("duration", Long.parseLong(time));
                    retriever.release();
                    values.put("create_at", System.currentTimeMillis());
                    values.put("update_at", System.currentTimeMillis());
                    getWritableDatabase().insert(
                            "video", null, values
                    );
                } catch (Exception ignore) {
                }

            }
        }

        public List<Video> queryDirectory(String directory, boolean order) {
            List<Video> videos = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery("select * from video where directory = ? " + (order ? "order by create_at desc" : "order by duration desc"), new String[]{
                    directory
            });
            while (cursor.moveToNext()) {
                Video video = new Video();
                video.Id = cursor.getInt(0);
                video.Directory = cursor.getString(1);
                video.Filename = cursor.getString(2);
                video.Length = cursor.getInt(3);
                video.Duration = cursor.getLong(4);
                video.CreateAt = cursor.getLong(5);
                video.UpdateAt = cursor.getLong(6);
                videos.add(video);
            }
            return videos;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table if not exists video (\n" +
                    "    id integer primary key autoincrement ,\n" +
                    "    directory text,\n" +
                    "    filename text,\n" +
                    "    length text,\n" +
                    "    duration integer,\n" +
                    "    create_at integer,\n" +
                    "    update_at integer\n" +
                    ");");
            db.execSQL("create unique index video_directory_filename_uindex\n" +
                    "\ton video (directory, filename);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public static class Video {
        public int Id;
        public String Directory;
        public String Filename;
        public int Length;
        public long Duration;
        public long CreateAt;
        public long UpdateAt;
    }
}
