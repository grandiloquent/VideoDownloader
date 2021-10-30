package euphoria.psycho.videos;

import android.content.Intent;
import android.text.Html;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.explorer.WebActivity;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

import static euphoria.psycho.videos.VideosHelper.getString;

public class XVideos extends BaseExtractor<String> {

    public XVideos(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("xvideos\\.com/video\\d+");
        if (pattern.matcher(uri).find()) {
            new XVideos(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }

    @Override
    protected String fetchVideoUri(String uri) {
        return Native.fetchXVideos(uri);
    }

    @Override
    protected void processVideo(String videoUri) {
        if (videoUri.length() == 0) {
            Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
            return;
        }
        Intent starter = new Intent(mMainActivity, WebActivity.class);
        starter.putExtra("extra.URI", videoUri);
        mMainActivity.startActivity(starter);
    }

    public static void fetchVideos(String url) {
        if (url.contains("/0/")) return;
        String htmlCode = getString(url, new String[][]{
                {"User-Agent", NetShare.PC_USER_AGENT}
        });
        if (htmlCode == null) {
            return;
        }
        JSONArray results = new JSONArray();
        String videoTitle = StringShare.substring(htmlCode, "html5player.setVideoTitle('", "');");
        String videoThumb = StringShare.substring(htmlCode, "html5player.setThumbUrl('", "');");
        String videoDuration = StringShare.substring(htmlCode, "<meta property=\"og:duration\" content=\"", "\"");
        JSONObject video = new JSONObject();
        try {
            video.put("title", Html.fromHtml(videoTitle).toString());
            video.put("thumbnail", videoThumb);
            video.put("url", url);
            int duration = 0;
            try {
                if (videoDuration != null) {
                    duration = Integer.parseInt(videoDuration);
                }
            } catch (Exception ignored) {
            }
            video.put("duration", duration);
        } catch (JSONException ignored) {
        }
        results.put(video);
        try {
            URL uri = new URL("http://47.106.105.122/api/video");
            HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            OutputStream out = connection.getOutputStream();
            out.write(results.toString().getBytes(StandardCharsets.UTF_8));
            out.close();
            int code = connection.getResponseCode();
        } catch (IOException ignored) {
        }


    }
}

