package euphoria.psycho.tasks;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;
import euphoria.psycho.tasks.RequestQueue.RequestEvent;
import euphoria.psycho.tasks.RequestQueue.RequestEventListener;

public class VideoActivity extends Activity implements RequestEventListener {
    private ListView mListView;
    private VideoAdapter mVideoAdapter;
    private final List<LifeCycle> mLifeCycles = new ArrayList<>();
    private View mProgressBar;

    public void addLifeCycle(LifeCycle lifeCycle) {
        mLifeCycles.add(lifeCycle);
    }

    @Override
    public void onRequestEvent(Request Request, int event) {
        if (event == RequestEvent.REQUEST_QUEUED) {
            mProgressBar.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
            List<VideoTask> videoTasks = new ArrayList<>();
            for (Request request : VideoManager.getInstance().getQueue().getCurrentRequests()) {
                videoTasks.add(request.getVideoTask());
            }
            mVideoAdapter.update(videoTasks);
        }
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
        mProgressBar = findViewById(R.id.progress_bar);
        mListView = findViewById(R.id.list_view);
        mVideoAdapter = new VideoAdapter(this);
        mListView.setAdapter(mVideoAdapter);
        startService();
        IntentFilter filter = new IntentFilter();
        filter.addAction("euphoria.psycho.tasks.FINISH");
        registerReceiver(mBroadcastReceiver, filter);
        VideoManager.newInstance(this).getQueue().addRequestEventListener(this);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
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
        unregisterReceiver(mBroadcastReceiver);
        VideoManager.getInstance().getQueue().removeRequestEventListener(this);
        super.onDestroy();
    }


}
