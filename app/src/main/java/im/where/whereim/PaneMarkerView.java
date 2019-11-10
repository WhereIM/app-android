package im.where.whereim;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import im.where.whereim.dialogs.DialogOpenIn;
import im.where.whereim.dialogs.DialogShareLocation;
import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Marker;

public class PaneMarkerView extends BasePane {
    public PaneMarkerView() {
        // Required empty public constructor
    }

    public final static String FIELD_ID = "id";

    @Override
    protected void onSetArguments(Bundle args) {
        mId = args.getString(FIELD_ID);
        updateUI();
    }

    @Override
    protected ChannelActivity.PaneSizePolicy getInitialSizePolicy() {
        return ChannelActivity.PaneSizePolicy.WRAP;
    }

    @Override
    public boolean requireFocus() {
        return true;
    }

    @Override
    public boolean clearOnChannelChanged() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    String mId;
    TextView mName;
    ImageView mIcon;
    TextView mEditGeofence;
    View mButtonEdit;
    Marker mMarker;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pane_marker_view, container, false);

        mName = view.findViewById(R.id.name);
        mIcon = view.findViewById(R.id.icon);
        mEditGeofence = view.findViewById(R.id.geofence);
        mButtonEdit = view.findViewById(R.id.edit);

        mButtonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mMarker == null){
                    return;
                }
                Bundle data = new Bundle();
                data.putString(PaneMarkerEdit.FIELD_ID, mMarker.id);
                data.putDouble(PaneMarkerEdit.FIELD_LAT, mMarker.latitude);
                data.putDouble(PaneMarkerEdit.FIELD_LNG, mMarker.longitude);
                data.putString(PaneMarkerEdit.FIELD_NAME, mMarker.name);
                data.putString(PaneMarkerEdit.FIELD_COLOR, mMarker.getIconColor());
                data.putInt(PaneMarkerEdit.FIELD_RADIUS, mMarker.radius);
                data.putBoolean(PaneMarkerEdit.FIELD_GEOFENCE, mMarker.geofence);
                data.putBoolean(PaneMarkerEdit.FIELD_PUBLIC, mMarker.isPublic);
                startPane(PaneMarkerEdit.class, data);
            }
        });
        mEditGeofence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle args = new Bundle();
                args.putBoolean(PaneGeofence.FIELD_ACTIVE, mMarker.geofence);
                args.putInt(PaneGeofence.FIELD_RADIUS, mMarker.radius);
                args.putBoolean(PaneGeofence.FIELD_APPLY, true);
                startPane(PaneGeofence.class, args);
            }
        });

        view.findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mMarker == null){
                    return;
                }
                QuadTree.LatLng location = channelActivity.getMapCenter();
                new DialogShareLocation(channelActivity, mMarker.name, location.latitude, location.longitude);
            }
        });


        view.findViewById(R.id.open_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mMarker == null){
                    return;
                }
                new DialogOpenIn(getActivity(), null, mMarker.latitude, mMarker.longitude);
            }
        });

        return view;
    }

    @Override
    boolean onResult(final Bundle data) {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(final CoreService.CoreBinder binder) {
                getChannel(new BaseChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(Channel channel) {
                        Marker marker = binder.getChannelMarker(channel.id, mId);
                        marker.geofence = data.getBoolean(PaneGeofence.FIELD_ACTIVE);
                        marker.radius = data.getInt(PaneGeofence.FIELD_RADIUS);

                        binder.setMarker(marker);
                    }
                });
            }
        });
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        getChannel(new BaseChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        binder.addMarkerListener(channel, mMarkerListener);
                    }
                });

            }
        });
        updateUI();
    }

    @Override
    public void onStop() {
        getChannel(new BaseChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        binder.removeMarkerListener(channel, mMarkerListener);
                    }
                });

            }
        });
        super.onStop();
    }

    private Runnable mMarkerListener = new Runnable() {
        @Override
        public void run() {
            updateUI();
        }
    };

    private void updateUI(){
        if(!isStarted()){
            return;
        }
        getChannel(new BaseChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        mMarker = binder.getChannelMarker(channel.id, mId);
                        if(mMarker == null){
                            return;
                        }
                        mIcon.setImageResource(mMarker.getIconResId());
                        mName.setText(mMarker.name);
                        if(mMarker.geofence){
                            mEditGeofence.setText(getString(R.string.radius_m, mMarker.radius));
                        }else{
                            mEditGeofence.setText(R.string.off);
                        }
                    }
                });

            }
        });
    }
}
