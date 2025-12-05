package Tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class NetworkChangeReceiver extends BroadcastReceiver {
    // 没连 WiFi 持续 5 秒后强制下线
    private static final long FORCE_OFFLINE_DELAY = 5000; // 5s
    // 用来做延迟任务（全局静态，保证多次广播之间能共享同一个倒计时）
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static Runnable sForceOfflineTask;

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;

        boolean wifiConnected = activeNetwork != null
                && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI
                && activeNetwork.isConnected();

        if (wifiConnected) {
            // ✅ 只要检测到 WiFi 连上，就取消之前的 “强制下线” 倒计时
            cancelForceOfflineTask();
        } else {
            // ❌ 当前不是 WiFi（可能没网，也可能是流量）：
            // 开始 5 秒倒计时，如果 5 秒内 WiFi 还没恢复，就发强制下线广播
            startForceOfflineCountdown(context.getApplicationContext());
        }
    }

    // 开始 5 秒倒计时（如果已经在倒计时就不重复开启）
    private void startForceOfflineCountdown(final Context appContext) {
        if (sForceOfflineTask != null) {
            // 已经有一次倒计时在跑了，直接用那次，不再重复开启
            return;
        }

        sForceOfflineTask = new Runnable() {
            @Override
            public void run() {
                // 5 秒内没有被取消，说明这 5 秒里 WiFi 一直没连上 → 真正执行“强制下线”
                Intent forceIntent = new Intent("com.example.pretend_qq.FORCE_OFFLINE");
                appContext.sendBroadcast(forceIntent);

                // 发送完后置空，方便下一次重新计时
                sForceOfflineTask = null;
            }
        };

        // 延迟 5 秒执行
        sHandler.postDelayed(sForceOfflineTask, FORCE_OFFLINE_DELAY);
    }

    // 取消倒计时
    private void cancelForceOfflineTask() {
        if (sForceOfflineTask != null) {
            sHandler.removeCallbacks(sForceOfflineTask);
            sForceOfflineTask = null;
        }
    }
}
