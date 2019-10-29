package im.where.whereim.views;

import android.content.Context;
import android.graphics.Typeface;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

/**
 * Created by buganini on 05/05/17.
 */

public class EmojiView extends androidx.appcompat.widget.AppCompatTextView {
    private static Typeface sNotoColorEmoji;

    public EmojiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setTypeface(_getTypeFace(context));
    }

    private Typeface _getTypeFace(Context context){
        if (sNotoColorEmoji == null) {
            sNotoColorEmoji = Typeface.createFromAsset(context.getAssets(), "NotoColorEmoji.ttf");
        }
        return sNotoColorEmoji;
    }

}
