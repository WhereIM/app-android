package im.where.whereim;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

public class LoginActivity extends BaseActivity {
    private CallbackManager mCallbackManager;

    private Handler mHandler = new Handler();
    private View mLogin;
    private View mRetry;
    private View mLoading;

    private Models.BinderTask mTask = new Models.BinderTask(){

        @Override
        public void onBinderReady(CoreService.CoreBinder binder) {
            startLoading();
            binder.register_client(mProvider, mAuthId, mName, new Runnable(){

                @Override
                public void run() {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkLogin();
                        }
                    }, 1500);
                }
            });
        }
    };

    private String mProvider;
    private String mAuthId;
    private String mName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mLoading = findViewById(R.id.loading);
        mLogin = findViewById(R.id.login);
        mRetry = findViewById(R.id.retry);

        mRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postBinderTask(mTask);
            }
        });

        FacebookSdk.sdkInitialize(getApplicationContext());
        mCallbackManager = CallbackManager.Factory.create();

        LoginButton fbLoginButton = (LoginButton) findViewById(R.id.fb_login_button);
        fbLoginButton.setReadPermissions("public_profile");
        fbLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                mLogin.setVisibility(View.GONE);
                mProvider = "facebook";
                startLoading();
                Profile profile = Profile.getCurrentProfile();
                if(profile==null){
                    new ProfileTracker() {
                        @Override
                        protected void onCurrentProfileChanged(
                                Profile oldProfile,
                                Profile currentProfile) {
                            String name = currentProfile.getName();
                            stopTracking();

                            mAuthId = loginResult.getAccessToken().getUserId();
                            mName = name;

                            SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                            editor.putString(Models.KEY_PROVIDER, mProvider);
                            editor.putString(Models.KEY_ID, mAuthId);
                            editor.putString(Models.KEY_NAME, mName);
                            editor.apply();

                            postBinderTask(mTask);
                        }
                    }.startTracking();
                }else{
                    mAuthId = loginResult.getAccessToken().getUserId();
                    mName = profile.getName();

                    SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString(Models.KEY_PROVIDER, mProvider);
                    editor.putString(Models.KEY_ID, mAuthId);
                    editor.putString(Models.KEY_NAME, mName);
                    editor.apply();

                    postBinderTask(mTask);
                }
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });
    }

    boolean mAutoTry = false;
    private void checkLogin(){
        if(getBinder().getClientId()==null){
            stopLoading();
            SharedPreferences sp = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            mAuthId = sp.getString(Models.KEY_ID, null);
            mName = sp.getString(Models.KEY_NAME, null);
            mProvider = sp.getString(Models.KEY_PROVIDER, null);
            if(mAuthId==null){
                mLogin.setVisibility(View.VISIBLE);
            }else{
                mLogin.setVisibility(View.GONE);
                if(!mAutoTry){
                    mAutoTry = true;
                    postBinderTask(mTask);
                }else{
                    mRetry.setVisibility(View.VISIBLE);
                }
            }
        }else{
            Log.e("LoginActivity", "start ChannelListActivity");
            Intent intent = new Intent(LoginActivity.this, ChannelListActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void startLoading(){
        mLoading.setVisibility(View.VISIBLE);
        mLogin.setVisibility(View.GONE);
        mRetry.setVisibility(View.GONE);
    }

    private void stopLoading(){
        mLoading.setVisibility(View.GONE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        checkLogin();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(FacebookSdk.isFacebookRequestCode(requestCode)) {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
            return;
        }
    }
}
