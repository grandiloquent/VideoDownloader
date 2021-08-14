package euphoria.psycho.videos;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.NetShare;

public class DouYinShare extends BaseVideoExtractor {

    public static Pattern MATCH_VIDEO_ID = Pattern.compile("(?<=douyin.com/).+(?=/)");

    public DouYinShare(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    private String getRealVideoUri(String uri) {
        try {
            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", BaseVideoExtractor.USER_AGENT);
            int code = urlConnection.getResponseCode();
            if (code < 400 && code >= 200) {
                String response = NetShare.readString(urlConnection);
                if (response == null) {
                    return null;
                }
                Pattern pattern = Pattern.compile("video/(\\d+)");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    return "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=" + matcher.group(1);
                }
            } else {
                return null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    protected boolean checkUri(String inputUri) {
        return MATCH_VIDEO_ID.matcher(inputUri).find();
    }

    @Override
    protected String fetchVideoUri(String uri) {
        if (uri == null) {
            return null;
        }
        String realVideoUri = getRealVideoUri(uri);
        if (realVideoUri == null) {
            return null;
        }
        try {
            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", BaseVideoExtractor.USER_AGENT);
            urlConnection.setRequestProperty("Referer", "https://www.iesdouyin.com/");
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
