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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }


    private void initialize() {
        setContentView(R.layout.activity_video_list);
        mGridView = findViewById(R.id.grid_view);
        mGridView.setNumColumns(2);
        mVideoAdapter = new VideoAdapter(this);
        mGridView.setAdapter(mVideoAdapter);
        List<File> videos = new ArrayList<>();
        List<File> files = FileShare.recursivelyListFiles(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS));
        for (File f : files) {
            if (FileShare.getExtension(f.getName()).equals("mp4")) {
                videos.add(f);
            }
        }
        mVideoAdapter.update(videos);
        registerForContextMenu(mGridView);
        ContextShare.initialize(this);
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(parent.getContext(), MovieActivity.class);
                intent.setData(Uri.fromFile(mVideoAdapter.getItem(position)));
                startActivity(intent);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.videos, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_share:
                startActivity(createShareIntent(Uri.fromFile(mVideoAdapter.getItem(info.position))));
                break;
            case R.id.action_delete:
                actionDelete(mVideoAdapter.getItem(info.position).getParentFile());
                break;

        }
        return super.onContextItemSelected(item);

    }

    private void actionDelete(File directory) {
        FileShare.recursivelyDeleteFile(directory, new Function<String, Boolean>() {
            @Override
            public Boolean apply(String input) {
                return true;
            }
        });

        List<File> files = FileShare.recursivelyListFiles(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS));
        List<File> videos = new ArrayList<>();

        for (File f : files) {
            if (FileShare.getExtension(f.getName()).equals("mp4")) {
                videos.add(f);
            }
        }
        mVideoAdapter.update(videos);
    }

    private Intent createShareIntent(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        return intent;
    }
}
