package euphoria.psycho.videos;

import android.net.Uri;
import android.util.Log;
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
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

import static euphoria.psycho.videos.VideosHelper.getString;
import static euphoria.psycho.videos.VideosHelper.invokeVideoPlayer;

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
        invokeVideoPlayer(mMainActivity, Uri.parse(videoUri));
    }

    public static void fetchVideos(String url) {
        String htmlCode = getString(url, new String[][]{
                {"User-Agent", NetShare.PC_USER_AGENT}
        });
        JSONArray results = new JSONArray();
        String videoUrl = url;
        String videoTitle = StringShare.substring(htmlCode, "html5player.setVideoTitle('", "');");
        String videoThumb = StringShare.substring(htmlCode, "html5player.setThumbUrl('", "');");
        String videoDuration = StringShare.substring(htmlCode, "<meta property=\"og:duration\" content=\"", "\"");
        JSONObject video = new JSONObject();
        try {
            video.put("title", videoTitle);
            video.put("thumbnail", videoThumb);
            video.put("url", videoUrl);
            int duration = 0;
            try {
                duration = Integer.parseInt(videoDuration);
            } catch (Exception ignored) {
            }
            video.put("duration", duration);
        } catch (JSONException ignored) {
            Log.e("B5aOx2", String.format("fetchVideos, %s", ignored));
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

