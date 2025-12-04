package Activity;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pretend_qq.R;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.web_view);

        // 基本设置
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);   // 有些站点需要 JS
        settings.setDomStorageEnabled(true);   // H5 本地存储

        // 在 WebView 内部打开链接，而不是跳系统浏览器
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        // 从 Intent 里拿链接
        String url = getIntent().getStringExtra("url");
        if (url == null || url.trim().length() == 0) {
            url = "https://www.baidu.com/";   // 没传就默认一个
        }
        webView.loadUrl(url);
    }

    // 支持网页内回退
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
