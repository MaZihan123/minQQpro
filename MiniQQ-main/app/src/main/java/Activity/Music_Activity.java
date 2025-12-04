package Activity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pretend_qq.R;

public class Music_Activity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;  // 媒体播放器
    private Button playButton;        // 播放按钮
    private Button pauseButton;       // 暂停/继续按钮
    private Button stopButton;        // 停止按钮
    private boolean isPaused = false; // 用于标记是否处于暂停状态

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music); // 设置布局文件

        // 获取按钮控件
        playButton = findViewById(R.id.play_button);
        pauseButton = findViewById(R.id.pause_button);
        stopButton = findViewById(R.id.stop_button);

        // 初始化 MediaPlayer，设置资源为 raw 目录下的 song.mp3 文件
        mediaPlayer = MediaPlayer.create(this, R.raw.song);

        // 播放按钮的点击事件
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mediaPlayer.isPlaying() && !isPaused) {
                    mediaPlayer.start();  // 开始播放
                    Toast.makeText(Music_Activity.this, "开始播放", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 暂停/继续按钮的点击事件
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();  // 暂停播放
                    isPaused = true; // 更新暂停状态
                    Toast.makeText(Music_Activity.this, "暂停播放", Toast.LENGTH_SHORT).show();
                } else if (isPaused) {
                    mediaPlayer.start();  // 恢复播放
                    isPaused = false; // 更新为播放状态
                    Toast.makeText(Music_Activity.this, "继续播放", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 停止按钮的点击事件
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying() || isPaused) {
                    mediaPlayer.stop();  // 停止播放
                    mediaPlayer.reset(); // 重置播放器
                    mediaPlayer = MediaPlayer.create(Music_Activity.this, R.raw.song); // 重新初始化播放器
                    isPaused = false; // 重置暂停标志
                    Toast.makeText(Music_Activity.this, "停止播放", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 在 Activity 销毁时释放资源
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}
