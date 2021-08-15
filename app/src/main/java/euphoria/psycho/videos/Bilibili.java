package euphoria.psycho.videos;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

public class Bilibili extends BaseVideoExtractor<String> {
    private static Pattern MATCH_BILIBILI = Pattern.compile("bilibili\\.com/.+");

    protected Bilibili(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    private String formatQueryUri(String jsonString) {
        try {
            JSONObject object = new JSONObject(jsonString);
            JSONObject videoData = object.getJSONObject("videoData");
            String aid;
            if (videoData.has("aid")) {
                aid = videoData.getString("aid");
            } else {
                return null;
            }
            String cid;
            if (videoData.has("cid")) {
                cid = videoData.getString("cid");
            } else {
                return null;
            }
            String bvid;
            if (videoData.has("bvid")) {
                bvid = videoData.getString("bvid");
            } else {
                return null;
            }
            return String.format("/x/player/playurl?avid=%s&cid=%s&bvid=%s&qn=120&type=&otype=json&fourk=1&fnver=0&fnval=16", aid, cid, bvid);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected String fetchVideoUri(String uri) {
        String response = getString(uri, null);
        if (response == null) {
            return null;
        }
        String jsonString = StringShare.substring(response, "window.__INITIAL_STATE__=", ";(function()");
        Logger.d(String.format("fetchVideoUri: %s", jsonString));
        
        if (jsonString == null) {
            return null;
        }
        String queryUri = formatQueryUri(jsonString);
        if (queryUri == null) {
            return null;
        }
        Logger.d(String.format("fetchVideoUri: %s", queryUri));
        return null;
    }

    @Override
    protected String processUri(String inputUri) {
        return null;
    }

    @Override
    protected void processVideo(String videoUri) {
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_BILIBILI.matcher(uri).find()) {
            new Bilibili(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
