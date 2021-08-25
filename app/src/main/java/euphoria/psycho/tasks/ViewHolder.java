package euphoria.psycho.tasks;

import android.widget.ProgressBar;
import android.widget.TextView;

public class ViewHolder implements VideoTaskListener, LifeCycle {
    public TextView title;
    public TextView subtitle;
    public ProgressBar progressBar;
    private VideoTask mVideoTask;
    private final VideoActivity mVideoActivity;

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
        if (videoTask.Status == TaskStatus.MERGE_VIDEO) {
            title.setText(mVideoTask.FileName);
            subtitle.setText("合并开始");
        } else if (videoTask.Status == TaskStatus.MERGE_VIDEO_FINISHED) {
            title.setText(mVideoTask.FileName);
            subtitle.setText("合并完成");
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
