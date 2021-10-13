package euphoria.psycho.videos;

import android.content.Intent;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.player.IqiyiActivity;
import euphoria.psycho.share.PreferenceShare;

public class Tencent extends BaseExtractor<String[]> {
    private static final Pattern MATCH_QQ = Pattern.compile("qq\\.com");

    protected Tencent(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_QQ.matcher(uri).find()) {
            new Tencent(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }


    @Override
    protected String[] fetchVideoUri(String uri) {
        return Native.fetchTencent(uri,
                PreferenceShare.getPreferences().getString("key_tencent", null));
    }


    @Override
    protected void processVideo(String[] videoUris) {
        Intent intent = new Intent(mMainActivity, IqiyiActivity.class);
        intent.putExtra(IqiyiActivity.EXTRA_PLAYLSIT, videoUris);
        intent.putExtra(IqiyiActivity.EXTRA_TYPE, true);
        mMainActivity.startActivity(intent);
    }
}
