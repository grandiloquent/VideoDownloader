package euphoria.psycho;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;

public class VideoActivity extends Activity implements VideoManager.Listener {
    private ListView mListView;
    private VideoAdapter mVideoAdapter;
    private List<LifeCycle> mLifeCycles = new ArrayList<>();

    public void addLifeCycle(LifeCycle lifeCycle) {
        mLifeCycles.add(lifeCycle);
    }

    public void removeLifeCycle(LifeCycle lifeCycle) {
        mLifeCycles.remove(lifeCycle);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        File database = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "" +
                "tasks.db");
        database.delete();
        VideoManager.newInstance(this).addListener(this);
        Intent service = new Intent(this, VideoService.class);
        service.setData(Uri.parse("https://cdn.91p07.com//m3u8/505694/505694.m3u8?st=L4N4OdIeD2TqZBQRo4logA&e=1629536998"));
        startService(service);
        mListView = findViewById(R.id.list_view);
        mVideoAdapter = new VideoAdapter(this);
        mListView.setAdapter(mVideoAdapter);
    }

    @Override
    protected void onDestroy() {
        for (LifeCycle lifeCycle : mLifeCycles) {
            lifeCycle.onDestroy();
        }
        VideoManager.getInstance().removeListener(this);
        super.onDestroy();
    }

    @Override
    public void addTask() {
        mVideoAdapter.update(VideoManager.getInstance().getVideoTasks());
    }

}
