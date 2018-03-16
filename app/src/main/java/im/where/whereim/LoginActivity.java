package im.where.whereim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.common.base.Optional;

public class LoginActivity extends BaseActivity {
    private CallbackManager mCallbackManager;

    private final static int RC_GOOGLE_SIGN_IN = 2;

    private Handler mHandler = new Handler();
    private View mLogin;
    private View mRetry;
    private View mLoading;

    private CoreService.BinderTask mTask = new CoreService.BinderTask(){

        @Override
        public void onBinderReady(final CoreService.CoreBinder binder) {
            startLoading();

            binder.register_client(mProvider, mToken, mAuthId, mName, new CoreService.RegisterClientCallback() {
                @Override
                public void onCaptchaRequired() {
                    Intent intent = new Intent(LoginActivity.this, CaptchaActivity.class);
                    startActivityForResult(intent, 0);
                }

                @Override
                public void onExhausted() {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, R.string.error_exhausted, Toast.LENGTH_LONG).show();
                            checkLogin();
                        }
                    }, 1500);
                }

                @Override
                public void onDone() {
                    checkLogin();
                }
            });
        }
    };

    private GoogleApiClient mGoogleApiClient;
    private String mProvider;
    private String mToken;
    private String mAuthId;
    private String mName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Config.SERVER_KEY_GOOGLE)
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(LoginActivity.this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        findViewById(R.id.google_login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
            }
        });

        mLoading = findViewById(R.id.loading);
        mLogin = findViewById(R.id.login);
        mRetry = findViewById(R.id.retry);

        mRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postBinderTask(mTask);
            }
        });

        mCallbackManager = CallbackManager.Factory.create();

        LoginButton fbLoginButton = (LoginButton) findViewById(R.id.fb_login_button);
        fbLoginButton.setReadPermissions("public_profile");
        fbLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                mTrial = true;
                mLogin.setVisibility(View.GONE);
                mProvider = Key.FACEBOOK;
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
                            mToken = loginResult.getAccessToken().getToken();
                            mName = name;

                            SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                            editor.putString(Key.PROVIDER, mProvider);
                            editor.putString(Key.ID, mAuthId);
                            editor.putString(Key.TOKEN, mToken);
                            editor.putString(Key.NAME, mName);
                            editor.apply();

                            postBinderTask(mTask);
                        }
                    }.startTracking();
                }else{
                    mAuthId = loginResult.getAccessToken().getUserId();
                    mToken = loginResult.getAccessToken().getToken();
                    mName = profile.getName();

                    SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString(Key.PROVIDER, mProvider);
                    editor.putString(Key.ID, mAuthId);
                    editor.putString(Key.TOKEN, mToken);
                    editor.putString(Key.NAME, mName);
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

        checkLogin();
    }

    boolean mTrial = false;
    private void checkLogin(){
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                if(binder.getClientId()==null){
                    stopLoading();
                    SharedPreferences sp = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                    mAuthId = Optional.fromNullable(sp.getString(Key.ID, null)).or(Optional.fromNullable(mAuthId)).orNull();
                    mToken = Optional.fromNullable(sp.getString(Key.TOKEN, null)).or(Optional.fromNullable(mToken)).orNull();
                    mName = Optional.fromNullable(sp.getString(Key.NAME, null)).or(Optional.fromNullable(mName)).orNull();
                    mProvider = Optional.fromNullable(sp.getString(Key.PROVIDER, null)).or(Optional.fromNullable(mProvider)).orNull();

                    SharedPreferences.Editor e = sp.edit();
                    e.remove(Key.ID);
                    e.remove(Key.TOKEN);
                    e.remove(Key.NAME);
                    e.remove(Key.PROVIDER);
                    e.apply();
                    if(mAuthId==null){
                        mLogin.setVisibility(View.VISIBLE);
                    }else{
                        mLogin.setVisibility(View.GONE);
                        if(!mTrial){
                            mTrial = true;
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
        });
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==0){
            postBinderTask(mTask);
            return;
        }

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount acct = result.getSignInAccount();
                mAuthId = acct.getId();
                mToken = acct.getIdToken();
                mName = acct.getDisplayName();
                mProvider = Key.GOOGLE;
                postBinderTask(mTask);
            }
        }

        if(FacebookSdk.isFacebookRequestCode(requestCode)) {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
            return;
        }
    }
}
