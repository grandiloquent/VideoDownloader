package euphoria.psycho.downloader;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.explorer.R;
import euphoria.psycho.share.Logger;

public class DownloaderAdapter extends BaseAdapter implements VideoTaskListener {
    private final DownloaderActivity mVideoActivity;
    private final List<DownloaderTask> mVideoTasks = new ArrayList<>();
    private final List<ViewHolder> mViewHolders = new ArrayList<>();

    public DownloaderAdapter(DownloaderActivity videoActivity) {
        mVideoActivity = videoActivity;
    }

    public void update(List<DownloaderTask> videoTasks) {
        Logger.e(String.format("update, %s", videoTasks.size()));
        mVideoTasks.clear();
        mVideoTasks.addAll(videoTasks);
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

    private static void renderCompletedStatus(Context context, ViewHolder viewHolder, DownloaderTask videoTask) {
        viewHolder.button.setImageResource(R.drawable.ic_action_play_arrow);
        viewHolder.progressBar.setProgress(100);
        viewHolder.subtitle.setText(context.getString(R.string.merge_complete));
        File videoFile = new File(
                videoTask.Directory + ".mp4"
        );
        Glide.with(context)
                .load(videoFile)
                .fitCenter()
                .into(viewHolder.thumbnail);
        viewHolder.layout.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), euphoria.psycho.player.VideoActivity.class);
            intent.setData(Uri.fromFile(videoFile));
            v.getContext().startActivity(intent);
        });
    }

    private static void renderVideoTask(Context context, ViewHolder viewHolder, DownloaderTask videoTask) {
        switch (videoTask.Status) {
            case TaskStatus.DOWNLOAD_VIDEO_FINISHED: {
                renderCompletedStatus(context, viewHolder, videoTask);
                break;
            }
            case TaskStatus.ERROR_STATUS_CODE: {
                break;
            }
            case TaskStatus.ERROR_UNKONW: {
                break;
            }
            case TaskStatus.DOWNLOADING: {
                break;
            }
            case TaskStatus.PAUSED: {
                viewHolder.button.setImageResource(R.drawable.ic_action_play_arrow);
                break;
            }
            case TaskStatus.RANGE: {
                break;
            }
            case TaskStatus.START: {
                break;
            }

        }
    }

    private static void resetViewHolderUI(Context context, ViewHolder viewHolder, DownloaderTask videoTask) {
        viewHolder.tag = videoTask.FileName;
        viewHolder.title.setText(videoTask.FileName);
        viewHolder.subtitle.setText(R.string.waiting);
        viewHolder.progressBar.setProgress(0);
        viewHolder.layout.setOnClickListener(null);
        viewHolder.thumbnail.setImageResource(R.drawable.ic_action_file_download);
        DownloaderHelper.renderPauseButton(context, viewHolder, videoTask);
    }

    @Override
    public int getCount() {
        return mVideoTasks.size();
    }

    @Override
    public DownloaderTask getItem(int position) {
        return mVideoTasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
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
        DownloaderTask videoTask = getItem(position);
        resetViewHolderUI(parent.getContext(), viewHolder, videoTask);
        renderVideoTask(parent.getContext(), viewHolder, videoTask);
        return convertView;
    }

    @Override
    public void synchronizeTask(DownloaderTask videoTask) {
        for (ViewHolder viewHolder : mViewHolders) {
            if (videoTask.FileName.equals(viewHolder.tag)) {
                renderVideoTask(mVideoActivity, viewHolder, videoTask);
                return;
            }
        }
    }

    @Override
    public void taskProgress(DownloaderTask videoTask) {
        for (ViewHolder viewHolder : mViewHolders) {
            if (videoTask.FileName.equals(viewHolder.tag)) {
                renderVideoTask(mVideoActivity, viewHolder, videoTask);
                return;
            }
        }
    }

}
