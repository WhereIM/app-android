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

public class PaneMarkerEdit extends BasePane {
    public PaneMarkerEdit() {
        // Required empty public constructor
    }

    private final static String FIELD_ID = "id";
    private final static String FIELD_NAME = "name";
    private final static String FIELD_COLOR = "color";
    private final static String FIELD_RADIUS = "radius";
    private final static String FIELD_GEOFENCE = "geofence";
    private final static String FIELD_PUBLIC = "public";

    private Handler mHandler = new Handler();

    public static PaneMarkerEdit newInstance(String id, String name, String color, int radius, boolean geofence, Boolean isPublic) {
        PaneMarkerEdit fragment = new PaneMarkerEdit();

        Bundle args = new Bundle();
        args.putString(FIELD_ID, id);
        args.putString(FIELD_NAME, name);
        args.putString(FIELD_COLOR, color);
        args.putInt(FIELD_RADIUS, radius);
        args.putBoolean(FIELD_GEOFENCE, geofence);
        if(isPublic != null){
            args.putBoolean(FIELD_PUBLIC, isPublic);
        }
        fragment.setArguments(args);

        return fragment;
    }

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
        return mId == null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    String mId;
    String mColor;
    Integer mRadius;
    Boolean mGeofence;
    Boolean mIsPublic;

    TextView mEditName;
    ImageView mEditIcon;
    TextView mEditGeofence;
    View mButtonDelete;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pane_marker_edit, container, false);

        mEditName = view.findViewById(R.id.name);
        mEditIcon = view.findViewById(R.id.icon);
        mEditGeofence = view.findViewById(R.id.geofence);
        mButtonDelete = view.findViewById(R.id.delete);

        Bundle args = getArguments();
        mId = args.getString(FIELD_ID);
        mEditName.setText(args.getString(FIELD_NAME));
        mColor = args.getString(FIELD_COLOR, null);
        if(mColor == null){
            mColor = Marker.DEFAULT_COLOR;
        }
        mRadius = args.getInt(FIELD_RADIUS);
        mGeofence = args.getBoolean(FIELD_GEOFENCE);
        if(args.containsKey(FIELD_PUBLIC)){
            mIsPublic = args.getBoolean(FIELD_PUBLIC);
        }else{
            mIsPublic = null;
        }

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
                new DialogGeofence(getContext(), mGeofence, mRadius, new DialogGeofence.Callback() {
                    @Override
                    public void onDone(boolean active, int radius) {
                        mGeofence = active;
                        mRadius = radius;
                        updateUI();
                    }

                    @Override
                    public void onCanceled() {

                    }
                });
            }
        });
        if(mId == null){
            mButtonDelete.setVisibility(View.GONE);
        }
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
                            close();
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
                close();
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

        updateUI();

        return view;
    }

    private void updateUI(){
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
                        close();
                    }
                });
            }
        });
    }

    private void close(){
        channelActivity.onBackPressed();
    }
}
