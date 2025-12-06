package Activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.pretend_qq.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends Service {

    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_FILE_NAME = "extra_file_name";
    private static final String CHANNEL_ID = "download_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra(EXTRA_URL);
        String fileName = intent.getStringExtra(EXTRA_FILE_NAME);

        // 1. 启动前台通知
        Notification notification = buildNotification("开始下载 " + fileName, 0);
        startForeground(1, notification);

        // 2. 开子线程下载，避免阻塞主线程
        new Thread(() -> {
            boolean ok = downloadFile(url, fileName);
            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (ok) {
                Notification done = buildNotification("下载完成：" + fileName, 100);
                manager.notify(1, done);
            } else {
                Notification fail = buildNotification("下载失败：" + fileName, -1);
                manager.notify(1, fail);
            }

            stopForeground(true);
            stopSelf(startId);
        }).start();

        return START_NOT_STICKY;
    }

    // 创建通知渠道（Android 8.0 以上必须）
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "文件下载",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    // 构建一个简单的通知（这里用进度条占位）
    private Notification buildNotification(String content, int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)  // 换成你自己的图标
                .setContentTitle("下载服务")
                .setContentText(content)
                .setOngoing(progress >= 0); // 下载中标记常驻

        if (progress >= 0 && progress <= 100) {
            builder.setProgress(100, progress, progress == 0);
        }
        return builder.build();
    }

    // 具体下载逻辑，你可以用 OkHttp 也可以用 HttpURLConnection
    private boolean downloadFile(String urlStr, String fileName) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            in = conn.getInputStream();
            // 保存到 app 专属目录：/Android/data/包名/files/Download
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir != null && !dir.exists()) dir.mkdirs();
            File outFile = new File(dir, fileName);

            out = new FileOutputStream(outFile);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (IOException ignored) {}
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 我们这里不需要绑定
    }
}

