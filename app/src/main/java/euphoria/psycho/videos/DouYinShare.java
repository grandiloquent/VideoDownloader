package euphoria.psycho.videos;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;

public class DouYinShare extends BaseVideoExtractor {

    public static Pattern MATCH_VIDEO_ID = Pattern.compile("(?<=douyin.com/).+(?=/)");

    public DouYinShare(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    private String getLocation(String videoId) throws IOException {
        URL url = new URL("https://v.douyin.com/" + videoId);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", BaseVideoExtractor.USER_AGENT);
        urlConnection.setInstanceFollowRedirects(false);
        return urlConnection.getHeaderField("Location");
    }

    private String getRealVideoUri(String videoId) {
        try {
            String videoUri = getLocation(videoId);
            Pattern pattern = Pattern.compile("video/(\\d+)");
            Matcher matcher = pattern.matcher(videoUri);
            if (matcher.find()) {
                return "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=" + matcher.group(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    protected String fetchVideoUri(String shareId) {
        if (shareId == null) {
            return null;
        }
        String realVideoUri = getRealVideoUri(shareId);
        if (realVideoUri == null) {
            return null;
        }
        try {
            URL url = new URL(realVideoUri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", BaseVideoExtractor.USER_AGENT);
            int code = urlConnection.getResponseCode();
            if (code < 400 && code >= 200) {
                String response = NetShare.readString(urlConnection);
                if (response == null) {
                    return null;
                }
                JSONObject jsonObject = new JSONObject(response);
                JSONArray itemList = jsonObject.getJSONArray("item_list");
                jsonObject = itemList.getJSONObject(0).getJSONObject("video");
                jsonObject = jsonObject.getJSONObject("play_addr");
                itemList = jsonObject.getJSONArray("url_list");
                return itemList.getString(0).replace("playwm", "play");

            } else {
                return null;
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    @Override
    protected String processUri(String inputUri) {
        Matcher matcher = MATCH_VIDEO_ID.matcher(inputUri);
        if (matcher.find()) return matcher.group();
        return null;
    }
} //
