package euphoria.psycho.tasks;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;
import euphoria.psycho.share.Logger;
import euphoria.psycho.tasks.RequestQueue.RequestEvent;
import euphoria.psycho.tasks.RequestQueue.RequestEventListener;

public class VideoActivity extends Activity implements RequestEventListener {
    public static final String ACTION_FINISH = "euphoria.psycho.tasks.FINISH";
    public static final String ACTION_REFRESH = "euphoria.psycho.tasks.REFRESH";
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
                mVideoAdapter.update(VideoManager.getInstance().getQueue().getCurrentRequests()
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


    private void startService() {
        String[] videoList = getIntent().getStringArrayExtra(VideoService.KEY_VIDEO_LIST);
        Uri videoUri = getIntent().getData();
        if (videoList == null && videoUri == null) {
            return;
        }
        Intent service = new Intent(this, VideoService.class);
        service.putExtra(VideoService.KEY_VIDEO_LIST, videoList);
        service.setData(videoUri);
        startService(service);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_s);
        mProgressBar = findViewById(R.id.progress_bar);
        mListView = findViewById(R.id.list_view);
        mVideoAdapter = new VideoAdapter(this);
        mListView.setAdapter(mVideoAdapter);
        startService();
        registerBroadcastReceiver(this, mBroadcastReceiver);
        VideoManager.newInstance(this).getQueue().addRequestEventListener(this);
        if (getIntent().getBooleanExtra(KEY_UPDATE, false)) {
            VideoHelper.updateList(mProgressBar, mListView, mVideoAdapter);
        }
    }


    @Override
    protected void onDestroy() {
        for (int i = 0; i < mLifeCycles.size(); i++) {
            mLifeCycles.get(i).onDestroy();
        }
        unregisterReceiver(mBroadcastReceiver);
        VideoManager.getInstance().getQueue().removeRequestEventListener(this);
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        VideoManager.getInstance().removeVideoTaskListener(mVideoAdapter);
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
        VideoManager.getInstance().addVideoTaskListener(mVideoAdapter);
    }


    @Override
    public void onRequestEvent(Request Request, int event) {
        if (event == RequestEvent.REQUEST_QUEUED) {
            VideoHelper.updateList(mProgressBar, mListView, mVideoAdapter);
        }
    }


}
