package euphoria.psycho.videos;

import android.net.Uri;
import android.util.Pair;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.StringShare;

import static euphoria.psycho.videos.VideosHelper.getResponse;
import static euphoria.psycho.videos.VideosHelper.getString;
import static euphoria.psycho.videos.VideosHelper.launchDialog;

public class PornHub extends BaseExtractor<List<Pair<String, String>>> {
    private static final Pattern MATCH_PORNHUB = Pattern.compile("pornhub\\.com/view_video\\.php");

    protected PornHub(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_PORNHUB.matcher(uri).find()) {
            new PornHub(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }

    // in order to avoid calling JavaScript Engine,
    // use a dirty way to splice the query URI
    private String formatUri(String response, String uri) {
        if (response == null) {
            return null;
        }
        String javascript = StringShare.substringLine(response, "['mediaDefinitions']");
        if (javascript == null) {
            return null;
        }
        String suffix = StringShare.substringAfter(javascript, "var media_0=");
        suffix = StringShare.substringBefore(suffix, ";flashvars");
        suffix = suffix.replaceAll("/\\*[^*]+\\*/", "");
        String[] keys = suffix.split("\\+");
        StringBuilder stringBuilder = new StringBuilder();
        for (String key : keys) {
            String s = StringShare.substring(javascript, key.trim() + "=", ";");
            if (s == null) continue;
            for (int j = 0; j < s.length(); j++) {
                char character = s.charAt(j);
                if (Character.isWhitespace(character)
                        || character == '"' || character == '+') continue;
                stringBuilder.append(character);
            }
        }
        stringBuilder.append("&v=")
                .append(Uri.parse(uri).getQueryParameters("viewkey").get(0));
        return stringBuilder.toString();
    }

    @Override
    protected List<Pair<String, String>> fetchVideoUri(String uri) {
        // here we should use the user-agent of pc
        // otherwise, the html code returned by the server
        // will not contain the code that can be spliced to the query uri
        // and we also must keep the cookie passed through the set-cookie header,
        // which will be used to request the real videos
        String[] values = getResponse(uri, new String[][]{
                {"User-Agent",
                        "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36",
                }
        });
        if (values == null) {
            return null;
        }
        String response = values[0];
        uri = formatUri(response, uri);
        if (uri == null) {
            return null;
        }
        response = getString(uri, new String[][]{
                {"Cookie", values[1]}
        });
        List<Pair<String, String>> videoList = new ArrayList<>();
        try {
            JSONArray videos = new JSONArray(response);
            for (int i = 0; i < videos.length(); i++) {
                videoList.add(Pair.create(
                        videos.getJSONObject(i).getString("quality"),
                        videos.getJSONObject(i).getString("videoUrl")
                ));
            }
        } catch (Exception ignored) {
        }
        return videoList;
    }

    @Override
    protected void processVideo(List<Pair<String, String>> videoUriList) {
        try {
            launchDialog(mMainActivity, videoUriList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
