package euphoria.psycho.videos;

import android.net.Uri;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;

import static euphoria.psycho.videos.VideosHelper.invokeVideoPlayer;

public class KuaiShou extends BaseExtractor<String> {
    private static final Pattern MATCH_KUAISHOU = Pattern.compile("https://v\\.kuaishou(app)?\\.com(/s)?/\\S+");

    protected KuaiShou(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        return Native.fetchKuaiShou(uri);
    }

    @Override
    protected String processUri(String inputUri) {
        Matcher matcher = MATCH_KUAISHOU.matcher(inputUri);
        if (matcher.find())
            return matcher.group();
        return null;
    }

    @Override
    protected void processVideo(String videoUri) {
        invokeVideoPlayer(mMainActivity, Uri.parse(videoUri));
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_KUAISHOU.matcher(uri).find()) {
            new KuaiShou(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
