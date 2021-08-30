package euphoria.psycho.videos;

import android.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import androidx.annotation.NonNull;
import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.videos.Twitter.TwitterVideo;

public class Twitter extends BaseExtractor<List<TwitterVideo>> {
    private static final Pattern MATCH_TWITTER = Pattern.compile("twitter\\.com/.+/status/(\\d+)");

    protected Twitter(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static List<TwitterVideo> extractTwitterVideo(String id) throws IOException, JSONException {
        URL url = new URL("https://twittervideodownloaderpro.com/twittervideodownloadv2/index.php");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.56 Mobile Safari/537.36");
        HashMap<String, String> postDataParams = new HashMap<>();
        postDataParams.put("id", id);
        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(getPostDataString(postDataParams));
        writer.flush();
        writer.close();
        os.close();
        int statusCode = connection.getResponseCode();
        if (statusCode == 200) {
            StringBuilder sb = new StringBuilder();
            InputStream in;
            String contentEncoding = connection.getHeaderField("Content-Encoding");
            if (contentEncoding != null && contentEncoding.equals("gzip")) {
                in = new GZIPInputStream(connection.getInputStream());
            } else {
                in = connection.getInputStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
            reader.close();
            JSONObject object = new JSONObject(sb.toString());
            if (object.has("state") && object.getString("state").equals("success")) {
                if (object.has("videos")) {
                    JSONArray videos = object.getJSONArray("videos");
                    List<TwitterVideo> twitterVideos = new ArrayList<>();
                    for (int i = 0; i < videos.length(); i++) {
                        JSONObject video = videos.getJSONObject(i);
                        TwitterVideo twitterVideo = new TwitterVideo();
                        if (video.has("duration")) {
                            twitterVideo.duration = video.getLong("duration");
                        }
                        if (video.has("size")) {
                            twitterVideo.size = video.getLong("size");
                        }
                        if (video.has("url")) {
                            twitterVideo.url = video.getString("url");
                        }
                        twitterVideos.add(twitterVideo);
                    }
                    return twitterVideos;
                }
            }
        }
        return null;
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_TWITTER.matcher(uri).find()) {
            new Twitter(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }

    private static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }

    @Override
    protected List<TwitterVideo> fetchVideoUri(String uri) {
        try {
            Matcher matcher = MATCH_TWITTER.matcher(uri);
            if (matcher.find()) {
                List<TwitterVideo> videoList = extractTwitterVideo(matcher.group(1));
                return videoList;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(List<TwitterVideo> videoList) {
        String[] names = new String[videoList.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = FileShare.formatFileSize(videoList.get(i).size);
        }
        new AlertDialog.Builder(mMainActivity)
                .setItems(names, (dialog, which) -> {
                    Helper.viewVideo(mMainActivity, videoList.get(which).url);
                })
                .show();
    }

    public static class TwitterVideo {
        public long duration;
        public long size;
        public String url;

        @NonNull
        @Override
        public String toString() {
            return super.toString();
        }
    }

}
