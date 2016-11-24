package org.dyndns.gill_roxrud.frodeg.gridwalking;


class Bonus {
    static final int HOR_BONUS_COUNT = 5000;             //Less than 2^16
    static final int VER_BONUS_COUNT = HOR_BONUS_COUNT/2; //Less than 2^15
    static final float BONUS_SIZE_RADIUS = 100.0f; //meters

    static final double HALF_HOR_BONUS_DEGREE = (Grid.HOR_DEGREES/HOR_BONUS_COUNT)/2; //Used for rounding
    static final double HALF_VER_BONUS_DEGREE = (Grid.VER_DEGREES/VER_BONUS_COUNT)/2; //Used for rounding


    Bonus() {
    }

    int GetUnusedBonusCount() {
        return GameState.getInstance().getDB().getUnusedBonusCount();
    }

    void ConsumeBonus() {
        GridWalkingDBHelper db = GameState.getInstance().getDB();
        if (db.getUnusedBonusCount() > 0) {
            db.consumeBonus();
        }
    }

    Integer ValidBonusKeyFromPos(final Point<Double> pos) throws InvalidPositionException {
        double horizontal_pos_rounding = pos.getX()+HALF_HOR_BONUS_DEGREE;
        if (Grid.EAST<=horizontal_pos_rounding) {
            horizontal_pos_rounding -= Grid.HOR_DEGREES;
        }

        Point<Integer> p = new Point<>(ToHorizontalBonusGrid(horizontal_pos_rounding), ToVerticalBonusGrid(pos.getY()+HALF_VER_BONUS_DEGREE));
        Point<Double> bonus_pos = new Point<>(FromHorizontalBonusGrid(p.getX()), FromVerticalBonusGrid(p.getY()));

        if (BONUS_SIZE_RADIUS >= CalculateDistance(pos, bonus_pos)) {
            int key = ToBonusKey(p);
            GameState.getInstance().getDB().persistBonus(key);
            return key;
        }
        return null;
    }

    int ToHorizontalBonusGrid(double x_pos) {
        if (Grid.WEST>x_pos) {
            x_pos += Grid.HOR_DEGREES;
        } else if (Grid.EAST<=x_pos) {
            x_pos -= Grid.HOR_DEGREES;
        }

        int value = Double.valueOf(HOR_BONUS_COUNT * ((x_pos-Grid.WEST)/(Grid.HOR_DEGREES))).intValue();
        if (HOR_BONUS_COUNT==value)
            value = HOR_BONUS_COUNT-1;

        return value;
    }

    private int ToHorizontalBonusGridBounded(final double x_pos) {
        if (Grid.WEST>x_pos) {
            return ToHorizontalBonusGridBounded(Grid.WEST);
        } else if (Grid.EAST<x_pos) {
            return ToHorizontalBonusGridBounded(Grid.EAST);
        }

        int value = Double.valueOf(HOR_BONUS_COUNT * ((x_pos-Grid.WEST)/(Grid.HOR_DEGREES))).intValue();
        if (HOR_BONUS_COUNT==value)
            value = HOR_BONUS_COUNT-1;

        return value;
    }

    int ToVerticalBonusGrid(final double y_pos) throws InvalidPositionException {
        if (Grid.GRID_MAX_SOUTH>y_pos || Grid.GRID_MAX_NORTH<=y_pos)
            throw new InvalidPositionException();

        return Double.valueOf(VER_BONUS_COUNT * ((y_pos-Grid.GRID_MAX_SOUTH)/(Grid.VER_GRID_DEGREES))).intValue();
    }

    int ToVerticalBonusGridBounded(final double y_pos) {
        if (Grid.GRID_MAX_SOUTH>y_pos) {
            return ToVerticalBonusGridBounded(Grid.GRID_MAX_SOUTH);
        } else if (Grid.GRID_MAX_NORTH<=y_pos) {
            return ToVerticalBonusGridBounded(Grid.GRID_MAX_NORTH);
        }

        int value = Double.valueOf(VER_BONUS_COUNT * ((y_pos-Grid.GRID_MAX_SOUTH)/(Grid.VER_GRID_DEGREES))).intValue();
        if (VER_BONUS_COUNT==value)
            value = VER_BONUS_COUNT-1;

        return value;
    }

    double FromHorizontalBonusGrid(final int x_grid) {
        return Grid.WEST + ((double)x_grid/(double)HOR_BONUS_COUNT) * (Grid.HOR_DEGREES);
    }

    double FromVerticalBonusGrid(final int y_grid) {
        return Grid.GRID_MAX_SOUTH + ((double)y_grid/(double)VER_BONUS_COUNT) * (Grid.VER_GRID_DEGREES);
    }

    boolean Contains(final int key) {
        return GameState.getInstance().getDB().containsBonus(key);
    }

    int ToBonusKey(final Point<Integer> p) throws InvalidPositionException {
        return ToBonusKey(p.getX(), p.getY());
    }

    int ToBonusKey(final int x, final int y) throws InvalidPositionException {
        if (VER_BONUS_COUNT<=y || HOR_BONUS_COUNT<=x)
            throw new InvalidPositionException();

        return (y<<16) | x;
    }

    Point<Integer> FromBonusKey(final int key) throws InvalidPositionException {
        Point<Integer> p = new Point<>(key&0xFFFF, (key>>16)&0xFFFF);
        if (VER_BONUS_COUNT<=p.getY() || HOR_BONUS_COUNT<=p.getX())
            throw new InvalidPositionException();

        return p;
    }

    /* http://stackoverflow.com/questions/27928/calculate-distance-between-two-latitude-longitude-points-haversine-formula */
    private double CalculateDistance(final Point<Double> p1, final Point<Double> p2) {

        double latDistance = Math.toRadians(p1.getY() - p2.getY());
        double lngDistance = Math.toRadians(p1.getX() - p2.getX());

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(p1.getY())) * Math.cos(Math.toRadians(p2.getY()))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Grid.AVERAGE_RADIUS_OF_EARTH * c;
    }

    String getBonusString() {
        return Integer.toString(GetUnusedBonusCount());
    }
}
