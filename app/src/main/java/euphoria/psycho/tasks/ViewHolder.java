package euphoria.psycho.tasks;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import euphoria.psycho.explorer.MovieActivity;

public class ViewHolder implements VideoTaskListener, LifeCycle {
    public TextView title;
    public TextView subtitle;
    public ProgressBar progressBar;
    private final VideoTask mVideoTask;
    private final VideoActivity mVideoActivity;
    public View layout;

    public ViewHolder(VideoActivity videoActivity, VideoTask videoTask) {
        mVideoTask = videoTask;
        mVideoActivity = videoActivity;
    }

    @Override
    public void onDestroy() {
        VideoManager.getInstance().removeVideoTaskListener(this);
        mVideoActivity.removeLifeCycle(this);
    }

    @Override
    public void synchronizeTask(VideoTask videoTask) {
        if (mVideoTask.Id != videoTask.Id) return;
        renderVideoTask(this, videoTask);
    }

    public static void renderVideoTask(ViewHolder viewHolder, VideoTask videoTask) {
        if (videoTask.Status == TaskStatus.MERGE_VIDEO) {
            viewHolder.title.setText(videoTask.FileName);
            viewHolder.subtitle.setText("合并开始");
        } else if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
            viewHolder.title.setText(videoTask.FileName);
            viewHolder.subtitle.setText("合并完成");
            viewHolder.layout.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), MovieActivity.class);
                intent.setData(Uri.fromFile(new File(
                        videoTask.Directory + ".mp4"
                )));
                v.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public void taskProgress(VideoTask videoTask) {
        if (mVideoTask.Id != videoTask.Id) return;
        title.setText(videoTask.FileName);
        subtitle.setText(String.format("%s/%s",
                videoTask.DownloadedFiles,
                videoTask.TotalFiles));
        progressBar.setProgress((int) ((videoTask.DownloadedFiles * 1.0 / videoTask.TotalFiles) * 100));
    }

    @Override
    public void taskStart(VideoTask videoTask) {
        if (mVideoTask.Id != videoTask.Id) return;
        title.setText(videoTask.Uri);
    }
}
