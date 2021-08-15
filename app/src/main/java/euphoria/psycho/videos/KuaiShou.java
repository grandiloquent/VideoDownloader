package euphoria.psycho.videos;

import android.app.ProgressDialog;
import android.os.Process;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

public class KuaiShou extends BaseVideoExtractor<String> {
    private static Pattern MATCH_KUAISHOU = Pattern.compile("https://v\\.kuaishou(app)?\\.com(/s)?/\\S+");

    protected KuaiShou(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        String response = getString(uri, new String[][]{
                {"Cookie", "did=web_9db94f4e2b1d480198b8b2078e5b54da; didv=1628353110000"}
        });
        if (response == null) {
            return null;
        }
        return StringShare.substring(response, "\"srcNoMark\":\"", "\"");
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
        Helper.viewVideo(mMainActivity, videoUri);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_KUAISHOU.matcher(uri).find()) {
            new KuaiShou(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
