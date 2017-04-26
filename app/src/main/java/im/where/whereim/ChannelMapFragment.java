package im.where.whereim;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import im.where.whereim.models.Channel;
import im.where.whereim.models.Enchantment;
import im.where.whereim.models.Marker;
import im.where.whereim.view.Joystick;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelMapFragment extends BaseFragment implements CoreService.MapDataDelegate {
    protected Handler mHandler = new Handler();

    protected View mEnchantmentController;
    protected View mMarkerController;
    protected View mMarkerView;
    protected TextView mMarkerViewTitle;
    protected Joystick mJoystick;

    protected double mEditingLatitude;
    protected double mEditingLongitude;

    protected Marker mEditingMarker = new Marker();
    protected Enchantment mEditingEnchantment = new Enchantment();
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
        view.findViewById(R.id.enchantment_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditingType = 0;
                refreshEditing();
            }
        });
        view.findViewById(R.id.enchantment_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new ChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Channel channel) {
                                binder.createEnchantment(mEditingEnchantment.name, channel.id, mEditingEnchantment.isPublic, mEditingLatitude, mEditingLongitude, Config.ENCHANTMENT_RADIUS[mEditingEnchantmentRadiusIndex], true);
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
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new ChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Channel channel) {
                                binder.createMarker(mEditingMarker.name, channel.id, mEditingMarker.isPublic, mEditingLatitude, mEditingLongitude, mEditingMarker.attr, true);
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

        final ToggleButton mock = (ToggleButton) view.findViewById(R.id.mock);
        mock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    postBinderTask(new CoreService.BinderTask() {
                        @Override
                        public void onBinderReady(CoreService.CoreBinder binder) {
                            binder.startMocking();
                        }
                    });
                }else{
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.mock)
                            .setMessage(R.string.mock_confirm_disable)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    postBinderTask(new CoreService.BinderTask() {
                                        @Override
                                        public void onBinderReady(CoreService.CoreBinder binder) {
                                            binder.stopMocking();
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mock.setChecked(true);
                                }
                            }).show();
                }
            }
        });
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                mock.setChecked(binder.isMocking());
            }
        });

        mJoystick = (Joystick) view.findViewById(R.id.joystick);
        mJoystick.addListener(mJoystickCallback);
    }

    @Override
    public void onDestroyView() {
        mJoystick.removeListener(mJoystickCallback);
        super.onDestroyView();
    }

    private Joystick.Callback mJoystickCallback = new Joystick.Callback(){

        @Override
        public void callback(final float x, final float y) {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    binder.moveMocking(x, y);
                }
            });
        }
    };

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
                final CheckBox isPublic = (CheckBox) dialog_view.findViewById(R.id.ispublic);
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.create_enchantment)
                        .setView(dialog_view)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEditingEnchantment.name = et_name.getText().toString();
                                mEditingEnchantment.isPublic = isPublic.isChecked();
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
                final CheckBox isPublic = (CheckBox) dialog_view.findViewById(R.id.ispublic);
                final Spinner icon = (Spinner) dialog_view.findViewById(R.id.icon);
                icon.setAdapter(new BaseAdapter() {
                    private String[] icon = Marker.getIconList();

                    class ViewHolder {
                        ImageView icon;

                        public ViewHolder(View view) {
                            view.setTag(this);
                            this.icon = (ImageView) view.findViewById(R.id.icon);
                        }

                        void setItem(String color){
                            icon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), Marker.getIconResource(color), null));
                        }
                    }

                    @Override
                    public int getCount() {
                        return icon.length;
                    }

                    @Override
                    public String getItem(int position) {
                        return icon[position];
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public View getView(int position, View view, ViewGroup parent) {
                        ViewHolder vh;
                        if(view==null){
                            view = LayoutInflater.from(getActivity()).inflate(R.layout.icon_item, parent, false);
                            vh = new ViewHolder(view);
                        }else{
                            vh = (ViewHolder) view.getTag();
                        }
                        vh.setItem(getItem(position));
                        return view;
                    }
                });
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.create_marker)
                        .setView(dialog_view)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEditingMarker.name = et_name.getText().toString();
                                mEditingMarker.isPublic = isPublic.isChecked();
                                mEditingMarker.attr = new JSONObject();
                                try {
                                    mEditingMarker.attr.put(Key.COLOR, icon.getSelectedItem());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
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
