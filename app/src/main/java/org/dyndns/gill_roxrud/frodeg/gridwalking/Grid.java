package org.dyndns.gill_roxrud.frodeg.gridwalking;

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
    private List<Integer> mru_list = new ArrayList<>();

    Paint gridColours[] = null;
    private Paint selectedGridColour = null;


    Grid() {
        if (gridColours == null) {
            gridColours = new Paint[LEVEL_COUNT];
            byte i;
            for (i = 0; i<LEVEL_COUNT; i++) {
                gridColours[i] = new Paint();
            }

            i=0;
/*0*/       if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xFA, 0x80, 0x72)); //Salmon
/*1*/       if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xD2, 0x69, 0x1E)); //Chocolate
/*2*/       if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xFF, 0x69, 0xB4)); //Hot Pink
/*3*/       if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xFF, 0x00, 0xFF)); //Magenta
/*4*/       if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xDD, 0xA0, 0xDD)); //Plum
/*5*/       if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0x8F, 0xBC, 0x8F)); //Dark Sea Green
/*6*/       if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0x00, 0x80, 0x80)); //Teal
/*7*/       if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0x99, 0x32, 0xCC)); //Dark Orchid
/*8*/       if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xEE, 0x82, 0xEE)); //Violet
/*9*/       if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xFF, 0xDE, 0xAD)); //Navajo White
/*10*/      if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xFF, 0x45, 0x00)); //Orange Red
/*11*/      if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0x00, 0xFF, 0x7F)); //Spring Green
/*12*/      if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xFF, 0x14, 0x93)); //Deep Pink
/*13*/      if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xB2, 0x22, 0x22)); //Firebrick
/*14*/      if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xFF, 0xD7, 0x00)); //Gold
/*15*/      if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xB0, 0x30, 0x60)); //Maroon
/*16*/      if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xBA, 0x55, 0xD3)); //Medium Orchid
/*17*/      if (i<LEVEL_COUNT) gridColours[i++].setColor(Color.argb(0x80, 0xFF, 0xE4, 0xE1)); //Misty Rose

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
        try {
            if (-1 == DiscoveredLevel(gridPoint)) {
                Integer selectedGridKey = ToKey(gridPoint);
                if (unselectIfSelected && oldSelectedGridKey!=null && oldSelectedGridKey.intValue()==selectedGridKey.intValue()) {
                    gameState.setSelectedGridKey(null);
                } else {
                    gameState.setSelectedGridKey(selectedGridKey);
                }
            } else {
                gameState.setSelectedGridKey(null);
            }
        } catch (InvalidPositionException e) {
            gameState.setSelectedGridKey(null);
        }

        return !gameState.getSelectedGridKey().equals(oldSelectedGridKey);
    }

    boolean DiscoverSelectedGrid() {
        GameState gameState = GameState.getInstance();
        Integer selectedGridKey = gameState.getSelectedGridKey();
        if (selectedGridKey == null) {
            return false;
        }
        try {
            Bonus bonus = gameState.getBonus();
            if (bonus.GetUnusedBonusCount()>0 &&
                DiscoverGrid(FromKey(selectedGridKey))) {
                gameState.getBonus().ConsumeBonus();
                return true;
            }
        } catch (InvalidPositionException e) {
        }
        return false;
    }

    boolean Discover(final Point<Double> pos) throws InvalidPositionException {
        return DiscoverGrid(new Point<>(ToHorizontalGrid(pos.getX(), LEVEL_0), ToVerticalGrid(pos.getY(), LEVEL_0)));
    }

    private boolean DiscoverGrid(final Point<Integer> p) throws InvalidPositionException {
        int key = ToKey(p);
        if (IsInMRU(key)) {
            return false;
        }

        if (-1 != DiscoveredLevel(p)) {
            return false;
        }

        AddToMRU(key);
        GameState.getInstance().getDB().persistGrid(key, (byte) 0);
        RecursiveCheck(p, (byte) 0);

        Integer selectedGridKey = GameState.getInstance().getSelectedGridKey();
        if (selectedGridKey != null) { //Check if selection should be removed
            SelectGridIfValid(FromKey(selectedGridKey), false);
        }
        return true;
    }

    private byte DiscoveredLevel(final Point<Integer> p) throws InvalidPositionException {
        GridWalkingDBHelper db = GameState.getInstance().getDB();
        Point<Integer> lowerLeft;
        int key;
        byte level;
        for (level = LEVEL_COUNT-1; 0<=level; level--) {
            if (0 == db.getLevelCount(level)) {
                continue;
            }
            lowerLeft = GetLowerLeft(p, level);
            key = ToKey(lowerLeft);
            if (db.containsGrid(key, level)) {
                return level;
            }
        }
        return -1;
    }

    private void RecursiveCheck(final Point<Integer> p, final byte level) throws InvalidPositionException {
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX() || LEVEL_COUNT<level)
            throw new InvalidPositionException();

        if (LEVEL_COUNT==level)
            return;

        Rect<Integer> r = GetBoundingBox(p, (byte)(level+1));

        Set<Integer> keys = new TreeSet<>();
        Integer lowerLeftKey = ToKey(r.getLowerLeft());
        keys.add(lowerLeftKey);
        keys.add(ToKey(r.getLowerRight()));
        keys.add(ToKey(r.getUpperLeft()));
        keys.add(ToKey(r.getUpperRight()));

        GridWalkingDBHelper db = GameState.getInstance().getDB();
        Set<Integer> keyMatches = db.containsGrid(keys, level);
        if (3 > keyMatches.size()) //Not enough. Bail out
            return;

        db.persistGrid(keyMatches, lowerLeftKey, (byte) (level + 1));

        RecursiveCheck(r.getLowerLeft(), (byte) (level + 1));
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

    private Rect<Integer> GetBoundingBox(final Point<Integer> p, final byte level) throws InvalidPositionException {
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX() || LEVEL_COUNT<=level)
            throw new InvalidPositionException();

        int mask = (1<<level) - 1;
        Rect<Integer> r = new Rect<>();
        r.setLeft(p.getX() & ~mask);
        r.setBottom(p.getY() & ~mask);
        r.setRight(r.getLeft() + mask);
        r.setTop(r.getBottom() + mask);
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
            levelCount = db.getLevelCount(i);
            if (levelCount > 0) {
                if (!first) {
                    sb.append(':');
                }
                sb.append(Integer.toString(levelCount));
                score += levelCount<<(2*i); //Each level up represents 4 squares
                first = false;
            }
        }
        sb.append(')');
        return Long.toString(score) + sb.toString();
    }
}
