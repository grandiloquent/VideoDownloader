package euphoria.psycho.videos;

import android.util.Base64;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.Logger;

public class MgTv extends BaseVideoExtractor<List<Pair<String, String>>> {
    private static Pattern MATCH_MGTV = Pattern.compile("mgtv\\.com/[a-z]/\\d+/(\\d+)\\.html");

    protected MgTv(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected List<Pair<String, String>> fetchVideoUri(String videoId) {
        String clit = String.format("clit=%d", System.currentTimeMillis() / 1000);
        String pm2 = getPm2(videoId, clit);
        if (pm2 == null) {
            return null;
        }
        String source = getSource(videoId, clit, pm2);
        if (source == null) {
            return null;
        }
        JSONArray[] jsonArrays = new JSONArray[2];
        if (extractStream(source, jsonArrays)) return null;
        try {
            List<Pair<String, String>> videoList = new ArrayList<>();
            for (int i = 0; i < jsonArrays[1].length(); i++) {
                String url = jsonArrays[1].getJSONObject(i).getString("url");
                if (url.length() == 0) continue;
                url = jsonArrays[0].getString(0) + url;
                String name = jsonArrays[1].getJSONObject(i).getString("name");
                String jsonBody = getString(url, new String[][]{
                        {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36"},
                        {"Cookie", "PM_CHKID=1"}
                });
                JSONObject obj = new JSONObject(jsonBody);
                String video = obj.getString("info");
                videoList.add(Pair.create(name, video));
            }
            return videoList;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean extractStream(String source, JSONArray[] jsonArrays) {
        try {
            JSONObject obj = new JSONObject(source);
            JSONObject data;
            if (obj.has("data")) {
                data = obj.getJSONObject("data");
            } else {
                return true;
            }
            // stream_domain
            if (data.has("stream_domain")) {
                jsonArrays[0] = data.getJSONArray("stream_domain");
            }
            if (data.has("stream")) {
                jsonArrays[1] = data.getJSONArray("stream");
            } else {
                return true;
            }

        } catch (Exception ignored) {
            Logger.d(String.format("fetchVideoUri: %s", ignored));
        }
        return false;
    }

    private String getSource(String videoId, String clit, String pm2) {
        return getString(String.format("https://pcweb.api.mgtv.com/player/getSource?video_id=%s&tk2=%s&pm2=%s",
                videoId, encodeTk(clit), pm2), new String[][]{
                {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36"},
                {"Cookie", "PM_CHKID=1"}
        });
    }

    private String getPm2(String videoId, String clit) {
        String jsonBody = getString(String.format("https://pcweb.api.mgtv.com/player/video?video_id=%s&tk2=%s",
                videoId, getTk2(clit)),
                new String[][]{
                        {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36"},
                        {"Cookie", "PM_CHKID=1"}
                });
        if (jsonBody == null) {
            return null;
        }
        try {
            JSONObject obj = new JSONObject(jsonBody);
            JSONObject data;
            if (obj.has("data")) {
                data = obj.getJSONObject("data");
            } else {
                return null;
            }
            JSONObject atc;
            if (data.has("atc")) {
                atc = data.getJSONObject("atc");
            } else {
                return null;
            }
            String pm2;
            if (atc.has("pm2")) {
                pm2 = atc.getString("pm2");
            } else {
                return null;
            }
            return pm2;
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getTk2(String clit) {
        return encodeTk("did=f11dee65-4e0d-4d25-bfce-719ad9dc991d|pno=1030|ver=5.5.1|" + clit);
    }

    private String encodeTk(String string) {
        byte[] buffer = Base64.encode(string.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        for (int i = 0, j = buffer.length; i < j; i++) {
            byte b = buffer[i];
            switch (b) {
                case 43:
                    buffer[i] = 95;
                    break;
                case 47:
                    buffer[i] = 126;
                    break;
                case 61:
                    buffer[i] = 45;
                    break;
            }
        }
        String encode = new String(buffer);
        StringBuilder stringBuilder = new StringBuilder(encode.length());
        for (int j = encode.length() - 1, i = j; i > -1; i--) {
            stringBuilder.append(encode.charAt(i));
        }
        return stringBuilder.toString();
    }

    @Override
    protected String processUri(String inputUri) {
        Matcher matcher = MATCH_MGTV.matcher(inputUri);
        return matcher.find() ? matcher.group(1) : inputUri;
    }

    @Override
    protected void processVideo(List<Pair<String, String>> videoList) {
        try {
            launchDialog(mMainActivity, videoList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_MGTV.matcher(uri).find()) {
            new MgTv(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
