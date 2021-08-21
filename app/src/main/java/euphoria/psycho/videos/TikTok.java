package euphoria.psycho.videos;

import android.os.Environment;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

public class TikTok extends BaseVideoExtractor<String> {
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
        }
        return null;
    }

    private String getLocation(String uri) throws IOException {
        String location = getLocation(uri, null);
        location = getLocation(location, null);
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
/*




<input name="__RequestVerificationToken" type="hidden" value="CfDJ8AsAY-udqeNCkc5nV3bkKO2J0OPaUkfzZAyIaJglrlnv9FpkL9ik4Lr_cKPaPMacHwj4-635XuV8ASwbVk4WWYodqkKiqcjsYy02OrECUiiS2xa6Isug4kHQ_J7wkBa9GTWYt-MtA2xTF8U_GpzaQog">
 */