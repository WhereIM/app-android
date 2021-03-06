package im.where.whereim;

import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import im.where.whereim.models.Channel;
import im.where.whereim.models.Marker;
import im.where.whereim.views.FilterBar;

public class PaneMarker extends BasePane {
    public PaneMarker() {
        // Required empty public constructor
    }

    private Handler mHandler = new Handler();

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private Channel mChannel;
    private Marker.List mMarkerList;
    private MarkerAdapter mAdapter;

    @Override
    protected void initChannel() {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(final CoreService.CoreBinder binder) {
                getChannel(new ChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(final Channel channel) {
                        mChannel = channel;

                        binder.addMarkerListener(channel, mMarkerListener);

                    }
                });
            }
        });
    }

    @Override
    protected void deinitChannel() {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                if(mChannel!=null){
                    binder.removeMarkerListener(mChannel, mMarkerListener);
                }
            }
        });
    }

    private class MarkerAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return 2;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            switch (groupPosition){
                case 0:
                    if(mMarkerList==null)
                        return 1;
                    return mMarkerList.public_list.size() > 0 ? mMarkerList.public_list.size() : 1;
                case 1:
                    if(mMarkerList==null)
                        return 1;
                    return mMarkerList.private_list.size() > 0 ? mMarkerList.private_list.size() : 1;
            }
            return 0;
        }

        private int viewType[] = {R.layout.marker_item, R.layout.placeholder_item, R.layout.no_match_placeholder_item};

        @Override
        public int getChildType(int groupPosition, int childPosition) {
            if(getChild(groupPosition, childPosition)!=null) {
                return 0;
            } else {
                if(mFilterKeyword == null) {
                    return 1;
                } else {
                    return 2;
                }
            }
        }

        @Override
        public int getChildTypeCount() {
            return viewType.length;
        }

        @Override
        public Object getGroup(int groupPosition) {
            switch (groupPosition){
                case 0:
                    return R.string.public_marker;
                case 1:
                    return R.string.private_marker;
            }
            return 0;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            switch (groupPosition){
                case 0:
                    if(mMarkerList==null)
                        return null;
                    if(childPosition<mMarkerList.public_list.size())
                        return mMarkerList.public_list.get(childPosition);
                    return null;
                case 1:
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
                if(m.enabled ==null){
                    this.loading.setVisibility(View.VISIBLE);
                    this.enable.setVisibility(View.GONE);
                }else{
                    this.loading.setVisibility(View.GONE);
                    this.enable.setVisibility(View.VISIBLE);
                    this.enable.setChecked(m.enabled);
                }
            }
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
            Marker m = (Marker) getChild(groupPosition, childPosition);
            if(m!=null){
                MarkerViewHolder vh;
                if(view==null) {
                    view = LayoutInflater.from(getActivity()).inflate(viewType[getChildType(groupPosition, childPosition)], parent, false);
                    vh = new MarkerViewHolder(view);
                    view.setTag(vh);
                } else {
                    vh = (MarkerViewHolder) view.getTag();
                }
                vh.setItem(m);
            }else{
                if(view==null)
                    view = LayoutInflater.from(getActivity()).inflate(viewType[getChildType(groupPosition, childPosition)], parent, false);
            }
            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    };

    private String mFilterKeyword = null;
    private FilterBar mFilter;
    private ExpandableListView mListView;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pane_marker, container, false);

        mFilter = (FilterBar) view.findViewById(R.id.filter);
        mListView = (ExpandableListView) view.findViewById(R.id.marker);

        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                mFilter.setCallback(new FilterBar.Callback() {
                    @Override
                    public void onFilter(String keyword) {
                        mFilterKeyword = keyword;
                        mHandler.post(mMarkerListener);
                    }
                });

                mAdapter = new MarkerAdapter();
                mListView.setAdapter(mAdapter);
                mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                    @Override
                    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                        ChannelActivity activity = (ChannelActivity) getActivity();
                        Marker marker = (Marker) mAdapter.getChild(groupPosition, childPosition);
                        if(marker!=null) {
                            setSizePolicy(ChannelActivity.PaneSizePolicy.FREE);
                            activity.moveToMarker(marker, true);
                        }
                        return true;
                    }
                });
                mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        long packedPos = mListView.getExpandableListPosition(position);
                        int itemType = ExpandableListView.getPackedPositionType(id);

                        if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                            int childPosition = ExpandableListView.getPackedPositionChild(packedPos);
                            int groupPosition = ExpandableListView.getPackedPositionGroup(packedPos);

                            Marker marker = (Marker) mAdapter.getChild(groupPosition, childPosition);
                            if(marker != null) {
                                Bundle data = new Bundle();
                                data.putString(PaneMarkerEdit.FIELD_ID, marker.id);
                                data.putDouble(PaneMarkerEdit.FIELD_LAT, marker.latitude);
                                data.putDouble(PaneMarkerEdit.FIELD_LNG, marker.longitude);
                                data.putString(PaneMarkerEdit.FIELD_NAME, marker.name);
                                data.putString(PaneMarkerEdit.FIELD_COLOR, marker.getIconColor());
                                data.putInt(PaneMarkerEdit.FIELD_RADIUS, marker.radius);
                                data.putBoolean(PaneMarkerEdit.FIELD_GEOFENCE, marker.geofence);
                                data.putBoolean(PaneMarkerEdit.FIELD_PUBLIC, marker.isPublic);
                                startPane(PaneMarkerEdit.class, data);
                                channelActivity.moveToMarker(marker, true);
                            }

                            return true;
                        }
                        return false;
                    }
                });
                for(int i=0;i<mAdapter.getGroupCount();i+=1){
                    mListView.expandGroup(i);
                }
                mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                    @Override
                    public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                        return true;
                    }
                });

                initChannel();
            }
        });

        return view;
    }

    private Runnable mMarkerListener = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(final CoreService.CoreBinder binder) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mMarkerList = binder.getChannelMarkers(mChannel.id, mFilterKeyword);
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }
    };

    @Override
    public void onDestroyView() {
        deinitChannel();
        super.onDestroyView();
    }
}
