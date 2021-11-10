package euphoria.psycho.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import euphoria.psycho.PlayerActivity;
import euphoria.psycho.explorer.R;

public class HLSDownloadActivity extends Activity implements HLSDownloadListener {
    private ListView mListView;
    private HLSDownloadAdapter mVideoAdapter;
    private Handler mHandler;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_s);
        mHandler = new Handler();
        mListView = findViewById(R.id.list_view);
        mVideoAdapter = new HLSDownloadAdapter(mHandler);
        mListView.setAdapter(mVideoAdapter);
        mHandler.post(() -> mVideoAdapter.update(HLSDownloadManager.getInstance(this).getRequests()));
        createDownloadTask(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        HLSDownloadManager.getInstance(this)
                .addHLSDownloadListener(this)
                .addHLSDownloadRequestListener(mVideoAdapter);
    }

    public static void createDownloadTask(Activity context) {
        String uri = context.getIntent().getDataString();
        if (uri == null) return;
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage("创建任务...");
        dialog.show();
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                HLSDownloadManager.getInstance(context).submit(new HLSDownloadTask(context).build(uri));
            } catch (IOException e) {
                e.printStackTrace();
            }
            context.runOnUiThread(() -> dialog.dismiss());
        }).start();
    }

    @Override
    protected void onStop() {
        HLSDownloadManager.getInstance(this)
                .removeHLSDownloadListener(this)
                .removeHLSDownloadRequestListener(mVideoAdapter);
        super.onStop();
    }

    @Override
    public void onFinish(HLSDownloadRequest request) {
    }

    @Override
    public void onSubmit(HLSDownloadRequest request) {
        mHandler.post(() -> mVideoAdapter.update(HLSDownloadManager.getInstance(this).getRequests()));
    }

    private static class HLSDownloadAdapter extends BaseAdapter implements HLSDownloadRequestListener {
        private final List<ViewHolder> mViewHolders = new ArrayList<>();
        private List<HLSDownloadRequest> mRequests = new ArrayList<>();
        private Handler mHandler;

        public HLSDownloadAdapter(Handler handler) {
            mHandler = handler;
        }

        public void update(List<HLSDownloadRequest> requests) {
            mRequests.clear();
            mRequests.addAll(requests);
            notifyDataSetChanged();
        }

        private static void bindViewHolder(ViewHolder viewHolder, View v) {
            viewHolder.layout = v;
            viewHolder.title = v.findViewById(R.id.title);
            viewHolder.subtitle = v.findViewById(R.id.subtitle);
            viewHolder.progressBar = v.findViewById(R.id.progress_bar);
            viewHolder.thumbnail = v.findViewById(R.id.thumbnail);
            viewHolder.button = v.findViewById(R.id.button);
        }

        private void renderComplete(Handler handler, ViewHolder viewHolder, File videoFile) {
            handler.post(() -> {
                viewHolder.button.setImageResource(R.drawable.ic_action_play_arrow);
                viewHolder.subtitle.setText(R.string.merge_complete);
                Glide.with(viewHolder.title.getContext())
                        .load(videoFile)
                        .fitCenter()
                        .into(viewHolder.thumbnail);
                viewHolder.button.setOnClickListener(view -> {
                    Intent intent = new Intent(viewHolder.title.getContext(), PlayerActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(PlayerActivity.KEY_VIDEO_FILE, videoFile.getAbsolutePath());
                    viewHolder.title.getContext().startActivity(intent);
                });
            });
        }

        private static void renderStart(Handler handler, ViewHolder viewHolder) {
            handler.post(() -> {
                viewHolder.subtitle.setText("开始下载");
            });
        }

        @Override
        public int getCount() {
            return mRequests.size();
        }

        @Override
        public HLSDownloadRequest getItem(int i) {
            return mRequests.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.video_item, parent, false);
                viewHolder = new ViewHolder();
                bindViewHolder(viewHolder, convertView);
                mViewHolders.add(viewHolder);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            HLSDownloadRequest request = getItem(position);
            viewHolder.title.setText(request.getTask().getUniqueId());
            viewHolder.subtitle.setText(R.string.waiting);
            viewHolder.progressBar.setProgress(0);
            viewHolder.layout.setOnClickListener(null);
            viewHolder.thumbnail.setImageResource(R.drawable.ic_action_file_download_light);
            viewHolder.tag = request.getTask().getUniqueId();
            return convertView;
        }

        @Override
        public void onProgress(HLSDownloadRequest hlsDownloadRequest) {
            int status = hlsDownloadRequest.getStatus();
            HLSDownloadTask task = hlsDownloadRequest.getTask();
            String uniqueId = task.getUniqueId();
            for (ViewHolder viewHolder : mViewHolders) {
                if (uniqueId.equals(viewHolder.tag)) {
                    switch (status) {
                        case HLSDownloadRequest.STATUS_START:
                            renderStart(mHandler, viewHolder);
                            break;
                        case HLSDownloadRequest.STATUS_MERGE_VIDEO:
                            mHandler.post(() -> {
                                viewHolder.subtitle.setText(R.string.merge_start);
                            });
                            break;
                        case HLSDownloadRequest.STATUS_MERGE_COMPLETED:
                            renderComplete(mHandler, viewHolder, task.getVideoFile());
                            break;
                        default:
                            int sequence = task.getSequence() + 1;
                            int total = task.getHLSDownloadTaskSegments().size();
                            mHandler.post(() -> {
                                viewHolder.subtitle.setText(String.format("%s/%s", sequence
                                        , total));
                                viewHolder.progressBar.setProgress((int) ((sequence * 1.0 / total) * 100));
                            });
                            break;
                    }
                    return;
                }
            }
        }
    }

    private static class ViewHolder {
        public TextView title;
        public TextView subtitle;
        public ProgressBar progressBar;
        public View layout;
        public String tag;
        public ImageView thumbnail;
        public ImageButton button;
    }
}
