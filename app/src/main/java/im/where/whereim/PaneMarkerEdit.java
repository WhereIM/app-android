package im.where.whereim;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import im.where.whereim.dialogs.DialogIconPicker;
import im.where.whereim.dialogs.DialogOpenIn;
import im.where.whereim.dialogs.DialogPublic;
import im.where.whereim.dialogs.DialogShareLocation;
import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Marker;

public class PaneMarkerEdit extends BasePane {
    public static PaneMarkerEdit newInstance(Bundle args) {
        PaneMarkerEdit fragment = new PaneMarkerEdit();
        fragment.setArguments(args);
        return fragment;
    }

    public PaneMarkerEdit() {
        // Required empty public constructor
    }

    public final static String FIELD_ID = "id";
    public final static String FIELD_LAT = "lat";
    public final static String FIELD_LNG = "lng";
    public final static String FIELD_NAME = "name";
    public final static String FIELD_COLOR = "color";
    public final static String FIELD_RADIUS = "radius";
    public final static String FIELD_GEOFENCE = "geofence";
    public final static String FIELD_PUBLIC = "public";

    String mId;
    String mName;
    QuadTree.LatLng mLocation;
    String mColor;
    Integer mRadius;
    Boolean mGeofence = false;
    Boolean mIsPublic;

    TextView mEditName;
    ImageView mEditIcon;
    TextView mEditGeofence;
    View mButtonDelete;

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
        return mId==null && mLocation==null;
    }

    @Override
    public boolean clearOnChannelChanged() {
        return mId != null;
    }

    @Override
    protected void onSetArguments(Bundle args) {
        mId = args.getString(FIELD_ID);
        if(args.containsKey(FIELD_LAT) && args.containsKey(FIELD_LNG)){
            mLocation = new QuadTree.LatLng(args.getDouble(FIELD_LAT), args.getDouble(FIELD_LNG));
        }
        mName = args.getString(FIELD_NAME);
        mColor = args.getString(FIELD_COLOR, null);
        if(mColor == null){
            mColor = Marker.DEFAULT_COLOR;
        }
        if(args.containsKey(FIELD_RADIUS)){
            mRadius = args.getInt(FIELD_RADIUS);
        }else{
            mRadius = Config.GEOFENCE_RADIUS[Config.DEFAULT_GEOFENCE_RADIUS_INDEX];
        }
        mGeofence = args.getBoolean(FIELD_GEOFENCE);
        if(args.containsKey(FIELD_PUBLIC)){
            mIsPublic = args.getBoolean(FIELD_PUBLIC);
        }else{
            mIsPublic = null;
        }
        updateUI();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pane_marker_edit, container, false);

        mEditName = view.findViewById(R.id.name);
        mEditIcon = view.findViewById(R.id.icon);
        mEditGeofence = view.findViewById(R.id.geofence);
        mButtonDelete = view.findViewById(R.id.delete);

        mEditIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogIconPicker(getContext(), mColor, new DialogIconPicker.Callback() {
                    @Override
                    public void onSelected(String color) {
                        mColor = color;
                        updateUI();
                    }

                    @Override
                    public void onCanceled() {

                    }
                });
            }
        });

        mEditGeofence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle args = new Bundle();
                args.putBoolean(PaneGeofence.FIELD_ACTIVE, mGeofence);
                args.putInt(PaneGeofence.FIELD_RADIUS, mRadius);
                args.putBoolean(PaneGeofence.FIELD_APPLY, false);
                startPane(PaneGeofence.class, args);
            }
        });
        mButtonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(channelActivity)
                        .setTitle(R.string.delete)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                postBinderTask(new CoreService.BinderTask() {
                                    @Override
                                    public void onBinderReady(final CoreService.CoreBinder binder) {
                                        getChannel(new BaseChannelActivity.GetChannelCallback() {
                                            @Override
                                            public void onGetChannel(Channel channel) {
                                                binder.deleteMarker(channel.id, mId, mIsPublic);
                                            }
                                        });
                                        channelActivity.onBackPressed();
                                    }
                                });
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
            }
        });

        view.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelActivity.closeKeyboard();
                if(mIsPublic == null){
                    new DialogPublic(channelActivity, getString(R.string.save_marker), new DialogPublic.Callback() {
                        @Override
                        public void onSelected(boolean isPublic) {
                            mIsPublic = isPublic;
                            save();
                        }

                        @Override
                        public void onCanceled() {
                        }
                    });
                }else{
                    save();
                }

            }
        });

        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mLocation != null){
                    channelActivity.setPOI(null);
                }
                finish();
            }
        });

        view.findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QuadTree.LatLng location = channelActivity.getMapCenter();
                new DialogShareLocation(channelActivity, mEditName.getText().toString(), location.latitude, location.longitude);
            }
        });


        view.findViewById(R.id.open_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QuadTree.LatLng location = channelActivity.getMapCenter();
                new DialogOpenIn(getActivity(), null, location.latitude, location.longitude);
            }
        });

        return view;
    }

    @Override
    void onResult(Bundle data) {
        super.onResult(data);
        mGeofence = data.getBoolean(PaneGeofence.FIELD_ACTIVE);
        mRadius = data.getInt(PaneGeofence.FIELD_RADIUS);
        updateUI();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUI();
    }

    private void updateUI(){
        if(!isStarted() || getArguments()==null){
            return;
        }
        mEditName.setText(mName);
        if(mId == null){
            mButtonDelete.setVisibility(View.GONE);
        }
        mEditIcon.setImageResource(Marker.getIconResource(mColor));
        if(mGeofence){
            mEditGeofence.setText(getString(R.string.radius_m, mRadius));
        }else{
            mEditGeofence.setText(R.string.off);
        }
    }

    private void save(){
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(final CoreService.CoreBinder binder) {
                getChannel(new BaseChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(Channel channel) {
                        QuadTree.LatLng location = channelActivity.getMapCenter();
                        Marker marker = new Marker();
                        marker.channel_id = channel.id;
                        marker.id = mId;
                        marker.name = mEditName.getText().toString();
                        marker.latitude = location.latitude;
                        marker.longitude = location.longitude;
                        marker.isPublic = mIsPublic;
                        marker.attr = new JSONObject();
                        try {
                            marker.attr.put(Key.COLOR, mColor);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }
                        marker.geofence = mGeofence;
                        marker.radius = mRadius;

                        binder.setMarker(marker);
                        finish();
                    }
                });
            }
        });
    }
}
