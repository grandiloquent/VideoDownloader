package euphoria.psycho.videos;

import android.net.Uri;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.StringShare;

import static euphoria.psycho.videos.VideosHelper.invokeVideoPlayer;

public class Ck52 extends BaseExtractor<String> {
    private static Pattern MATCH_52CK = Pattern.compile("/vodplay/[\\d-]+\\.html");

    protected Ck52(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        String response = VideosHelper.getString(uri, null);
        if (response == null) {
            return null;
        }
        String videoUri = StringShare.substring(response, "\"link_pre\":\"\",\"url\":\"", "\",\"");
        if (videoUri == null) {
            return null;
        }
        videoUri = videoUri.replace("\\/", "/");
        return videoUri;
    }

    @Override
    protected void processVideo(String videoUri) {
        invokeVideoPlayer(mMainActivity, Uri.parse(videoUri));
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_52CK.matcher(uri).find()) {
            new Ck52(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}