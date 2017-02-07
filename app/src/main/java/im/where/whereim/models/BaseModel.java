package im.where.whereim.models;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by buganini on 17/01/17.
 */

public abstract class BaseModel {
    public abstract String getTableName();
    public abstract ContentValues buildContentValues(SQLiteDatabase db);
}
