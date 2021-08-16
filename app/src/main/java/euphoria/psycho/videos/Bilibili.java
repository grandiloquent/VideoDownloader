package euphoria.psycho.videos;

import org.json.JSONArray;
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
            return String.format("https://api.bilibili.com/x/player/playurl?avid=%s&cid=%s&bvid=%s&qn=120&type=&otype=json&fourk=1&fnver=0&fnval=16", aid, cid, bvid);

        } catch (JSONException e) {
            Logger.d(String.format("formatQueryUri: %s", e.getMessage()));
        }
        return null;
    }

    private String extractUrl(String jsonBody) {
        try {
            JSONObject obj = new JSONObject(jsonBody);
            JSONObject data;
            if (obj.has("data")) {
                data = obj.getJSONObject("data");
            } else {
                return null;
            }
            JSONObject dash;
            if (data.has("dash")) {
                dash = data.getJSONObject("dash");
            } else {
                return null;
            }
            JSONArray video;
            if (dash.has("video")) {
                video = dash.getJSONArray("video");
            } else {
                return null;
            }
            String baseUrl;
            if (video.getJSONObject(0).has("baseUrl")) {
                baseUrl = video.getJSONObject(0).getString("baseUrl");
                return baseUrl;
            } else {
                return null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    protected String fetchVideoUri(String uri) {
       
        String response = getString(uri, new String[][]{
                {"Accept-Encoding", "gzip"},
                {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36"}
        });
        if (response == null) {
            return null;
        }
        String jsonString = StringShare.substring(response, "window.__INITIAL_STATE__=", ";(function()");
        if (jsonString == null) {
            return null;
        }
        String queryUri = formatQueryUri(jsonString);
        if (queryUri == null) {
            return null;
        }
        String jsonBody = getString(queryUri, null);
        return extractUrl(jsonBody);
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(String videoUri) {
        Logger.d(String.format("processVideo: %s", videoUri));
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_BILIBILI.matcher(uri).find()) {
            new Bilibili(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
