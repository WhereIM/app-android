package im.where.whereim;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Switch;
import android.widget.TextView;

import im.where.whereim.models.Channel;
import im.where.whereim.models.Enchantment;

public class ChannelEnchantmentFragment extends BaseFragment {
    public ChannelEnchantmentFragment() {
        // Required empty public constructor
    }

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private Channel mChannel;
    private Enchantment.List mEnchantmentList;
    private EnchantmentAdapter mAdapter;
    private class EnchantmentAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return 3;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            switch (groupPosition){
                case 0:
                    return 1;
                case 1:
                    if(mEnchantmentList==null)
                        return 1;
                    return mEnchantmentList.public_list.size() > 0 ? mEnchantmentList.public_list.size() : 1;
                case 2:
                    if(mEnchantmentList==null)
                        return 1;
                    return mEnchantmentList.private_list.size() > 0 ? mEnchantmentList.private_list.size() : 1;
            }
            return 0;
        }

        @Override
        public int getChildType(int groupPosition, int childPosition) {
            if(groupPosition==0){
                return 0;
            }else{
                if(getChild(groupPosition, childPosition)!=null) {
                    return 0;
                } else {
                    return 1;
                }
            }
        }

        @Override
        public int getChildTypeCount() {
            return 2;
        }

        @Override
        public Object getGroup(int groupPosition) {
            switch (groupPosition){
                case 0:
                    return R.string.self;
                case 1:
                    return R.string.public_enchantment;
                case 2:
                    return R.string.private_enchantment;
            }
            return 0;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            switch (groupPosition){
                case 0:
                    return null;
                case 1:
                    if(mEnchantmentList==null)
                        return null;
                    if(childPosition<mEnchantmentList.public_list.size())
                        return mEnchantmentList.public_list.get(childPosition);
                    return null;
                case 2:
                    if(mEnchantmentList==null)
                        return null;
                    if(childPosition<mEnchantmentList.private_list.size())
                        return mEnchantmentList.private_list.get(childPosition);
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

        private class ChildViewHolder {
            private Enchantment enchantment;
            private Channel channel;
            TextView name;
            Switch enable;
            View loading;

            public ChildViewHolder(View view) {
                this.name = (TextView) view.findViewById(R.id.name);
                this.enable = (Switch) view.findViewById(R.id.enable);
                this.loading = view.findViewById(R.id.loading);
                this.enable.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                if(enchantment!=null) {
                                    binder.toggleEnchantmentEnabled(enchantment);
                                }
                                if(channel!=null){
                                    binder.toggleRadiusEnabled(channel);
                                }
                            }
                        });
                    }
                });
            }

            public void setItem(Enchantment e){
                enchantment = e;
                channel = null;
                this.name.setText(e.name);
                if(e.enable==null){
                    this.loading.setVisibility(View.VISIBLE);
                    this.enable.setVisibility(View.GONE);
                }else{
                    this.loading.setVisibility(View.GONE);
                    this.enable.setVisibility(View.VISIBLE);
                    this.enable.setChecked(e.enable);
                }
            }

            public void setItem(Channel c){
                enchantment = null;
                channel = c;
                this.name.setText(getString(R.string.radius_m, (int)c.radius));
                if(c.enable_radius ==null){
                    this.loading.setVisibility(View.VISIBLE);
                    this.enable.setVisibility(View.GONE);
                }else{
                    this.loading.setVisibility(View.GONE);
                    this.enable.setVisibility(View.VISIBLE);
                    this.enable.setChecked(c.enable_radius);
                }
            }
        }
        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
            if(groupPosition==0){
                ChildViewHolder vh;
                if(view==null){
                    view = LayoutInflater.from(getActivity()).inflate(R.layout.enchantment_item, parent, false);
                    vh = new ChildViewHolder(view);
                    view.setTag(vh);
                }else{
                    vh = (ChildViewHolder) view.getTag();
                }
                vh.setItem(mChannel);
            }else{
                Enchantment e = (Enchantment) getChild(groupPosition, childPosition);
                if(e!=null) {
                    ChildViewHolder vh;
                    if (view == null) {
                        view = LayoutInflater.from(getActivity()).inflate(R.layout.enchantment_item, parent, false);
                        vh = new ChildViewHolder(view);
                        view.setTag(vh);
                    } else {
                        vh = (ChildViewHolder) view.getTag();
                    }
                    vh.setItem(e);
                }else{
                    if (view == null) {
                        view = LayoutInflater.from(getActivity()).inflate(R.layout.placeholder_item, parent, false);
                    }
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
        View view = inflater.inflate(R.layout.fragment_channel_enchantment, container, false);

        mListView = (ExpandableListView) view.findViewById(R.id.enchantment);

        getChannel(new ChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                mChannel = channel;

                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        mAdapter = new EnchantmentAdapter();
                        mListView.setAdapter(mAdapter);
                        for(int i=0;i<mAdapter.getGroupCount();i+=1){
                            mListView.expandGroup(i);
                        }
                        mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                            @Override
                            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                                Enchantment enchantment = (Enchantment) mAdapter.getChild(groupPosition, childPosition);
                                if(enchantment!=null){
                                    ChannelActivity activity = (ChannelActivity) getActivity();
                                    activity.moveToEnchantment(enchantment);
                                    return true;
                                }
                                return false;
                            }
                        });
                        // disable click-to-collapse
                        mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                            @Override
                            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                                return true;
                            }
                        });

                        binder.addChannelChangedListener(channel.id, mChannelListener);
                        binder.addEnchantmentListener(channel, mEnchantmentListener);
                    }
                });
            }
        });
        return view;
    }

    private Runnable mChannelListener = new Runnable() {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private Runnable mEnchantmentListener = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(final CoreService.CoreBinder binder) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mEnchantmentList = binder.getChannelEnchantment(mChannel.id);
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
                binder.removeChannelChangedListener(mChannel.id, mChannelListener);
                binder.removeEnchantmentListener(mChannel, mEnchantmentListener);
            }
        });
        super.onDestroyView();
    }
}
