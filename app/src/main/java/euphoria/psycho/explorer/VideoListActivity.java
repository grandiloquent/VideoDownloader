package euphoria.psycho.explorer;

import android.app.Activity;
import android.os.Bundle;

public class VideoListActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    private void initialize() {
        setContentView(R.layout.activity_video_list);
    }
}
