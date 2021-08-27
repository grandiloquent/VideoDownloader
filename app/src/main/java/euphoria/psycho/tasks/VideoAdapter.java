package euphoria.psycho.tasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.explorer.R;

public class VideoAdapter extends BaseAdapter {
    private final List<VideoTask> mVideoTasks = new ArrayList<>();
    private final VideoActivity mVideoActivity;

    public VideoAdapter(VideoActivity videoActivity) {
        mVideoActivity = videoActivity;
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_item, parent, false);
            viewHolder = new ViewHolder(mVideoActivity, getItem(position));
            mVideoActivity.addLifeCycle(viewHolder);
            viewHolder.layout = convertView;
            viewHolder.title = convertView.findViewById(R.id.title);
            viewHolder.subtitle = convertView.findViewById(R.id.subtitle);
            viewHolder.progressBar = convertView.findViewById(R.id.progress_bar);
            VideoManager.getInstance().addVideoTaskListener(viewHolder);
            viewHolder.subtitle.setText("");
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        ViewHolder.renderVideoTask(viewHolder, getItem(position));
        return convertView;
    }
}
