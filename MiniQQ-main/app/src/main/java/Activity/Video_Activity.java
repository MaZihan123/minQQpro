package Activity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pretend_qq.R;

public class Video_Activity extends AppCompatActivity {

    private VideoView videoView;    // 视频控件
    private Button playButton;      // 播放按钮
    private Button stopButton;      // 停止按钮
    private Button pauseButton;     // 暂停按钮
    private boolean isPaused = false; // 用于标记视频是否已暂停

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video); // 设置布局文件

        // 获取控件
        videoView = findViewById(R.id.videoView);
        playButton = findViewById(R.id.play_button);
        stopButton = findViewById(R.id.stop_button);
        pauseButton = findViewById(R.id.pause_button); // 获取暂停按钮

        // 设置 VideoView 的视频资源（从 raw 文件夹加载视频）
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sample_video); // 这里的 sample_video.mp4 是你放在 raw 文件夹中的视频文件
        videoView.setVideoURI(videoUri);  // 设置视频文件的路径

        // 播放按钮的点击事件
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!videoView.isPlaying()) {  // 如果视频没有在播放
                    videoView.start();  // 开始播放
                    isPaused = false; // 确保暂停状态被重置
                    Toast.makeText(Video_Activity.this, "开始播放", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 暂停按钮的点击事件
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {  // 如果视频正在播放
                    videoView.pause();  // 暂停播放
                    isPaused = true; // 设置为暂停状态
                    Toast.makeText(Video_Activity.this, "暂停播放", Toast.LENGTH_SHORT).show();
                } else if (isPaused) {  // 如果视频是暂停状态
                    videoView.start();  // 恢复播放
                    isPaused = false; // 设置为播放状态
                    Toast.makeText(Video_Activity.this, "恢复播放", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 停止按钮的点击事件
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying() || isPaused) {
                    videoView.stopPlayback();  // 停止播放
                    videoView.resume();  // 重置并准备播放
                    isPaused = false; // 重置暂停标志
                    Toast.makeText(Video_Activity.this, "停止播放", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
