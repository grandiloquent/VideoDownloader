package euphoria.psycho.videos;

import android.content.Intent;
import android.widget.Toast;

import java.util.regex.Pattern;

import euphoria.psycho.PlayerActivity;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;

import static euphoria.psycho.PlayerActivity.KEY_WEB_VIDEO;

public class CCTV extends BaseExtractor<String> {
    private static Pattern MATCH_CCTV = Pattern.compile("tv\\.cctv\\.com/");

    protected CCTV(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_CCTV.matcher(uri).find()) {
            new CCTV(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }

    @Override
    protected String fetchVideoUri(String videoId) {
        return Native.fetchCCTV(videoId);
    }

    @Override
    protected void processVideo(String videoUri) {
        if (videoUri.length() == 0) {
            Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(mMainActivity, PlayerActivity.class);
        intent.putExtra(KEY_WEB_VIDEO, videoUri);
        mMainActivity.startActivity(intent);

    }
}
