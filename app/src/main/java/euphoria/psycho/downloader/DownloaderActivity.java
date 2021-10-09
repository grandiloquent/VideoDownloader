package euphoria.psycho.downloader;

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
import java.util.stream.Collectors;

import androidx.annotation.Nullable;
import euphoria.psycho.downloader.RequestQueue.RequestEvent;
import euphoria.psycho.downloader.RequestQueue.RequestEventListener;
import euphoria.psycho.explorer.R;

public class DownloaderActivity extends Activity implements RequestEventListener {
    public static final String ACTION_FINISH = "FINISH";
    public static final String ACTION_REFRESH = "REFRESH";
    public static final String KEY_UPDATE = "update";
    private final List<LifeCycle> mLifeCycles = new ArrayList<>();
    private ListView mListView;
    private VideoAdapter mVideoAdapter;
    private View mProgressBar;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_REFRESH)) {
                mProgressBar.setVisibility(View.GONE);
                mListView.setVisibility(View.VISIBLE);
                mVideoAdapter.update(DownloaderManager.getInstance().getQueue().getCurrentRequests()
                        .stream()
                        .map(Request::getVideoTask)
                        .collect(Collectors.toList()));
            } else {
                finish();
            }
        }
    };

    public void addLifeCycle(LifeCycle lifeCycle) {
        mLifeCycles.add(lifeCycle);
    }

    public static void registerBroadcastReceiver(Context context, BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REFRESH);
        filter.addAction(ACTION_FINISH);
        context.registerReceiver(receiver, filter);
    }

    public void removeLifeCycle(LifeCycle lifeCycle) {
        mLifeCycles.remove(lifeCycle);
    }

    // Initialize the UI
    private void initializeUI() {
        mProgressBar = findViewById(R.id.progress_bar);
        mListView = findViewById(R.id.list_view);
        mVideoAdapter = new VideoAdapter(this);
        mListView.setAdapter(mVideoAdapter);
    }

    // Start download service
    // Register a broadcast receiver
    // Add this instance as a request handler to the download message queue
    private void startProcessing() {
        Intent service = new Intent(this, DownloaderService.class);
        startService(service);
        registerBroadcastReceiver(this, mBroadcastReceiver);
        DownloaderManager.newInstance(this).getQueue().addRequestEventListener(this);
    }

    // If the download task has been executed before
    // this class of instantiation,
    // the UI should be updated immediately
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_s);
        initializeUI();
        startProcessing();
        if (getIntent().getBooleanExtra(KEY_UPDATE, false)) {
            DownloaderHelper.updateList(mProgressBar, mListView, mVideoAdapter);
        }
    }

    @Override
    protected void onDestroy() {
        // Log\.e\([^\n]+\);\n
        for (int i = 0; i < mLifeCycles.size(); i++) {
            mLifeCycles.get(i).onDestroy();
        }
        unregisterReceiver(mBroadcastReceiver);
        DownloaderManager.getInstance().getQueue().removeRequestEventListener(this);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        DownloaderManager.getInstance().removeVideoTaskListener(mVideoAdapter);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DownloaderManager.getInstance().addVideoTaskListener(mVideoAdapter);
    }

    @Override
    public void onRequestEvent(Request Request, int event) {
        if (event == RequestEvent.REQUEST_QUEUED) {
            DownloaderHelper.updateList(mProgressBar, mListView, mVideoAdapter);
        }
    }
}
