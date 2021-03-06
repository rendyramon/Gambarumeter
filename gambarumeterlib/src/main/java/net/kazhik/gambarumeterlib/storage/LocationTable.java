package net.kazhik.gambarumeterlib.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kazhik on 14/11/08.
 */
public class LocationTable extends AbstractTable {
    public static final String TABLE_NAME = "gm_location";
    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    "timestamp DATETIME PRIMARY KEY," +
                    "start_time DATETIME," +
                    "latitude REAL," +
                    "longitude REAL," +
                    "altitude REAL," +
                    "accuracy REAL)";
    private static final String TAG = "LocationTable";

    public LocationTable(Context context) {
        super(context);
    }
    public LocationTable(Context context, SQLiteDatabase db) {
        super(context, db);
    }
    public static void init(SQLiteDatabase db){
        db.execSQL(CREATE_TABLE);

    }
    public static void upgrade(SQLiteDatabase db) {
        AbstractTable.upgrade(db, TABLE_NAME, CREATE_TABLE);
    }

    public int insert(long startTime, Location loc) {
        ContentValues values = new ContentValues();

        values.put("timestamp", this.formatDateMsec(loc.getTime()));
        values.put("start_time", this.formatDateMsec(startTime));
        values.put("latitude", loc.getLatitude());
        values.put("longitude", loc.getLongitude());
        values.put("altitude", loc.getAltitude());
        values.put("accuracy", loc.getAccuracy());

        return (int)this.db.insertOrThrow(TABLE_NAME, null, values);

    }

    public List<Location> selectAll(long startTime) {
        return this.selectAll(startTime, 0);
    }
    public List<Location> selectAll(long startTime, int max) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_NAME);

        String[] columns = { "timestamp, latitude, longitude, altitude, accuracy" };
        String selection = "start_time = ?";
        String[] selectionArgs = {this.formatDateMsec(startTime)};
        String sortOrder = "timestamp";
        String limit = (max == 0)? null: Integer.toString(max);

        Cursor cursor = qb.query(this.db, columns, selection, selectionArgs, null,
                null, sortOrder, limit);

        List<Location> dataList = new ArrayList<>();

        if (cursor.getCount() == 0) {
            return dataList;
        }

        cursor.moveToFirst();
        try {
            while (!cursor.isAfterLast()) {
                Location loc = new Location("");
                loc.setTime(this.parseDate(cursor.getString(0)));
                loc.setLatitude(cursor.getDouble(1));
                loc.setLongitude(cursor.getDouble(2));
                loc.setAltitude(cursor.getDouble(3));
                loc.setAccuracy(cursor.getFloat(4));
                dataList.add(loc);
                cursor.moveToNext();
            }
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        cursor.close();

        return dataList;
    }
    public boolean delete(long startTime) {
        String where = "start_time = ?";
        String[] whereArgs = {this.formatDateMsec(startTime)};
        int deleted = this.db.delete(TABLE_NAME, where, whereArgs);
        if (deleted == 0) {
            whereArgs[0] = this.formatDateSec(startTime);
            deleted = this.db.delete(TABLE_NAME, where, whereArgs);
        }
        return (deleted > 0);
    }
    public boolean clean(long startTime) {
        String where = "start_time <= ?";
        String[] whereArgs = {this.formatDateMsec(startTime)};
        int deleted = this.db.delete(TABLE_NAME, where, whereArgs);
        return (deleted > 0);
    }
    public String getTableName(){
        return TABLE_NAME;
    }


}
