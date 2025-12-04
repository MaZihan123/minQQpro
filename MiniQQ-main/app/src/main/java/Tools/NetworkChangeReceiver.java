package Tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork != null) {
            // 判断网络类型
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // 如果是Wi-Fi
                Toast.makeText(context, "连接到 Wi-Fi 网络", Toast.LENGTH_SHORT).show();
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // 如果是流量
                Toast.makeText(context, "连接到移动数据网络", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 没有网络连接
            Toast.makeText(context, "没有网络连接", Toast.LENGTH_SHORT).show();
        }
    }
}
