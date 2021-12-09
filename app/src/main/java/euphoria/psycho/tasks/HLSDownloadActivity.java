package euphoria.psycho.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import euphoria.psycho.PlayerActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.explorer.R;

public class HLSDownloadActivity extends Activity implements HLSDownloadListener {
    public static final String EXTRA_FILE_NAME = "FILE_NAME";
    private HLSDownloadAdapter mVideoAdapter;
    private Handler mHandler;

    // Generate a video download task through the m3u8 file address
    // and submit the task to the thread pool
    public static void createDownloadTask(Activity context) {
        String uri = context.getIntent().getDataString();
        if (uri == null) return;
        String fileName = context.getIntent().getStringExtra(EXTRA_FILE_NAME);
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.create_task));
        dialog.show();
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                HLSDownloadTask task = new HLSDownloadTask(context)
                        .setFileName(fileName).build(uri);
                if (task == null) {
                    context.runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(context, context.getString(R.string.failed_to_create_task), Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                HLSDownloadManager.getInstance(context).submit(task);
                context.runOnUiThread(dialog::dismiss);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hls_download);
        mHandler = new Handler();
        GridView gridView = findViewById(R.id.grid_view);
        mVideoAdapter = new HLSDownloadAdapter(mHandler);
        gridView.setAdapter(mVideoAdapter);
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

    @Override
    protected void onStop() {
        HLSDownloadManager.getInstance(this)
                .removeHLSDownloadListener(this)
                .removeHLSDownloadRequestListener(mVideoAdapter);
        super.onStop();
    }

    @Override
    public void onFinish(HLSDownloadRequest request) {
        request.setPaused(true);
        boolean matched = HLSDownloadManager.getInstance(this)
                .getRequests().stream().anyMatch(
                        r -> !r.isPaused()
                );
        if (!matched) {
            Intent service = new Intent(this, HLSDownloadService.class);
            service.setAction(HLSDownloadService.CLOSE_SERVICE);
            startService(service);
        }
    }

    @Override
    public void onSubmit(HLSDownloadRequest request) {
        mHandler.post(() -> mVideoAdapter.update(HLSDownloadManager.getInstance(this).getRequests()));
    }

    private static class HLSDownloadAdapter extends BaseAdapter implements HLSDownloadRequestListener {
        private final Handler mHandler;
        private final List<HLSDownloadRequest> mRequests = new ArrayList<>();
        private final List<ViewHolder> mViewHolders = new ArrayList<>();

        public HLSDownloadAdapter(Handler handler) {
            mHandler = handler;
        }

        public void update(List<HLSDownloadRequest> requests) {
            mRequests.clear();
            mRequests.addAll(requests);
            notifyDataSetChanged();
        }

        private static void bindViewHolder(ViewHolder viewHolder, View v) {
            viewHolder.title = v.findViewById(R.id.title);
            viewHolder.progressBar = v.findViewById(R.id.progress_bar);
            viewHolder.thumbnail = v.findViewById(R.id.thumbnail);
            // viewHolder.button = v.findViewById(R.id.button);
        }

        private void playVideo(Context context, String videoFile) {
            Intent intent = new Intent(context, PlayerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(PlayerActivity.KEY_VIDEO_FILE, videoFile);
            context.startActivity(intent);
        }

        private void renderComplete(Handler handler, ViewHolder viewHolder, File videoFile) {
            handler.post(() -> {
                //viewHolder.button.setImageResource(R.drawable.ic_action_play_arrow);
                viewHolder.progressBar.setVisibility(View.INVISIBLE);
                viewHolder.thumbnail.setVisibility(View.VISIBLE);
                Glide.with(viewHolder.title.getContext())
                        .load(videoFile)
                        .fitCenter()
                        .into(viewHolder.thumbnail);
                viewHolder.thumbnail.setOnClickListener(view -> {
                    playVideo(view.getContext(), videoFile.getAbsolutePath());
                });
//                viewHolder.button.setOnClickListener(view -> {
//                    playVideo(view.getContext(), videoFile.getAbsolutePath());
//                });
            });
        }

        private void renderStart(HLSDownloadRequest request, ViewHolder viewHolder) {
            mHandler.post(() -> {
                viewHolder.tag = request.getTask().getUniqueId();
            });
        }

        private void renderTask(HLSDownloadRequest hlsDownloadRequest, ViewHolder viewHolder) {
            HLSDownloadTask task = hlsDownloadRequest.getTask();
            switch (hlsDownloadRequest.getStatus()) {
                case HLSDownloadRequest.STATUS_START:
                    renderStart(hlsDownloadRequest, viewHolder);
                    break;
                case HLSDownloadRequest.STATUS_MERGE_VIDEO:
                    mHandler.post(() -> {
                    });
                    break;
                case HLSDownloadRequest.STATUS_MERGE_COMPLETED:
                    renderComplete(mHandler, viewHolder, task.getVideoFile());
                      Native.deleteDirectory(task.getDirectory().getAbsolutePath());
                    break;
                case HLSDownloadRequest.STATUS_FILE_CACHED:
                case HLSDownloadRequest.STATUS_CONTENT_LENGTH:
                    int sequence = task.getSequence() + 1;
                    int total = task.getHLSDownloadTaskSegments().size();
                    mHandler.post(() -> {
                        viewHolder.progressBar.setProgress((int) ((sequence * 1.0 / total) * 100));
                    });
                    break;
                case HLSDownloadRequest.STATUS_PAUSED:
                    mHandler.post(() -> {
                    });
                    break;
            }
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
                        .inflate(R.layout.hls_download_item, parent, false);
                viewHolder = new ViewHolder();
                bindViewHolder(viewHolder, convertView);
                mViewHolders.add(viewHolder);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            HLSDownloadRequest request = getItem(position);
            viewHolder.title.setText(request.getTask().getFileName());
            viewHolder.progressBar.setProgress(0);

            if (viewHolder.tag != null && !viewHolder.tag.equals(request.getTask().getUniqueId())) {
//                Log.e("B5aOx2", String.format("getView, position = %s;\n mRequests.size() = %s;\n request.getTask().getFileName() = %s;\n request.getTask().getUniqueId() = %s;\n request.getStatus() = %s;\n mViewHolders.size() = %s;\n viewHolder.tag = %s",
//                        position,
//                        mRequests.size(),
//                        request.getTask().getFileName(),
//                        request.getTask().getUniqueId(),
//                        request.getStatus(),
//                        mViewHolders.size(),
//                        viewHolder.tag));
                viewHolder.thumbnail.setVisibility(View.INVISIBLE);
                viewHolder.progressBar.setVisibility(View.VISIBLE);
                viewHolder.thumbnail.setOnClickListener(null);
            }
            viewHolder.tag = request.getTask().getUniqueId();

            if (request.getStatus() == HLSDownloadRequest.STATUS_MERGE_COMPLETED) {
                viewHolder.progressBar.setVisibility(View.INVISIBLE);
                viewHolder.thumbnail.setVisibility(View.VISIBLE);
                Glide.with(viewHolder.title.getContext())
                        .load(request.getTask().getVideoFile())
                        .fitCenter()
                        .into(viewHolder.thumbnail);
                viewHolder.thumbnail.setOnClickListener(view -> {
                    playVideo(view.getContext(), request.getTask().getVideoFile().getAbsolutePath());
                });
            }
//            viewHolder.button.setOnClickListener(view -> {
//                if (!request.isPaused()) {
//                    request.setPaused(true);
//                    viewHolder.button.setImageResource(R.drawable.ic_action_play_arrow);
//                } else {
//                    HLSDownloadManager.getInstance(parent.getContext()).finish(request);
//                    HLSDownloadManager.getInstance(parent.getContext())
//                            .submit(request.getTask());
//                }
//
//            });
//            viewHolder.thumbnail.setBackgroundDrawable(null);
//            viewHolder.thumbnail.setBackground(null);
            return convertView;
        }

        @Override
        public void onProgress(HLSDownloadRequest hlsDownloadRequest) {
            HLSDownloadTask task = hlsDownloadRequest.getTask();
            String uniqueId = task.getUniqueId();
            for (ViewHolder viewHolder : mViewHolders) {
                if (uniqueId.equals(viewHolder.tag)) {
                    renderTask(hlsDownloadRequest, viewHolder);
                    return;
                }
            }
        }
    }

    private static class ViewHolder {
        public TextView title;
        public ProgressBar progressBar;
        public String tag;
        public ImageView thumbnail;
        //public ImageButton button;
    }
}
