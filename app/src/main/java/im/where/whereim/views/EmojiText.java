package im.where.whereim.views;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

/**
 * Created by buganini on 05/05/17.
 */

public class EmojiText extends SpannableString {
    private static Typeface sNotoColorEmoji;

    public EmojiText(Context context, String source) {
        super(source);
        setSpan(new CustomTypefaceSpan("", getTypeFace(context)), 0, source.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public  static Typeface getTypeFace(Context context){
        if (sNotoColorEmoji == null) {
            sNotoColorEmoji = Typeface.createFromAsset(context.getAssets(), "NotoColorEmoji.ttf");
        }
        return sNotoColorEmoji;
    }

    public static class CustomTypefaceSpan extends TypefaceSpan {

        private final Typeface newType;

        public CustomTypefaceSpan(String family, Typeface type) {
            super(family);
            newType = type;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            applyCustomTypeFace(ds, newType);
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            applyCustomTypeFace(paint, newType);
        }

        private static void applyCustomTypeFace(Paint paint, Typeface tf) {
            int oldStyle;
            Typeface old = paint.getTypeface();
            if (old == null) {
                oldStyle = 0;
            } else {
                oldStyle = old.getStyle();
            }

            int fake = oldStyle & ~tf.getStyle();
            if ((fake & Typeface.BOLD) != 0) {
                paint.setFakeBoldText(true);
            }

            if ((fake & Typeface.ITALIC) != 0) {
                paint.setTextSkewX(-0.25f);
            }

            paint.setTypeface(tf);
        }
    }
}
