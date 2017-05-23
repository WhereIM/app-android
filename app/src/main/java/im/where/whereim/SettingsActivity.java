package im.where.whereim;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    private View mMapsGoogleSelected;
    private View mMapsMapboxSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
