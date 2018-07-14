package im.where.whereim;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;

import im.where.whereim.ChannelActivity.AuxComp;
import im.where.whereim.dialogs.DialogEditMarker;
import im.where.whereim.dialogs.DialogEditMate;
import im.where.whereim.dialogs.DialogEditSelf;
import im.where.whereim.dialogs.DialogMatesInfo;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;
import im.where.whereim.views.EmojiText;
import im.where.whereim.views.FilterBar;

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
