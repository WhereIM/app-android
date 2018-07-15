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

public class PaneForgeLocation extends BasePane {
    public PaneForgeLocation() {
        // Required empty public constructor
    }

    private Handler mHandler = new Handler();

    @Override
    protected ChannelActivity.PaneSizePolicy getInitialSizePolicy() {
        return ChannelActivity.PaneSizePolicy.WRAP;
    }

    @Override
    public boolean showCrosshair() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pane_forge_location, container, false);

        view.findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new BaseChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Channel channel) {
                                QuadTree.LatLng location = channelActivity.getMapCenter();
                                binder.forgeLocation(channelActivity, channel, location.latitude, location.longitude);
                                close();
                            }
                        });
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

        return view;
    }

    private void close(){
        channelActivity.onBackPressed();
    }
}
