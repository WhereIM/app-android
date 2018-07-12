package im.where.whereim.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import im.where.whereim.R;

/**
 * Created by buganini on 22/05/2017.
 */

public class FilterBar extends FrameLayout {
    public interface Callback {
        void onFilter(String keyword);
    }

    private EditText mKeyword;
    private ImageView mBtnClear;
    private Callback mCallback;

    public FilterBar(final Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(R.layout.view_filterbar, null);
        mKeyword = view.findViewById(R.id.keyword);
        mBtnClear = view.findViewById(R.id.clear);
        mKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String k = mKeyword.getText().toString().trim();
                if(k.isEmpty()) {
                    if(mCallback!=null){
                        mCallback.onFilter(null);
                    }
                    mBtnClear.setVisibility(View.GONE);
                } else {
                    if(mCallback!=null){
                        mCallback.onFilter(k);
                    }
                    mBtnClear.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mBtnClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mKeyword.getWindowToken(), 0);

                mKeyword.setText("");
                mKeyword.clearFocus();
                if(mCallback!=null) {
                    mCallback.onFilter(null);
                }
            }
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);
        addView(view);
        requestLayout();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }
}
