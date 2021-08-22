package euphoria.psycho.videos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

public class QQ extends BaseVideoExtractor<String> {
    private static Pattern MATCH_QQ = Pattern.compile("qq\\.com");

    protected QQ(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        String vid = getVid(uri);
        if (vid == null) return null;
        uri = String.format("http://vv.video.qq.com/getinfo?otype=json&platform=11&defnpayver=1&appver=%s&defn=%s&vid=%s",
                "3.2.19.333", "shd", vid);
        String response = getString(uri, null);
        if (response == null) {
            Logger.d(String.format("'%s' is null.", "response"));
            return null;
        }
        String jsonBody = StringShare.substring(response, "QZOutputJson=", ";");
        if (jsonBody == null) {
            Logger.d(String.format("'%s' is null.", "jsonBody"));
            return null;
        }
        Logger.d(String.format("fetchVideoUri: %s", jsonBody));
        return null;
    }

    private String getVid(String uri) {
        String vid = StringShare.substringLeast(uri, "/", ".html");
        if (vid.length() == 11) {
            return vid;
        }
        String response = getString(uri, null);
        if (response == null) {
            Logger.d(String.format("'%s' is null.", "response"));
            return null;
        }
        String[] patterns = new String[]{
                "vid=(\\w+)",
                "vid:\\s*[\"'](\\w+)",
                "vid\\s*=\\s*[\"']\\s*(\\w+)"
        };
        for (int i = 0; i < 3; i++) {
            Pattern pattern = Pattern.compile(patterns[i]);
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                vid = matcher.group(1);
                break;
            }
        }
        if (vid == null) {
            Logger.d(String.format("'%s' is null.", "vid"));
            return null;
        }
        return vid;
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(String videoUri) {
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_QQ.matcher(uri).find()) {
            new QQ(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
// https://v.qq.com/x/cover/k16928rkrk217zb/z00401l30ys.html
