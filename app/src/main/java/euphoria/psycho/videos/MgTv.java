package euphoria.psycho.videos;

import android.net.Uri;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.share.Logger;

import static euphoria.psycho.videos.VideosHelper.getString;
import static euphoria.psycho.videos.VideosHelper.invokeVideoPlayer;

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
        invokeVideoPlayer(mMainActivity, Uri.parse(videoUri));
    }
}
