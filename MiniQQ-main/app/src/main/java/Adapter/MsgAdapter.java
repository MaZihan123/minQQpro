package Adapter;

// MsgAdapter.java


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.pretend_qq.R;

import java.util.List;

import Tools.Msg;
import Activity.WebViewActivity;

import androidx.recyclerview.widget.RecyclerView;
import java.util.Locale;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import Activity.DownloadService;   // 你新建的前台下载 Service（和 WebViewActivity 同一个包）


public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {

    private List<Msg>mMsgList; //存储消息的列表
    private byte[]user_avatar;
    private byte[]friend_avatar;
    static class ViewHolder extends RecyclerView.ViewHolder {


        LinearLayout leftLayout;
        LinearLayout rightLayout;
        TextView leftMsg; //左边的文本框
        TextView rightMsg; //右边的文本框
        ImageView left_avatar;
        ImageView right_avatar;
        TextView left_time;//左侧时间
        TextView right_time;//右侧时间

        public ViewHolder(View view) {
            super(view);
            leftLayout= view.findViewById(R.id.left_layout);
            rightLayout= view.findViewById(R.id.right_layout);
            leftMsg= view.findViewById(R.id.left_msg);
            rightMsg= view.findViewById(R.id.right_msg);
            left_avatar= view.findViewById(R.id.left_avatar);
            right_avatar= view.findViewById(R.id.right_avatar);
            left_time=view.findViewById(R.id.left_time);
            right_time=view.findViewById(R.id.right_time);
        }

    }

    public MsgAdapter(List<Msg> msgList, byte[]user_avatar, byte[]friend_avatar) {
        mMsgList = msgList;
        this.user_avatar=user_avatar;
        this.friend_avatar=friend_avatar;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //创建一个ViewHolder实例,并根据viewType参数加载不同的布局文件
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_item ,parent, false);
        return new ViewHolder(view); //传入viewType参数
    }



    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        // 根据位置获取对应的消息对象
        Msg msg = mMsgList.get(position);

        // 收到的消息（左边）
        if (msg.getType() == Msg.TYPE_RECEIVED) {

            holder.leftLayout.setVisibility(View.VISIBLE);
            holder.rightLayout.setVisibility(View.GONE);

            Bitmap bitmap = BitmapFactory.decodeByteArray(friend_avatar, 0, friend_avatar.length);
            holder.left_avatar.setImageBitmap(bitmap);      // 左侧头像
            holder.left_time.setText(msg.getTime());        // 左侧时间

            // 左侧文本：普通文字正常显示，URL 变成蓝色可点击并用 WebView 打开
            bindMessageText(holder.leftMsg, msg.getContent());

            // 发送的消息（右边）
        } else if (msg.getType() == Msg.TYPE_SENT) {

            holder.rightLayout.setVisibility(View.VISIBLE);
            holder.leftLayout.setVisibility(View.GONE);

            Bitmap bitmap = BitmapFactory.decodeByteArray(user_avatar, 0, user_avatar.length);
            holder.right_avatar.setImageBitmap(bitmap);     // 右侧头像
            holder.right_time.setText(msg.getTime());       // 右侧时间

            // 右侧文本同样处理
            bindMessageText(holder.rightMsg, msg.getContent());
        }
    }


    @Override
    public int getItemCount() {
        //返回消息的数量
        return mMsgList.size();
    }
    // 绑定消息文本：普通文字正常显示，URL 变成蓝色可点击并用 WebViewActivity 打开
    // 把一条消息内容绑定到 TextView：如果是链接，就变蓝 + 下划线 + 点击打开 WebViewActivity
    private void bindMessageText(TextView textView, String content) {
        if (content == null) content = "";
        textView.setText(content);

        // 先清理复用状态
        textView.setTextColor(android.graphics.Color.BLACK);
        textView.setPaintFlags(textView.getPaintFlags()
                & (~android.graphics.Paint.UNDERLINE_TEXT_FLAG));
        textView.setOnClickListener(null);

        // ① 文件消息：[文件] xxx
        if (isFileMessage(content)) {
            textView.setTextColor(Color.BLUE);
            textView.setPaintFlags(textView.getPaintFlags()
                    | Paint.UNDERLINE_TEXT_FLAG);

            // 从 "[文件] test.png" 切出 "test.png"
            String fileName = content.substring("[文件]".length()).trim();
            if (fileName.startsWith("]")) {   // 兼容你可能写成 "[文件] test.png"
                fileName = fileName.substring(1).trim();
            }

            String finalFileName = fileName;
            textView.setOnClickListener(v -> {
                Context ctx = v.getContext();

                // 这里是你 Flask 静态文件的访问前缀，按你服务器实际 IP 改
                String baseUrl = "http://192.168.1.120:5000/uploads/";
                String downloadUrl = baseUrl + finalFileName;

                Intent intent = new Intent(ctx, DownloadService.class);
                intent.putExtra("url", downloadUrl);
                intent.putExtra("fileName", finalFileName);

                // 前台服务启动（Android 8+ 推荐这样）
                ContextCompat.startForegroundService(ctx, intent);
            });

            return; // 已经处理完“文件消息”，下面的 URL 逻辑就不用再走了
        }

        // 如果整条就是 URL，则高亮并设置点击
        if (isUrl(content)) {
            textView.setTextColor(android.graphics.Color.BLUE);
            textView.setPaintFlags(textView.getPaintFlags()
                    | android.graphics.Paint.UNDERLINE_TEXT_FLAG);

            String finalContent = content.trim();
            textView.setOnClickListener(v -> {
                String url = finalContent;
                // 支持 "www.baidu.com" 这种省略协议的写法
                if (url.startsWith("www.")) {
                    url = "https://" + url;
                }
                android.content.Intent intent =
                        new android.content.Intent(v.getContext(), Activity.WebViewActivity.class);
                intent.putExtra("url", url);
                v.getContext().startActivity(intent);
            });
        }
    }
    // 判断是不是 "[文件] xxx" 这种格式
    private boolean isFileMessage(String text) {
        if (text == null) return false;
        return text.trim().startsWith("[文件]");
    }


    // 简单判断一整条消息是不是一个 URL
    private boolean isUrl(String text) {
        if (text == null) return false;
        String lower = text.trim().toLowerCase();
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("www.");
    }



}

