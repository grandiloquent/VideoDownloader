package euphoria.psycho.videos;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.player.VideoActivity;
import euphoria.psycho.share.KeyShare;

public class Iqiyi extends BaseExtractor<String[]> {
    public static Pattern MATCH_IQIYI = Pattern.compile("\\.iqiyi\\.com/v_");

    public Iqiyi(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String[] fetchVideoUri(String uri) {
        return Native.fetchIqiyi(uri);
    }

    @Override
    protected void processVideo(String[] videoUris) {
        for (int i = 0; i < videoUris.length; i++) {
            Log.e("B5aOx2", String.format("processVideo, %s", videoUris[i]));
        }
        Intent intent = new Intent(mMainActivity, euphoria.psycho.player.VideoActivity.class);
        intent.putExtra(VideoActivity.EXTRA_PLAYLSIT, videoUris);
        mMainActivity.startActivity(intent);
    }


}
