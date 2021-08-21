package euphoria.psycho.explorer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import euphoria.psycho.share.ContextShare;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.Logger;

public class VideoListActivity extends Activity {
    private VideoAdapter mVideoAdapter;
    private GridView mGridView;

    // Delete the entire directory where the video file is
    private void actionDelete(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        File directory = mVideoAdapter.getItem(info.position).getParentFile();
        FileShare.recursivelyDeleteFile(directory, input -> true);
        List<File> videos = getVideos();
        mVideoAdapter.update(videos);
    }

    // Share the video
    private void actionShare(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        File videoFile = mVideoAdapter.getItem(info.position);
        startActivity(createShareIntent(Uri.fromFile(videoFile)));
    }

    private Intent createShareIntent(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        return intent;
    }

    public static List<File> recursivelyListFiles(File directory) {
        ArrayList<File> results = new ArrayList<>();
        File[] files = directory.listFiles(file -> file.isDirectory() || (file.isFile() && file.getName().endsWith(".mp4")));
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    results.addAll(recursivelyListFiles(file));
                } else {
                    results.add(file);
                }
            }
        }
        return results;
    }

    private List<File> getVideos() {
        return recursivelyListFiles(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        );
    }

    private void initialize() {
        setContentView(R.layout.activity_video_list);
        mGridView = findViewById(R.id.grid_view);
        mGridView.setNumColumns(2);
        registerForContextMenu(mGridView);
        mVideoAdapter = new VideoAdapter(this);
        mGridView.setAdapter(mVideoAdapter);
        List<File> videos = getVideos();
        mVideoAdapter.update(videos);
        ContextShare.initialize(this);
        mGridView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(parent.getContext(), MovieActivity.class);
            intent.setData(Uri.fromFile(mVideoAdapter.getItem(position)));
            startActivity(intent);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            actionShare(item);
        } else if (item.getItemId() == R.id.action_delete) {
            actionDelete(item);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.videos, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }
}
