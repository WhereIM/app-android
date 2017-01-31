package im.where.whereim.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by buganini on 17/01/17.
 */

public class WimDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "whereim.db";
    public static final int VERSION = 1;
    private static SQLiteDatabase mDatabase;

    public WimDBHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        mDatabase = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Message.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public SQLiteDatabase getDatabase(){
        return mDatabase;
    }

    public void insert(ORM orm){
        mDatabase.insertOrThrow(orm.getTableName(), null, orm.buildInsert());
    }
}
