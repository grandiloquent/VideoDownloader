package euphoria.psycho.videos;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.DateTimeShare;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.tasks.VideoActivity;
import euphoria.psycho.tasks.VideoService;

import static euphoria.psycho.videos.VideosHelper.getLocationAddCookie;
import static euphoria.psycho.videos.VideosHelper.getString;
import static euphoria.psycho.videos.VideosHelper.invokeVideoPlayer;

public class Porn91 extends BaseExtractor<String> {

    private static final Pattern MATCH_91PORN = Pattern.compile("(?<=<a href=\")https://91porn.com/view_video.php\\?[^\"]+(?=\")");

    public Porn91(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public void fetchVideoList(String uri) {
        new Thread(() -> {
            String response = getString(uri, null);
            if (response == null) {
                return;
            }
            Matcher matcher = MATCH_91PORN.matcher(response);
            List<String> videoList = new ArrayList<>();
            while (matcher.find()) {
                videoList.add(matcher.group());
            }
            startVideoService(mMainActivity, videoList.parallelStream()
                    .map(VideosHelper::extract91PornVideoAddress)
                    .collect(Collectors.toList()));
        }).start();

    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("91porn.com/view_video.php\\?viewkey=[a-zA-Z0-9]+");
        if (pattern.matcher(uri).find()) {
            new Porn91(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }


    static void startVideoService(MainActivity mainActivity, List<String> videoList) {
        mainActivity.runOnUiThread(() -> {
            Intent service = new Intent(mainActivity, VideoActivity.class);
            service.putExtra(VideoService.KEY_VIDEO_LIST, videoList.toArray(new String[0]));
            mainActivity.startActivity(service);
        });
    }

    //
    @Override
    protected String fetchVideoUri(String uri) {
        return VideosHelper.extract91PornVideoAddress(uri);
    }


    @Override
    protected void processVideo(String videoUri) {
        invokeVideoPlayer(mMainActivity, Uri.parse(videoUri));
    }

    public static void fetchVideos(String url, int max) {
        Log.e("B5aOx2", String.format("fetchVideos, %s", ""));
        String[] headers = null;
        try {
            headers = getLocationAddCookie(
                    "https://91porn.com/index.php",
                    new String[][]{
                            {"Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"},
                            {"Accept-Encoding", "gzip, deflate, br"},
                            {"Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"},
                            {"Cache-Control", "no-cache"},
                            {"Connection", "keep-alive"},
                            {"Host", "91porn.com"},
                            {"Pragma", "no-cache"},
                            {"sec-ch-ua", "\"Google Chrome\";v=\"93\", \" Not;A Brand\";v=\"99\", \"Chromium\";v=\"93\""},
                            {"sec-ch-ua-mobile", "?0"},
                            {"sec-ch-ua-platform", "\"Windows\""},
                            {"Sec-Fetch-Dest", "document"},
                            {"Sec-Fetch-Mode", "navigate"},
                            {"Sec-Fetch-Site", "none"},
                            {"Sec-Fetch-User", "?1"},
                            {"Upgrade-Insecure-Requests", "1"},
                            {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36"}}
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray results = new JSONArray();
        String res = getString(url, new String[][]{
                {"Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"},
                {"Accept-Encoding", "gzip, deflate, br"},
                {"Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"},
                {"Cache-Control", "no-cache"},
                {"Connection", "keep-alive"},
                {"Host", "91porn.com"},
                {"Pragma", "no-cache"},
                {"sec-ch-ua", "\"Google Chrome\";v=\"93\", \" Not;A Brand\";v=\"99\", \"Chromium\";v=\"93\""},
                {"sec-ch-ua-mobile", "?0"},
                {"sec-ch-ua-platform", "\"Windows\""},
                {"Sec-Fetch-Dest", "document"},
                {"Sec-Fetch-Mode", "navigate"},
                {"Sec-Fetch-Site", "none"},
                {"Sec-Fetch-User", "?1"},
                {"Upgrade-Insecure-Requests", "1"},
                {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36"},
                {"X-Forwarded-For", NetShare.randomIp()},
                {"Cookie", headers[1]}
        });
        if (res == null) {
            return;
        }
        Document document = Jsoup.parse(res);
        Elements elements = document.select(".videos-text-align");
        elements.forEach(element -> {
            String videoUrl = element.selectFirst("a").attr("href");
            String videoTitle = Parser.unescapeEntities(element.selectFirst(".video-title").text(), true);
            String videoThumb = element.selectFirst(".img-responsive").attr("src");
            String videoDuration = element.selectFirst(".duration").text();
            JSONObject video = new JSONObject();
            try {
                video.put("title", videoTitle);
                video.put("thumbnail", videoThumb);
                video.put("url", videoUrl);
                int duration = 0;
                try {
                    duration = DateTimeShare.DurationToSeconds(videoDuration);
                } catch (Exception ignored) {
                }
                video.put("duration", duration);
            } catch (JSONException ignored) {
                Log.e("B5aOx2", String.format("fetchVideos, %s", ignored));
            }
            results.put(video);
        });
        try {
            URL uri = new URL("http://47.106.105.122/api/video");
            HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            OutputStream out = connection.getOutputStream();
            out.write(results.toString().getBytes(StandardCharsets.UTF_8));
            out.close();
            int code = connection.getResponseCode();
            Log.e("B5aOx2", String.format("fetchVideos, %s", code));
        } catch (IOException ignored) {
        }


    }
}
