package euphoria.psycho.videos;

import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.explorer.WebActivity;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.tasks.HLSDownloadActivity;
import euphoria.psycho.tasks.HLSDownloadService;

import static euphoria.psycho.videos.VideosHelper.getString;

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
                    .map(v -> Native.fetch91Porn(StringShare.substringAfter(v, "91porn.com")))
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
            Intent service = new Intent(mainActivity, HLSDownloadActivity.class);
            service.putExtra(HLSDownloadService.KEY_VIDEO_LIST, videoList.toArray(new String[0]));
            mainActivity.startActivity(service);
        });
    }

    //
    @Override
    protected String fetchVideoUri(String uri) {
        return Native.fetch91Porn(StringShare.substringAfter(uri, "91porn.com"));
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
        // invokeVideoPlayer(mMainActivity, Uri.parse(videoUri));
    }

}
