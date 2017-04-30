package im.where.whereim;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;

import java.util.ArrayList;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelSearchFragment extends BaseFragment {
    protected Handler mHandler = new Handler();

    public static class SearchResult {
        String name;
        Double latitude;
        Double longitude;
    }

    abstract protected void search(String keyword);
    abstract protected BaseAdapter getAdapter();

    private void do_search(String keyword){
        final ChannelActivity activity = (ChannelActivity) getActivity();
        if(activity!=null) {
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

        if(keyword.isEmpty()){
            mAdView.setVisibility(View.VISIBLE);
            setSearchResult(new ArrayList<SearchResult>());
            return;
        }

        mAdView.setVisibility(View.GONE);

        mLoading.setVisibility(View.VISIBLE);
        mListView.setAdapter(null);

        search(keyword);
    }

    protected ArrayList<SearchResult> mSearchResult = new ArrayList<>();
    protected void setSearchResult(final ArrayList<SearchResult> result){
        mSearch.setVisibility(View.GONE);
        mClear.setVisibility(View.VISIBLE);

        final ChannelActivity activity = (ChannelActivity) getActivity();
        activity.setSearchResult(result);
        mLoading.setVisibility(View.GONE);
        mSearchResult = result;
        mAdapter.notifyDataSetChanged();
        mListView.setAdapter(mAdapter);
    }

    private EditText mKeyword;
    private Button mSearch;
    private Button mClear;
    private View mLoading;
    private ListView mListView;
    private NativeExpressAdView mAdView;
    private BaseAdapter mAdapter = getAdapter();

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mClear = (Button) view.findViewById(R.id.clear);
        mClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKeyword.setText("");
                do_search("");
                mSearch.setVisibility(View.VISIBLE);
                mClear.setVisibility(View.GONE);
            }
        });

        mSearch = (Button) view.findViewById(R.id.search);
        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String q = mKeyword.getText().toString();
                do_search(q);
            }
        });

        mListView = (ListView) view.findViewById(R.id.results);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ChannelActivity activity = (ChannelActivity) getActivity();
                if(activity!=null){
                    activity.moveToSearchResult(position);
                }
            }
        });
        mListView.setAdapter(mAdapter);

        mAdView = (NativeExpressAdView) view.findViewById(R.id.adView);

        AdRequest request = new AdRequest.Builder()
                .build();
        mAdView.loadAd(request);

        mLoading = view.findViewById(R.id.loading);

        mKeyword = (EditText) view.findViewById(R.id.keyword);
        mKeyword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String q = mKeyword.getText().toString();
                    do_search(q);
                    handled = true;
                }
                return handled;
            }
        });
        mKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mClear.setVisibility(View.GONE);
                mSearch.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}
