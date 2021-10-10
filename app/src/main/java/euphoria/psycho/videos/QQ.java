package euphoria.psycho.videos;

import android.content.Intent;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.player.IqiyiActivity;
import euphoria.psycho.player.VideoActivity;

public class QQ extends BaseExtractor<String[]> {
    private static final Pattern MATCH_QQ = Pattern.compile("qq\\.com");

    protected QQ(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_QQ.matcher(uri).find()) {
            new QQ(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }


    @Override
    protected String[] fetchVideoUri(String uri) {
        return Native.fetchQQ(uri);
    }


    @Override
    protected void processVideo(String[] videoUris) {
        Intent intent = new Intent(mMainActivity, IqiyiActivity.class);
        intent.putExtra(VideoActivity.EXTRA_PLAYLSIT, videoUris);
        mMainActivity.startActivity(intent);
    }
}
