package euphoria.psycho.tasks;

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
    private final List<LifeCycle> mLifeCycles = new ArrayList<>();

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
        VideoManager.newInstance(this).addListener(this);
        mListView = findViewById(R.id.list_view);
        mVideoAdapter = new VideoAdapter(this);
        mListView.setAdapter(mVideoAdapter);
        Intent service = new Intent(this, VideoService.class);
        service.setData(getIntent().getData());
        startService(service);
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < mLifeCycles.size(); i++) {
            mLifeCycles.get(i).onDestroy();
        }
        VideoManager.getInstance().removeListener(this);
        super.onDestroy();
    }

    @Override
    public void addTask() {
        mVideoAdapter.update(VideoManager.getInstance().getVideoTasks());
    }

}
