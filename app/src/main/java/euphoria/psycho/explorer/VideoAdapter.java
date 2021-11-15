package euphoria.psycho.explorer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import euphoria.psycho.explorer.VideoListActivity.Video;

public class VideoAdapter extends BaseAdapter {
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final List<Video> mVideos = new ArrayList<>();
    private final StringBuilder mStringBuilder = new StringBuilder();
    private final Formatter mFormatter = new Formatter(mStringBuilder);

    public VideoAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
    }

    public void update(List<Video> videos) {
        mVideos.clear();
        mVideos.addAll(videos);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mVideos.size();
    }

    @Override
    public Video getItem(int position) {
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
            viewHolder.duration = convertView.findViewById(R.id.duration);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.title.setText(mVideos.get(position).Filename);
        viewHolder.duration.setText(Util.getStringForTime(mStringBuilder, mFormatter, mVideos.get(position).Duration));
        Glide
                .with(mContext)
                .load(new File(mVideos.get(position).Directory,
                        mVideos.get(position).Filename))
                .centerCrop()
                //.placeholder(R.drawable.loading_spinner)
                .into(viewHolder.thumbnail);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public static class ViewHolder {
        public TextView title;
        public ImageView thumbnail;
        public TextView duration;
    }
}