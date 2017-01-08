package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;


final class GridWalkingDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME         = "GridWalking.db";

    private static final String GRID_TABLE_NAME       = "grid";
    private static final String GRID_COLUMN_KEY       = "key";
    private static final String GRID_COLUMN_LEVEL     = "level";

    private static final String BONUS_TABLE_NAME      = "bonus";
    private static final String BONUS_COLUMN_KEY      = "key";

    private static final String PROPERTY_TABLE_NAME   = "properties";
    private static final String PROPERTY_COLUMN_KEY   = "key";
    private static final String PROPERTY_COLUMN_VALUE = "value";

    private static final String PROPERTY_LEVELCOUNT_PREFIX = "levelcount_";
    private static final String PROPERTY_BONUSES_USED    = "bonuses_used";
    static final String PROPERTY_X_POS                   = "x_pos";
    static final String PROPERTY_Y_POS                   = "y_pos";
    static final String PROPERTY_ZOOM_LEVEL              = "zoom_level";
    static final String PROPERTY_USER_GUID               = "user_guid";
    static final String PROPERTY_BUGFIX_PURGE_DUPLICATES = "bugfix_purge_duplicates";


    GridWalkingDBHelper(Context context) {
        super(context, DATABASE_NAME, null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        db.execSQL("CREATE TABLE "+GRID_TABLE_NAME+"("+GRID_COLUMN_KEY+" INTEGER PRIMARY KEY, "+GRID_COLUMN_LEVEL+" INTEGER)");
        db.execSQL("CREATE TABLE "+BONUS_TABLE_NAME+"("+BONUS_COLUMN_KEY+" INTEGER PRIMARY KEY)");
        db.execSQL("CREATE TABLE "+PROPERTY_TABLE_NAME+"("+PROPERTY_COLUMN_KEY+" TEXT PRIMARY KEY, "+PROPERTY_COLUMN_VALUE+" INTEGER)");

        ContentValues contentValues;
        byte level;
        for (level=0; level<Grid.LEVEL_COUNT; level++) {
            contentValues = new ContentValues();
            contentValues.put(PROPERTY_COLUMN_KEY, ToLevelKey(level));
            contentValues.put(PROPERTY_COLUMN_VALUE, 0);
            db.insert(PROPERTY_TABLE_NAME, null, contentValues);
        }

        contentValues = new ContentValues();
        contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_BONUSES_USED);
        contentValues.put(PROPERTY_COLUMN_VALUE, 0);
        db.insert(PROPERTY_TABLE_NAME, null, contentValues);

        contentValues = new ContentValues();
        contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_X_POS);
        contentValues.put(PROPERTY_COLUMN_VALUE, 0);
        db.insert(PROPERTY_TABLE_NAME, null, contentValues);

        contentValues = new ContentValues();
        contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_Y_POS);
        contentValues.put(PROPERTY_COLUMN_VALUE, 0);
        db.insert(PROPERTY_TABLE_NAME, null, contentValues);

        contentValues = new ContentValues();
        contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_ZOOM_LEVEL);
        contentValues.put(PROPERTY_COLUMN_VALUE, 11);
        db.insert(PROPERTY_TABLE_NAME, null, contentValues);

        contentValues = new ContentValues();
        contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_USER_GUID);
        contentValues.put(PROPERTY_COLUMN_VALUE, UUID.randomUUID().toString());
        db.insert(PROPERTY_TABLE_NAME, null, contentValues);

        contentValues = new ContentValues();
        contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_BUGFIX_PURGE_DUPLICATES);
        contentValues.put(PROPERTY_COLUMN_VALUE, 1);
        db.insert(PROPERTY_TABLE_NAME, null, contentValues);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.beginTransaction();

            ContentValues contentValues = new ContentValues();
            contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_USER_GUID);
            contentValues.put(PROPERTY_COLUMN_VALUE, UUID.randomUUID().toString());
            db.insert(PROPERTY_TABLE_NAME, null, contentValues);

            contentValues = new ContentValues();
            contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_BUGFIX_PURGE_DUPLICATES);
            contentValues.put(PROPERTY_COLUMN_VALUE, 1);
            db.insert(PROPERTY_TABLE_NAME, null, contentValues);

            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    SQLiteDatabase StartTransaction() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        return db;
    }

    void EndTransaction(SQLiteDatabase db, boolean successful) {
        if (successful) {
            db.setTransactionSuccessful();
        }
        db.endTransaction();
    }

    boolean ContainsGrid(final int gridKey, final byte level) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT COUNT(*)"
                                    + " FROM " + GRID_TABLE_NAME
                                    + " WHERE " + GRID_COLUMN_KEY + "=?"
                                    + " AND " + GRID_COLUMN_LEVEL + "=?",
                            new String[]{Integer.toString(gridKey), Byte.toString(level)});
            return cursor.moveToFirst() && !cursor.isAfterLast() && (1 == cursor.getInt(0));
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    Set<Integer> ContainsGrid(final Set<Integer> gridKeys, final byte level) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT " + GRID_COLUMN_KEY
                           + " FROM " + GRID_TABLE_NAME
                           + " WHERE " + GRID_COLUMN_LEVEL + "=?"
                           + " AND " + GRID_COLUMN_KEY + " IN (" + SetTostring(gridKeys) + ")",
                            new String[]{Byte.toString(level)});
            Set<Integer> result = new TreeSet<>();
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    result.add(cursor.getInt(0));
                    cursor.moveToNext();
                }
            }
            return result;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    Set<Integer> ContainsGrid(final int fromGridKey, final int toGridKey, final byte level) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT " + GRID_COLUMN_KEY
                                    + " FROM " + GRID_TABLE_NAME
                                    + " WHERE " + GRID_COLUMN_LEVEL + "=? "
                                    + " AND " + GRID_COLUMN_KEY + ">=?"
                                    + " AND " + GRID_COLUMN_KEY + "<=?",
                            new String[]{Byte.toString(level), Integer.toString(fromGridKey), Integer.toString(toGridKey)});
            Set<Integer> result = new TreeSet<>();
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    result.add(cursor.getInt(0));
                    cursor.moveToNext();
                }
            }
            return result;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    Set<Integer> GetLevelGrids(final byte level) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT " + GRID_COLUMN_KEY
                                    + " FROM " + GRID_TABLE_NAME
                                    + " WHERE " + GRID_COLUMN_LEVEL + "=?",
                            new String[]{Byte.toString(level)});
            Set<Integer> result = new TreeSet<>();
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    result.add(cursor.getInt(0));
                    cursor.moveToNext();
                }
            }
            return result;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    boolean ContainsBonus(final int bonusKey) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT COUNT(*)"
                                    + " FROM " + BONUS_TABLE_NAME
                                    + " WHERE " + BONUS_COLUMN_KEY + "=?",
                            new String[]{Integer.toString(bonusKey)});

            return cursor.moveToFirst() && !cursor.isAfterLast() && (1 == cursor.getInt(0));
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    void PersistGrid(final int gridKey, final byte level, final boolean consumeBonus) {
        SQLiteDatabase db = StartTransaction();

        ContentValues contentValues = new ContentValues();
        contentValues.put(GRID_COLUMN_KEY, gridKey);
        contentValues.put(GRID_COLUMN_LEVEL, level);
        db.insert(GRID_TABLE_NAME, null, contentValues);

        AdjustLevelCount(db, level, 1);

        if (consumeBonus) {
            ConsumeBonus(db);
        }

        EndTransaction(db, true);
    }

    void PersistGrid(final Set<Integer> oldGridKeys, final int newGridKey, final byte newLevel) {
        SQLiteDatabase db = StartTransaction();

        db.execSQL("DELETE FROM "+GRID_TABLE_NAME
                 +" WHERE "+GRID_COLUMN_KEY+" IN ("+ SetTostring(oldGridKeys)+")");

        AdjustLevelCount(db, (byte) (newLevel-1), -(oldGridKeys.size()));

        ContentValues contentValues = new ContentValues();
        contentValues.put(GRID_COLUMN_KEY, newGridKey);
        contentValues.put(GRID_COLUMN_LEVEL, newLevel);
        db.insert(GRID_TABLE_NAME, null, contentValues);

        AdjustLevelCount(db, newLevel, 1);

        EndTransaction(db, true);
    }

    void DeleteGrid(final SQLiteDatabase dbInTransaction, final int gridKey, final byte level) {
        dbInTransaction.execSQL("DELETE FROM "+GRID_TABLE_NAME
                              +" WHERE " + GRID_COLUMN_LEVEL + "=?"
                              +" AND "+GRID_COLUMN_KEY+"=?",
                new String[]{Integer.toString(level), Integer.toString(gridKey)});

        AdjustLevelCount(dbInTransaction, level, -1);
    }

    void PersistBonus(final int bonusKey) {
        SQLiteDatabase db = StartTransaction();

        ContentValues contentValues = new ContentValues();
        contentValues.put(BONUS_COLUMN_KEY, bonusKey);
        db.insert(BONUS_TABLE_NAME, null, contentValues);

        EndTransaction(db, true);
    }

    int GetUnusedBonusCount() {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + BONUS_TABLE_NAME, null);
            if (!cursor.moveToFirst()) {
                return 0;
            }
            int bonusesFound = cursor.isAfterLast() ? 0 : cursor.getInt(0);
            return bonusesFound - GetProperty(PROPERTY_BONUSES_USED) + Bonus.START_BONUS;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    void ConsumeBonus() {
        SQLiteDatabase db = StartTransaction();

        ConsumeBonus(db);

        EndTransaction(db, true);
    }

    private void ConsumeBonus(final SQLiteDatabase dbInTransaction) {
        AdjustProperty(dbInTransaction, PROPERTY_BONUSES_USED, 1);
    }

    int GetLevelCount(final byte level) {
        return GetProperty(ToLevelKey(level));
    }

    int GetProperty(final String property) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT " + PROPERTY_COLUMN_VALUE
                                    + " FROM " + PROPERTY_TABLE_NAME
                                    + " WHERE " + PROPERTY_COLUMN_KEY + "=?",
                            new String[]{property});
            if (!cursor.moveToFirst()) {
                return 0;
            }
            return cursor.isAfterLast() ? 0 : cursor.getInt(0);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    String GetStringProperty(final String property) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT " + PROPERTY_COLUMN_VALUE
                           + " FROM " + PROPERTY_TABLE_NAME
                           + " WHERE " + PROPERTY_COLUMN_KEY + "=?",
                            new String[]{property});
            if (!cursor.moveToFirst()) {
                return "";
            }
            return cursor.isAfterLast() ? "" : cursor.getString(0);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    void SetProperty(final String property, final int value) {
        SQLiteDatabase db = StartTransaction();

        db.execSQL("UPDATE "+PROPERTY_TABLE_NAME
                        +" SET "+PROPERTY_COLUMN_VALUE+" = ?"
                        +" WHERE "+PROPERTY_COLUMN_KEY+"=?",
                new String[] {Integer.toString(value), property});

        EndTransaction(db, true);
    }

    void SetStringProperty(final String property, final String value) {
        SQLiteDatabase db = StartTransaction();

        db.execSQL("UPDATE "+PROPERTY_TABLE_NAME
                        +" SET "+PROPERTY_COLUMN_VALUE+" = ?"
                        +" WHERE "+PROPERTY_COLUMN_KEY+"=?",
                new String[] {value, property});

        EndTransaction(db, true);
    }

    private void AdjustLevelCount(final SQLiteDatabase db, final byte level, final int value) {
        AdjustProperty(db, ToLevelKey(level), value);
    }

    private void AdjustProperty(final SQLiteDatabase db, final String property, final int value) {
        db.execSQL("UPDATE "+PROPERTY_TABLE_NAME
                        +" SET "+PROPERTY_COLUMN_VALUE+" = "+PROPERTY_COLUMN_VALUE+"+?"
                        +" WHERE "+PROPERTY_COLUMN_KEY+"=?",
                new String[] {Integer.toString(value), property});
    }

    private String SetTostring(final Set<Integer> keys) {
        StringBuilder sb = new StringBuilder();
        for (Integer key : keys) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(Integer.toString(key));
        }
        return sb.toString();
    }

    private String ToLevelKey(final byte level) {
        return PROPERTY_LEVELCOUNT_PREFIX+Byte.toString(level);
    }

}
