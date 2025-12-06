package Activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.example.pretend_qq.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import androidx.core.content.FileProvider;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.content.ActivityNotFoundException;
import android.widget.Toast;
import android.os.Build;


public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    public static final String EXTRA_URL = "fileUrl";
    public static final String EXTRA_FILE_NAME = "fileName";
    private static final String CHANNEL_ID = "download_channel_id";
    private static final int NOTIFICATION_ID = 1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 我们是 startService / startForegroundService，不绑定
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        final String url = intent.getStringExtra(EXTRA_URL);
        final String fileName = intent.getStringExtra(EXTRA_FILE_NAME);

        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(fileName)) {
            Log.e(TAG, "onStartCommand: url or fileName is empty, stop self");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        Log.e(TAG, "onStartCommand: url = " + url + ", fileName = " + fileName);

        // 先启动为前台服务，防止被系统杀掉
        Notification notification = buildProgressNotification("正在下载：" + fileName, 0);
        startForeground(NOTIFICATION_ID, notification);

        new Thread(() -> {
            File downloadedFile = downloadFile(url, fileName, progress -> {
                // 更新进度条通知
                Notification progressNotif = buildProgressNotification("正在下载：" + fileName, progress);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(NOTIFICATION_ID, progressNotif);
            });

            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (downloadedFile != null && downloadedFile.exists()) {
                // 下载成功：发一个“点击查看”的通知
                Notification done = buildFinishNotification(downloadedFile);
                manager.notify(NOTIFICATION_ID, done);
            } else {
                // 下载失败：简单提示一下
                Notification fail = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        // 你项目里已有的图标
                        .setContentTitle("下载失败")
                        .setContentText(fileName)
                        .setAutoCancel(true)
                        .build();
                manager.notify(NOTIFICATION_ID, fail);
            }

            // 前台服务降级为普通（保留“下载完成”通知）
            stopForeground(false);
            stopSelf(startId);
        }).start();

        return START_NOT_STICKY;
    }

    // ================== 通知相关 ==================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, "文件下载", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("聊天文件下载通知");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /** 下载中的进度通知 */
    private Notification buildProgressNotification(String title, int progress) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setOnlyAlertOnce(true)   // 只第一次响铃/震动
                        .setOngoing(true);        // 正在进行中

        if (progress >= 0 && progress <= 100) {
            builder.setProgress(100, progress, false)
                    .setContentText(progress + "%");
        }
        return builder.build();
    }

    /** 下载完成，点击通知即可预览文件 */
    private Notification buildFinishNotification(File file) {
        Uri contentUri = FileProvider.getUriForFile(
                this,
                "com.example.pretend_qq.fileprovider", // 和 manifest 中的 authorities 一致
                file
        );

        String mimeType = getMimeTypeFromFileName(file.getName());
        if (mimeType == null) {
            mimeType = "*/*"; // 兜底，交给系统挑应用
        }

        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(contentUri, mimeType);
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                        PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)

                .setContentTitle("下载完成")
                .setContentText(file.getName())
                .setContentIntent(pi)
                .setAutoCancel(true)  // 点了以后自动消失
                .build();
    }

    private String getMimeTypeFromFileName(String fileName) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (TextUtils.isEmpty(ext)) return null;
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
    }

    // ================== 真正的下载逻辑 ==================

    private File downloadFile(String urlStr, String fileName, OnDownloadListener listener) {
        HttpURLConnection connection = null;
        InputStream in = null;
        FileOutputStream out = null;
        File outFile = null;

        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "downloadFile: http code = " + responseCode);
                return null;
            }

            int contentLength = connection.getContentLength();
            in = new BufferedInputStream(connection.getInputStream());

            // =============== 保存目录处理 ===============
            File downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (downloadsDir == null) {
                Log.e(TAG, "downloadFile: downloadsDir == null");
                return null;
            }
            if (!downloadsDir.exists()) {
                boolean mkdirs = downloadsDir.mkdirs();
                Log.d(TAG, "downloadFile: mkdirs = " + mkdirs);
            }

            outFile = new File(downloadsDir, fileName);
            out = new FileOutputStream(outFile);

            byte[] buffer = new byte[8 * 1024];
            int len;
            long total = 0;
            int lastProgress = 0;

            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                total += len;

                if (contentLength > 0) {
                    int progress = (int) (total * 100 / contentLength);
                    if (listener != null && progress != lastProgress) {
                        lastProgress = progress;
                        listener.onProgress(progress);
                    }
                }
            }

            out.flush();

            // 保险起见，最后回调一下 100%
            if (listener != null) {
                listener.onProgress(100);
            }

            Log.d(TAG, "downloadFile: success -> " + outFile.getAbsolutePath());

            // =============== ★ 下载成功后自动预览 ★ ===============
            File finalOutFile = outFile;
            new Handler(Looper.getMainLooper()).post(() -> {
                // 这里就是你之前在 DownloadService 里写的 openFile(File file)
                openFile(finalOutFile);
            });

            return outFile;

        } catch (Exception e) {
            Log.e(TAG, "downloadFile: error", e);
            // 下载失败时，删除不完整文件（可选）
            if (outFile != null && outFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                outFile.delete();
            }
            return null;

        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {}
            try {
                if (out != null) out.close();
            } catch (Exception ignored) {}
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    /** 简单的进度回调接口 */
    private interface OnDownloadListener {
        void onProgress(int progress);
    }
    private void openFile(File file) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 注意和 AndroidManifest.xml 的 authorities 一致
            uri = FileProvider.getUriForFile(
                    this,
                    "com.example.pretend_qq.fileprovider",
                    file
            );
        } else {
            uri = Uri.fromFile(file);
        }

        // 尝试根据后缀猜 MIME 类型
        String mimeType = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        if (extension != null) {
            mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension.toLowerCase());
        }
        if (mimeType == null) {
            mimeType = "*/*";  // 保底，交给系统选择
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "没有可以打开该文件的应用", Toast.LENGTH_SHORT).show();
        }
    }

}
