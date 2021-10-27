package euphoria.psycho.videos;

import android.util.Log;
import android.widget.Toast;

import java.util.regex.Pattern;

import euphoria.psycho.PlayerActivity;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.share.StringShare;

public class PornOne extends BaseExtractor<String> {
    private static final Pattern MATCH_PORNONE = Pattern.compile("pornone\\.com/.+/.+/\\d{5,}");

    protected PornOne(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        return Native.fetchPornOne(StringShare.substringAfter(uri, "pornone.com"));
    }

    @Override
    protected void processVideo(String videoUri) {
        if (videoUri.length() == 0) {
            Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
            return;
        }
        PlayerActivity.launchActivity(mMainActivity, videoUri, false);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_PORNONE.matcher(uri).find()) {
            new PornOne(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
