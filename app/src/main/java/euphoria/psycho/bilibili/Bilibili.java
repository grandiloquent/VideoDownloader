package euphoria.psycho.bilibili;

import android.widget.Toast;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.videos.BaseExtractor;

public class Bilibili extends BaseExtractor<String[]> {
    private static final Pattern MATCH_BILIBILI = Pattern.compile("bilibili\\.com/.+");

    protected Bilibili(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String[] fetchVideoUri(String uri) {
        return Native.fetchBilibili(uri);
    }

    @Override
    protected void processVideo(String[] videoUris) {
        if (videoUris.length == 0) {
            Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
            return;
        }
        String fileName = BilibiliUtils.getBilibiliThreadFile(mMainActivity, getInputUri()).getAbsolutePath();
        BilibiliThread videoThread = new BilibiliThread();
        videoThread.Filename = fileName + ".mp4";
        videoThread.Url = videoUris[0];
        BilibiliThread audioThread = new BilibiliThread();
        audioThread.Filename = fileName + ".mp3";
        audioThread.Url = videoUris[1];
        BilibiliTask bilibiliTask = new BilibiliTask();
        bilibiliTask.Filename = BilibiliUtils.getBilibiliVideoFile(mMainActivity, getInputUri()).getAbsolutePath();
        bilibiliTask.Url = getInputUri();
        bilibiliTask.BilibiliThreads=new BilibiliThread[]{videoThread,audioThread};
        new BilibiliDatabase(mMainActivity).insertBilibiliTask(bilibiliTask);
        //        Intent intent = new Intent(mMainActivity, BilibiliActivity.class);
//        intent.putExtra(BilibiliActivity.EXTRA_PLAYLSIT, videoUris);
//        mMainActivity.startActivity(intent);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_BILIBILI.matcher(uri).find()) {
            new Bilibili(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }

}
