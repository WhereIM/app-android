package im.where.whereim.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Created by buganini on 22/05/2017.
 */

public class FilterBar extends LinearLayout {
    public interface Callback {
        void onFilter(String keyword);
    }


    private EditText mKeyword;
    private Button mBtnClear;
    private Callback mCallback;

    public FilterBar(final Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mKeyword = new EditText(context);
        mBtnClear = new Button(context);

        setOrientation(LinearLayout.HORIZONTAL);

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

        LinearLayout.LayoutParams kwparams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT);
        kwparams.weight = 1;
        mKeyword.setLayoutParams(kwparams);
        addView(mKeyword);

        LinearLayout.LayoutParams btparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        mBtnClear.setLayoutParams(btparams);
        mBtnClear.setVisibility(View.GONE);
        mBtnClear.setText("✘");
        addView(mBtnClear);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }
}
