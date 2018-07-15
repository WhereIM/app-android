package im.where.whereim;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import im.where.whereim.ChannelActivity.PaneComp;

public class PaneRoot extends BasePane {
    public PaneRoot() {
        // Required empty public constructor
    }

    private Handler mHandler = new Handler();

    @Override
    protected ChannelActivity.PaneSizePolicy getInitialSizePolicy() {
        return ChannelActivity.PaneSizePolicy.TAB;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pane_root, container, false);

        view.findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelActivity.showPane(PaneComp.SEARCH);
            }
        });

        view.findViewById(R.id.marker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelActivity.showPane(PaneComp.MARKER);
            }
        });

        view.findViewById(R.id.message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelActivity.showPane(PaneComp.MESSAGE);
            }
        });

        view.findViewById(R.id.mates).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelActivity.showPane(PaneComp.MATE);
            }
        });

        return view;
    }
}
