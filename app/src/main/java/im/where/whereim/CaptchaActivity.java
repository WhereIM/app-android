package im.where.whereim;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CaptchaActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captcha);

        WebView webView = (WebView) findViewById(R.id.recaptcha);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        WebViewClient webViewClient = new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith(Config.CAPTCHA_PREFIX)) {
                    final String otp = url.substring(Config.CAPTCHA_PREFIX.length());
                    postBinderTask(new CoreService.BinderTask() {
                        @Override
                        public void onBinderReady(CoreService.CoreBinder binder) {
                            binder.setOTP(otp);
                            setResult(0);
                            finish();
                        }
                    });
                    return true;
                } else {
                    return false;
                }
            }
        };
        webView.setWebViewClient(webViewClient);
        webView.loadUrl(Config.CAPTCHA_URL);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
    }
}
