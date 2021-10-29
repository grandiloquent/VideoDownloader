package euphoria.psycho.bilibili;

import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import euphoria.psycho.PlayerActivity;

public class BilibiliService extends Service {

    public static final String BILIBILI_CHANNEL = "Bilibili";
    private final Handler mHandler = new Handler();
    private NotificationManager mNotificationManager;
    private ExecutorService mExecutorService;
    private int mStartId;
    private BilibiliDatabase mBilibiliDatabase;

    private Builder getBuilder() {
        Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Builder(getApplicationContext(), BILIBILI_CHANNEL);
        } else {
            builder = new Builder(getApplicationContext());
        }
        return builder;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = getSystemService(NotificationManager.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    BILIBILI_CHANNEL,
                    "Bilibili", NotificationManager.IMPORTANCE_LOW
            );
            mNotificationManager.createNotificationChannel(channel);
        }
        mExecutorService = Executors.newSingleThreadExecutor();
        mStartId = hashCode();
        mBilibiliDatabase = new BilibiliDatabase(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        List<BilibiliTask> bilibiliTasks = mBilibiliDatabase.queryUnfinishedTasks();
        for (BilibiliTask bilibiliTask : bilibiliTasks) {
            mExecutorService.submit(new DownloadThread(bilibiliTask));
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private class DownloadThread implements Runnable {
        private final BilibiliTask mBilibiliTask;

        private DownloadThread(BilibiliTask bilibiliTask) {
            mBilibiliTask = bilibiliTask;
        }

        private void download(int index) {
            URL url;
            try {
                url = new URL(mBilibiliTask.BilibiliThreads[index].Url);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestProperty("Referer", "https://www.bilibili.com/");
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Safari/537.36");
                File videoFile = new File(mBilibiliTask.BilibiliThreads[index].Filename);
                if (videoFile.exists()) {
                    c.setRequestProperty("Range", "bytes=" + videoFile.length() + "-");
                }
                int status = c.getResponseCode();
                // If the requested range exceeds
                // the size of the file to be downloaded
                // the server will return this code
                // so we should think that the file has been downloaded correctly
                // and immediately terminate the execution of the function
                if (status == 416) {
                    return;
                }
                if (status != 200 && status != 206) {
                    notifyFailed();
                    mBilibiliTask.Status = BilibiliStatus.ERROR_STATUS_CODE;
                    mBilibiliDatabase.updateBilibiliTask(mBilibiliTask);
                    return;
                }
                notify("开始下载B站视频", mBilibiliTask.Url);
                RandomAccessFile out = new RandomAccessFile(videoFile, "rw");
                long currentBytes = 0;
                long totalBytes = c.getContentLengthLong();
                if (videoFile.exists()) {
                    currentBytes = (int) videoFile.length();
                    totalBytes += currentBytes;
                    out.seek(currentBytes);
                }
                InputStream in;
                try {
                    try {
                        in = c.getInputStream();
                    } catch (IOException e) {
                        notifyFailed();
                        mBilibiliTask.Status = BilibiliStatus.ERROR_STATUS_HTTP_DATA;
                        mBilibiliDatabase.updateBilibiliTask(mBilibiliTask);
                        return;
                    }
                    final byte[] buffer = new byte[8192];
                    long speedSampleStart = 0;
                    long speedSampleBytes = 0;
                    long speed = 0;
                    while (true) {
//
//                        if (mShutdownRequested) {
//                            throw new StopRequestException(STATUS_HTTP_DATA_ERROR,
//                                    "Local halt requested; job probably timed out");
//                        }
                        int len = -1;
                        try {
                            len = in.read(buffer);
                        } catch (IOException e) {
                            notifyFailed();
                            mBilibiliTask.Status = BilibiliStatus.ERROR_STATUS_HTTP_DATA;
                            mBilibiliDatabase.updateBilibiliTask(mBilibiliTask);
                            return;
                        }
                        if (len == -1) {
                            break;
                        }
                        try {
                            out.write(buffer, 0, len);
                            currentBytes += len;
                            final long now = SystemClock.elapsedRealtime();
                            final long sampleDelta = now - speedSampleStart;
                            if (sampleDelta > 500) {
                                final long sampleSpeed = ((currentBytes - speedSampleBytes) * 1000)
                                        / sampleDelta;
                                if (speed == 0) {
                                    speed = sampleSpeed;
                                } else {
                                    speed = ((speed * 3) + sampleSpeed) / 4;
                                }
                                if (speedSampleStart != 0) {
                                    final int percent = (int) ((currentBytes * 100) / totalBytes);
                                    final long remainingMillis = ((totalBytes - currentBytes) * 1000) / speed;
                                    notifyProgress(
                                            "剩余时间" + BilibiliUtils.formatDuration(remainingMillis), "正在下载B站视频", percent);
                                }
                                speedSampleStart = now;
                                speedSampleBytes = currentBytes;
                            }
                        } catch (IOException e) {
                            notifyFailed();
                            mBilibiliTask.Status = BilibiliStatus.ERROR_STATUS_FILE;
                            mBilibiliDatabase.updateBilibiliTask(mBilibiliTask);
                        }
                    }

                } finally {
                    try {
                        out.close();
                    } catch (IOException ignored) {
                    }
                }
            } catch (Exception e) {
                notifyFailed();
            }
        }

        private void notify(String title, String content) {
            mHandler.post(() -> {
                Builder builder = getBuilder();
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setWhen(System.currentTimeMillis())
                        .setShowWhen(true)
                        .setOngoing(true);
                mNotificationManager.notify(mStartId, builder.build());
            });
        }

        private void notifyFailed() {
            mHandler.post(() -> {
                Builder builder = getBuilder();
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle("下载失败")
                        .setContentText(mBilibiliTask.Url)
                        .setWhen(System.currentTimeMillis())
                        .setShowWhen(true)
                ;
                mNotificationManager.notify(mStartId, builder.build());
            });
        }

        private void notifyProgress(String title, String content, int progress) {
            mHandler.post(() -> {
                Builder builder = getBuilder();
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(title)
                        .setSubText(content)
                        .setWhen(System.currentTimeMillis())
                        .setShowWhen(true)
                        .setOngoing(true)
                        .setProgress(100, progress, false);
                mNotificationManager.notify(mStartId, builder.build());
            });
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            notify("准备下载B站视频", mBilibiliTask.Url);
            download(0);
            notify("已成功下载视频", mBilibiliTask.Url);
            download(1);
            notify("开始合并视频", mBilibiliTask.Url);
            try {
                Movie countVideo = MovieCreator.build(mBilibiliTask.BilibiliThreads[0].Filename);
                Movie countAudioEnglish = MovieCreator.build(mBilibiliTask.BilibiliThreads[1].Filename);
                Track audioTrackEnglish = countAudioEnglish.getTracks().get(0);
                countVideo.addTrack(audioTrackEnglish);
                Container out = new DefaultMp4Builder().build(countVideo);
                FileOutputStream fos = new FileOutputStream(new File(mBilibiliTask.Filename));
                out.writeContainer(fos.getChannel());
                fos.close();
            } catch (Exception ignored) {
            }
            mHandler.post(() -> {
                Intent intent = new Intent(BilibiliService.this, PlayerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(PlayerActivity.KEY_VIDEO_FILE, mBilibiliTask.Filename);
                Builder builder = getBuilder();
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle("已成功合并视频")
                        .setContentText(mBilibiliTask.Url)
                        .setWhen(System.currentTimeMillis())
                        .setShowWhen(true)
                        .setContentIntent(PendingIntent.getActivity(BilibiliService.this,
                                0, intent, 0));
                mNotificationManager.notify(mStartId, builder.build());
            });
        }
    }
}
