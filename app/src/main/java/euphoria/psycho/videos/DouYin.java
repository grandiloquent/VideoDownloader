package euphoria.psycho.videos;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;

public class DouYin extends BaseVideoExtractor<String> {

    private String mVideoId;

    public DouYin(String inputUri, MainActivity mainActivity) {
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
        mVideoId = videoId;
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
            String response = getString(realVideoUri, null);
            if (response == null) {
                return null;
            }
            JSONObject jsonObject = new JSONObject(response);
            JSONArray itemList = jsonObject.getJSONArray("item_list");
            jsonObject = itemList.getJSONObject(0).getJSONObject("video");
            jsonObject = jsonObject.getJSONObject("play_addr");
            itemList = jsonObject.getJSONArray("url_list");
            return itemList.getString(0).replace("playwm", "play");
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    protected void processVideo(String videoUri) {
        mMainActivity.getWebView().loadUrl(videoUri);
        Helper.openDownloadDialog(mMainActivity, mVideoId, videoUri);
    }

    @Override
    protected String processUri(String inputUri) {
        Pattern pattern = Pattern.compile("(?<=douyin.com/).+(?=/)");
        Matcher matcher = pattern.matcher(inputUri);
        if (matcher.find()) return matcher.group();
        return null;
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("(?<=douyin.com/).+(?=/)");
        if (pattern.matcher(uri).find()) {
            new DouYin(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
