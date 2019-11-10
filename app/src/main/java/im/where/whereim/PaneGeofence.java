package im.where.whereim;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Switch;

public class PaneGeofence extends BasePane {
    public PaneGeofence() {
        // Required empty public constructor
    }

    public final static String ACTION_RADIUS = "radius";
    public final static String ACTION_GEOFENCE = "geofence";

    public final static String FIELD_RADIUS = "radius";
    public final static String FIELD_ACTIVE = "active";
    public final static String FIELD_APPLY = "apply";

    private String mAction;
    private int mRadius = Config.DEFAULT_GEOFENCE_RADIUS;
    private NumberPicker mRadiusPicker;
    private Switch mActiveSwitch;
    private boolean mActive = true;
    private boolean mApply = true;
    private View mBtnOk;
    private View mBtnSave;

    @Override
    protected ChannelActivity.PaneSizePolicy getInitialSizePolicy() {
        return ChannelActivity.PaneSizePolicy.WRAP;
    }

    @Override
    public boolean lockFocus() {
        return true;
    }

    @Override
    public boolean showCrosshair() {
        return false;
    }

    @Override
    public boolean clearOnChannelChanged() {
        return true;
    }

    @Override
    protected void onSetArguments(Bundle args) {
        if(args != null){
            mAction = args.getString(BasePane.FIELD_ACTION);
            mActive = args.getBoolean(FIELD_ACTIVE);
            mRadius = args.getInt(FIELD_RADIUS);
            mApply = args.getBoolean(FIELD_APPLY);
        }
        updateUI();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pane_geofence,  null);
        mRadiusPicker = view.findViewById(R.id.radius);
        mActiveSwitch = view.findViewById(R.id.active);

        mRadiusPicker.setMinValue(0);
        mRadiusPicker.setMaxValue(Config.GEOFENCE_RADIUS.length - 1);
        mRadiusPicker.setWrapSelectorWheel(false);
        String[] rs = new String[Config.GEOFENCE_RADIUS.length];
        for(int i=0;i<Config.GEOFENCE_RADIUS.length;i+=1){
            rs[i] = getString(R.string.radius_m, Config.GEOFENCE_RADIUS[i]);
        }
        mRadiusPicker.setDisplayedValues(rs);
        int i;
        for(i=0;i<Config.GEOFENCE_RADIUS.length-1;i+=1){
            if(Config.GEOFENCE_RADIUS[i] >= mRadius){
                break;
            }
        }
        mRadiusPicker.setValue(i);
        mRadiusPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
                mRadius = Config.GEOFENCE_RADIUS[newVal];
            }
        });


        mActiveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mActive = b;
                updateUI();
            }
        });
        mBtnOk = view.findViewById(R.id.ok);
        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle data = new Bundle();
                data.putString(BasePane.FIELD_ACTION, mAction);
                data.putBoolean(FIELD_ACTIVE, mActive);
                data.putInt(FIELD_RADIUS, mRadius);
                setResult(data);
                finish();
            }
        });
        mBtnSave = view.findViewById(R.id.save);
        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle data = new Bundle();
                data.putBoolean(FIELD_ACTIVE, mActive);
                data.putInt(FIELD_RADIUS, mRadius);
                setResult(data);
                finish();
            }
        });
        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUI();
    }

    private void updateUI(){
        if(!isStarted()){
            return;
        }
        mActiveSwitch.setChecked(mActive);
        mRadiusPicker.setVisibility(mActiveSwitch.isChecked() ? View.VISIBLE : View.GONE);
        mBtnOk.setVisibility(mApply ? View.GONE : View.VISIBLE);
        mBtnSave.setVisibility(mApply ? View.VISIBLE : View.GONE);
    }
}
