package euphoria.psycho.videos;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.player.VideoActivity;

public class MgTv extends BaseExtractor<String> {
    private static Pattern MATCH_MGTV = Pattern.compile("mgtv\\.com/[a-z]/\\d+/(\\d+)\\.html");

    protected MgTv(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_MGTV.matcher(uri).find()) {
            new MgTv(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }

    @Override
    protected String fetchVideoUri(String videoId) {
        return Native.fetchMangoTV(videoId);
    }

    @Override
    protected void processVideo(String videoUri) {
        if (videoUri.length() == 0) {
            Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(mMainActivity, VideoActivity.class);
        intent.setData(Uri.parse(videoUri));
        intent.putExtra(VideoActivity.EXTRA_HEADER, 1);
        mMainActivity.startActivity(intent);
    }
}
