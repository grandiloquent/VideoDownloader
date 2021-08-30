package euphoria.psycho.videos;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.tasks.VideoActivity;
import euphoria.psycho.tasks.VideoService;

import static euphoria.psycho.videos.VideosHelper.getString;
import static euphoria.psycho.videos.VideosHelper.viewVideoBetter;

public class Porn91 extends BaseVideoExtractor<String> {

    private static Pattern MATCH_91PORN = Pattern.compile("(?<=<a href=\")https://91porn.com/view_video.php\\?[^\"]+(?=\")");

    public Porn91(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("91porn.com/view_video.php\\?viewkey=[a-zA-Z0-9]+");
        if (pattern.matcher(uri).find()) {
            new Porn91(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }

    //
    @Override
    protected String fetchVideoUri(String uri) {
        return VideosHelper.extract91PornVideoAddress(uri);
        //return new String(buffer, 0, result);
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(String videoUri) {
        viewVideoBetter(mMainActivity, videoUri);
    }

    public void fetchVideoList(String uri) {
        new Thread(() -> {
            String response = getString(uri, null);
            Matcher matcher = MATCH_91PORN.matcher(response);
            List<String> videoList = new ArrayList<>();
            while (matcher.find()) {
                videoList.add(matcher.group());
            }
            videoList = videoList.parallelStream()
                    .map(VideosHelper::extract91PornVideoAddress)
                    .collect(Collectors.toList());
            List<String> finalVideoList = videoList;
            mMainActivity.runOnUiThread(() -> {
                Intent service = new Intent(mMainActivity, VideoActivity.class);
                service.putExtra(VideoService.KEY_VIDEO_LIST, finalVideoList.toArray(new String[0]));
                mMainActivity.startActivity(service);
            });
        }).start();


    }
}
