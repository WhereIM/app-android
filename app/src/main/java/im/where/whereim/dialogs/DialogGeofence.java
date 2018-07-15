package im.where.whereim.dialogs;

/**
 * Created by buganini on 10/06/17.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Arrays;

import im.where.whereim.Config;
import im.where.whereim.R;
import im.where.whereim.models.Marker;

public class DialogGeofence {
    public interface Callback {
        void onDone(boolean active, int radius);
        void onCanceled();
    }
    private View mRadiusSetting;
    private TextView mRadiusView;
    private Switch mActive;
    private int mRadius;
    public DialogGeofence(final Context context, boolean active, int radius, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_geofence,  null);
        mRadius = radius;
        mRadiusSetting = dialog_view.findViewById(R.id.radius_setting);
        mRadiusView = dialog_view.findViewById(R.id.radius);
        mActive = dialog_view.findViewById(R.id.active);
        mActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateUI();
            }
        });
        mActive.setChecked(active);
        dialog_view.findViewById(R.id.dec).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRadius -= Config.getRadiusStep(mRadius);
                updateUI();
            }
        });
        dialog_view.findViewById(R.id.inc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRadius += Config.getRadiusStep(mRadius);
                updateUI();
            }
        });
        new AlertDialog.Builder(context)
                .setView(dialog_view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onDone(mActive.isChecked(), mRadius);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onCanceled();
                    }
                }).show();
    }

    private void updateUI(){
        mRadiusSetting.setVisibility(mActive.isChecked() ? View.VISIBLE : View.GONE);
        mRadiusView.setText(String.valueOf(mRadius));
    }
}
