package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;

import org.osmdroid.api.IGeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


class Grid {
    static final double AVERAGE_RADIUS_OF_EARTH = 6371000; //meters
    static final double AVERAGE_CIRCUMFENCE_OF_EARTH = AVERAGE_RADIUS_OF_EARTH*2*Math.PI; //meters

    static final byte LEVEL_COUNT = 14;
    static final byte LEVEL_0 = 0;
    private static final int HOR_GRID_COUNT = (1<<(LEVEL_COUNT-1))*2; //Less than 2^16. Times two because East and West
    private static final int VER_GRID_COUNT = HOR_GRID_COUNT/2; //Less than 2^15

    static final double NORTH = 90.0; //degrees
    static final double EAST = 180.0; //degrees
    static final double SOUTH = -90.0; //degrees
    static final double WEST = -180.0; //degrees
    static final double VER_DEGREES = NORTH-SOUTH;
    static final double HOR_DEGREES = EAST-WEST;

    static final double GRID_MAX_NORTH = 80.0; //degrees
    static final double GRID_MAX_SOUTH = -80.0; //degrees
    static final double VER_GRID_DEGREES = GRID_MAX_NORTH-GRID_MAX_SOUTH;

    private static final byte MAX_MRU_COUNT = 10;
    private final List<Integer> mru_list = new ArrayList<>();

    Paint gridColours[] = null;
    private Paint selectedGridColour = null;


    Grid() {
        if (gridColours == null) {
            gridColours = new Paint[LEVEL_COUNT];
            byte i;
            for (i = 0; i<LEVEL_COUNT; i++) {
                gridColours[i] = new Paint();
            }

            gridColours[0].setColor(Color.argb(0x80, 0xFA, 0x80, 0x72)); //Salmon
            gridColours[1].setColor(Color.argb(0x80, 0xD2, 0x69, 0x1E)); //Chocolate
            gridColours[2].setColor(Color.argb(0x80, 0xFF, 0x69, 0xB4)); //Hot Pink
            gridColours[3].setColor(Color.argb(0x80, 0xFF, 0x00, 0xFF)); //Magenta
            gridColours[4].setColor(Color.argb(0x80, 0xDD, 0xA0, 0xDD)); //Plum
            gridColours[5].setColor(Color.argb(0x80, 0x8F, 0xBC, 0x8F)); //Dark Sea Green
            gridColours[6].setColor(Color.argb(0x80, 0x00, 0x80, 0x80)); //Teal
            gridColours[7].setColor(Color.argb(0x80, 0x99, 0x32, 0xCC)); //Dark Orchid
            gridColours[8].setColor(Color.argb(0x80, 0xEE, 0x82, 0xEE)); //Violet
            gridColours[9].setColor(Color.argb(0x80, 0xFF, 0xDE, 0xAD)); //Navajo White
            gridColours[10].setColor(Color.argb(0x80, 0xFF, 0x45, 0x00)); //Orange Red
            gridColours[11].setColor(Color.argb(0x80, 0x00, 0xFF, 0x7F)); //Spring Green
            gridColours[12].setColor(Color.argb(0x80, 0xFF, 0x14, 0x93)); //Deep Pink
            gridColours[13].setColor(Color.argb(0x80, 0xFF, 0xD7, 0x00)); //Gold

            selectedGridColour = new Paint();
            selectedGridColour.setColor(Color.argb(0xA0, 0xFF, 0xFF, 0x00));
        }
    }

    Paint getSelectedGridColour() {
        return selectedGridColour;
    }

    boolean SelectGridIfValid(IGeoPoint geoPoint, boolean unselectIfSelected) {
        try {
            return SelectGridIfValid(ToGrid(geoPoint), unselectIfSelected);
        } catch (InvalidPositionException e) {
            GameState gameState = GameState.getInstance();
            Integer oldSelectedGridKey = gameState.getSelectedGridKey();
            gameState.setSelectedGridKey(null);
            return !gameState.getSelectedGridKey().equals(oldSelectedGridKey);
        }
    }

    private boolean SelectGridIfValid(Point<Integer> gridPoint, boolean unselectIfSelected) {
        GameState gameState = GameState.getInstance();
        Integer oldSelectedGridKey = gameState.getSelectedGridKey();
        Integer currentSelectedGridKey = null;
        try {
            if (-1 == DiscoveredLevel(gridPoint, gameState.getShowGridState())) {
                currentSelectedGridKey = ToKey(gridPoint);
                if (unselectIfSelected &&
                    oldSelectedGridKey!=null && oldSelectedGridKey.intValue()==currentSelectedGridKey.intValue()) {
                    currentSelectedGridKey = null;
                }
            }
        } catch (InvalidPositionException e) {
            currentSelectedGridKey = null;
        }

        gameState.setSelectedGridKey(currentSelectedGridKey);

        return ((oldSelectedGridKey==null && currentSelectedGridKey!=null) ||
                (oldSelectedGridKey!=null && currentSelectedGridKey==null) ||
                (oldSelectedGridKey!=null && currentSelectedGridKey!=null && oldSelectedGridKey.intValue()!=currentSelectedGridKey.intValue()));
    }

    boolean DiscoverSelectedGrid() {
        GameState gameState = GameState.getInstance();
        if (GameState.ShowGridState.SELF!=gameState.getShowGridState()) {
            return false;
        }

        Integer selectedGridKey = gameState.getSelectedGridKey();
        if (selectedGridKey == null) {
            return false;
        }
        try {
            Bonus bonus = gameState.getBonus();
            if (bonus.GetUnusedBonusCount() > 0) {
                DiscoverGrid(FromKey(selectedGridKey), true);
                return true;
            }
        } catch (InvalidPositionException e) {
        }
        return false;
    }

    boolean Discover(final Point<Double> pos, final boolean consumeBonus) throws InvalidPositionException {
        return DiscoverGrid(new Point<>(ToHorizontalGrid(pos.getX(), LEVEL_0), ToVerticalGrid(pos.getY(), LEVEL_0)), consumeBonus);
    }

    private boolean DiscoverGrid(final Point<Integer> p, final boolean consumeBonus) throws InvalidPositionException {
        int key = ToKey(p);
        if (IsInMRU(key)) {
            return false;
        }

        if (-1 != DiscoveredLevel(p, GameState.ShowGridState.SELF)) {
            return false;
        }

        AddToMRU(key);

        GridWalkingDBHelper db = GameState.getInstance().getDB();
        SQLiteDatabase dbInTransaction = db.StartTransaction();
        boolean success = false;
        try {
            GameState.getInstance().getDB().PersistGrid(key, (byte) 0, consumeBonus);
            RecursiveCheck(db, dbInTransaction, p, (byte) 0);
            success = true;
        } finally {
            db.EndTransaction(dbInTransaction, success);
        }

        Integer selectedGridKey = GameState.getInstance().getSelectedGridKey();
        if (selectedGridKey != null) { //Check if selection should be removed
            SelectGridIfValid(FromKey(selectedGridKey), false);
        }
        return true;
    }

    private byte DiscoveredLevel(final Point<Integer> p, final GameState.ShowGridState showGridState) throws InvalidPositionException {
        GameState gameState = GameState.getInstance();
        GridWalkingDBHelper db = gameState.getDB();
        Point<Integer> lowerLeft;
        int key;
        byte level;
        for (level = LEVEL_COUNT-1; 0<=level; level--) {
            if (0 == db.GetLevelCount(level)) {
                continue;
            }
            lowerLeft = GetLowerLeft(p, level);
            key = ToKey(lowerLeft);
            if (db.ContainsGrid(key, level, showGridState)) {
                return level;
            }
        }
        return -1;
    }

    private void RecursiveCheck(final GridWalkingDBHelper db, final SQLiteDatabase dbInTransaction, final Point<Integer> p, final byte level) throws InvalidPositionException {
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX() || LEVEL_COUNT<level)
            throw new InvalidPositionException();

        if (LEVEL_COUNT==level)
            return;

        Rect<Integer> r = GetBoundingBoxKeys(p, (byte)(level+1));

        Set<Integer> keys = new TreeSet<>();
        Integer[] gridKeys = new Integer[4];
        gridKeys[0] = ToKey(r.getLowerLeft());
        gridKeys[1] = ToKey(r.getLowerRight());
        gridKeys[2] = ToKey(r.getUpperLeft());
        gridKeys[3] = ToKey(r.getUpperRight());
        int i;
        for (i=0; i<4; i++) {
            keys.add(gridKeys[i]);
        }

        Set<Integer> keyMatches = db.ContainsGrid(keys, level, GameState.ShowGridState.SELF);
        if (3 > keyMatches.size()) { //Not enough. Bail out
            return;
        }

        db.PersistGrid(keyMatches, gridKeys[0], (byte) (level + 1));

        for (i=0; i<4; i++) {
            if (!keyMatches.contains(gridKeys[i])) {
                RecursiveRemoveGrid(db, dbInTransaction, FromKey(gridKeys[i]), level);
            }
        }

        RecursiveCheck(db, dbInTransaction, r.getLowerLeft(), (byte) (level + 1));
    }

    int ToHorizontalGrid(double x_pos, final byte level) {
        if (WEST>x_pos) {
            x_pos += HOR_DEGREES;
        } else if (EAST<=x_pos) {
            x_pos -= HOR_DEGREES;
        }

        int value = Double.valueOf(HOR_GRID_COUNT * ((x_pos-WEST)/(HOR_DEGREES))).intValue();
        if (HOR_GRID_COUNT==value)
            value = HOR_GRID_COUNT-1;

        value &= ~((1<<level) - 1);
        return value;
    }

    private int ToHorizontalGridBounded(final double x_pos, final byte level) {
        if (Grid.WEST>x_pos) {
            return ToHorizontalGridBounded(Grid.WEST,level);
        } else if (Grid.EAST<x_pos) {
            return ToHorizontalGridBounded(Grid.EAST, level);
        }

        int value = Double.valueOf(HOR_GRID_COUNT * ((x_pos-WEST)/(HOR_DEGREES))).intValue();
        if (HOR_GRID_COUNT==value)
            value = HOR_GRID_COUNT-1;

        value &= ~((1<<level) - 1);
        return value;
    }

    private int ToVerticalGrid(final double y_pos, final byte level) throws InvalidPositionException {
        if (GRID_MAX_SOUTH>y_pos || GRID_MAX_NORTH<=y_pos)
            throw new InvalidPositionException();

        int value = Double.valueOf(VER_GRID_COUNT * ((y_pos-GRID_MAX_SOUTH)/(VER_GRID_DEGREES))).intValue();

        value &= ~((1<<level) - 1);
        return value;
    }

    int ToVerticalGridBounded(final double y_pos, final byte level) {
        if (Grid.GRID_MAX_SOUTH>y_pos) {
            return ToVerticalGridBounded(Grid.GRID_MAX_SOUTH, level);
        } else if (Grid.GRID_MAX_NORTH<y_pos) {
            return ToVerticalGridBounded(Grid.GRID_MAX_NORTH, level);
        }

        int value = Double.valueOf(VER_GRID_COUNT * ((y_pos-GRID_MAX_SOUTH)/(VER_GRID_DEGREES))).intValue();
        if (VER_GRID_COUNT<=value)
            value = VER_GRID_COUNT-1;

        value &= ~((1<<level) - 1);
        return value;
    }

    double FromHorizontalGrid(final int x_grid) {
        return WEST + ((double)x_grid/(double)HOR_GRID_COUNT) * (HOR_DEGREES);
    }

    double FromVerticalGrid(final int y_grid) {
        return GRID_MAX_SOUTH + ((double)y_grid/(double)VER_GRID_COUNT) * (VER_GRID_DEGREES);
    }

    private Point<Integer> GetLowerLeft(final Point<Integer> p, final byte level) throws InvalidPositionException {
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX() || LEVEL_COUNT<=level)
            throw new InvalidPositionException();

        int mask = (1<<level) - 1;
        return new Point<>(p.getX() & ~mask, p.getY() & ~mask);
    }

    private Rect<Integer> GetBoundingBoxKeys(final Point<Integer> p, final byte level) throws InvalidPositionException {
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX() || LEVEL_COUNT<=level)
            throw new InvalidPositionException();

        int mask = (1<<level) - 1;
        int offset = 1<<(level-1);
        Rect<Integer> r = new Rect<>();
        r.setLeft(p.getX() & ~mask);
        r.setBottom(p.getY() & ~mask);
        r.setRight(r.getLeft() + offset);
        r.setTop(r.getBottom() + offset);
        return r;
    }

    private int ToKey(final Point<Integer> p) throws InvalidPositionException {
        return ToKey(p.getX(), p.getY());
    }

    int ToKey(final int x, final int y) throws InvalidPositionException {
        if (VER_GRID_COUNT<=y || HOR_GRID_COUNT<=x)
            throw new InvalidPositionException();

        return (y<<16) | x;
    }

    private Point<Integer> FromKey(final int key) throws InvalidPositionException {
        Point<Integer> p = new Point<>(key & 0xFFFF, key>>16);
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX())
            throw new InvalidPositionException();

        return p;
    }

    int XFromKey(final int key) throws InvalidPositionException {
        int x = key & 0xFFFF;
        if (HOR_GRID_COUNT<=x)
            throw new InvalidPositionException();

        return x;
    }

    int YFromKey(final int key) throws InvalidPositionException {
        int y = key>>16;
        if (VER_GRID_COUNT<=y)
            throw new InvalidPositionException();

        return y;
    }

    private Point<Integer> ToGrid(final IGeoPoint geoPoint) throws InvalidPositionException {
        return new Point<>(ToHorizontalGrid(geoPoint.getLongitude(), LEVEL_0), ToVerticalGrid(geoPoint.getLatitude(), LEVEL_0));
    }

    private boolean IsInMRU(final int key)
    {
        return mru_list.contains(key);
    }

    private void AddToMRU(final int key) {
        mru_list.add(0, key);
        if (MAX_MRU_COUNT<mru_list.size()) {
            mru_list.remove(MAX_MRU_COUNT-1);
        }
    }

    byte OsmToGridLevel(final int osmZoomLevel) {
        int gridLevel = LEVEL_COUNT - osmZoomLevel;
        if (0>gridLevel) {
            gridLevel = 0;
        } else if (LEVEL_COUNT<=gridLevel) {
            gridLevel = LEVEL_COUNT-1;
        }
        return (byte)gridLevel;
    }

    String getScoreString() {
        GridWalkingDBHelper db = GameState.getInstance().getDB();
        StringBuilder sb = new StringBuilder();
        long score = 0;
        boolean first = true;
        int levelCount;
        byte i;
        sb.append(" (");
        for (i = LEVEL_COUNT - 1; i >= 0; i--) {
            levelCount = db.GetLevelCount(i);
            if (!(levelCount==0 && first)) {
                if (!first) {
                    sb.append(':');
                }
                sb.append(Integer.toString(levelCount));
                score += (levelCount<<(2*i)) * (i+1); //Each level up represents 4 squares
                first = false;
            }
        }
        sb.append(')');
        return Long.toString(score) + sb.toString();
    }

    private void RecursiveRemoveGrid(final GridWalkingDBHelper db, final SQLiteDatabase dbInTransaction, final Point<Integer> p, final byte level) {
        try {
            int gridKey = ToKey(p);
            if (db.ContainsGrid(gridKey, level, GameState.ShowGridState.SELF)) {
                db.DeleteGrid(dbInTransaction, gridKey, level);

            }

            if (level > 0) {
                Rect<Integer> r = GetBoundingBoxKeys(p, level);
                RecursiveRemoveGrid(db, dbInTransaction, r.getLowerLeft(), (byte) (level-1));
                RecursiveRemoveGrid(db, dbInTransaction, r.getLowerRight(), (byte) (level-1));
                RecursiveRemoveGrid(db, dbInTransaction, r.getUpperLeft(), (byte) (level-1));
                RecursiveRemoveGrid(db, dbInTransaction, r.getUpperRight(), (byte) (level-1));
            }
        } catch (InvalidPositionException e) {
        }
    }

    void BugfixPurgeDuplicates() {
        GridWalkingDBHelper db = GameState.getInstance().getDB();
        SQLiteDatabase dbInTransaction = db.StartTransaction();
        boolean success = false;
        try {
            byte currentLevel;
            for (currentLevel = Grid.LEVEL_COUNT - 1; currentLevel >= 1; currentLevel--) {
                if (db.GetLevelCount(currentLevel) == 0) {
                    continue;
                }

                Set<Integer> levelKeys = db.GetLevelGrids(currentLevel, GameState.ShowGridState.SELF);
                for (Integer levelKey : levelKeys) {
                    try {
                        Rect<Integer> r = GetBoundingBoxKeys(FromKey(levelKey), currentLevel);
                        RecursiveRemoveGrid(db, dbInTransaction, r.getLowerLeft(), (byte) (currentLevel - 1));
                        RecursiveRemoveGrid(db, dbInTransaction, r.getLowerRight(), (byte) (currentLevel - 1));
                        RecursiveRemoveGrid(db, dbInTransaction, r.getUpperLeft(), (byte) (currentLevel - 1));
                        RecursiveRemoveGrid(db, dbInTransaction, r.getUpperRight(), (byte) (currentLevel - 1));
                    } catch (InvalidPositionException e) {
                    }
                }
            }

            db.SetProperty(GridWalkingDBHelper.PROPERTY_BUGFIX_PURGE_DUPLICATES, 0);
            success = true;
        } finally {
            db.EndTransaction(dbInTransaction, success);
        }
    }
}
