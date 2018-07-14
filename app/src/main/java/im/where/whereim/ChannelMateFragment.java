package im.where.whereim;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
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
import android.widget.TextView;

import java.util.ArrayList;

import im.where.whereim.dialogs.DialogChannelInvite;
import im.where.whereim.dialogs.DialogEditMate;
import im.where.whereim.dialogs.DialogEditSelf;
import im.where.whereim.dialogs.DialogMatesInfo;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Mate;
import im.where.whereim.views.EmojiText;
import im.where.whereim.views.FilterBar;

public class ChannelMateFragment extends AuxFragment {
    public ChannelMateFragment() {
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
    private Mate mSelfMate;
    private ArrayList<Mate> mMateList;
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

                        binder.addMateListener(channel, mMateListener);

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
                    binder.removeMateListener(mChannel, mMateListener);
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
                    if(mSelfMate==null)
                        return 0;
                    return 1;
                case 1:
                    if(mMateList==null)
                        return 0;
                    return mMateList.size() > 0 ? mMateList.size() : 1;
            }
            return 0;
        }

        private int viewType[] = {R.layout.mate_item, R.layout.mate_placeholder_item, R.layout.no_match_placeholder_item};

        @Override
        public int getChildType(int groupPosition, int childPosition) {
            if(getChild(groupPosition, childPosition)!=null) {
                return 0;
            } else {
                if(mFilterKeyword == null){
                    return 1;
                }else {
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
                    return R.string.self;
                case 1:
                    return R.string.mate;
            }
            return 0;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            switch (groupPosition){
                case 0:
                    if(mSelfMate==null)
                        return null;
                    return mSelfMate;
                case 1:
                    if(mMateList==null)
                        return null;
                    if(childPosition < mMateList.size())
                        return mMateList.get(childPosition);
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
                this.name.setText(TextUtils.concat(getString(title_id), " ", new EmojiText(getActivity(), "ℹ️")));
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
            TextView indicator;
            TextView title;
            TextView subtitle;

            public MateViewHolder(View view) {
                this.indicator = (TextView) view.findViewById(R.id.indicator);
                this.title = (TextView) view.findViewById(R.id.title);
                this.subtitle = (TextView) view.findViewById(R.id.subtitle);
            }

            public void setItem(Mate m){
                mate = m;
                if(mate.user_mate_name !=null && !mate.user_mate_name.isEmpty()) {
                    subtitle.setVisibility(View.VISIBLE);
                    title.setText(mate.user_mate_name);
                    subtitle.setText(mate.mate_name);
                } else {
                    subtitle.setVisibility(View.GONE);
                    title.setText(mate.mate_name);
                }
                if(mate.latitude==null || mate.longitude==null) {
                    indicator.setTextColor(0xff7f7f7f);
                } else if (mate.stale) {
                    indicator.setTextColor(0xffff7f00);
                } else {
                    indicator.setTextColor(0xff00ff00);
                }
            }
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
            Mate m = (Mate) getChild(groupPosition, childPosition);

            if (m!=null) {
                MateViewHolder vh;
                if (view == null) {
                    view = LayoutInflater.from(getActivity()).inflate(viewType[getChildType(groupPosition, childPosition)], parent, false);
                    vh = new MateViewHolder(view);
                    view.setTag(vh);
                } else {
                    vh = (MateViewHolder) view.getTag();
                }
                vh.setItem(m);
            } else {
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

    private Mate mEditingMate;
    private ActionMode.Callback mSelfAction = new ActionMode.Callback() {
        private final static int ACTION_EDIT = 0;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(0, ACTION_EDIT, 0, "✏️");
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            mode.finish();
            switch(item.getItemId()) {
                case ACTION_EDIT:
                    Activity activity = getActivity();
                    new DialogEditSelf(activity, mEditingMate.mate_name, new DialogEditSelf.Callback() {
                        @Override
                        public void onEditSelf(final String name) {
                            postBinderTask(new CoreService.BinderTask() {
                                @Override
                                public void onBinderReady(CoreService.CoreBinder binder) {
                                    binder.editSelf(mEditingMate, name);
                                }
                            });

                        }
                    });
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    };
    private ActionMode.Callback mMateAction = new ActionMode.Callback() {
        private final static int ACTION_EDIT = 0;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(0, ACTION_EDIT, 0, "✏️");

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            mode.finish();
            switch(item.getItemId()) {
                case ACTION_EDIT:
                    Activity activity = getActivity();
                    new DialogEditMate(activity, mEditingMate.mate_name, mEditingMate.user_mate_name, new DialogEditMate.Callback() {
                        @Override
                        public void onEditMate(final String name) {
                            postBinderTask(new CoreService.BinderTask() {
                                @Override
                                public void onBinderReady(CoreService.CoreBinder binder) {
                                    binder.editMate(mEditingMate, name);
                                }
                            });
                        }
                    });
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    };

    private String mFilterKeyword = null;
    private FilterBar mFilter;
    private ExpandableListView mListView;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_mate, container, false);

        mFilter = view.findViewById(R.id.filter);
        mListView = view.findViewById(R.id.mate);

        view.findViewById(R.id.invite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getChannel(new BaseChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(final Channel channel) {
                        new DialogChannelInvite(getActivity(), channel);
                    }
                });
            }
        });

        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                mFilter.setCallback(new FilterBar.Callback() {
                    @Override
                    public void onFilter(String keyword) {
                        mFilterKeyword = keyword;
                        mHandler.post(mMateListener);
                    }
                });

                mAdapter = new MarkerAdapter();
                mListView.setAdapter(mAdapter);
                mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                    @Override
                    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                        ChannelActivity activity = (ChannelActivity) getActivity();
                        Mate mate = (Mate) mAdapter.getChild(groupPosition, childPosition);
                        if(mate!=null) {
                            setSizePolicy(ChannelActivity.AuxSize.FREE);
                            activity.moveToMate(mate, true);
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

                            if (groupPosition==0) {
                                mEditingMate = (Mate) mAdapter.getChild(groupPosition, childPosition);
                                if(mEditingMate!=null) {
                                    getActivity().startActionMode(mSelfAction);
                                }
                            } else if (groupPosition==1) {
                                mEditingMate = (Mate) mAdapter.getChild(groupPosition, childPosition);
                                if(mEditingMate!=null) {
                                    getActivity().startActionMode(mMateAction);
                                }
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
                    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                        switch(groupPosition){
                            case 0:
                            case 1:
                                new DialogMatesInfo(getActivity());
                                break;
                        }
                        return true; // disable click-to-collapse
                    }
                });

                initChannel();
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
                            ArrayList<Mate> list = new ArrayList<Mate>();
                            for (Mate mate : binder.getChannelMates(mChannel.id, mFilterKeyword)) {
                                if (mate.id.equals(mChannel.mate_id)) {
                                    mSelfMate = mate;
                                } else {
                                    list.add(mate);
                                }
                            }
                            mMateList = list;
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
