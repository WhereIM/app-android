package im.where.whereim;

import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Channel;

public class PaneForgeLocation extends BasePane {
    public PaneForgeLocation() {
        // Required empty public constructor
    }

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

        view.findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new BaseChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Channel channel) {
                                binder.deactivateChannel(channel);
                                finish();
                            }
                        });
                    }
                });
            }
        });

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
                                finish();
                            }
                        });
                    }
                });
            }
        });

        return view;
    }
}
