package euphoria.psycho.tasks;

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

public class VideoAdapter extends BaseAdapter implements VideoTaskListener {
    private final VideoActivity mVideoActivity;
    private final List<VideoTask> mVideoTasks = new ArrayList<>();
    private List<ViewHolder> mViewHolders = new ArrayList<>();

    public VideoAdapter(VideoActivity videoActivity) {
        mVideoActivity = videoActivity;
    }

    public static void renderVideoTask(Context context, ViewHolder viewHolder, VideoTask videoTask) {
        if (videoTask.Status == TaskStatus.MERGE_VIDEO) {
            viewHolder.title.setText(videoTask.FileName);
            viewHolder.subtitle.setText("合并开始");
        } else if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
            renderCompletedStatus(context, viewHolder, videoTask);

        } else {
            viewHolder.title.setText(videoTask.FileName);
            viewHolder.subtitle.setText(String.format("%s/%s",
                    videoTask.DownloadedFiles,
                    videoTask.TotalFiles));
            viewHolder.progressBar.setProgress((int) ((videoTask.DownloadedFiles * 1.0 / videoTask.TotalFiles) * 100));
        }
    }

    public void update(List<VideoTask> videoTasks) {
        mVideoTasks.clear();
        mVideoTasks.addAll(videoTasks);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mVideoTasks.size();
    }

    @Override
    public VideoTask getItem(int position) {
        return mVideoTasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public static void renderCompletedStatus(Context context, ViewHolder viewHolder, VideoTask videoTask) {
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.layout = convertView;
            viewHolder.title = convertView.findViewById(R.id.title);
            viewHolder.subtitle = convertView.findViewById(R.id.subtitle);
            viewHolder.progressBar = convertView.findViewById(R.id.progress_bar);
            viewHolder.thumbnail = convertView.findViewById(R.id.thumbnail);
            mViewHolders.add(viewHolder);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        VideoTask videoTask = getItem(position);
        viewHolder.tag = videoTask.FileName;
        viewHolder.title.setText(videoTask.FileName);
        if (videoTask.TotalFiles > 0 && videoTask.DownloadedFiles == videoTask.TotalFiles) {
            renderCompletedStatus(parent.getContext(), viewHolder, videoTask);
        } else {
            viewHolder.subtitle.setText(String.format("%s/%s",
                    videoTask.DownloadedFiles,
                    videoTask.TotalFiles));
            viewHolder.progressBar.setProgress((int) ((videoTask.DownloadedFiles * 1.0 / videoTask.TotalFiles) * 100));
        }
        return convertView;
    }

    @Override
    public void synchronizeTask(VideoTask videoTask) {
        for (ViewHolder viewHolder : mViewHolders) {
            if (videoTask.FileName.equals(viewHolder.tag)) {
                renderVideoTask(mVideoActivity, viewHolder, videoTask);
                return;
            }
        }


    }

    @Override
    public void taskProgress(VideoTask videoTask) {
    }


}
