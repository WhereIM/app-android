package im.where.whereim;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    private View mMapsGoogleSelected;
    private View mMapsMapboxSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        mMapsGoogleSelected = findViewById(R.id.maps_google_selected);
        mMapsMapboxSelected = findViewById(R.id.maps_mapbox_selected);

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


        SharedPreferences settings = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        TextView logout = findViewById(R.id.logout);
        logout.setText(String.format("%s (%s)", getString(R.string.logout, settings.getString(Key.NAME, null)), settings.getString(Key.PROVIDER, null)));
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.logout)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent logout = new Intent(SettingsActivity.this, CoreService.class);
                                logout.putExtra(Key.ACTION, "logout");
                                startService(logout);

                                Intent service = new Intent(SettingsActivity.this, CoreService.class);
                                stopService(service);
                                startService(service);
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
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
    }
}
