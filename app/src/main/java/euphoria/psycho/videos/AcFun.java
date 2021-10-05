package euphoria.psycho.videos;

import android.net.Uri;
import android.widget.Toast;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;

import static euphoria.psycho.videos.VideosHelper.invokeVideoPlayer;

public class AcFun extends BaseExtractor<String> {

    private static final Pattern MATCH_AC_FUN = Pattern.compile("acfun\\.cn/v/ac\\d+");

    public AcFun(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_AC_FUN.matcher(uri).find()) {
            new Porn91(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }


    @Override
    protected String fetchVideoUri(String uri) {
        return Native.fetchAcFun(uri);
    }


    @Override
    protected void processVideo(String videoUri) {
        if (videoUri.length() == 0) {
            Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
            return;
        }
        invokeVideoPlayer(mMainActivity, Uri.parse(videoUri));
    }

}
