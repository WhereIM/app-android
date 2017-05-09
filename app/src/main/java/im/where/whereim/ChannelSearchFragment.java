package im.where.whereim;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import im.where.whereim.models.POI;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelSearchFragment extends BaseFragment {
    protected Handler mHandler = new Handler();

    abstract protected void search(String keyword);
    abstract protected void autoComplete(String keyword);
    abstract protected BaseAdapter getSearchResultAdapter();
    abstract protected BaseAdapter getAutoCompleteAdapter();

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
            setSearchResult(new ArrayList<POI>());
            return;
        }

        mAdView.setVisibility(View.GONE);

        mLoading.setVisibility(View.VISIBLE);
        mListView.setAdapter(null);

        search(keyword);
    }

    protected ArrayList<POI> mSearchResult = new ArrayList<>();
    protected void setSearchResult(final ArrayList<POI> result){
        mSearch.setVisibility(View.GONE);
        mClear.setVisibility(View.VISIBLE);

        final ChannelActivity activity = (ChannelActivity) getActivity();
        activity.setSearchResult(result);
        mLoading.setVisibility(View.GONE);
        mSearchResult = result;
        mSearchResultAdapter.notifyDataSetChanged();
        mListView.setAdapter(mSearchResultAdapter);
    }

    protected ArrayList<String> mPredictions = new ArrayList<>();
    protected void setAutoComplete(final ArrayList<String> predictions){
        mAdView.setVisibility(View.GONE);
        mLoading.setVisibility(View.GONE);
        mPredictions = predictions;
        mAutoCompleteAdapter.notifyDataSetChanged();
        mListView.setAdapter(mAutoCompleteAdapter);
    }

    private EditText mKeyword;
    private Button mSearch;
    private Button mClear;
    private View mLoading;
    private ListView mListView;
    private View mAdView;
    private BaseAdapter mSearchResultAdapter = getSearchResultAdapter();
    private BaseAdapter mAutoCompleteAdapter = getAutoCompleteAdapter();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_search, container, false);

        mAdView = view.findViewById(R.id.adView);

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
                if(mListView.getAdapter()==mSearchResultAdapter){
                    final ChannelActivity activity = (ChannelActivity) getActivity();
                    if(activity!=null){
                        activity.moveToSearchResult(position);
                    }
                    return;
                }
                if(mListView.getAdapter()==mAutoCompleteAdapter){
                    String k = (String)mAutoCompleteAdapter.getItem(position);
                    do_search(k);
                    mKeyword.setText(k);
                }
            }
        });
        mListView.setAdapter(mSearchResultAdapter);

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
                if(s.length()==0){

                }else{
                    autoComplete(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;
    }
}
