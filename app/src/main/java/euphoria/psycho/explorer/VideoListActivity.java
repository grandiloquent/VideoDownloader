package euphoria.psycho.explorer;

import android.app.AlertDialog;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.GridView;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import euphoria.psycho.PlayerActivity;
import euphoria.psycho.share.ContextShare;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.StringShare;

public class VideoListActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    public static final String EXTRA_LOAD_EXTERNAL_STORAGE_CARD = "load_external_storage_card";
    private VideoAdapter mVideoAdapter;
    private GridView mGridView;
    private VideoDatabase mVideoDatabase;


    private static final String KEY_LAST_SORTED_BY = "lastSortedBy";
    private int mLastSortedBy;

    private static final String KEY_SORT_DIRECTION = "sortDirection";
    private int mSortDirection;

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
    private void actionDelete(File videoFile) {
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
        getVideos();
    }

    // Share the video
    private void actionShare(MenuItem item) {
//        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
//                .getMenuInfo();
//        File videoFile = mVideoAdapter.getItem(info.position);
//        startActivity(IntentShare.createShareVideoIntent(Uri.fromFile(videoFile)));
    }

    private void getVideos() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("加载...");
        dialog.show();
        try {
            new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                List<Video> videos = new ArrayList<>();
                mVideoDatabase.scanDirectory(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                if (isLoadExternalStorageCard()) {
                    String sdcard = getExternalStoragePath(this);
                    if (sdcard != null) {
                        mVideoDatabase.scanDirectory(new File(sdcard, "Videos").getAbsolutePath());
                    }
                }
                videos.addAll(mVideoDatabase.queryDirectory(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), mLastSortedBy));
                if (isLoadExternalStorageCard()) {
                    String sdcard = getExternalStoragePath(this);
                    if (sdcard != null) {
                        videos.addAll(mVideoDatabase.queryDirectory(new File(sdcard, "Videos").getAbsolutePath(), mLastSortedBy));
                    }
                }
                runOnUiThread(() -> {
                    dialog.dismiss();
                    mVideoAdapter.update(videos);

                });
            }).start();

        } catch (Exception ignored) {
        }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().show();
        mLastSortedBy = PreferenceManager.getDefaultSharedPreferences(this).getInt(KEY_LAST_SORTED_BY, 2);
        mSortDirection = PreferenceManager.getDefaultSharedPreferences(this).getInt(KEY_SORT_DIRECTION, 1);
        initialize();
        if (getIntent().getBooleanExtra("update", false)) {
            getVideos();
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
                new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath());

    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(KEY_LAST_SORTED_BY, mLastSortedBy).putInt(KEY_SORT_DIRECTION, mSortDirection).apply();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getVideos();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.order_size) {
            item.setChecked(true);
            mLastSortedBy = 3 * mSortDirection;
            getVideos();
        } else if (item.getItemId() == R.id.order_create_at) {
            item.setChecked(true);
            mLastSortedBy = 2 * mSortDirection;
            getVideos();
        } else if (item.getItemId() == R.id.order_duration) {
            item.setChecked(true);
            mLastSortedBy = 1 * mSortDirection;
            getVideos();
        } else if (item.getItemId() == R.id.order_increase) {
            item.setChecked(true);
            mSortDirection = 1;
            mLastSortedBy = Math.abs(mLastSortedBy);
            getVideos();
        } else if (item.getItemId() == R.id.order_decrease) {
            item.setChecked(true);
            mSortDirection = -1;
            mLastSortedBy = Math.abs(mLastSortedBy) * mSortDirection;
            getVideos();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        if (item.getItemId() == R.id.action_delete) {
            File videoFile = new File(mVideoAdapter.getItem(info.position).Directory,
                    mVideoAdapter.getItem(info.position).Filename);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("确定要删除 " + videoFile.getName() + " 吗？")
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        actionDelete(videoFile);
                        mVideoDatabase.remove(videoFile);
                        dialogInterface.dismiss();
                    }).setNegativeButton(android.R.string.cancel, (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                    })
                    .create();
            dialog.show();
        }
        if (item.getItemId() == R.id.action_rename) {
            File videoFile = new File(mVideoAdapter.getItem(info.position).Directory,
                    mVideoAdapter.getItem(info.position).Filename);
            EditText editText = new EditText(this);
            editText.setText(StringShare.substringBeforeLast(videoFile.getName(), "."));
            editText.requestFocus();
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("重命名文件")
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        File n = new File(videoFile.getParentFile(), FileShare.getValidFileName(editText.getText().toString().trim()) +
                                "." + StringShare.substringAfterLast(videoFile.getName(), "."));
                        if (!n.exists()) {
                            videoFile.renameTo(n);
                        }
                        mVideoDatabase.remove(videoFile);
                        getVideos();
                        dialogInterface.dismiss();
                    }).setNegativeButton(android.R.string.cancel, (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                    })
                    .create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.videos, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.browser, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
        if (Math.abs(mLastSortedBy) == 1)
            menu.findItem(R.id.order_duration).setChecked(true);
        else if (Math.abs(mLastSortedBy) == 2)
            menu.findItem(R.id.order_create_at).setChecked(true);
        else if (Math.abs(mLastSortedBy) == 3)
            menu.findItem(R.id.order_size).setChecked(true);
        if (mSortDirection == 1)
            menu.findItem(R.id.order_increase).setChecked(true);
        else
            menu.findItem(R.id.order_decrease).setChecked(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private static class VideoDatabase extends SQLiteOpenHelper {

        public VideoDatabase(@Nullable Context context, @Nullable String name) {
            super(context, name, null, 1);
        }

        // sortBy
        public List<Video> queryDirectory(String directory, int sortBy) {
            StringBuilder stringBuilder = new StringBuilder("select * from video where directory = ? ");
            switch (sortBy) {
                case 1:
                    stringBuilder.append("order by duration");
                    break;
                case -1:
                    stringBuilder.append("order by duration desc");
                    break;
                case 3:
                    stringBuilder.append("order by length");
                    break;
                case -3:
                    stringBuilder.append("order by length desc");
                    break;
                case 2:
                    stringBuilder.append("order by create_at");
                    break;
                case -2:
                    stringBuilder.append("order by create_at desc");
                    break;
            }
            // + (order ? "" : "order by duration desc")
            List<Video> videos = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(stringBuilder.toString(), new String[]{
                    directory
            });
            while (cursor.moveToNext()) {
                Video video = new Video();
                video.Id = cursor.getInt(0);
                video.Directory = cursor.getString(1);
                video.Filename = cursor.getString(2);
                video.Length = cursor.getLong(3);
                video.Duration = cursor.getLong(4);
                video.CreateAt = cursor.getLong(5);
                video.UpdateAt = cursor.getLong(6);
                videos.add(video);
            }
            return videos;
        }

        public void remove(File video) {
            getWritableDatabase().delete("video", "directory = ? and filename = ?", new String[]{
                    video.getParentFile().getAbsolutePath(),
                    video.getName()
            });
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
                    values.put("create_at", video.lastModified());
                    values.put("update_at", System.currentTimeMillis());
                    getWritableDatabase().insert(
                            "video", null, values
                    );
                } catch (Exception ignore) {
                }

            }
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table if not exists video (\n" +
                    "    id integer primary key autoincrement ,\n" +
                    "    directory text,\n" +
                    "    filename text,\n" +
                    "    length integer,\n" +
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
        public long Length;
        public long Duration;
        public long CreateAt;
        public long UpdateAt;
    }
}
