package euphoria.psycho.explorer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import euphoria.psycho.share.FileShare;

public class VideoListActivity extends Activity {
    private VideoAdapter mVideoAdapter;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    private void initialize() {
        setContentView(R.layout.activity_video_list);
        mListView = findViewById(R.id.list_view);
        mVideoAdapter = new VideoAdapter(this);
        mListView.setAdapter(mVideoAdapter);
        List<File> files = FileShare.recursivelyListFiles(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS));
        List<File> videos = new ArrayList<>();
        for (File f : files) {
            if (FileShare.getExtension(f.getName()).equals("mp4")) {
                videos.add(f);
            }
        }
        mVideoAdapter.update(videos);
        registerForContextMenu(mListView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.videos, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        return super.onContextItemSelected(item);
    }
}
