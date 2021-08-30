package euphoria.psycho.videos;

import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

import static euphoria.psycho.videos.VideosHelper.getLocationAddCookie;
import static euphoria.psycho.videos.VideosHelper.getResponse;
import static euphoria.psycho.videos.VideosHelper.postFormUrlencoded;

public class TikTok extends BaseExtractor<String> {
    private static Pattern MATCH_TIKTOK_SHARE = Pattern.compile("https://[a-zA-Z]{2}\\.tiktok\\.com/\\w+/");
    private static Pattern MATCH_TIKTOK_PC = Pattern.compile("https://www\\.tiktok\\.com/@\\w+/video/\\d{19,}");

    /*

     https://www.tiktok.com/@travelscenerykj/video/6990367736601922822?_d=secCgYIASAHKAESMgow8IvJAxm6lOFVr3hQ47oJ%2BnLNPLIJv4bW%2BvT1mpRYtxRSJAtjIvS
     */
    protected TikTok(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        try {
            String location = getLocation(uri);
            if (location == null) {
                return null;
            }
            String[] response = getResponse("https://dltik.com/?hl=en", null);
            Pattern pattern = Pattern.compile("(?<=<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\")[^\"]+(?=\")");
            Matcher matcher = pattern.matcher(response[0]);
            if (!matcher.find()) return null;
            String jsonBody = postFormUrlencoded("https://dltik.com/?hl=en", new String[][]{
                    {"Cookie", StringShare.substringBefore(response[1], ";")}
            }, new String[][]{{"m", "getlink"}, {"url", location}, {"__RequestVerificationToken", matcher.group()},
            });
            JSONObject obj = new JSONObject(jsonBody);
            JSONObject data;
            if (obj.has("data")) {
                data = obj.getJSONObject("data");
            } else {
                return null;
            }
            String destinationUrl;
            if (data.has("destinationUrl")) {
                return data.getString("destinationUrl");
            } else {
                return null;
            }
        } catch (Exception exception) {
            Logger.d(String.format("fetchVideoUri: %s", exception));

        }
        return null;
    }

    private String getLocation(String uri) throws IOException {
        String[] response = getLocationAddCookie(uri, null);
        String location = VideosHelper.getLocation(response[0], new String[][]{
                {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36"},
                {"Cookie", response[1]}
        });
        return location;
    }


    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(String videoUri) {
        mMainActivity.getWebView().loadUrl(videoUri);
        try {
            Helper.openDownloadDialog(mMainActivity, KeyShare.toHex(KeyShare.md5encode(videoUri)), videoUri);
        } catch (Exception exception) {
        }
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_TIKTOK_SHARE.matcher(uri).find()
                || MATCH_TIKTOK_PC.matcher(uri).find()) {
            new TikTok(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
