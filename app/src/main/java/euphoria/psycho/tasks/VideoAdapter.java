package euphoria.psycho.tasks;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.explorer.R;

public class VideoAdapter extends BaseAdapter implements VideoTaskListener {
    private final List<VideoTask> mVideoTasks = new ArrayList<>();
    private final VideoActivity mVideoActivity;

    public VideoAdapter(VideoActivity videoActivity) {
        mVideoActivity = videoActivity;
    }

    @Override
    public void synchronizeTask(VideoTask videoTask) {
        for (ViewHolder viewHolder : mViewHolders) {
            if (videoTask.FileName.equals(viewHolder.tag)) {
                renderVideoTask(viewHolder, videoTask);
                return;
            }
        }


    }

    public static void renderVideoTask(ViewHolder viewHolder, VideoTask videoTask) {
        if (videoTask.Status == TaskStatus.MERGE_VIDEO) {
            viewHolder.title.setText(videoTask.FileName);
            viewHolder.subtitle.setText("合并开始");
        } else if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
            viewHolder.title.setText(videoTask.FileName);
            viewHolder.subtitle.setText("合并完成");
            viewHolder.layout.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), euphoria.psycho.player.VideoActivity.class);
                intent.setData(Uri.fromFile(new File(
                        videoTask.Directory + ".mp4"
                )));
                v.getContext().startActivity(intent);
            });

        } else {
            viewHolder.title.setText(videoTask.FileName);
            viewHolder.subtitle.setText(String.format("%s/%s",
                    videoTask.DownloadedFiles,
                    videoTask.TotalFiles));
            viewHolder.progressBar.setProgress((int) ((videoTask.DownloadedFiles * 1.0 / videoTask.TotalFiles) * 100));
        }
    }

    @Override
    public void taskProgress(VideoTask videoTask) {
    }

    @Override
    public void taskStart(VideoTask videoTask) {
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

    private List<ViewHolder> mViewHolders = new ArrayList<>();

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
            mViewHolders.add(viewHolder);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        VideoTask videoTask = getItem(position);
        viewHolder.tag = videoTask.FileName;
        viewHolder.title.setText(videoTask.FileName);
        if (videoTask.TotalFiles > 0 && videoTask.DownloadedFiles == videoTask.TotalFiles) {
            viewHolder.progressBar.setVisibility(View.INVISIBLE);
            viewHolder.subtitle.setText("合并完成");
            viewHolder.layout.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), euphoria.psycho.player.VideoActivity.class);
                intent.setData(Uri.fromFile(new File(
                        videoTask.Directory + ".mp4"
                )));
                v.getContext().startActivity(intent);
            });
        } else {
            viewHolder.progressBar.setVisibility(View.VISIBLE);
            viewHolder.subtitle.setText(String.format("%s/%s",
                    videoTask.DownloadedFiles,
                    videoTask.TotalFiles));
            viewHolder.progressBar.setProgress((int) ((videoTask.DownloadedFiles * 1.0 / videoTask.TotalFiles) * 100));
        }
        return convertView;
    }
}
