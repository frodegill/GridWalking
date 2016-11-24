package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Set;
import java.util.TreeSet;


public final class GridWalkingDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "GridWalking.db";

    public static final String GRID_TABLE_NAME = "grid";
    public static final String GRID_COLUMN_KEY = "key";
    public static final String GRID_COLUMN_LEVEL = "level";

    public static final String BONUS_TABLE_NAME = "bonus";
    public static final String BONUS_COLUMN_KEY = "key";

    public static final String PROPERTY_TABLE_NAME = "properties";
    public static final String PROPERTY_COLUMN_KEY = "key";
    public static final String PROPERTY_COLUMN_VALUE = "value";

    public static final String PROPERTY_LEVELCOUNT_PREFIX = "levelcount_";
    public static final String PROPERTY_BONUSES_USED = "bonuses_used";


    public GridWalkingDBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
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
            contentValues.put(PROPERTY_COLUMN_KEY, toLevelKey(level));
            contentValues.put(PROPERTY_COLUMN_VALUE, 0);
            db.insert(PROPERTY_TABLE_NAME, null, contentValues);
        }

        contentValues = new ContentValues();
        contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_BONUSES_USED);
        contentValues.put(PROPERTY_COLUMN_VALUE, 0);
        db.insert(PROPERTY_TABLE_NAME, null, contentValues);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public boolean containsGrid(final long gridKey, final byte level) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT COUNT(*)"
                                    + " FROM " + GRID_TABLE_NAME
                                    + " WHERE " + GRID_COLUMN_KEY + "=?"
                                    + " AND " + GRID_COLUMN_LEVEL + "=?",
                            new String[]{Long.toString(gridKey), Byte.toString(level)});
            if (!cursor.moveToFirst()) {
                return false;
            }
            return cursor.isAfterLast() ? false : 1 == cursor.getInt(0);
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Set<Long> containsGrid(final Set<Long> gridKeys, final byte level) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT COUNT(*)"
                                    + " FROM " + GRID_TABLE_NAME
                                    + " WHERE " + GRID_COLUMN_LEVEL + "=?"
                                    + " AND " + GRID_COLUMN_KEY + " IN (" + setTostring(gridKeys) + ")",
                            new String[]{Byte.toString(level)});
            Set<Long> result = new TreeSet<>();
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    result.add(cursor.getLong(0));
                    cursor.moveToNext();
                }
            }
            return result;
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Set<Long> containsGrid(final long fromGridKey, final long toGridKey, final byte level) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT " + GRID_COLUMN_KEY
                                    + " FROM " + GRID_TABLE_NAME
                                    + " WHERE " + GRID_COLUMN_LEVEL + "=? "
                                    + " AND " + GRID_COLUMN_KEY + ">=?"
                                    + " AND " + GRID_COLUMN_KEY + "<=?",
                            new String[]{Byte.toString(level), Long.toString(fromGridKey), Long.toString(toGridKey)});
            Set<Long> result = new TreeSet<>();
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    result.add(cursor.getLong(0));
                    cursor.moveToNext();
                }
            }
            return result;
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean containsBonus(final int bonusKey) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT COUNT(*)"
                                    + " FROM " + BONUS_TABLE_NAME
                                    + " WHERE " + BONUS_COLUMN_KEY + "=?",
                            new String[]{Long.toString(bonusKey)});
            if (!cursor.moveToFirst()) {
                return false;
            }
            return cursor.isAfterLast() ? false : 1 == cursor.getInt(0);
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void persistGrid(final long gridKey, final byte level) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        ContentValues contentValues = new ContentValues();
        contentValues.put(GRID_COLUMN_KEY, gridKey);
        contentValues.put(GRID_COLUMN_LEVEL, level);
        db.insert(GRID_TABLE_NAME, null, contentValues);

        adjustLevelCount(db, level, 1);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void persistGrid(final Set<Long> oldGridKeys, final long newGridKey, final byte newLevel) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.execSQL("DELETE FROM "+GRID_TABLE_NAME
                 +" WHERE "+GRID_COLUMN_KEY+" IN ("+ setTostring(oldGridKeys)+")");

        adjustLevelCount(db, (byte) (newLevel-1), -(oldGridKeys.size()));

        ContentValues contentValues = new ContentValues();
        contentValues.put(GRID_COLUMN_KEY, newGridKey);
        contentValues.put(GRID_COLUMN_LEVEL, newLevel);
        db.insert(GRID_TABLE_NAME, null, contentValues);

        adjustLevelCount(db, newLevel, 1);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void persistBonus(final int bonusKey) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        ContentValues contentValues = new ContentValues();
        contentValues.put(BONUS_COLUMN_KEY, bonusKey);
        db.insert(BONUS_TABLE_NAME, null, contentValues);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public int getUnusedBonusCount() {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + BONUS_TABLE_NAME, null);
            if (!cursor.moveToFirst()) {
                return 0;
            }
            int bonusesFound = cursor.isAfterLast() ? 0 : cursor.getInt(0);
            return bonusesFound - (int) getProperty(PROPERTY_BONUSES_USED);
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void consumeBonus() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        adjustProperty(db, PROPERTY_BONUSES_USED, 1);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public long getLevelCount(final byte level) {
        return getProperty(toLevelKey(level));
    }

    private long getProperty(final String property) {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT " + PROPERTY_COLUMN_VALUE
                                    + " FROM " + PROPERTY_TABLE_NAME
                                    + " WHERE " + PROPERTY_COLUMN_KEY + "=?",
                            new String[]{property});
            if (!cursor.moveToFirst()) {
                return 0L;
            }
            return cursor.isAfterLast() ? 0L : cursor.getLong(0);
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void adjustLevelCount(final SQLiteDatabase db, final byte level, final int value) {
        adjustProperty(db, toLevelKey(level), value);
    }

    private void adjustProperty(final SQLiteDatabase db, final String property, final int value) {
        db.execSQL("UPDATE "+PROPERTY_TABLE_NAME
                        +" SET "+PROPERTY_COLUMN_VALUE+" = "+PROPERTY_COLUMN_VALUE+"+?"
                        +" WHERE "+PROPERTY_COLUMN_KEY+"=?",
                new String[] {Integer.toString(value), property});
    }

    private String setTostring(final Set<Long> keys) {
        StringBuilder sb = new StringBuilder();
        for (Long key : keys) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(Long.toString(key));
        }
        return sb.toString();
    }

    private String toLevelKey(final byte level) {
        return PROPERTY_LEVELCOUNT_PREFIX+Byte.toString(level);
    }

}