package euphoria.psycho.videos;

import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;

import static euphoria.psycho.videos.VideosHelper.getString;
import static euphoria.psycho.videos.VideosHelper.launchDialog;

public class XVideos extends BaseExtractor<List<Pair<String, String>>> {

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
    //  Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36

    private void parseHls(String hlsUri, List<Pair<String, String>> videoList) {
        String hls = getString(hlsUri, null);
        if (hls == null) return;
        String[] pieces = hls.split("\n");
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].startsWith("#EXT-X-STREAM-INF")) {
                String name = StringShare.substring(pieces[i], "NAME=\"", "\"");
                String url = StringShare.substringBeforeLast(hlsUri, "/") + "/" + pieces[i + 1];
                videoList.add(Pair.create(name, url));
                i++;
            }
        }

    }

    @Override
    protected List<Pair<String, String>> fetchVideoUri(String uri) {
        List<Pair<String, String>> videoList = new ArrayList<>();
        String htmlCode = getString(uri, new String[][]{
                {"User-Agent", NetShare.PC_USER_AGENT}
        });
        if (htmlCode == null) return null;
        String low = StringShare.substring(htmlCode, "html5player.setVideoUrlLow('", "'");
        if (low != null) {
            videoList.add(Pair.create("标清", low));
        }
        String high = StringShare.substring(htmlCode, "html5player.setVideoUrlHigh('", "'");
        if (high != null) {
            videoList.add(Pair.create("高清", high));
        }
        String hls = StringShare.substring(htmlCode, "html5player.setVideoHLS('", "'");
        if (hls != null) {
            parseHls(hls, videoList);
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

    public static void fetchVideos(String url) {
        String htmlCode = getString(url, new String[][]{
                {"User-Agent", NetShare.PC_USER_AGENT}
        });
        JSONArray results = new JSONArray();
        String videoUrl = url;
        String videoTitle = StringShare.substring(htmlCode,"html5player.setVideoTitle('","');");
        String videoThumb =  StringShare.substring(htmlCode,"html5player.setThumbUrl('","');");
        String videoDuration = StringShare.substring(htmlCode,"<meta property=\"og:duration\" content=\"","\"");
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

