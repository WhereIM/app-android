package im.where.whereim;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelMapFragment extends BaseFragment implements CoreService.MapDataReceiver {
    protected Handler mHandler = new Handler();

    protected View mEnchantmentController;
    protected View mMarkerController;
    protected View mMarkerView;
    protected TextView mMarkerViewTitle;

    protected double mEditingLatitude;
    protected double mEditingLongitude;

    protected String mEditingName;
    protected int mEditingType = 0;
    protected int mEditingEnchantmentRadiusIndex = Config.DEFAULT_ENCHANTMENT_RADIUS_INDEX;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEnchantmentController = view.findViewById(R.id.enchantment_controller);
        view.findViewById(R.id.enchantment_enlarge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int n = mEditingEnchantmentRadiusIndex + 1;
                if(n < Config.ENCHANTMENT_RADIUS.length){
                    mEditingEnchantmentRadiusIndex = n;
                    refreshEditing();
                }
            }
        });
        view.findViewById(R.id.enchantment_reduce).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int n = mEditingEnchantmentRadiusIndex - 1;
                if(n >= 0){
                    mEditingEnchantmentRadiusIndex = n;
                    refreshEditing();
                }
            }
        });
        view.findViewById(R.id.enchantment_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postBinderTask(new Models.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new ChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Models.Channel channel) {
                                binder.createEnchantment(mEditingName, channel.id, mEditingLatitude, mEditingLongitude, Config.ENCHANTMENT_RADIUS[mEditingEnchantmentRadiusIndex], true);
                                mEditingType = 0;
                                refreshEditing();
                            }
                        });
                    }
                });
            }
        });

        mMarkerController = view.findViewById(R.id.marker_controller);
        view.findViewById(R.id.marker_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postBinderTask(new Models.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new ChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Models.Channel channel) {
                                binder.createMarker(mEditingName, channel.id, mEditingLatitude, mEditingLongitude);
                                mEditingType = 0;
                                refreshEditing();
                            }
                        });
                    }
                });

            }
        });
        view.findViewById(R.id.marker_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditingType = 0;
                refreshEditing();
            }
        });

    }

    abstract protected void refreshEditing();

    protected void startEditing(){
        if(mEditingType!=0){
            refreshEditing();
            return;
        }
        final View dialog_view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_map_object_create,  null);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();
        dialog_view.findViewById(R.id.enchantment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                final View dialog_view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_enchantment_create,  null);
                final EditText et_name = (EditText) dialog_view.findViewById(R.id.name);
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.create_enchantment)
                        .setView(dialog_view)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEditingName = et_name.getText().toString();
                                mEditingType = R.string.create_enchantment;
                                refreshEditing();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
            }
        });
        dialog_view.findViewById(R.id.marker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                final View dialog_view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_marker_create,  null);
                final EditText et_name = (EditText) dialog_view.findViewById(R.id.name);
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.create_marker)
                        .setView(dialog_view)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEditingName = et_name.getText().toString();
                                mEditingType = R.string.create_marker;
                                refreshEditing();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
            }
        });
        dialog.show();
    }
}
