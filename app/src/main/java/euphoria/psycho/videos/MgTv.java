package euphoria.psycho.videos;

import android.content.Intent;
import android.widget.Toast;

import java.util.regex.Pattern;

import euphoria.psycho.PlayerActivity;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;

import static euphoria.psycho.PlayerActivity.KEY_M3U8;
import static euphoria.psycho.PlayerActivity.KEY_REQUEST_HEADERS;
import static euphoria.psycho.PlayerActivity.KEY_WEB_VIDEO;

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
        Intent intent = new Intent(mMainActivity, PlayerActivity.class);
        intent.putExtra(KEY_WEB_VIDEO, videoUri);
        intent.putExtra(KEY_M3U8, true);
        intent.putExtra(KEY_REQUEST_HEADERS, 1);
        mMainActivity.startActivity(intent);
//        Intent intent = new Intent(mMainActivity, VideoActivity.class);
//        intent.setData(Uri.parse(videoUri));
//        intent.putExtra(VideoActivity.EXTRA_HEADER, 1);
//        mMainActivity.startActivity(intent);
    }
}
