package Activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.pretend_qq.R;

import java.util.ArrayList;
import java.util.List;

public class ContactsListActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1; // 请求权限的请求码
    private TextView contactInfoTextView;     // 用于显示联系人信息的 TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list); // 设置布局文件

        // 初始化 TextView
        contactInfoTextView = findViewById(R.id.contact_info);

        // 检查权限，如果没有权限则请求权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            // 获取联系人
            getContacts();
        } else {
            // 请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CODE);
        }
    }

    // 获取联系人信息
    private void getContacts() {
        // 定义一个列表来存储联系人信息
        List<String> phoneNumbers = new ArrayList<>();
        List<String> contactNames = new ArrayList<>();

        // 确保查询姓名和电话号码列
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,  // 联系人姓名
                ContactsContract.CommonDataKinds.Phone.NUMBER       // 联系人电话号码
        };

        // 查询联系人数据，确保查询指定的列
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,  // 查询 URI
                projection,  // 需要查询的列
                null,  // 没有筛选条件
                null,  // 没有排序条件
                null   // 没有排序顺序
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                // 获取联系人姓名和电话号码的列索引
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                if (nameIndex >= 0 && numberIndex >= 0) {  // 确保找到列
                    // 获取联系人姓名和电话号码
                    String contactName = cursor.getString(nameIndex);
                    String phoneNumber = cursor.getString(numberIndex);

                    contactNames.add(contactName);
                    phoneNumbers.add(phoneNumber);
                }
            }
            cursor.close();
        }

        // 在这里，你可以将获取到的联系人姓名和电话号码显示出来或做其他处理
        if (!contactNames.isEmpty() && !phoneNumbers.isEmpty()) {
            // 将第一个联系人的信息显示在 TextView 中
            String contactInfo = "联系人: " + contactNames.get(0) + "\n电话: " + phoneNumbers.get(0);
            contactInfoTextView.setText(contactInfo);  // 显示在 TextView 中
        } else {
            contactInfoTextView.setText("没有找到联系人");  // 如果没有联系人，则显示提示信息
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予后获取联系人
                getContacts();
            } else {
                Toast.makeText(this, "权限被拒绝，无法获取联系人", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
