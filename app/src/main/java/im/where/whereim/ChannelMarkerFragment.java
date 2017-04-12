package im.where.whereim;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import im.where.whereim.models.Channel;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;

public class ChannelMarkerFragment extends BaseFragment {
    public ChannelMarkerFragment() {
        // Required empty public constructor
    }

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private Channel mChannel;
    private ArrayList<Mate> mMateList;
    private Marker.List mMarkerList;
    private MarkerAdapter mAdapter;
    private class MarkerAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return 3;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            switch (groupPosition){
                case 0:
                    if(mMateList==null)
                        return 0;
                    return mMateList.size();
                case 1:
                    if(mMarkerList==null)
                        return 1;
                    return mMarkerList.public_list.size() > 0 ? mMarkerList.public_list.size() : 1;
                case 2:
                    if(mMarkerList==null)
                        return 1;
                    return mMarkerList.private_list.size() > 0 ? mMarkerList.private_list.size() : 1;
            }
            return 0;
        }

        private int viewType[] = {R.layout.mate_item, R.layout.marker_item, R.layout.placeholder_item};

        @Override
        public int getChildType(int groupPosition, int childPosition) {
            if(groupPosition==0){
                return 0;
            }else{
                if(getChild(groupPosition, childPosition)!=null) {
                    return 1;
                } else {
                    return 2;
                }
            }
        }

        @Override
        public int getChildTypeCount() {
            return 3;
        }

        @Override
        public Object getGroup(int groupPosition) {
            switch (groupPosition){
                case 0:
                    return R.string.mate;
                case 1:
                    return R.string.public_marker;
                case 2:
                    return R.string.private_marker;
            }
            return 0;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            switch (groupPosition){
                case 0:
                    if(mMateList==null)
                        return null;
                    return mMateList.get(childPosition);
                case 1:
                    if(mMarkerList==null)
                        return null;
                    if(childPosition<mMarkerList.public_list.size())
                        return mMarkerList.public_list.get(childPosition);
                    return null;
                case 2:
                    if(mMarkerList==null)
                        return null;
                    if(childPosition<mMarkerList.private_list.size())
                        return mMarkerList.private_list.get(childPosition);
                    return null;
            }
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            long r = 0;
            int i;
            for(i=0;i<groupPosition;i+=1){
                r += getChildrenCount(groupPosition);
            }
            r += childPosition;
            return r;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        private class GroupViewHolder {
            TextView name;

            public GroupViewHolder(View view) {
                this.name = (TextView) view.findViewById(R.id.name);
            }

            public void setItem(int title_id){
                this.name.setText(title_id);
            }
        }
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup parent) {
            GroupViewHolder vh;
            if(view==null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.header_item, parent, false);
                vh = new GroupViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (GroupViewHolder) view.getTag();
            }
            vh.setItem((Integer) getGroup(groupPosition));
            return view;
        }

        private class MateViewHolder {
            private Mate mate;
            TextView title;
            TextView subtitle;

            public MateViewHolder(View view) {
                this.title = (TextView) view.findViewById(R.id.title);
                this.subtitle = (TextView) view.findViewById(R.id.subtitle);
            }

            public void setItem(Mate m){
                mate = m;
                if(mate.user_mate_name !=null && !mate.user_mate_name.isEmpty()){
                    subtitle.setVisibility(View.VISIBLE);
                    title.setText(mate.user_mate_name);
                    subtitle.setText(mate.mate_name);
                }else{
                    subtitle.setVisibility(View.GONE);
                    title.setText(mate.mate_name);
                }
            }
        }

        private class MarkerViewHolder {
            private Marker marker;
            ImageView icon;
            TextView name;
            Switch enable;
            View loading;

            public MarkerViewHolder(View view) {
                this.icon = (ImageView) view.findViewById(R.id.icon);
                this.name = (TextView) view.findViewById(R.id.name);
                this.enable = (Switch) view.findViewById(R.id.enable);
                this.loading = view.findViewById(R.id.loading);
                this.enable.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                binder.toggleMarkerEnabled(marker);
                            }
                        });
                    }
                });
            }

            public void setItem(Marker m){
                marker = m;
                this.icon.setImageResource(m.getIconResId());
                this.name.setText(m.name);
                if(m.enable==null){
                    this.loading.setVisibility(View.VISIBLE);
                    this.enable.setVisibility(View.GONE);
                }else{
                    this.loading.setVisibility(View.GONE);
                    this.enable.setVisibility(View.VISIBLE);
                    this.enable.setChecked(m.enable);
                }
            }
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
            if(groupPosition==0){
                MateViewHolder vh;
                if(view==null){
                    view = LayoutInflater.from(getActivity()).inflate(viewType[getChildType(groupPosition, childPosition)], parent, false);
                    vh = new MateViewHolder(view);
                    view.setTag(vh);
                }else{
                    vh = (MateViewHolder) view.getTag();
                }
                Mate m = (Mate) getChild(groupPosition, childPosition);
                vh.setItem(m);
            }else{
                Marker m = (Marker) getChild(groupPosition, childPosition);
                if(m!=null){
                    MarkerViewHolder vh;
                    if(view==null){
                        view = LayoutInflater.from(getActivity()).inflate(viewType[getChildType(groupPosition, childPosition)], parent, false);
                        vh = new MarkerViewHolder(view);
                        view.setTag(vh);
                    }else{
                        vh = (MarkerViewHolder) view.getTag();
                    }
                    vh.setItem(m);
                }else{
                    if(view==null)
                        view = LayoutInflater.from(getActivity()).inflate(viewType[getChildType(groupPosition, childPosition)], parent, false);
                }
            }
            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    };

    private ExpandableListView mListView;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_marker, container, false);

        mListView = (ExpandableListView) view.findViewById(R.id.marker);

        getChannel(new ChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                mChannel = channel;

                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        mAdapter = new MarkerAdapter();
                        mListView.setAdapter(mAdapter);
                        mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                            @Override
                            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                                ChannelActivity activity = (ChannelActivity) getActivity();
                                if(groupPosition==0){
                                    Mate mate = (Mate) mAdapter.getChild(groupPosition, childPosition);
                                    activity.moveToMate(mate);
                                }else{
                                    Marker marker = (Marker) mAdapter.getChild(groupPosition, childPosition);
                                    if(marker!=null) {
                                        activity.moveToMaker(marker);
                                    }
                                }
                                return true;
                            }
                        });
                        for(int i=0;i<mAdapter.getGroupCount();i+=1){
                            mListView.expandGroup(i);
                        }

                        // disable click-to-collapse
                        mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                            @Override
                            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                                return true;
                            }
                        });

                        binder.addMateListener(channel, mMateListener);
                        binder.addMarkerListener(channel, mMarkerListener);
                    }
                });
            }
        });
        return view;
    }

    private Runnable mMateListener = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(final CoreService.CoreBinder binder) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mMateList = binder.getChannelMate(mChannel.id);
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }
    };

    private Runnable mMarkerListener = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(final CoreService.CoreBinder binder) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mMarkerList = binder.getChannelMarker(mChannel.id);
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }
    };

    @Override
    public void onDestroyView() {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.removeMateListener(mChannel, mMateListener);
                binder.removeMarkerListener(mChannel, mMarkerListener);
            }
        });
        super.onDestroyView();
    }
}
