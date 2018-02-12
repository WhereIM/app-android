package im.where.whereim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    private View mMapsGoogleSelected;
    private View mMapsMapboxSelected;
    private View mResetTips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        mMapsGoogleSelected = findViewById(R.id.maps_google_selected);
        mMapsMapboxSelected = findViewById(R.id.maps_mapbox_selected);
        mResetTips = findViewById(R.id.reset_tips);

        findViewById(R.id.maps_google).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Config.setMapProvider(SettingsActivity.this, Config.MapProvider.GOOGLE);
                updateUI();
            }
        });

        findViewById(R.id.maps_mapbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Config.setMapProvider(SettingsActivity.this, Config.MapProvider.MAPBOX);
                updateUI();
            }
        });

        mResetTips.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                editor.remove(Key.TIP_ACTIVE_CHANNEL);
                editor.remove(Key.TIP_ACTIVE_CHANNEL_2);
                editor.remove(Key.TIP_ENTER_CHANNEL);
                editor.remove(Key.TIP_INVITE_CHANNEL);
                editor.remove(Key.TIP_NEW_CHANNEL);
                editor.apply();
                updateUI();
            }
        });

        findViewById(R.id.about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
                startActivity(intent);
            }
        });

        updateUI();
    }

    private void updateUI(){
        switch(Config.getMapProvider(this)){
            case GOOGLE:
                mMapsGoogleSelected.setVisibility(View.VISIBLE);
                mMapsMapboxSelected.setVisibility(View.INVISIBLE);
                break;
            case MAPBOX:
                mMapsGoogleSelected.setVisibility(View.INVISIBLE);
                mMapsMapboxSelected.setVisibility(View.VISIBLE);
                break;
        }

        boolean resettable = false;
        SharedPreferences sp = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        resettable = resettable || sp.getBoolean(Key.TIP_ACTIVE_CHANNEL, false);
        resettable = resettable || sp.getBoolean(Key.TIP_ACTIVE_CHANNEL_2, false);
        resettable = resettable || sp.getBoolean(Key.TIP_ENTER_CHANNEL, false);
        resettable = resettable || sp.getBoolean(Key.TIP_INVITE_CHANNEL, false);
        resettable = resettable || sp.getBoolean(Key.TIP_NEW_CHANNEL, false);
        mResetTips.setEnabled(resettable);
    }
}
