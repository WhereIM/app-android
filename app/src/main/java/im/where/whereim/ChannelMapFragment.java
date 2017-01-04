package im.where.whereim;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.maps.SupportMapFragment;

public class ChannelMapFragment extends SupportMapFragment {
    private CoreService.CoreBinder mBinder;

    public ChannelMapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mBinder = ((ChannelActivity) context).getBinder();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBinder = null;
    }
}
