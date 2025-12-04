package Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pretend_qq.R;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import SQLite.UserDbHelper;


public class Revise_Avatar_Activity extends AppCompatActivity {

    // 定义常量
    private static final int REQUEST_CODE_VIEW = 1; //查看头像的请求码
    private static final int REQUEST_CODE_GALLERY = 2; //从相册选择的请求码

    private static final int REQUEST_CODE_CAMERA = 3;    // 拍照
    private static final int REQUEST_PERMISSION_CAMERA = 100;

    private byte[]main_avatar;

    private UserDbHelper db;
    private SharedPreferences sp;
    private int main_qq;

    // 定义控件
    private ImageView save_avatar;//保存头像
    private ImageView backIcon; //返回图标
    private ImageView avatar; //头像
    private byte[]avatar1;//新头像字节数组
    private LinearLayout menu; //选择菜单
    private TextView viewAvatar; //查看头像
    private TextView selectFromGallery; //从相册选择
    private TextView takePhoto;        // 拍照  ← 新增
    private TextView cancel; //取消

    //定义变量
    private Uri avatarUri; //头像的Uri

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // 这里使用默认返回的缩略图就够当头像用
            startActivityForResult(intent, REQUEST_CODE_CAMERA);
        } else {
            Toast.makeText(this, "没有可用的相机应用", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revise_avatar);
        db=UserDbHelper.getInstance(this);
        sp=getSharedPreferences("user_data",MODE_PRIVATE);
        main_qq=sp.getInt("main_qq",0);
        main_avatar=db.getAvatar(main_qq);

        // 初始化控件
        backIcon = findViewById(R.id.back_icon);
        avatar = findViewById(R.id.avatar);
        menu = findViewById(R.id.menu);
        takePhoto = findViewById(R.id.take_photo);
        viewAvatar = findViewById(R.id.view_avatar);
        selectFromGallery = findViewById(R.id.select_from_gallery);
        cancel = findViewById(R.id.cancel);
        save_avatar=findViewById(R.id.save_avatar);
        //执行保存头像的逻辑
        save_avatar.setOnClickListener(view -> {
            //获取ImageView的图片源，转换为Bitmap对象
            Bitmap bitmap = ((BitmapDrawable)avatar.getDrawable()).getBitmap();
            //创建一个字节数组输出流对象，用于存储压缩后的图片
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //使用Bitmap类的compress方法，将图片压缩为JPEG格式，质量为100%，输出到字节数组输出流对象中
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            //从字节数组输出流对象中获取字节数组，赋值给avatar变量
            avatar1= baos.toByteArray();
            //关闭字节数组输出流对象，释放资源
            try {
                baos.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            db.updateAvatar(main_qq,avatar1);
            db.updateAvatar_in_friend_table(main_qq,avatar1);//friend_table中的信息
            Toast.makeText(Revise_Avatar_Activity.this,"保存成功!",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Revise_Message_Activity.class));

        });

        //设置图片到控件上
        Bitmap bitmap= BitmapFactory.decodeByteArray(main_avatar,0,main_avatar.length);
        avatar.setImageBitmap(bitmap);

        // 设置返回图标的点击事件
        backIcon.setOnClickListener(view -> {
            startActivity(new Intent(Revise_Avatar_Activity.this,Revise_Message_Activity.class));
        });

        // 设置头像的点击事件,显示或隐藏选择菜单
        avatar.setOnClickListener(v -> {
            if (menu.getVisibility() == View.GONE) {
                menu.setVisibility(View.VISIBLE);
            } else {
                menu.setVisibility(View.GONE);
            }
        });

        //设置查看头像的点击事件跳转到查看头像的界面
        viewAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Revise_Avatar_Activity.this, ViewAvatarActivity.class);
            intent.putExtra("avatarUri", avatarUri); //传递头像的Uri
            intent.putExtra("identification","user");
            startActivityForResult(intent, REQUEST_CODE_VIEW); //启动查看头像的活动,并传递请求码
        });

        //设置从相册选择的点击事件,打开相册
        selectFromGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*"); // 设置选择的类型为图片
            startActivityForResult(intent, REQUEST_CODE_GALLERY); //启动相册,并传递请求码
        });
        takePhoto.setOnClickListener(v -> {
            // 先检查相机权限（Android 6.0+ 必须）
            if (ContextCompat.checkSelfPermission(
                    Revise_Avatar_Activity.this,
                    Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        Revise_Avatar_Activity.this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_PERMISSION_CAMERA
                );
            } else {
                openCamera();
            }
        });

        //设置取消的点击事件,隐藏选择菜单
        cancel.setOnClickListener(v -> menu.setVisibility(View.GONE));
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        //判断请求码和结果码
//        if (requestCode == REQUEST_CODE_VIEW && resultCode == RESULT_OK) {
//            //如果是从查看头像的界面返回,获取返回的数据
//            //修改完后貌似没啥用
//            boolean isChanged = data.getBooleanExtra("isChanged", false); //是否修改了头像
//            Uri newAvatarUri = data.getParcelableExtra("newAvatarUri"); //新的头像的Uri
//            if (isChanged) {
//                //如果修改了头像,更新头像的Uri和显示
//                avatarUri = newAvatarUri;
//                avatar.setImageURI(avatarUri);
//            }
//        } else if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK) {
//            //如果是从相册返回,获取选择的图片的Uri
//            Uri selectedImageUri = data.getData();
//            if (selectedImageUri != null) {
//                //如果选择了图片,更新头像的Uri和显示
//                avatarUri = selectedImageUri;
//                avatar.setImageURI(avatarUri);
//            }
//        }
//    }
    @Override
    public void onBackPressed(){
        //自定义返回按键
        startActivity(new Intent(this, Revise_Message_Activity.class));
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户同意之后再打开相机
                openCamera();
            } else {
                Toast.makeText(this, "未授予相机权限，无法拍照", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 如果不是成功结果，直接返回，避免空指针
        if (resultCode != RESULT_OK) {
            return;
        }

        // 1. 从“查看头像”界面返回
        if (requestCode == REQUEST_CODE_VIEW) {
            if (data == null) return;

            // 是否修改了头像
            boolean isChanged = data.getBooleanExtra("isChanged", false);
            // 新的头像 Uri
            Uri newAvatarUri = data.getParcelableExtra("newAvatarUri");

            if (isChanged && newAvatarUri != null) {
                // 更新头像 Uri 和显示
                avatarUri = newAvatarUri;
                avatar.setImageURI(avatarUri);
            }

            // 2. 从相册选择返回
        } else if (requestCode == REQUEST_CODE_GALLERY) {
            if (data == null) return;

            // 选择的图片 Uri
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // 更新头像 Uri 和显示
                avatarUri = selectedImageUri;
                avatar.setImageURI(avatarUri);
            }

            // 3. 从相机拍照返回
        } else if (requestCode == REQUEST_CODE_CAMERA) {
            if (data == null) return;

            // 相机返回的是一张缩略图，放在 extras 里
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap photo = (Bitmap) extras.get("data");
                if (photo != null) {
                    // 直接把 Bitmap 显示到头像上
                    avatar.setImageBitmap(photo);

                    // 【可选】把这张图插入相册，生成一个 Uri，方便“查看头像”页面使用
                    try {
                        String uriString = MediaStore.Images.Media.insertImage(
                                getContentResolver(),
                                photo,
                                "avatar_" + System.currentTimeMillis(),
                                null
                        );
                        if (uriString != null) {
                            avatarUri = Uri.parse(uriString);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }



}
