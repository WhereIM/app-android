package im.where.whereim;

import android.content.Context;

public abstract class BaseChannelFragment extends BaseFragment {
    ChannelActivity channelActivity;

    protected abstract void initChannel();
    protected abstract void deinitChannel();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        channelActivity = (ChannelActivity) getActivity();
    }

    @Override
    public void onDetach() {
        channelActivity = null;
        super.onDetach();
    }
}
