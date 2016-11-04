package org.dyndns.gill_roxrud.frodeg.gridwalking.db;

import android.provider.BaseColumns;


public final class BonusContract {
    private BonusContract() {}

    public static class GridEntry implements BaseColumns {
        public static final String TABLE_NAME = "grid";
        public static final String COLUMN_NAME_KEY = "key";
        public static final String COLUMN_NAME_LEVEL = "level";
    }
}
