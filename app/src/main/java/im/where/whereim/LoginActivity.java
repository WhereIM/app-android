package im.where.whereim;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
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

    private Handler mHandler = new Handler();
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
                            mPendingRegistration = new Registration("facebook", loginResult.getAccessToken().getUserId(), name);
                            processRegistration();
                        }
                    }.startTracking();
                }else{
                    mPendingRegistration = new Registration("facebook", loginResult.getAccessToken().getUserId(), profile.getName());
                    processRegistration();
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

    private void processRegistration() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mBinder==null || mPendingRegistration==null){
                    return;
                }
                Registration reg = mPendingRegistration;
                mPendingRegistration = null;
                mBinder.register_client(reg.auth_provider, reg.auth_id, reg.name, new Runnable(){

                    @Override
                    public void run() {
                        checkLogin();
                    }
                });
            }
        });
    }

    private void checkLogin(){
        if(getBinder().getClientId()==null){
            stopLoading();
        }else{
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
        if(mPendingRegistration!=null){
            processRegistration();
        }else{
            checkLogin();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
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
