package euphoria.psycho.tasks;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.Logger;

public class VideoActivity extends Activity implements VideoManager.Listener {
    private ListView mListView;
    private VideoAdapter mVideoAdapter;
    private final List<LifeCycle> mLifeCycles = new ArrayList<>();
    private View mProgressBar;

    public void addLifeCycle(LifeCycle lifeCycle) {
        mLifeCycles.add(lifeCycle);
    }

    public void removeLifeCycle(LifeCycle lifeCycle) {
        mLifeCycles.remove(lifeCycle);
    }

    private void startService() {
        Intent service = new Intent(this, VideoService.class);
        service.setData(getIntent().getData());
        startService(service);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        VideoManager.newInstance(this).addListener(this);
        mProgressBar = findViewById(R.id.progress_bar);
        mListView = findViewById(R.id.list_view);
        mVideoAdapter = new VideoAdapter(this);
        mListView.setAdapter(mVideoAdapter);
        startService();
        IntentFilter filter = new IntentFilter();
        filter.addAction("euphoria.psycho.tasks.FINISH");
        registerReceiver(mBroadcastReceiver, filter);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onDestroy() {
        for (int i = 0; i < mLifeCycles.size(); i++) {
            mLifeCycles.get(i).onDestroy();
        }
        VideoManager.getInstance().removeListener(this);
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void addTask() {
        mProgressBar.setVisibility(View.GONE);
        mListView.setVisibility(View.VISIBLE);
        mVideoAdapter.update(VideoManager.getInstance().getVideoTasks());
    }

    @Override
    public void finished() {
        finish();
    }

}
