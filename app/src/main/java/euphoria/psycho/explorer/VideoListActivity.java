package euphoria.psycho.explorer;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.GridView;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import euphoria.psycho.player.VideoActivity;
import euphoria.psycho.share.ContextShare;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.IntentShare;
import euphoria.psycho.share.StringShare;

public class VideoListActivity extends Activity {
    private VideoAdapter mVideoAdapter;
    private GridView mGridView;

    // Delete the entire directory where the video file is
    private void actionDelete(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        File videoFile = mVideoAdapter.getItem(info.position);
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
        List<File> videos = getVideos();
        mVideoAdapter.update(videos);
    }

    // Share the video
    private void actionShare(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        File videoFile = mVideoAdapter.getItem(info.position);
        startActivity(IntentShare.createShareVideoIntent(Uri.fromFile(videoFile)));
    }

    private List<File> getVideos() {
        List<File> files = FileShare.recursivelyListFiles(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                ".mp4"
        );
        files.sort((o1, o2) -> {
            final long result = o2.lastModified() - o1.lastModified();
            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        });
        return files;
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<File> videos = getVideos();
        mVideoAdapter.update(videos);
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
            IntentShare.launchActivity(VideoListActivity.this,
                    VideoActivity.class,
                    Uri.fromFile(mVideoAdapter.getItem(position)));
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
        if (getIntent().getBooleanExtra("update", false)) {
            List<File> videos = getVideos();
            mVideoAdapter.update(videos);
        }
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
}