package im.where.whereim;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import im.where.whereim.dialogs.DialogGeofence;
import im.where.whereim.dialogs.DialogIconPicker;
import im.where.whereim.dialogs.DialogOpenIn;
import im.where.whereim.dialogs.DialogPublic;
import im.where.whereim.dialogs.DialogShareLocation;
import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Marker;

public class PaneMarkerView extends BasePane {
    public PaneMarkerView() {
        // Required empty public constructor
    }

    private final static String FIELD_ID = "id";

    private Handler mHandler = new Handler();

    public static PaneMarkerView newInstance(String id) {
        PaneMarkerView fragment = new PaneMarkerView();

        Bundle args = new Bundle();
        args.putString(FIELD_ID, id);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected ChannelActivity.PaneSizePolicy getInitialSizePolicy() {
        return ChannelActivity.PaneSizePolicy.WRAP;
    }

    public void setMarker(Marker marker){
        Bundle args = new Bundle();
        args.putString(FIELD_ID, marker.id);
        setArguments(args);

        mId = marker.id;
        mMarker = marker;
        updateUI();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    Marker mMarker;
    String mId;
    TextView mName;
    ImageView mIcon;
    TextView mGeofence;
    View mButtonEdit;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pane_marker_view, container, false);

        mName = view.findViewById(R.id.name);
        mIcon = view.findViewById(R.id.icon);
        mGeofence = view.findViewById(R.id.geofence);
        mButtonEdit = view.findViewById(R.id.edit);

        Bundle args = getArguments();
        mId = args.getString(FIELD_ID);


        getChannel(new BaseChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        mMarker = binder.getChannelMarker(channel.id, mId);
                        mButtonEdit.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                channelActivity.editMarker(mMarker.id, new QuadTree.LatLng(mMarker.latitude, mMarker.longitude), mMarker.name, mMarker.getIconColor(), mMarker.radius, mMarker.geofence, mMarker.isPublic);
                            }
                        });

                        view.findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                QuadTree.LatLng location = channelActivity.getMapCenter();
                                new DialogShareLocation(channelActivity, mMarker.name, location.latitude, location.longitude);
                            }
                        });


                        view.findViewById(R.id.open_in).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                QuadTree.LatLng location = channelActivity.getMapCenter();
                                new DialogOpenIn(getActivity(), null, location.latitude, location.longitude);
                            }
                        });

                    }
                });

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
        mIcon.setImageResource(mMarker.getIconResId());
        mName.setText(mMarker.name);
        if(mMarker.geofence){
            mGeofence.setText(getString(R.string.radius_m, mMarker.radius));
        }else{
            mGeofence.setText(R.string.off);
        }
    }
}
