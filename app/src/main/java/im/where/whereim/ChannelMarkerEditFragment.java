package im.where.whereim;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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

public class ChannelMarkerEditFragment extends AuxFragment {
    public ChannelMarkerEditFragment() {
        // Required empty public constructor
    }

    private final static String FIELD_NAME = "name";
    private final static String FIELD_COLOR = "color";
    private final static String FIELD_PUBLIC = "public";

    private Handler mHandler = new Handler();

    public static ChannelMarkerEditFragment newInstance(String name, String color, boolean isPublic) {
        ChannelMarkerEditFragment fragment = new ChannelMarkerEditFragment();

        Bundle args = new Bundle();
        args.putString(FIELD_NAME, name);
        args.putString(FIELD_COLOR, color);
        args.putBoolean(FIELD_PUBLIC, isPublic);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected ChannelActivity.AuxSize getInitialSizePolicy() {
        return ChannelActivity.AuxSize.WRAP;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    String mColor;

    TextView mNameEdit;
    ImageView mIconEdit;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_marker_edit, container, false);

        Bundle args = getArguments();
        mColor = args.getString(FIELD_COLOR, null);
        if(mColor == null){
            mColor = Marker.DEFAULT_COLOR;
        }
        mNameEdit = view.findViewById(R.id.name);
        mIconEdit = view.findViewById(R.id.icon);
        mNameEdit.setText(args.getString(FIELD_NAME));
        mIconEdit.setImageResource(Marker.getIconResource(mColor));
        mIconEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogIconPicker(getContext(), mColor, new DialogIconPicker.Callback() {
                    @Override
                    public void onSelected(String color) {
                        mColor = color;
                        mIconEdit.setImageResource(Marker.getIconResource(mColor));
                    }

                    @Override
                    public void onCanceled() {

                    }
                });
            }
        });

        view.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelActivity.closeKeyboard();
                new DialogPublic(channelActivity, getString(R.string.save_marker), new DialogPublic.Callback() {
                    @Override
                    public void onSelected(final boolean isPublic) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(final CoreService.CoreBinder binder) {
                                getChannel(new BaseChannelActivity.GetChannelCallback() {
                                    @Override
                                    public void onGetChannel(Channel channel) {
                                        QuadTree.LatLng location = channelActivity.getMapCenter();
                                        Marker marker = new Marker();
                                        marker.channel_id = channel.id;
                                        marker.id = null;
                                        marker.name = mNameEdit.getText().toString();
                                        marker.latitude = location.latitude;
                                        marker.longitude = location.longitude;
                                        marker.isPublic = isPublic;
                                        marker.attr = new JSONObject();
                                        try {
                                            marker.attr.put(Key.COLOR, mColor);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            return;
                                        }

                                        binder.setMarker(marker);
                                        close();
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onCanceled() {
                        close();
                    }
                });
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
                new DialogShareLocation(channelActivity, mNameEdit.getText().toString(), location.latitude, location.longitude);
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

    private void close(){
        channelActivity.onBackPressed();
    }
}
