package euphoria.psycho.videos;

import android.net.Uri;
import android.widget.Toast;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;

import static euphoria.psycho.videos.VideosHelper.invokeVideoPlayer;

public class XiGua extends BaseExtractor<String> {
    protected XiGua(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        return Native.fetchXiGua(uri);
    }

    @Override
    protected void processVideo(String videoUri) {
        if (videoUri.length() == 0) {
            Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
            return;
        }
        invokeVideoPlayer(mMainActivity, Uri.parse(videoUri));
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("m\\.ixigua\\.com/video/\\d+");
        if (pattern.matcher(uri).find()) {
            new XiGua(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}

