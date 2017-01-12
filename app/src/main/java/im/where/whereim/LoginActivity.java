package im.where.whereim;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
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

    private View mLogin;
    private View mLoading;

    private static class Registration {
        String auth_provider;
        String auth_id;
        String name;

        public Registration(String auth_provider, String auth_id, String name) {
            this.auth_provider = auth_provider;
            this.auth_id = auth_id;
            this.name = name;
        }
    }

    private Registration mPendingRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mLoading = findViewById(R.id.loading);
        mLogin = findViewById(R.id.login);

        FacebookSdk.sdkInitialize(getApplicationContext());
        mCallbackManager = CallbackManager.Factory.create();

        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("public_profile");

        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            private Models.BinderTask mTask = new Models.BinderTask(){

                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    startLoading();
                    binder.register_client("facebook", mAuthId, mName, new Runnable(){

                        @Override
                        public void run() {
                            checkLogin();
                        }
                    });
                }
            };

            private String mAuthId;
            private String mName;
            @Override
            public void onSuccess(final LoginResult loginResult) {
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
                            postBinderTask(mTask);
                        }
                    }.startTracking();
                }else{
                    mAuthId = loginResult.getAccessToken().getUserId();
                    mName = profile.getName();
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

    private void checkLogin(){
        if(getBinder().getClientId()==null){
            stopLoading();
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
    }

    private void stopLoading(){
        mLoading.setVisibility(View.GONE);
        mLogin.setVisibility(View.VISIBLE);
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
