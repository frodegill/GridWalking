package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


public class Grid {
    static final double AVERAGE_RADIUS_OF_EARTH = 6371000; //meters
    static final double AVERAGE_CIRCUMFENCE_OF_EARTH = AVERAGE_RADIUS_OF_EARTH*2*Math.PI; //meters

    static final byte LEVEL_COUNT = 18;
    private static final int HOR_GRID_COUNT = (1<<(LEVEL_COUNT-1))*2; //Less than 2^32. Times two because East and West
    private static final int VER_GRID_COUNT = HOR_GRID_COUNT/2; //Less than 2^31

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

    List<Long> mru_list = new ArrayList();

    static SortedSet<Long> grids[];
    static Object gridsLock = new Object();

    static Paint gridColours[];


    public Grid() {
        if (grids == null) {
            grids = new SortedSet[LEVEL_COUNT];
            for (byte i = 0; i<LEVEL_COUNT; i++) {
                grids[i] = new TreeSet();
            }
        }
        if (gridColours == null) {
            gridColours = new Paint[LEVEL_COUNT];
            for (byte i = 0; i<LEVEL_COUNT; i++) {
                gridColours[i] = new Paint();
            }
            if (0<LEVEL_COUNT)  gridColours[0] .setColor(Color.argb(0x80, 0xFF, 0x69, 0xB4)); //Hot Pink
            if (1<LEVEL_COUNT)  gridColours[1] .setColor(Color.argb(0x80, 0xFF, 0x00, 0xFF)); //Magenta
            if (2<LEVEL_COUNT)  gridColours[2] .setColor(Color.argb(0x80, 0xB0, 0x30, 0x60)); //Maroon
            if (3<LEVEL_COUNT)  gridColours[3] .setColor(Color.argb(0x80, 0xBA, 0x55, 0xD3)); //Medium Orchid
            if (4<LEVEL_COUNT)  gridColours[4] .setColor(Color.argb(0x80, 0xFF, 0xE4, 0xE1)); //Misty Rose
            if (5<LEVEL_COUNT)  gridColours[5] .setColor(Color.argb(0x80, 0xDD, 0xA0, 0xDD)); //Plum
            if (6<LEVEL_COUNT)  gridColours[6] .setColor(Color.argb(0x80, 0x00, 0x80, 0x80)); //Teal
            if (7<LEVEL_COUNT)  gridColours[7] .setColor(Color.argb(0x80, 0xEE, 0x82, 0xEE)); //Violet
            if (8<LEVEL_COUNT)  gridColours[8] .setColor(Color.argb(0x80, 0xFF, 0xDE, 0xAD)); //Navajo White
            if (9<LEVEL_COUNT)  gridColours[9] .setColor(Color.argb(0x80, 0xFF, 0x45, 0x00)); //Orange Red
            if (10<LEVEL_COUNT) gridColours[10].setColor(Color.argb(0x80, 0xFA, 0x80, 0x72)); //Salmon
            if (11<LEVEL_COUNT) gridColours[11].setColor(Color.argb(0x80, 0x00, 0xFF, 0x7F)); //Spring Green
            if (12<LEVEL_COUNT) gridColours[12].setColor(Color.argb(0x80, 0xD2, 0x69, 0x1E)); //Chocolate
            if (13<LEVEL_COUNT) gridColours[13].setColor(Color.argb(0x80, 0x99, 0x32, 0xCC)); //Dark Orchid
            if (14<LEVEL_COUNT) gridColours[14].setColor(Color.argb(0x80, 0x8F, 0xBC, 0x8F)); //Dark Sea Green
            if (15<LEVEL_COUNT) gridColours[15].setColor(Color.argb(0x80, 0xFF, 0x14, 0x93)); //Deep Pink
            if (16<LEVEL_COUNT) gridColours[16].setColor(Color.argb(0x80, 0xB2, 0x22, 0x22)); //Firebrick
            if (17<LEVEL_COUNT) gridColours[17].setColor(Color.argb(0x80, 0xFF, 0xD7, 0x00)); //Gold
        }
    }

    public boolean Discover(final Point<Double> pos) throws InvalidPositionException {
        return DiscoverGrid(new Point(ToHorizontalGrid(pos.getX()), ToVerticalGrid(pos.getY())));
    }

    private boolean DiscoverGrid(final Point<Integer> p) throws InvalidPositionException {
        if (0 != DiscoveredLevel(p)) {
            return false;
        }

        long key = ToKey(p);

        if (IsInMRU(key)) {
            return false;
        }

        synchronized(gridsLock) {
            AddToMRU(key);
            grids[0].add(key);

            RecursiveCheck(p, (byte) 0);
        }

        return true;
    }

    private byte DiscoveredLevel(final Point<Integer> p) throws InvalidPositionException {
        Point<Integer> upperLeft;
        long key;
        byte level;
        synchronized(gridsLock) {
            for (level = LEVEL_COUNT-1; 0 < level; level--) {
                upperLeft = GetUpperLeft(p, level);
                key = ToKey(upperLeft);
                if (grids[level - 1].contains(key)) {
                    break;
                }
            }
        }
        return level;
    }

    private void RecursiveCheck(final Point<Integer> p, final byte level) throws InvalidPositionException {
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX() || LEVEL_COUNT<level)
            throw new InvalidPositionException();

        if (LEVEL_COUNT==level)
            return;

        Rect r = GetBoundingBox(p, level);

        long keys[] = new long[4];
        keys[0] = ToKey(r.getUpperLeft());
        keys[1] = ToKey(r.getUpperRight());
        keys[2] = ToKey(r.getLowerLeft());
        keys[3] = ToKey(r.getLowerRight());

        byte i;
        byte matches = 0;
        synchronized(gridsLock) {
            for (i = 0; 4 > i; i++) {
                if (grids[level].contains(keys[i]))
                    matches++;
            }
            if (3 > matches) //Not enough. Bail out
                return;

            for (i = 0; 4 > i; i++) {
                grids[level].remove(keys[i]);
            }

            grids[level + 1].add(keys[0]);
            RecursiveCheck(r.getUpperLeft(), (byte) (level + 1));
        }
    }

    static int ToHorizontalGrid(double x_pos) {
        if (WEST>x_pos) {
            x_pos += HOR_DEGREES;
        } else if (EAST<=x_pos) {
            x_pos -= HOR_DEGREES;
        }

        int value = new Double(HOR_GRID_COUNT * ((x_pos-WEST)/(HOR_DEGREES))).intValue();
        if (HOR_GRID_COUNT==value)
            value = HOR_GRID_COUNT-1;

        return value;
    }

    static int ToHorizontalGridBounded(final double x_pos) {
        if (Grid.WEST>x_pos) {
            return ToHorizontalGridBounded(Grid.WEST);
        } else if (Grid.EAST<x_pos) {
            return ToHorizontalGridBounded(Grid.EAST);
        }

        int value = new Double(HOR_GRID_COUNT * ((x_pos-WEST)/(HOR_DEGREES))).intValue();
        if (HOR_GRID_COUNT==value)
            value = HOR_GRID_COUNT-1;

        return value;
    }

    static int ToVerticalGrid(final double y_pos) throws InvalidPositionException {
        if (GRID_MAX_SOUTH>y_pos || GRID_MAX_NORTH<=y_pos)
            throw new InvalidPositionException();

        return new Double(VER_GRID_COUNT * ((y_pos-GRID_MAX_SOUTH)/(VER_GRID_DEGREES))).intValue();
    }

    static int ToVerticalGridBounded(final double y_pos) {
        if (Grid.GRID_MAX_SOUTH>y_pos) {
            return ToVerticalGridBounded(Grid.GRID_MAX_SOUTH);
        } else if (Grid.GRID_MAX_NORTH<=y_pos) {
            return ToVerticalGridBounded(Grid.GRID_MAX_NORTH);
        }

        int value = new Double(VER_GRID_COUNT * ((y_pos-GRID_MAX_SOUTH)/(VER_GRID_DEGREES))).intValue();
        if (VER_GRID_COUNT==value)
            value = VER_GRID_COUNT-1;

        return value;
    }

    private double FromHorizontalGrid(final int x_grid) throws InvalidPositionException {
        if (HOR_GRID_COUNT<=x_grid)
            throw new InvalidPositionException();

        return WEST + ((double)x_grid/(double)HOR_GRID_COUNT) * (HOR_DEGREES);
    }

    private double FromVerticalGrid(final int y_grid) throws InvalidPositionException {
        if (VER_GRID_COUNT<=y_grid)
            throw new InvalidPositionException();

        return GRID_MAX_SOUTH + ((double)y_grid/(double)VER_GRID_COUNT) * (VER_GRID_DEGREES);
    }

    private Point<Integer> GetUpperLeft(final Point<Integer> p, final byte level) throws InvalidPositionException {
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX() || LEVEL_COUNT<=level)
            throw new InvalidPositionException();

        int mask = (1<<level) - 1;
        return new Point(p.getX() & ~mask, p.getY() & ~mask);
    }

    private Rect GetBoundingBox(final Point<Integer> p, final byte level) throws InvalidPositionException {
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX() || LEVEL_COUNT<=level)
            throw new InvalidPositionException();

        int mask = (1<<level) - 1;
        Rect<Integer> r = new Rect();
        r.setUpperLeft(new Point(p.getX() & ~mask, p.getY() & ~mask));
        r.setLowerRight(new Point(r.getLeft() + mask, r.getTop() - mask));
        return r;
    }

    private long ToKey(final Point<Integer> p) throws InvalidPositionException {
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX())
            throw new InvalidPositionException();

        long key = (((long)p.getY())<<32) | p.getX();
        return key;
    }

    private Point<Integer> FromKey(final long key) throws InvalidPositionException {
        Point<Integer> p = new Point((int)(key&0xFFFFFFFF), (int)((key>>32)&0xFFFFFFFF));
        if (VER_GRID_COUNT<=p.getY() || HOR_GRID_COUNT<=p.getX())
            throw new InvalidPositionException();

        return p;
    }

    private boolean IsInMRU(final long key)
    {
        return mru_list.contains(key);
    }

    private void AddToMRU(final long key)
    {
        mru_list.add(0, key);
        if (MAX_MRU_COUNT<mru_list.size())
        {
            mru_list.remove(MAX_MRU_COUNT-1);
        }
    }

    static int OsmToGridLevel(int osmZoomLevel) {
        int gridLevel = osmZoomLevel - 15;
        if (0>gridLevel) {
            gridLevel = 0;
        } else if (LEVEL_COUNT<=gridLevel) {
            gridLevel = LEVEL_COUNT-1;
        }
        return gridLevel;
    }
}
