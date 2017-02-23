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
            return 2;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if(mEnchantmentList==null)
                return 0;
            switch (groupPosition){
                case 0:
                    return mEnchantmentList.public_list.size();
                case 1:
                    return mEnchantmentList.private_list.size();
            }
            return 0;
        }

        @Override
        public Object getGroup(int groupPosition) {
            switch (groupPosition){
                case 0:
                    return R.string.is_public;
                case 1:
                    return R.string.is_private;
            }
            return 0;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            if(mEnchantmentList==null)
                return null;
            switch (groupPosition){
                case 0:
                    return mEnchantmentList.public_list.get(childPosition);
                case 1:
                    return mEnchantmentList.private_list.get(childPosition);
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
                                binder.toggleEnchantmentEnabled(enchantment);
                            }
                        });
                    }
                });
            }

            public void setItem(Enchantment e){
                enchantment = e;
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
        }
        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
            ChildViewHolder vh;
            if(view==null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.enchantment_item, parent, false);
                vh = new ChildViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ChildViewHolder) view.getTag();
            }
            Enchantment e = (Enchantment) getChild(groupPosition, childPosition);
            vh.setItem(e);
            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
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

                        // disable click-to-collapse
                        mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                            @Override
                            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                                return true;
                            }
                        });

                        binder.addEnchantmentListener(channel, mEnchantmentListener);
                    }
                });
            }
        });
        return view;
    }

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
                binder.removeEnchantmentListener(mChannel, mEnchantmentListener);
            }
        });
        super.onDestroyView();
    }
}
