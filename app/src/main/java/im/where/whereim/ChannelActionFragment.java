package im.where.whereim;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import im.where.whereim.ChannelActivity.AuxComp;

public class ChannelActionFragment extends AuxFragment {
    public ChannelActionFragment() {
        // Required empty public constructor
    }

    private Handler mHandler = new Handler();

    @Override
    protected ChannelActivity.AuxSize getInitialSizePolicy() {
        return ChannelActivity.AuxSize.TAB;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_action, container, false);

        view.findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelActivity.showAux(AuxComp.SEARCH);
            }
        });

        view.findViewById(R.id.marker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelActivity.showAux(AuxComp.MARKER);
            }
        });

        view.findViewById(R.id.message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelActivity.showAux(AuxComp.MESSAGE);
            }
        });

        view.findViewById(R.id.mates).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                channelActivity.showAux(AuxComp.MATE);
            }
        });

        return view;
    }
}
