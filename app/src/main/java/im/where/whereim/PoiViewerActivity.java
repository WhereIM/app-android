package im.where.whereim;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import im.where.whereim.models.POI;

public class PoiViewerActivity extends BaseActivity {

    public POI poi = new POI();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        poi.latitude = data.getDouble(Key.LATITUDE);
        poi.longitude = data.getDouble(Key.LONGITUDE);
        poi.name = data.getString(Key.NAME);

        setContentView(R.layout.activity_poi_viewer);

        Fragment fragment = null;
        switch(Config.getMapProvider(this)){
            case GOOGLE:
                fragment = new GooglePoiViewerFragment();
                break;
            case MAPBOX:
                fragment = new MapboxPoiViewerFragment();
                break;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.frame, fragment);
        transaction.commit();

        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.open_in_channel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(Key.LATITUDE, poi.latitude);
                intent.putExtra(Key.LONGITUDE, poi.longitude);
                intent.putExtra(Key.NAME, poi.name);
                setResult(1, intent);
                finish();
            }
        });
    }
}
