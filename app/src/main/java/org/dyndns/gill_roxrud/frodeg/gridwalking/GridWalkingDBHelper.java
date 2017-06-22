package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;


public final class GridWalkingDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME         = "GridWalking.db";

    private static final String GRID_TABLE_NAME       = "grid";
    private static final String GRID_COLUMN_KEY       = "key";
    private static final String GRID_COLUMN_LEVEL     = "level";
    private static final String GRID_COLUMN_STATUS    = "status";
    private static final String GRID_COLUMN_OWNER     = "owner";

    private static final int GRID_STATUS_SYNCED       = 0;
    private static final int GRID_STATUS_NEW          = 1;
    private static final int GRID_STATUS_DELETED      = 2;

    private static final int GRID_OWNER_SELF          = 0;
    private static final int GRID_OWNER_SYNCED        = 1;

    private static final String BONUS_TABLE_NAME      = "bonus";
    private static final String BONUS_COLUMN_KEY      = "key";

    private static final String PROPERTY_TABLE_NAME   = "properties";
    private static final String PROPERTY_COLUMN_KEY   = "key";
    private static final String PROPERTY_COLUMN_VALUE = "value";

    private static final String PROPERTY_LEVELCOUNT_PREFIX = "levelcount_";
    private static final String PROPERTY_BONUSES_USED    = "bonuses_used";
    public static final String PROPERTY_X_POS                   = "x_pos";
    public static final String PROPERTY_Y_POS                   = "y_pos";
    public static final String PROPERTY_ZOOM_LEVEL              = "zoom_level";
    public static final String PROPERTY_USER_GUID               = "user_guid";
    public static final String PROPERTY_BUGFIX_PURGE_DUPLICATES = "bugfix_purge_duplicates";
    public static final String PROPERTY_BUGFIX_ADJUST_LEVELCOUNT = "bugfix_adjust_levelcount";


    GridWalkingDBHelper(Context context) {
        super(context, DATABASE_NAME, null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        boolean successful = true;

        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE "+GRID_TABLE_NAME+"("+GRID_COLUMN_KEY+" INTEGER NOT NULL, "
                                                          +GRID_COLUMN_LEVEL+" INTEGER NOT NULL, "
                                                          +GRID_COLUMN_STATUS+" INTEGER NOT NULL DEFAULT "+Integer.toString(GRID_STATUS_NEW)+", "
                                                          +GRID_COLUMN_OWNER+" INTEGER NOT NULL DEFAULT "+Integer.toString(GRID_OWNER_SELF)+", "
                                                          +"PRIMARY KEY ("+GRID_COLUMN_KEY+","+GRID_COLUMN_LEVEL+","+GRID_COLUMN_OWNER+"))");
            db.execSQL("CREATE TABLE "+BONUS_TABLE_NAME+"("+BONUS_COLUMN_KEY+" INTEGER PRIMARY KEY)");
            db.execSQL("CREATE TABLE "+PROPERTY_TABLE_NAME+"("+PROPERTY_COLUMN_KEY+" TEXT PRIMARY KEY, "+PROPERTY_COLUMN_VALUE+" INTEGER)");

            ContentValues contentValues;
            byte level;
            for (level=0; level<Grid.LEVEL_COUNT; level++) {
                contentValues = new ContentValues();
                contentValues.put(PROPERTY_COLUMN_KEY, ToLevelKey(level));
                contentValues.put(PROPERTY_COLUMN_VALUE, 0);
                successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));
            }

            contentValues = new ContentValues();
            contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_BONUSES_USED);
            contentValues.put(PROPERTY_COLUMN_VALUE, 0);
            successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));

            contentValues = new ContentValues();
            contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_X_POS);
            contentValues.put(PROPERTY_COLUMN_VALUE, 0);
            successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));

            contentValues = new ContentValues();
            contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_Y_POS);
            contentValues.put(PROPERTY_COLUMN_VALUE, 0);
            successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));

            contentValues = new ContentValues();
            contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_ZOOM_LEVEL);
            contentValues.put(PROPERTY_COLUMN_VALUE, 11);
            successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));

            contentValues = new ContentValues();
            contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_USER_GUID);
            contentValues.put(PROPERTY_COLUMN_VALUE, UUID.randomUUID().toString());
            successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));

            contentValues = new ContentValues();
            contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_BUGFIX_PURGE_DUPLICATES);
            contentValues.put(PROPERTY_COLUMN_VALUE, 1);
            successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));

            contentValues = new ContentValues();
            contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_BUGFIX_ADJUST_LEVELCOUNT);
            contentValues.put(PROPERTY_COLUMN_VALUE, 1);
            successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));
        } catch (SQLException e) {
            successful = false;
            Toast.makeText(GridWalkingApplication.getContext(), "ERR1: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        EndTransaction(db, successful);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        boolean successful = true;

        if (oldVersion < 4) {
            db.beginTransaction();
        }

        try {
            if (oldVersion < 3) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_USER_GUID);
                contentValues.put(PROPERTY_COLUMN_VALUE, UUID.randomUUID().toString());
                successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));

                contentValues = new ContentValues();
                contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_BUGFIX_PURGE_DUPLICATES);
                contentValues.put(PROPERTY_COLUMN_VALUE, 1);
                successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));
            }

            if (oldVersion < 4) {
                db.execSQL("CREATE TABLE "+GRID_TABLE_NAME+"_tmp("+GRID_COLUMN_KEY+" INTEGER NOT NULL, "
                        +GRID_COLUMN_LEVEL+" INTEGER NOT NULL, "
                        +GRID_COLUMN_STATUS+" INTEGER NOT NULL DEFAULT "+Integer.toString(GRID_STATUS_NEW)+", "
                        +GRID_COLUMN_OWNER+" INTEGER NOT NULL DEFAULT "+Integer.toString(GRID_OWNER_SELF)+", "
                        +"PRIMARY KEY ("+GRID_COLUMN_KEY+","+GRID_COLUMN_LEVEL+","+GRID_COLUMN_OWNER+"))");

                db.execSQL("INSERT INTO "+GRID_TABLE_NAME+"_tmp"
                        +"("+GRID_COLUMN_KEY+","+GRID_COLUMN_LEVEL+","+GRID_COLUMN_STATUS+","+GRID_COLUMN_OWNER+") "
                        +"SELECT "
                        +GRID_COLUMN_KEY+","+GRID_COLUMN_LEVEL+","+Integer.toString(GRID_STATUS_NEW)+","+Integer.toString(GRID_OWNER_SELF)
                        +" FROM "+GRID_TABLE_NAME);

                db.execSQL("DROP TABLE "+GRID_TABLE_NAME);

                db.execSQL("ALTER TABLE "+GRID_TABLE_NAME+"_tmp RENAME TO "+GRID_TABLE_NAME);

                ContentValues contentValues = new ContentValues();
                contentValues.put(PROPERTY_COLUMN_KEY, PROPERTY_BUGFIX_ADJUST_LEVELCOUNT);
                contentValues.put(PROPERTY_COLUMN_VALUE, 1);
                successful &= (-1 != db.insert(PROPERTY_TABLE_NAME, null, contentValues));
            }
        } catch (SQLException e) {
            successful = false;
            Toast.makeText(GridWalkingApplication.getContext(), "ERR2: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        if (oldVersion < 4) {
            EndTransaction(db, successful);
        }
    }

    public SQLiteDatabase StartTransaction() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        return db;
    }

    public void EndTransaction(SQLiteDatabase db, boolean successful) {
        if (successful) {
            db.setTransactionSuccessful();
        }
        db.endTransaction();
    }

    boolean ContainsGrid(final int gridKey, final byte level, final GameState.ShowGridState gridState) {
        if (GameState.ShowGridState.NONE == gridState) {
            return false;
        }

        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT COUNT(*)"
                                    + " FROM " + GRID_TABLE_NAME
                                    + " WHERE " + GRID_COLUMN_KEY + "=?"
                                    + " AND " + GRID_COLUMN_LEVEL + "=?"
                                    + " AND " + GRID_COLUMN_STATUS + "!="+Integer.toString(GRID_STATUS_DELETED)
                                    + " AND " + GRID_COLUMN_OWNER + "="+Integer.toString(GameState.ShowGridState.SELF==gridState ? GRID_OWNER_SELF : GRID_OWNER_SYNCED),
                            new String[]{Integer.toString(gridKey), Byte.toString(level)});
            return cursor.moveToFirst() && !cursor.isAfterLast() && (1 == cursor.getInt(0));
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    Set<Integer> ContainsGrid(final Set<Integer> gridKeys, final byte level, final GameState.ShowGridState gridState) {
        Set<Integer> result = new TreeSet<>();

        if (GameState.ShowGridState.NONE == gridState) {
            return result;
        }

        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT " + GRID_COLUMN_KEY
                           + " FROM " + GRID_TABLE_NAME
                           + " WHERE " + GRID_COLUMN_LEVEL + "=?"
                           + " AND " + GRID_COLUMN_KEY + " IN (" + SetToString(gridKeys) + ")"
                           + " AND " + GRID_COLUMN_STATUS + "!="+Integer.toString(GRID_STATUS_DELETED)
                           + " AND " + GRID_COLUMN_OWNER + "="+Integer.toString(GameState.ShowGridState.SELF==gridState ? GRID_OWNER_SELF : GRID_OWNER_SYNCED),
                            new String[]{Byte.toString(level)});
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

    public Set<Integer> ContainsGrid(final int fromGridKey, final int toGridKey, final byte level, final GameState.ShowGridState gridState) {
        Set<Integer> result = new TreeSet<>();

        if (GameState.ShowGridState.NONE == gridState) {
            return result;
        }

        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT " + GRID_COLUMN_KEY
                                    + " FROM " + GRID_TABLE_NAME
                                    + " WHERE " + GRID_COLUMN_LEVEL + "=? "
                                    + " AND " + GRID_COLUMN_KEY + ">=?"
                                    + " AND " + GRID_COLUMN_KEY + "<=?"
                                    + " AND " + GRID_COLUMN_STATUS + "!="+Integer.toString(GRID_STATUS_DELETED)
                                    + " AND " + GRID_COLUMN_OWNER + "="+Integer.toString(GameState.ShowGridState.SELF==gridState ? GRID_OWNER_SELF : GRID_OWNER_SYNCED),
                            new String[]{Byte.toString(level), Integer.toString(fromGridKey), Integer.toString(toGridKey)});
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

    Set<Integer> GetLevelGrids(final byte level, final GameState.ShowGridState gridState) {
        Set<Integer> result = new TreeSet<>();

        if (GameState.ShowGridState.NONE == gridState) {
            return result;
        }

        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT " + GRID_COLUMN_KEY
                                    + " FROM " + GRID_TABLE_NAME
                                    + " WHERE " + GRID_COLUMN_LEVEL + "=?"
                                    + " AND " + GRID_COLUMN_STATUS + "!="+Integer.toString(GRID_STATUS_DELETED)
                                    + " AND " + GRID_COLUMN_OWNER + "="+Integer.toString(GameState.ShowGridState.SELF==gridState ? GRID_OWNER_SELF : GRID_OWNER_SYNCED),
                            new String[]{Byte.toString(level)});
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

    public void GetModifiedGrids(final SQLiteDatabase dbInTransaction, final Set<Integer> deletedGrids, final ArrayList<Set<Integer>> newGrids) {
        Cursor cursor = null;

        deletedGrids.clear();
        try {
            cursor = dbInTransaction
                    .rawQuery("SELECT " + GRID_COLUMN_KEY
                             + " FROM " + GRID_TABLE_NAME
                            + " WHERE " + GRID_COLUMN_STATUS + "="+Integer.toString(GRID_STATUS_DELETED)
                              + " AND " + GRID_COLUMN_OWNER + "="+Integer.toString(GRID_OWNER_SELF),
                            null);
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    deletedGrids.add(cursor.getInt(0));
                    cursor.moveToNext();
                }
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        byte level;
        for (level=0; level<Grid.LEVEL_COUNT; level++) {
            newGrids.get(level).clear();
            try {
                cursor = dbInTransaction
                        .rawQuery("SELECT " + GRID_COLUMN_KEY
                                 + " FROM " + GRID_TABLE_NAME
                                + " WHERE " + GRID_COLUMN_LEVEL + "=?"
                                  + " AND " + GRID_COLUMN_STATUS + "=" + Integer.toString(GRID_STATUS_NEW)
                                  + " AND " + GRID_COLUMN_OWNER + "=" + Integer.toString(GRID_OWNER_SELF),
                                new String[]{Byte.toString(level)});
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        newGrids.get(level).add(cursor.getInt(0));
                        cursor.moveToNext();
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public void CommitModifiedGrids(final SQLiteDatabase dbInTransaction, final Set<Integer> deletedGrids, final ArrayList<Set<Integer>> newGrids) throws SQLException {
        dbInTransaction.execSQL("DELETE FROM "+GRID_TABLE_NAME
                               +" WHERE " + GRID_COLUMN_KEY + " IN ("+ SetToString(deletedGrids)+")"
                                 +" AND " + GRID_COLUMN_STATUS + "="+Integer.toString(GRID_STATUS_DELETED)
                                 +" AND " + GRID_COLUMN_OWNER + "="+Integer.toString(GRID_OWNER_SELF));

        byte level;
        for (level=0; level<Grid.LEVEL_COUNT; level++) {
            dbInTransaction.execSQL("UPDATE "+GRID_TABLE_NAME
                                     +" SET " + GRID_COLUMN_STATUS + "="+Integer.toString(GRID_STATUS_SYNCED)
                                   +" WHERE " + GRID_COLUMN_KEY + " IN ("+ SetToString(newGrids.get(level))+")"
                                     +" AND " + GRID_COLUMN_STATUS + "="+Integer.toString(GRID_STATUS_NEW)
                                     +" AND " + GRID_COLUMN_OWNER + "="+Integer.toString(GRID_OWNER_SELF));
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

    boolean PersistGrid(final SQLiteDatabase dbInTransaction, final int gridKey, final byte level, final boolean consumeBonus) {
        boolean successful = true;
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(GRID_COLUMN_KEY, gridKey);
            contentValues.put(GRID_COLUMN_LEVEL, level);
            contentValues.put(GRID_COLUMN_STATUS, GRID_STATUS_NEW);
            contentValues.put(GRID_COLUMN_OWNER, GRID_OWNER_SELF);
            successful &= (-1 != dbInTransaction.insert(GRID_TABLE_NAME, null, contentValues));

            AdjustLevelCount(dbInTransaction, level, 1);

            if (consumeBonus) {
                ConsumeBonus(dbInTransaction);
            }
        } catch (SQLException e) {
            successful = false;
            Toast.makeText(GridWalkingApplication.getContext(), "ERR3: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return successful;
    }

    boolean PersistGrid(final SQLiteDatabase dbInTransaction, final Set<Integer> oldGridKeys, final int newGridKey, final byte newLevel) {
        boolean successful = true;
        try {
            byte oldLevel = (byte) (newLevel - 1);

            dbInTransaction.execSQL("UPDATE " + GRID_TABLE_NAME
                    + " SET " + GRID_COLUMN_STATUS + "=" + Integer.toString(GRID_STATUS_DELETED)
                    + " WHERE " + GRID_COLUMN_KEY + " IN (" + SetToString(oldGridKeys) + ")"
                    + " AND " + GRID_COLUMN_LEVEL + "=" + Integer.toString(oldLevel)
                    + " AND " + GRID_COLUMN_OWNER + "=" + Integer.toString(GRID_OWNER_SELF)
            );

            AdjustLevelCount(dbInTransaction, oldLevel, -(oldGridKeys.size()));

            ContentValues contentValues = new ContentValues();
            contentValues.put(GRID_COLUMN_KEY, newGridKey);
            contentValues.put(GRID_COLUMN_LEVEL, newLevel);
            contentValues.put(GRID_COLUMN_STATUS, GRID_STATUS_NEW);
            contentValues.put(GRID_COLUMN_OWNER, GRID_OWNER_SELF);
            successful &= (-1 != dbInTransaction.insert(GRID_TABLE_NAME, null, contentValues));

            AdjustLevelCount(dbInTransaction, newLevel, 1);
        } catch (SQLException e) {
            successful = false;
            Toast.makeText(GridWalkingApplication.getContext(), "ERR4: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return successful;
    }

    void DeleteGrid(final SQLiteDatabase dbInTransaction, final int gridKey, final byte level) throws SQLException {
        dbInTransaction.execSQL("UPDATE "+GRID_TABLE_NAME
                              + " SET " + GRID_COLUMN_STATUS + "="+Integer.toString(GRID_STATUS_DELETED)
                              +" WHERE " + GRID_COLUMN_LEVEL + "=?"
                              +" AND " + GRID_COLUMN_KEY + "=?"
                              +" AND " + GRID_COLUMN_OWNER + "="+Integer.toString(GRID_OWNER_SELF),
                new String[]{Integer.toString(level), Integer.toString(gridKey)});

        AdjustLevelCount(dbInTransaction, level, -1);
    }

    void PersistBonusT(final int bonusKey) {
        boolean successful = true;
        SQLiteDatabase dbInTransaction = StartTransaction();

        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(BONUS_COLUMN_KEY, bonusKey);
            successful &= (-1 != dbInTransaction.insert(BONUS_TABLE_NAME, null, contentValues));
        } catch (SQLException e) {
            successful = false;
            Toast.makeText(GridWalkingApplication.getContext(), "ERR5: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        EndTransaction(dbInTransaction, successful);
    }

    public int GetUnusedBonusCount() {
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + BONUS_TABLE_NAME, null);
            if (!cursor.moveToFirst()) {
                return 0;
            }
            int bonusesFound = cursor.isAfterLast() ? 0 : cursor.getInt(0);
            int bonusesLeft = bonusesFound - GetProperty(PROPERTY_BONUSES_USED) + Bonus.START_BONUS;
            return (bonusesLeft<0) ? 0 : bonusesLeft;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    void ConsumeBonusT() {
        SQLiteDatabase dbInTransaction = StartTransaction();

        boolean successful = true;
        try {
            ConsumeBonus(dbInTransaction);
        } catch (SQLException e) {
            successful = false;
            Toast.makeText(GridWalkingApplication.getContext(), "ERR6: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        EndTransaction(dbInTransaction, successful);
    }

    private void ConsumeBonus(final SQLiteDatabase dbInTransaction) throws SQLException {
        AdjustProperty(dbInTransaction, PROPERTY_BONUSES_USED, 1);
    }

    public int GetLevelCount(final byte level) {
        return GetProperty(ToLevelKey(level));
    }

    public int GetProperty(final String property) {
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

    public String GetStringProperty(final String property) {
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

    public void SetProperty(final SQLiteDatabase dbInTransaction, final String property, final int value) throws SQLException {
        dbInTransaction.execSQL("UPDATE "+PROPERTY_TABLE_NAME
                               +" SET "+PROPERTY_COLUMN_VALUE+" = ?"
                               +" WHERE "+PROPERTY_COLUMN_KEY+"=?",
                new String[] {Integer.toString(value), property});
    }

    public void SetPropertyT(final String property, final int value) {
        boolean successful = true;
        SQLiteDatabase dbInTransaction = StartTransaction();

        try {
            SetProperty(dbInTransaction, property, value);
        } catch (SQLException e) {
            successful = false;
            Toast.makeText(GridWalkingApplication.getContext(), "ERR7: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        EndTransaction(dbInTransaction, successful);
    }

    void SetStringProperty(final SQLiteDatabase dbInTransaction, final String property, final String value) throws SQLException {
        dbInTransaction.execSQL("UPDATE "+PROPERTY_TABLE_NAME
                        +" SET "+PROPERTY_COLUMN_VALUE+" = ?"
                        +" WHERE "+PROPERTY_COLUMN_KEY+"=?",
                new String[] {value, property});
    }

    void SetStringPropertyT(final String property, final String value) {
        boolean successful = true;
        SQLiteDatabase dbInTransaction = StartTransaction();

        try {
            SetStringProperty(dbInTransaction, property, value);
        } catch (SQLException e) {
            successful = false;
            Toast.makeText(GridWalkingApplication.getContext(), "ERR8: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        EndTransaction(dbInTransaction, successful);
    }

    private void AdjustLevelCount(final SQLiteDatabase db, final byte level, final int value) throws SQLException {
        AdjustProperty(db, ToLevelKey(level), value);
    }

    private void AdjustProperty(final SQLiteDatabase dbInTransaction, final String property, final int value) throws SQLException {
        dbInTransaction.execSQL("UPDATE "+PROPERTY_TABLE_NAME
                        +" SET "+PROPERTY_COLUMN_VALUE+" = "+PROPERTY_COLUMN_VALUE+"+?"
                        +" WHERE "+PROPERTY_COLUMN_KEY+"=?",
                new String[] {Integer.toString(value), property});
    }

    public void UpdateLevelCountFromDb(final SQLiteDatabase dbInTransaction, final byte level) throws SQLException {
        int value = 0;

        Cursor cursor = null;
        try {
            cursor = dbInTransaction
                    .rawQuery("SELECT COUNT(*)"
                                    + " FROM " + GRID_TABLE_NAME
                                    + " WHERE " + GRID_COLUMN_LEVEL + "=?"
                                    + " AND " + GRID_COLUMN_STATUS + "!="+Integer.toString(GRID_STATUS_DELETED)
                                    + " AND " + GRID_COLUMN_OWNER + "="+Integer.toString(GRID_OWNER_SELF),
                            new String[]{Byte.toString(level)});
            if (cursor.moveToFirst() && !cursor.isAfterLast()) {
                value = cursor.getInt(0);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        SetProperty(dbInTransaction, ToLevelKey(level), value);
    }

    private String SetToString(final Set<Integer> keys) {
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

    public void RestoreFromFileT(final File restoreFile) throws Exception {
        boolean successful = true;
        SQLiteDatabase dbInTransaction = StartTransaction();

        try {
            FileInputStream is = new FileInputStream(restoreFile);
            Secrets secrets = new Secrets();

            dbInTransaction.execSQL("DELETE FROM "+GRID_TABLE_NAME
                                   +" WHERE "+GRID_COLUMN_OWNER+"="+Integer.toString(GRID_OWNER_SELF));

            byte level;
            int gridKey;
            for (level=0; level<Grid.LEVEL_COUNT; level++) {
                while (0xFFFFFFFF != (gridKey=FetchInt32(is, secrets))) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(GRID_COLUMN_KEY, gridKey);
                    contentValues.put(GRID_COLUMN_LEVEL, level);
                    contentValues.put(GRID_COLUMN_STATUS, GRID_STATUS_SYNCED);
                    contentValues.put(GRID_COLUMN_OWNER, GRID_OWNER_SELF);

                    successful &= (-1 != dbInTransaction.insert(GRID_TABLE_NAME, null, contentValues));
                }
                UpdateLevelCountFromDb(dbInTransaction, level);
            }

            int uuidLength = FetchByte(is, secrets);
            String uuid = FetchString(is, uuidLength, secrets);
            SetStringProperty(dbInTransaction, PROPERTY_USER_GUID, uuid);

            long crc32 = FetchInt32(is, null);
            if (crc32 != secrets.Crc32()) {
                throw new Exception("Invalid CRC");
            }
        } catch (Exception e) {
            successful = false;
            throw e;
        } finally {
            EndTransaction(dbInTransaction, successful);
        }
    }

    public Integer SyncExternalGridsT(final InputStream is) throws Exception {
        Integer aGrid = null;
        boolean successful = true;
        SQLiteDatabase dbInTransaction = StartTransaction();

        try {
            dbInTransaction.execSQL("DELETE FROM "+GRID_TABLE_NAME
                                  +" WHERE " + GRID_COLUMN_OWNER + "="+Integer.toString(GRID_OWNER_SYNCED));

            byte level;
            int gridKey;
            for (level=0; level<Grid.LEVEL_COUNT; level++) {
                while (0xFFFFFFFF != (gridKey=FetchInt32(is, null))) {
                    aGrid = gridKey;
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(GRID_COLUMN_KEY, gridKey);
                    contentValues.put(GRID_COLUMN_LEVEL, level);
                    contentValues.put(GRID_COLUMN_STATUS, GRID_STATUS_SYNCED);
                    contentValues.put(GRID_COLUMN_OWNER, GRID_OWNER_SYNCED);

                    //System.out.println("Inserting "+Integer.toString(gridKey)+","+Byte.toString(level));

                    successful &= (-1 != dbInTransaction.insert(GRID_TABLE_NAME, null, contentValues));
                }
            }
        } catch (Exception e) {
            successful = false;
            throw e;
        } finally {
            EndTransaction(dbInTransaction, successful);
        }
        return aGrid;
    }

    private int FetchByte(final InputStream is, final Secrets secrets) throws IOException {
        int b;
        if (-1 == (b = is.read())) {
            throw new RuntimeException();
        }

        if (secrets != null) {
            secrets.Append((byte)(b&0xFF));
        }

        return b&0xFF;
    }

    private int FetchInt32(final InputStream is, final Secrets secrets) throws IOException {
        int[] bytes = new int[4];
        byte i;
        for (i=0; i<4; i++) {
            if (-1 == (bytes[i] = is.read())) {
                throw new RuntimeException();
            }

            if (secrets != null) {
                secrets.Append((byte)(bytes[i]&0xFF));
            }
        }

        return ((bytes[0]&0xFF)<<24) | ((bytes[1]&0xFF)<<16) | ((bytes[2]&0xFF)<<8) | (bytes[3]&0xFF);
    }

    private String FetchString(final InputStream is, final int stringLength, final Secrets secrets) throws IOException {
        byte[] bytes = new byte[stringLength];
        if (-1 == is.read(bytes, 0, stringLength)) {
            throw new RuntimeException();
        }

        if (secrets != null) {
            secrets.Append(bytes);
        }

        return new String(bytes, "UTF-8");
    }

    String DumpDB() {
        StringBuilder sb = new StringBuilder();
        Cursor cursor = null;
        try {
            cursor = this.getReadableDatabase()
                    .rawQuery("SELECT "+GRID_COLUMN_KEY+","+GRID_COLUMN_LEVEL+","+GRID_COLUMN_STATUS+","+GRID_COLUMN_OWNER
                                    + " FROM " + GRID_TABLE_NAME, null);
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    sb.append(Integer.toString(cursor.getInt(0)));
                    sb.append(',');
                    sb.append(Integer.toString(cursor.getInt(1)));
                    sb.append(',');
                    sb.append(Integer.toString(cursor.getInt(2)));
                    sb.append(',');
                    sb.append(Integer.toString(cursor.getInt(3)));
                    sb.append('\n');
                    cursor.moveToNext();
                }
            }
            return sb.toString();
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
