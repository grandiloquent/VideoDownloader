package euphoria.psycho.explorer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import euphoria.psycho.share.VideoShare;

public class VideoAdapter extends BaseAdapter {
    private List<File> mVideos = new ArrayList<>();
    private final LayoutInflater mInflater;

    public VideoAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mVideos.size();
    }

    @Override
    public Object getItem(int position) {
        return mVideos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.video_row, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.title = convertView.findViewById(R.id.title);
            viewHolder.thumbnail = convertView.findViewById(R.id.thumbnail);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.title.setText(mVideos.get(position).getName());
        viewHolder.thumbnail.setImageBitmap(VideoShare.createVideoThumbnail(mVideos.get(position).getAbsolutePath()));
        return convertView;
    }

    public void update(List<File> videos) {
        mVideos.clear();
        mVideos.addAll(videos);
        notifyDataSetChanged();
    }

    public class ViewHolder {
        public TextView title;
        public ImageView thumbnail;
    }
}