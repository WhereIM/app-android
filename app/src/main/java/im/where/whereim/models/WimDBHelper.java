package im.where.whereim.models;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by buganini on 17/01/17.
 */

public class WimDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "whereim.db";
    public static final int VERSION = 4;
    private static SQLiteDatabase mDatabase;

    public WimDBHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        mDatabase = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Channel.createTable(db);
        Message.createTable(db);
        Mate.createTable(db);
        Marker.createTable(db);
        Enchantment.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Mate.upgradeTable(db, oldVersion);
        Channel.upgradeTable(db, oldVersion);
    }

    public SQLiteDatabase getDatabase(){
        return mDatabase;
    }

    public void insert(BaseModel model){
        synchronized (this) {
            mDatabase.insertOrThrow(model.getTableName(), null, model.buildContentValues(mDatabase));
        }
    }

    public void replace(BaseModel model){
        synchronized (this) {
            mDatabase.replaceOrThrow(model.getTableName(), null, model.buildContentValues(mDatabase));
        }
    }
}
