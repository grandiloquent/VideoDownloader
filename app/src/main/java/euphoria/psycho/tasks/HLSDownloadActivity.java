package euphoria.psycho.tasks;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import euphoria.psycho.explorer.R;

public class HLSDownloadActivity extends Activity implements HLSDownloadRequestListener {

    private View mProgressBar;
    private ListView mListView;
    private HLSDownloadAdapter mVideoAdapter;

    @Override
    public void onSubmit(HLSDownloadRequest request) {

    }

    @Override
    public void onFinish(HLSDownloadRequest request) {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_s);
        mProgressBar = findViewById(R.id.progress_bar);
        mListView = findViewById(R.id.list_view);
        mVideoAdapter = new HLSDownloadAdapter();
        mListView.setAdapter(mVideoAdapter);
    }

    private class HLSDownloadAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            return null;
        }
    }
}
