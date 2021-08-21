package euphoria.psycho.videos;

import android.os.Environment;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

public class TikTok extends BaseVideoExtractor<String> {
    private static Pattern MATCH_TIKTOK = Pattern.compile("tiktok\\.com");

    protected TikTok(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        String response = getString(uri, new String[][]{
                //{"Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7"},
                //{"Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"},
                {"User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36"}
        });
        if (response == null) {
            return null;
        }
        String jsonBody = StringShare.substring(response,
                "<script id=\"__NEXT_DATA__\"",
                "</script>");
        if (jsonBody == null) {
            return null;
        }
        jsonBody = StringShare.substringAfter(jsonBody, ">");
//        try {
//            FileShare.appendAllText(new File("/storage/emulated/0/Download/","tiktok.json"),jsonBody);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        if (jsonBody == null) {
            return null;
        }
        try {
            JSONObject obj = new JSONObject(jsonBody);
            JSONObject props;
            if (obj.has("props")) {
                props = obj.getJSONObject("props");
            } else {
                return null;
            }
            JSONObject pageProps;
            if (props.has("pageProps")) {
                pageProps = props.getJSONObject("pageProps");
            } else {
                return null;
            }
            JSONObject itemInfo;
            if (pageProps.has("itemInfo")) {
                itemInfo = pageProps.getJSONObject("itemInfo");
            } else {
                return null;
            }
            JSONObject itemStruct;
            if (itemInfo.has("itemStruct")) {
                itemStruct = itemInfo.getJSONObject("itemStruct");
            } else {
                return null;
            }
            JSONObject video;
            if (itemStruct.has("video")) {
                video = itemStruct.getJSONObject("video");
            } else {
                return null;
            }
            String downloadAddr;
            if (video.has("downloadAddr")) {
                downloadAddr = video.getString("downloadAddr");
            } else {
                return null;
            }
            response = getString(downloadAddr,  new String[][]{
                    {"Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"},
                    //{"Cookie","tt_webid_v2=6998451551669175813; tt_webid=6998451551669175813; cookie-consent={%22ga%22:true%2C%22af%22:true%2C%22fbp%22:true%2C%22lip%22:true%2C%22version%22:%22v2%22}; tt_csrf_token=DFviqAEyEGVsQZ4YJZhVipu4; R6kq3TV7=AIDieGV7AQAAjePT-BVdDtQoFgqGs480XBDLEMFdHJRuoZwDKMBrsRXyLmbP|1|0|49053166d57414a1259663078381ac9b6f5839aa; ttwid=1%7CwghW3YWYhV8O13pwnXYo6CgtlfXm59AWaYy7IEYfUG8%7C1629495027%7Cf79d378c7e5f3aeee175c0157cfe897d0f1958da40bc34d427d8e3a2b7f8122f"},
                    {"Range","bytes=0-200000"},
                    {"Referer","https://v16-web.tiktok.com/video/tos/useast2a/tos-useast2a-ve-0068c001/4931dc045bbf40ebae59bde47f3a16f2/?a=1988&br=3900&bt=1950&cd=0%7C0%7C1&ch=0&cr=0&cs=0&cv=1&dr=0&ds=3&er=&expire=1629519426&ft=Q9BExEwM_4ka&l=20210820221659010189073137103AB10F&lr=tiktok_m&mime_type=video_mp4&net=0&pl=0&policy=3&qs=0&rc=MzZ1ODU6ZnNmNzMzNzczM0ApO2dnaDgzOjw3NzY0aWVmZmctNjAxcjRfYC5gLS1kMTZzc2E0YmNfYzJhLTZeNTA1MWA6Yw%3D%3D&signature=4527cdc7609858433ea47802b4daa942&tk=0&vl=&vr="},
                    {"User-Agent","Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Mobile Safari/537.36"},
            });
            Logger.d(String.format("fetchVideoUri: %s %d ",downloadAddr, response.indexOf("vid:")));

        } catch (Exception ignored) {
            Logger.d(String.format("fetchVideoUri: %s", ignored.getMessage()));
        }
        return null;
    }
    // video.playAddr

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(String videoUri) {
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_TIKTOK.matcher(uri).find()) {
            new TikTok(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
