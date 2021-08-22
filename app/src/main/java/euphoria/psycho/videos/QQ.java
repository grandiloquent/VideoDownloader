package euphoria.psycho.videos;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    // data.vl.vi[0].ul.ui[0].url
    private String parseCDN(JSONObject obj) {
        try {
            JSONObject vl;
            if (obj.has("vl")) {
                vl = obj.getJSONObject("vl");
            } else {
                return null;
            }
            JSONArray vi;
            if (vl.has("vi")) {
                vi = vl.getJSONArray("vi");
            } else {
                return null;
            }
            JSONObject ul;
            if (vi.getJSONObject(0).has("ul")) {
                ul = vi.getJSONObject(0).getJSONObject("ul");
            } else {
                return null;
            }
            JSONArray ui;
            if (ul.has("ui")) {
                ui = ul.getJSONArray("ui");
            } else {
                return null;
            }
            String url;
            if (ui.getJSONObject(0).has("url")) {
                url = ui.getJSONObject(0).getString("url");
                return url;
            } else {
                return null;
            }

        } catch (Exception ignored) {
            Logger.d(String.format("parseJSON: %s", ignored));
        }
        return null;
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
        String jsonBody = StringShare.substringMax(response, "QZOutputJson=", ";");
        if (jsonBody == null) {
            Logger.d(String.format("'%s' is null.", "jsonBody"));
            return null;
        }
        JSONObject obj;
        try {
            obj = new JSONObject(jsonBody);
        } catch (JSONException e) {
            return null;
        }
        String cdn = parseCDN(obj);
        if (cdn == null) {
            Logger.d(String.format("'%s' is null.", "cdn"));
            return null;
        }
        Logger.d(String.format("fetchVideoUri: %s", cdn));
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
