package euphoria.psycho;

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
    }

    @Override
    public void taskProgress(VideoTask videoTask) {
        if (mVideoTask.Id != videoTask.Id) return;
        subtitle.setText(String.format("%s/%s",
                videoTask.DownloadedFiles,
                videoTask.TotalFiles));
    }

    @Override
    public void taskStart(VideoTask videoTask) {
        if (mVideoTask.Id != videoTask.Id) return;
        title.setText(videoTask.FileName);
    }
}
