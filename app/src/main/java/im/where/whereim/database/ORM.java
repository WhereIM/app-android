package im.where.whereim.database;

import android.content.ContentValues;

/**
 * Created by buganini on 17/01/17.
 */

public abstract class ORM {
    public abstract String getTableName();
    public abstract ContentValues buildInsert();
}
